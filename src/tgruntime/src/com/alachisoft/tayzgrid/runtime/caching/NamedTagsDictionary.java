/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.runtime.caching;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class NamedTagsDictionary {

    private HashMap namedTags;


    public NamedTagsDictionary()
    {
        this.namedTags = new HashMap();
    }

    /**
     * Add integer value against key
     * @param key to refer element
     * @param value integer to be added in tag dictionary
     */

    public void add(String key, int value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add long value against key
     * @param key to refer element
     * @param value long to be added in tag dictionary
     */

    public void add(String key, long value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add String value against key
     * @param key to refer element
     * @param value float to be added in tag dictionary
     */
    public void add(String key, float value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add double value against key
     * @param key to refer element
     * @param value double to be added in tag dictionary
     */

    public void add(String key, double value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add String value against key
     * @param key to refer element
     * @param value String to be added in tag dictionary
     * @throws ArgumentNullException is thrown if value is null
     */

    public void add(String key, String value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");
        if(value == null)
            throw new ArgumentNullException("Value cannot be null.\r\nParameter name: value");

        this.namedTags.put(key, value);
    }

    /**
     * Add String value against key
     * @param key to refer element
     * @param value char to be added in tag dictionary
     */

    public void add(String key, char value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add String value against key
     * @param key to refer element
     * @param value boolean to be added in tag dictionary
     */


    public void add(String key, boolean value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");

        this.namedTags.put(key, value);
    }

    /**
     * Add String value against key
     * @param key to refer element
     * @param value Date to be added in tag dictionary
     */


    public void add(String key, Date value) throws ArgumentNullException, ArgumentException
    {
        if(key==null)
            throw new ArgumentNullException("Key cannot be null.\r\nParameter name: key");
        if(this.namedTags.containsKey(key))
            throw new ArgumentException("Key already exists.");
        if(value==null)
            throw new ArgumentNullException("Value cannot be null.\r\nParameter name: value");

        this.namedTags.put(key, value);
    }

    /**
     * remove value against the key from tags dictionary
     * @param key to be removed
     */

    public void remove(String key)
    {
        this.namedTags.remove(key);
    }

    /**
     * @return total number of elements in tags dictionary
     */

    public int getCount()
    {
        return this.namedTags.size();
    }

    /**
     * @return iterator of tags dictionary entries
     */

    public Iterator getIterator()
    {
        return this.namedTags.entrySet().iterator();
    }

    /**
     * @return iterator of tags dictionary keys
     */

    public Iterator getKeysIterator()
    {
        return this.namedTags.keySet().iterator();
    }

    /**
     * @return true if key exists otherwise false
     * @param key to be verified
     */

    public boolean contains(String key)
    {
        return this.namedTags.containsKey(key);
    }

    public Object getValue(String key)
    {
        return this.namedTags.get(key);
    }

}
