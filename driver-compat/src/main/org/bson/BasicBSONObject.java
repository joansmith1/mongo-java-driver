/*
 * Copyright (c) 2008 - 2012 10gen, Inc. <http://10gen.com>
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

package org.bson;

import org.bson.types.ObjectId;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A simple implementation of <code>DBObject</code>. A <code>DBObject</code> can be created as follows, using this
 * class:
 * <blockquote><pre>
 * DBObject obj = new BasicBSONObject();
 * obj.put( "foo", "bar" );
 * </pre></blockquote>
 */
@SuppressWarnings({ "rawtypes" })
public class BasicBSONObject extends LinkedHashMap<String, Object> implements BSONObject {

    private static final long serialVersionUID = -4415279469780082174L;

    /**
     * Creates an empty object.
     */
    public BasicBSONObject() {
    }

    public BasicBSONObject(final int size) {
        super(size);
    }

    /**
     * Convenience CTOR
     *
     * @param key   key under which to store
     * @param value value to stor
     */
    public BasicBSONObject(final String key, final Object value) {
        put(key, value);
    }

    /**
     * Creates a DBObject from a map.
     *
     * @param m map to convert
     */
    @SuppressWarnings("unchecked")
    public BasicBSONObject(final Map m) {
        super(m);
    }

    /**
     * Converts a DBObject to a map.
     *
     * @return the DBObject
     */
    public Map toMap() {
        return new LinkedHashMap<String, Object>(this);
    }

    /**
     * Deletes a field from this object.
     *
     * @param key the field name to remove
     * @return the object removed
     */
    public Object removeField(final String key) {
        return remove(key);
    }

