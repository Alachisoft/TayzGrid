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

import com.alachisoft.tayzgrid.common.IDisposable;
import  com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import  com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import  com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;


public class WebCacheEnumerator implements Enumeration, IDisposable
{
        private Cache _cache;
        private ArrayList<EnumerationDataChunk> _currentChunks;
        private Iterator<String> _currentChunkEnumerator;
        private String _group;
        private String _serializationContext;
        private String _subGroup;
        private Cache.Entry _de;

    public WebCacheEnumerator(String serializationContext, String group, String subGroup, Cache cache)
    {
        _serializationContext= serializationContext;
        _cache= cache;
        _group = group;
        _subGroup = subGroup;
        _de = _cache.new Entry();

        Initialize(_group, _subGroup);
    }

    /**
     *
     * @param group
     * @param subGroup
     */
    public void Initialize(String group, String subGroup)
    {
        ArrayList<EnumerationPointer> pointers = new ArrayList<EnumerationPointer>();

        if (group != null && !group.equals(""))
        {
            pointers.add(new GroupEnumerationPointer(group, subGroup));
        }
        else
        {
            pointers.add(new EnumerationPointer());
        }

        _currentChunks = _cache.getNextChunk(pointers);
        ArrayList<String> data = new ArrayList<String>();
        for (int i = 0; i < _currentChunks.size(); i++)
        {
           EnumerationDataChunk chunk = _currentChunks.get(i);
            if (chunk != null && chunk.getData() != null)
            {
                data.addAll(chunk.getData());
            }
        }
        _currentChunkEnumerator = data.iterator();
    }

    /// <summary>
    /// Advance the enumerator to the next element of the collection
    /// </summary>
    /// <returns></returns>
    public boolean hasMoreElements()
    {

        boolean result = false;
        if (_currentChunkEnumerator != null)
        {
            result = _currentChunkEnumerator.hasNext();

            if (!result)
            {
                if (_currentChunks != null && !isLastChunk(_currentChunks))
                {
                    _currentChunks = _cache.getNextChunk(getPointerList(_currentChunks));

                    ArrayList<String> data = new ArrayList<String>();
                    for (int i = 0; i < _currentChunks.size(); i++)
                    {
                        if (_currentChunks.get(i) != null && _currentChunks.get(i).getData() != null)
                        {
                            data.addAll(_currentChunks.get(i).getData());
                        }
                    }
                    if (data != null && data.size() > 0)
                    {
                        _currentChunkEnumerator = data.iterator();
                        result = _currentChunkEnumerator.hasNext();
                    }
                }
                else if (_currentChunks != null && _currentChunks.size() > 0)
                {
                    ArrayList<EnumerationPointer> pointers = getPointerList(_currentChunks);
                    if (pointers.size() > 0)
                    {
                        _cache.getNextChunk(pointers); //just an empty call to dispose enumerator for this particular list of pointer
                    }
                }
            }
        }
        return result;
    }

    public Object nextElement()
    {
        Object key = getKey();
        _de.setKey(key);
        _de.setValue(getValue(key));
        return _de;
    }

    /// <summary>
    /// Gets the key of the current dictionary entry
    /// </summary>
    /**
     *
     * @return
     */
    public Object getKey()
    {
        Object key = null;
        if (_currentChunkEnumerator != null)
        {
            key = _currentChunkEnumerator.next();
        }

        return key;
    }

    /// <summary>
    /// Gets the value of the current dictionary entry
    /// </summary>
    /**
     *
     * @param key
     * @return
     */
    public Object getValue(Object key)
    {
        Object value = null;
        try
        {
            value = _cache.get(key);
        }
        catch (Exception ex)
        {
            if (ex.getMessage().startsWith("Connection with server lost"))
            {
                try
                {
                    value =_cache.get(key);
                }
                catch (Exception inner)
                {
                    throw new RuntimeException(inner);
                }
            }
            throw new RuntimeException(ex);
        }
        return value;
    }

    private boolean isLastChunk(List<EnumerationDataChunk> chunks)
    {
        for (int i = 0; i < chunks.size(); i++)
        {
            if (!chunks.get(i).isLastChunk())
            {
                return false;
            }
        }

        return true;
    }

    private ArrayList<EnumerationPointer> getPointerList(ArrayList<EnumerationDataChunk> chunks)
    {
        ArrayList<EnumerationPointer> pointers = new ArrayList<EnumerationPointer>();
        for (int i = 0; i < chunks.size(); i++)
        {
            if (!chunks.get(i).isLastChunk())
            {
                pointers.add(chunks.get(i).getPointer());
            }
        }
        return pointers;
    }

    private ArrayList<EnumerationDataChunk> getChunk(ArrayList<EnumerationPointer> pointer)
    {
        ArrayList<EnumerationDataChunk> chunks = null;

        try
        {
            chunks = _cache.getNextChunk(pointer);
        }
        catch (Exception ex)
        {
            //this is a empty call just to dispose the enumeration pointers for this particular enumerator
            //on all the nodes.
            for (int i = 0; i < pointer.size(); i++)
            {
                pointer.get(i).setDisposable(true);
            }
            try
            {
                _cache.getNextChunk(pointer);
            }
            catch (Exception exc)
            {
            }

        }

        return chunks;
    }

    @Override
    public void dispose()
    {
       if (_cache != null && _currentChunks != null)
            {
                ArrayList<EnumerationPointer> pointerlist = getPointerList(_currentChunks);
                if (pointerlist.size() > 0)
                {
                    _cache.getNextChunk(pointerlist); //just an empty call to dispose enumerator for this particular pointer
                }
               
            }
        _serializationContext= null;
        _cache= null;
        _group = null;
        _subGroup = null;
    }
}
