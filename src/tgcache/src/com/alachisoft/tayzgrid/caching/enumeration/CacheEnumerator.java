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

package com.alachisoft.tayzgrid.caching.enumeration;

import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Iterator;

public class CacheEnumerator implements Iterator
{

    private Cache _cache;
    private EnumerationDataChunk _currentChunk;
    private java.util.Iterator<String> _currentChunkEnumerator;
    private CompressedValueEntry _currentValue;
    private String _serializationContext;
    private String _group;
    private String _subGroup;
    private Map.Entry _de;

    public CacheEnumerator(String serializationContext, String group, String subGroup, Cache cache) throws OperationFailedException
    {
        _cache = cache;
        _serializationContext = serializationContext;
        _group = group;
        _subGroup = subGroup;

        Initialize(_group, _subGroup);
    }

    public final void Initialize(String group, String subGroup) throws OperationFailedException
    {
        EnumerationPointer pointer = null;

        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(group))
        {
            pointer = new GroupEnumerationPointer(group, subGroup);
        }
        else
        {
            pointer = new EnumerationPointer();
        }

        _currentChunk = _cache.GetNextChunk(pointer, new OperationContext());

        if (_currentChunk != null && _currentChunk.getData() != null)
        {
            java.util.ArrayList<String> data = _currentChunk.getData();
            _currentChunkEnumerator = data.iterator();
        }
    }


    /**
     * Set the enumerator to its initial position. which is before the first element in the collection
     * @throws OperationFailedException if Unable to reset
     */
    public final void Reset() throws OperationFailedException
    {
        if (_currentChunk != null)
        {
            _currentChunk.getPointer().Reset();
        }

        if (_currentChunkEnumerator != null)
        {
            this.Initialize(_group, _subGroup);
        }
    }

    /**
     * Advance the enumerator to the next element of the collection
     *
     * @return
     * @throws OperationFailedException
     */
    public final boolean MoveNext() throws OperationFailedException
    {
        boolean result = false;

        if (_currentChunkEnumerator != null)
        {
            result = _currentChunkEnumerator.hasNext();

            if (!result)
            {
                if (_currentChunk != null && !_currentChunk.isLastChunk())
                {
                    _currentChunk = _cache.GetNextChunk(_currentChunk.getPointer(), new OperationContext());

                    if (_currentChunk != null && _currentChunk.getData() != null)
                    {
                        _currentChunkEnumerator = _currentChunk.getData().iterator();
                        result = _currentChunkEnumerator.hasNext();
                    }
                }
            }
        }

        if (result)
        {
            _currentValue = _cache.Get(getKey());
        }

        return result;
    }

    /**
     * Gets the current element in the collection
     */
    public final Object getCurrent()
    {
        return getEntry();
    }



    /**
     * Gets the key and value of the current dictionary entry.
     */
    public final Map.Entry getEntry()
    {
        AbstractMap.SimpleEntry sEntry = new AbstractMap.SimpleEntry(getKey(), getValue());
        _de  = (Map.Entry)sEntry;

        return _de;
    }

    /**
     * Gets the key of the current dictionary entry
     */
    public final Object getKey()
    {
        Object key = null;

        if (_currentChunkEnumerator != null)
        {
            key = _currentChunkEnumerator.next();
        }

        return key;
    }

    /**
     * Gets the value of the current dictionary entry
     */
    public final Object getValue()
    {
        return _currentValue;
    }

    @Override
    public boolean hasNext()
    {
       boolean result = false;

        if (_currentChunkEnumerator != null)
             result = _currentChunkEnumerator.hasNext();

        return result;
    }

    @Override
    public Object next()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