    /**
     * Checks if this object contains a given field
     *
     * @param field field name
     * @return if the field exists
     */
    public boolean containsField(final String field) {
        return super.containsKey(field);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean containsKey(final String key) {
        return containsField(key);
    }

    /**
     * Gets a value from this object
     *
     * @param key field name
     * @return the value
     */
    public Object get(final String key) {
        return super.get(key);
    }

    /**
     * Returns the value of a field as an <code>int</code>.
     *
     * @param key the field to look for
     * @return the field value (or default)
     */
    public int getInt(final String key) {
        final Object o = get(key);
        if (o == null) {
            throw new NullPointerException("no value for: " + key);
        }

        return toInt(o);
    }

    /**
     * Returns the value of a field as an <code>int</code>.
     *
     * @param key the field to look for
     * @param def the default to return
     * @return the field value (or default)
     */
    public int getInt(final String key, final int def) {
        final Object foo = get(key);
        if (foo == null) {
            return def;
        }

        return toInt(foo);
    }

    /**
     * Returns the value of a field as a <code>long</code>.
     *
     * @param key the field to return
     * @return the field value
     */
    public long getLong(final String key) {
        final Object foo = get(key);
        return ((Number) foo).longValue();
    }

    /**
     * Returns the value of a field as an <code>long</code>.
     *
     * @param key the field to look for
     * @param def the default to return
     * @return the field value (or default)
     */
    public long getLong(final String key, final long def) {
        final Object foo = get(key);
        if (foo == null) {
            return def;
        }

        return ((Number) foo).longValue();
    }

    /**
     * Returns the value of a field as a <code>double</code>.
     *
     * @param key the field to return
     * @return the field value
     */
    public double getDouble(final String key) {
        final Object foo = get(key);
        return ((Number) foo).doubleValue();
    }

    /**
     * Returns the value of a field as an <code>double</code>.
     *
     * @param key the field to look for
     * @param def the default to return
     * @return the field value (or default)
     */
    public double getDouble(final String key, final double def) {
        final Object foo = get(key);
        if (foo == null) {
            return def;
        }

        return ((Number) foo).doubleValue();
    }

    /**
     * Returns the value of a field as a string
     *
     * @param key the field to look up
     * @return the value of the field, converted to a string
     */
    public String getString(final String key) {
        final Object foo = get(key);
        if (foo == null) {
            return null;
        }
        return foo.toString();
    }

    /**
     * Returns the value of a field as a string
     *
     * @param key the field to look up
     * @param def the default to return
     * @return the value of the field, converted to a string
     */
    public String getString(final String key, final String def) {
        final Object foo = get(key);
        if (foo == null) {
            return def;
        }

        return foo.toString();
    }

    /**
     * Returns the value of a field as a boolean.
     *
     * @param key the field to look up
     * @return the value of the field, or false if field does not exist
     */
    public boolean getBoolean(final String key) {
        return getBoolean(key, false);
    }

    /**
     * Returns the value of a field as a boolean
     *
     * @param key the field to look up
     * @param def the default value in case the field is not found
     * @return the value of the field, converted to a string
     */
    public boolean getBoolean(final String key, final boolean def) {
        final Object foo = get(key);
        if (foo == null) {
            return def;
        }
        if (foo instanceof Number) {
            return ((Number) foo).intValue() > 0;
        }
        if (foo instanceof Boolean) {
            return (Boolean) foo;
        }
        throw new IllegalArgumentException("can't coerce to bool:" + foo.getClass());
    }

    /**
     * Returns the object id or null if not set.
     *
     * @param field The field to return
     * @return The field object value or null if not found (or if null :-^).
     */
    public ObjectId getObjectId(final String field) {
        return (ObjectId) get(field);
    }

    /**
     * Returns the object id or def if not set.
     *
     * @param field The field to return
     * @param def   the default value in case the field is not found
     * @return The field object value or def if not set.
     */
    public ObjectId getObjectId(final String field, final ObjectId def) {
        final Object foo = get(field);
        return (foo != null) ? (ObjectId) foo : def;
    }

    /**
     * Returns the date or null if not set.
     *
     * @param field The field to return
     * @return The field object value or null if not found.
     */
    public Date getDate(final String field) {
        return (Date) get(field);
    }

    /**
     * Returns the date or def if not set.
     *
     * @param field The field to return
     * @param def   the default value in case the field is not found
     * @return The field object value or def if not set.
     */
    public Date getDate(final String field, final Date def) {
        final Object foo = get(field);
        return (foo != null) ? (Date) foo : def;
    }

    /**
     * Add a key/value pair to this object
     *
     * @param key the field name
     * @param val the field value
     * @return the <code>val</code> parameter
     */
    public Object put(final String key, final Object val) {
        return super.put(key, val);
    }

    @SuppressWarnings("unchecked")
    public void putAll(final Map m) {
        for (final Map.Entry entry : (Set<Map.Entry>) m.entrySet()) {
            put(entry.getKey().toString(), entry.getValue());
        }
    }

    public void putAll(final BSONObject o) {
        for (final String k : o.keySet()) {
            put(k, o.get(k));
        }
    }

    /**
     * Add a key/value pair to this object
     *
     * @param key the field name
     * @param val the field value
     * @return <code>this</code>
     */
    public BasicBSONObject append(final String key, final Object val) {
        put(key, val);

        return this;
    }

    //CHECKSTYLE:OFF
    public boolean equals(final Object o) {
        if (!(o instanceof BSONObject)) {
            return false;
        }

        final BSONObject other = (BSONObject) o;
        if (!keySet().equals(other.keySet())) {
            return false;
        }

        for (final String key : keySet()) {
            final Object a = get(key);
            final Object b = other.get(key);

            if (a == null) {
                if (b != null) {
                    return false;
                }
            }
            if (b == null) {
                if (a != null) {
                    return false;
                }
            }
            else if (a instanceof Number && b instanceof Number) {
                if (((Number) a).doubleValue() != ((Number) b).doubleValue()) {
                    return false;
                }
            }
            else if (a instanceof Pattern && b instanceof Pattern) {
                final Pattern p1 = (Pattern) a;
                final Pattern p2 = (Pattern) b;
                if (!p1.pattern().equals(p2.pattern()) || p1.flags() != p2.flags()) {
                    return false;
                }
            }
            else {
                if (!a.equals(b)) {
                    return false;
                }
            }
        }
        return true;
    }
    //CHECKSTYLE:ON

    private int toInt(final Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }

        if (o instanceof Boolean) {
            return ((Boolean) o) ? 1 : 0;
        }

        throw new IllegalArgumentException("can't convert: " + o.getClass().getName() + " to int");
    }
}