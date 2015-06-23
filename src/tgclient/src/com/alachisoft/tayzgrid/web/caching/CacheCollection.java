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

package com.alachisoft.tayzgrid.web.caching;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

//~--- classes ----------------------------------------------------------------

/**
 * List of all the caches.
 */
public class CacheCollection implements Iterable<Cache> {
    private HashMap caches = null;

    //~--- constructors -------------------------------------------------------

    /**
     * Default Constructor
     */
    public CacheCollection() {
        // should be initilized with int values which must be power of 2
        caches= new HashMap(8);
    }

    //~--- methods ------------------------------------------------------------

    /**
     *
     * @param cacheId Name of the cache
     * @param c Cache instance
     */
    public void addCache(String cacheId, Cache c) {
        caches.put(cacheId.toLowerCase(), c);
    }

    /**
     *
     * @param cacheId Name of cache
     * @return true if it exists
     */
    public boolean contains(String cacheId) {
        return caches.containsKey(cacheId.toLowerCase());
    }

    /**
     *
     * @param cacheId Name of the cache
     * @return Cache instance
     */
    public Cache removeCache(String cacheId) {
        return (Cache) caches.remove(cacheId.toLowerCase());
    }

    /**
     *
     * @return Number of caches
     */
    public int size() {
        return caches.size();
    }

    /**
     * Returns an enumeration of the components of this Collection. The
     * returned <tt>Enumeration</tt> object will generate all items in
     * this vector. The first item generated is the item at index <tt>0</tt>,
     * then the item at index <tt>1</tt>, and so on.
     *
     * @return  an enumeration of the components of this vector.
     * @see     Enumeration
     * @see     Iterator
     */
    public Collection values() {
        return caches.values();
    }

    //~--- get methods --------------------------------------------------------

    /**
     *
     * @param cacheId Name of cache
     * @return Cache instance
     */
    public Cache getCache(String cacheId) {
        return (Cache)caches.get(cacheId.toLowerCase());
    }

    public Iterator<Cache> iterator() {
        return new CacheIterator();
    }

    private class CacheIterator implements Iterator<Cache> {

        private HashMap _caches = (HashMap) caches.clone();
        Iterator keys = _caches.keySet().iterator();
        public boolean hasNext() {
            return keys.hasNext();
        }

        public Cache next() {
            return (Cache)_caches.get((Object)keys.next());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }



    }
}


//~ Formatted by Jindent --- http://www.jindent.com
