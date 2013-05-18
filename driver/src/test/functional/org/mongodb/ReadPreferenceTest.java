/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb;

import org.junit.Before;
import org.junit.Test;
import org.mongodb.connection.ClusterDescription;
import org.mongodb.connection.ServerAddress;
import org.mongodb.connection.ServerDescription;
import org.mongodb.connection.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReadPreferenceTest {
    private static final int FOUR_MEG = 4 * 1024 * 1024;
    private static final String HOST = "localhost";

    private ServerDescription primary, secondary, otherSecondary;
    private ClusterDescription set;
    private ClusterDescription setNoSecondary;
    private ClusterDescription setNoPrimary;

    @Before
    public void setUp() throws IOException {
        final Set<Tag> tagSet1 = new HashSet<Tag>();
        tagSet1.add(new Tag("foo", "1"));
        tagSet1.add(new Tag("bar", "2"));
        tagSet1.add(new Tag("baz", "1"));

        final Set<Tag> tagSet2 = new HashSet<Tag>();
        tagSet2.add(new Tag("foo", "1"));
        tagSet2.add(new Tag("bar", "2"));
        tagSet2.add(new Tag("baz", "2"));

        final Set<Tag> tagSet3 = new HashSet<Tag>();
        tagSet3.add(new Tag("foo", "1"));
        tagSet3.add(new Tag("bar", "2"));
        tagSet3.add(new Tag("baz", "3"));

        final long acceptableLatencyMS = 15;
        final long bestPingTime = 50;
        final long acceptablePingTime = bestPingTime + (acceptableLatencyMS / 2);
        final long unacceptablePingTime = bestPingTime + acceptableLatencyMS + 1;

        primary = ServerDescription.builder().address(new ServerAddress(HOST, 27017))
                .averagePingTime(acceptablePingTime * 1000000L)
                .ok(true)
                .primary(true)
                .secondary(false)
                .tags(tagSet1)
                .maxDocumentSize(FOUR_MEG).build();

        secondary = ServerDescription.builder().address(new ServerAddress(HOST, 27018))
                .averagePingTime(bestPingTime * 1000000L)
                .ok(true)
                .primary(false)
                .secondary(true).tags(tagSet2)
                .maxDocumentSize(FOUR_MEG).build();

        otherSecondary = ServerDescription.builder().address(new ServerAddress(HOST, 27019))
                .averagePingTime(unacceptablePingTime * 1000000L)
                .ok(true)
                .primary(false)
                .secondary(true)
                .tags(tagSet3)
                .maxDocumentSize(FOUR_MEG)
                .build();

        final List<ServerDescription> nodeList = new ArrayList<ServerDescription>();
        nodeList.add(primary);
        nodeList.add(secondary);
        nodeList.add(otherSecondary);

        set = new ClusterDescription(nodeList, (new Random()), (int) acceptableLatencyMS);
        setNoPrimary = new ClusterDescription(Arrays.asList(secondary, otherSecondary), (new Random()),
                (int) acceptableLatencyMS);
        setNoSecondary = new ClusterDescription(Arrays.asList(primary), (new Random()), (int) acceptableLatencyMS);
    }


    @Test
    public void testStaticPreferences() {
        assertEquals(new Document("mode", "primary"), ReadPreference.primary().toDocument());
        assertEquals(new Document("mode", "secondary"), ReadPreference.secondary().toDocument());
        assertEquals(new Document("mode", "secondaryPreferred"), ReadPreference.secondaryPreferred().toDocument());
        assertEquals(new Document("mode", "primaryPreferred"), ReadPreference.primaryPreferred().toDocument());
        assertEquals(new Document("mode", "nearest"), ReadPreference.nearest().toDocument());
    }

    @Test
    public void testPrimaryReadPreference() {
        assertEquals(primary, ReadPreference.primary().choose(set));
        assertNull(ReadPreference.primary().choose(setNoPrimary));
    }

    @Test
    public void testSecondaryReadPreference() {
        assertTrue(ReadPreference.secondary().toString().startsWith("secondary"));

        ServerDescription candidate = ReadPreference.secondary().choose(set);
        assertTrue(!candidate.isPrimary());

        candidate = ReadPreference.secondary().choose(setNoSecondary);
        assertNull(candidate);

        // Test secondary mode, with tags
        ReadPreference pref = ReadPreference.secondary(new Document("foo", "1"), new Document("bar", "2"));
        assertTrue(pref.toString().startsWith("secondary"));

        candidate = ReadPreference.secondary().choose(set);
        assertTrue((candidate.equals(secondary) || candidate.equals(otherSecondary)) && !candidate.equals(primary));

        pref = ReadPreference.secondary(new Document("baz", "1"));
        assertTrue(pref.choose(set) == null);

        pref = ReadPreference.secondary(new Document("baz", "2"));
        assertTrue(pref.choose(set).equals(secondary));

        pref = ReadPreference.secondary(new Document("madeup", "1"));
        assertEquals(new Document("mode", "secondary").append("tags", Arrays.asList(new Document("madeup", "1"))),
                pref.toDocument());
        assertTrue(pref.choose(set) == null);
    }

    @Test
    public void testPrimaryPreferredMode() {
        ReadPreference pref = ReadPreference.primaryPreferred();
        final ServerDescription candidate = pref.choose(set);
        assertEquals(primary, candidate);

        assertNotNull(ReadPreference.primaryPreferred().choose(setNoPrimary));

        pref = ReadPreference.primaryPreferred(new Document("baz", "2"));
        assertTrue(pref.choose(set).equals(primary));
        assertTrue(pref.choose(setNoPrimary).equals(secondary));
    }

    @Test
    public void testSecondaryPreferredMode() {
        ReadPreference pref = ReadPreference.secondary(new Document("baz", "2"));
        assertTrue(pref.choose(set).equals(secondary));

        // test that the primary is returned if no secondaries match the tag
        pref = ReadPreference.secondaryPreferred(new Document("madeup", "1"));
        assertTrue(pref.choose(set).equals(primary));

        pref = ReadPreference.secondaryPreferred();
        final ServerDescription candidate = pref.choose(set);
        assertTrue((candidate.equals(secondary) || candidate.equals(otherSecondary)) && !candidate.equals(primary));

        assertEquals(primary, ReadPreference.secondaryPreferred().choose(setNoSecondary));
    }

    @Test
    public void testNearestMode() {
        ReadPreference pref = ReadPreference.nearest();
        assertTrue(pref.choose(set) != null);

        pref = ReadPreference.nearest(new Document("baz", "1"));
        assertTrue(pref.choose(set).equals(primary));

        pref = ReadPreference.nearest(new Document("baz", "2"));
        assertTrue(pref.choose(set).equals(secondary));

        pref = ReadPreference.nearest(new Document("madeup", "1"));
        assertEquals(new Document("mode", "nearest").append("tags", Arrays.asList(new Document("madeup", "1"))),
                pref.toDocument());
        assertTrue(pref.choose(set) == null);
    }

    @Test
    public void testValueOf() {
        assertEquals(ReadPreference.primary(), ReadPreference.valueOf("primary"));
        assertEquals(ReadPreference.secondary(), ReadPreference.valueOf("secondary"));
        assertEquals(ReadPreference.primaryPreferred(), ReadPreference.valueOf("primaryPreferred"));
        assertEquals(ReadPreference.secondaryPreferred(), ReadPreference.valueOf("secondaryPreferred"));
        assertEquals(ReadPreference.nearest(), ReadPreference.valueOf("nearest"));

        final Document first = new Document("dy", "ny");
        final Document remaining = new Document();
        assertEquals(ReadPreference.secondary(first, remaining), ReadPreference.valueOf("secondary", first, remaining));
        assertEquals(ReadPreference.primaryPreferred(first, remaining),
                ReadPreference.valueOf("primaryPreferred", first, remaining));
        assertEquals(ReadPreference.secondaryPreferred(first, remaining),
                ReadPreference.valueOf("secondaryPreferred", first, remaining));
        assertEquals(ReadPreference.nearest(first, remaining), ReadPreference.valueOf("nearest", first, remaining));
    }

    @Test
    public void testGetName() {
        assertEquals("primary", ReadPreference.primary().getName());
        assertEquals("secondary", ReadPreference.secondary().getName());
        assertEquals("primaryPreferred", ReadPreference.primaryPreferred().getName());
        assertEquals("secondaryPreferred", ReadPreference.secondaryPreferred().getName());
        assertEquals("nearest", ReadPreference.nearest().getName());

        final Document first = new Document("dy", "ny");
        final Document remaining = new Document();
        assertEquals(ReadPreference.secondary(first, remaining), ReadPreference.valueOf("secondary", first, remaining));
        assertEquals(ReadPreference.primaryPreferred(first, remaining),
                ReadPreference.valueOf("primaryPreferred", first, remaining));
        assertEquals(ReadPreference.secondaryPreferred(first, remaining),
                ReadPreference.valueOf("secondaryPreferred", first, remaining));
        assertEquals(ReadPreference.nearest(first, remaining), ReadPreference.valueOf("nearest", first, remaining));
    }
}