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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Provides Enumerator over Cache. This has to be marshall by ref so that it can be used outside the service as well.
 */
public class CacheEnumerator /*extends MarshalByRefObject*/ implements ResetableIterator
{

    /**
     * Enumerator over Keys
     */
    private ResetableIterator _enumerator;
    /**
     * Dictionary Entry
     */
    private Map.Entry _de;
    /**
     * Cache name used for deserialization.
     */
    private String _cacheContext;

    /**
     * Constructs CacheStoreBase Enumerator      *
     * @param c
     * @param etr
     */
    public CacheEnumerator(String cacheContext, ResetableIterator enumerator)
    {
        _enumerator = enumerator;
        _cacheContext = cacheContext;
    }

    /**
     * Set the enumerator to its initial position. which is before the first element in the collection
     */
    public void reset()
    {
        _enumerator.reset();
    }

    /**
     * Advance the enumerator to the next element of the collection
     *
     * @return
     */
    private boolean MoveNext()
    {
        if (_enumerator.hasNext())
        {
            _de = new AbstractMap.SimpleEntry(_enumerator.next(), null);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Gets the current element in the collection
     */
    private Object getCurrent()
    {
        if (_de.getValue() == null)
        {
            _de.setValue(FetchObject());
        }
        return _de;
    }

    /**
     * Gets the key and value of the current dictionary entry.
     */
    private Map.Entry getEntry()
    {
        if (_de.getValue() == null)
        {
            _de.setValue(FetchObject());
        }
        return _de;
    }

    /**
     * Gets the key of the current dictionary entry
     */
    private Object getKey()
    {
        return _de.getKey();
    }

    /**
     * Gets the value of the current dictionary entry
     */
    private Object getValue()
    {
        if (_de.getValue() == null)
        {
            _de.setValue(FetchObject());
        }
        return _de.getValue();
    }

    /**
     * Does the lazy loading of object.      *
     * @return
     */
    protected final Object FetchObject()
    {
        Map.Entry pair = (Map.Entry)_enumerator.next();
        CacheEntry e = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
        return e;
    }

    public boolean hasNext()
    {
        return _enumerator.hasNext();
    }

    @Override
    public Object next()
    {
        return _enumerator.next();
    }

    @Override
    public void remove()
    {
        _enumerator.remove();
    }
}
