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

package com.alachisoft.tayzgrid.caching.evictionpolicies;

import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import java.util.HashMap;
import java.util.Iterator;

public class EvictionIndex implements ISizableIndex
{

    private java.util.HashMap _index = new java.util.HashMap();
    private long _head = -1; //min
    private long _tail = -1; //max
    private Object _syncLock = new Object();
    
    private long _evictionIndexEntriesSize = 0;
    private long _evictionIndexMaxCount = 0;
    private int _keyCount;
    
    public int getKeysCount()
    {
        return _keyCount;
    }
    public void setKeysCount(int value)
    {
        _keyCount = value;
    }

    public final Object getSyncRoot()
    {
        return _syncLock;
    }

    public final boolean Contains(long key, Object value)
    {
        if (_index.containsKey(key))
        {
            EvictionIndexEntry indexEntry = (EvictionIndexEntry) ((_index.get(key) instanceof EvictionIndexEntry) ? _index.get(key) : null);
            return indexEntry.Contains(value);
        }
        return false;
    }

    public final void Add(long key, Object value)
    {
        if (_index.isEmpty())
        {
            _head = key;
        }
        
        int addSize = 0, removeSize = 0;
        boolean incrementKeyCount = true;

        if (_index.containsKey(key))
        {
            EvictionIndexEntry indexEntry = (EvictionIndexEntry) _index.get(key);
            if (indexEntry != null)
            {
                removeSize = indexEntry.getInMemorySize();
                
                if(indexEntry.Contains(value))
                    incrementKeyCount = false;
                
                indexEntry.Insert(value);
                
                addSize = indexEntry.getInMemorySize();
            }
        }
        else
        {
            EvictionIndexEntry indexEntry = new EvictionIndexEntry();
            indexEntry.Insert(value);
            
            addSize = indexEntry.getInMemorySize();
            
            _index.put(key, indexEntry);

            if(_index.size() > _evictionIndexMaxCount)
                _evictionIndexMaxCount = _index.size();
            
            EvictionIndexEntry prevEntry = (EvictionIndexEntry) ((_index.get(_tail) instanceof EvictionIndexEntry) ? _index.get(_tail) : null);

            if (prevEntry != null)
            {
                prevEntry.setNext(key);
            }
            indexEntry.setPrevious(_tail);

        }
        if (key > _tail)
        {
            _tail = key;
        }
        
        _evictionIndexEntriesSize -= removeSize;
        _evictionIndexEntriesSize += addSize;
        
        if(incrementKeyCount)
            _keyCount++;
    }

    /**
     * insert at the begining...
     *
     * @param key
     * @param value
     */
    public final void Insert(long key, Object value)
    {
        Insert(key, value, -1, _head);
    }

    /**
     * Add method only adds the new node at the tail... Insert method can add the new nodes in between also....
     *
     * @param key
     * @param value
     */
    public final void Insert(long key, Object value, long previous, long next)
    {
        EvictionIndexEntry nextEntry = (EvictionIndexEntry) ((_index.get(next) instanceof EvictionIndexEntry) ? _index.get(next) : null);
        EvictionIndexEntry prevEntry = (EvictionIndexEntry) ((_index.get(previous) instanceof EvictionIndexEntry) ? _index.get(previous) : null);

        if (_index.isEmpty() || key < _head)
        {
            _head = key;
        }

        int addSize = 0;
        int removeSize = 0;
        boolean incrementKeyCount = true;
        
        if (_index.containsKey(key))
        {
            EvictionIndexEntry indexEntry = (EvictionIndexEntry) _index.get(key);
            if (indexEntry != null)
            {
                removeSize = indexEntry.getInMemorySize();
                
                if(indexEntry.Contains(value))
                    incrementKeyCount = false;
                
                indexEntry.Insert(value);
                
                addSize = indexEntry.getInMemorySize();
            }
        }
        else
        {
            EvictionIndexEntry indexEntry = new EvictionIndexEntry();
            indexEntry.Insert(value);
            
            addSize = indexEntry.getInMemorySize();
            
            _index.put(key, indexEntry);
            
            if(_index.size() > _evictionIndexMaxCount)
                _evictionIndexMaxCount = _index.size();
            
            //very first node
            if (prevEntry == null && nextEntry == null)
            {
                indexEntry.setNext(-1);
                ;
                indexEntry.setPrevious(-1);
            }
            //insert at begining
            else if (prevEntry == null && nextEntry != null)
            {
                indexEntry.setNext(next);
                indexEntry.setPrevious(-1);
                nextEntry.setPrevious(key);
            }
            //insert at end
            else if (prevEntry != null && nextEntry == null)
            {
                indexEntry.setPrevious(previous);
                indexEntry.setNext(-1);
                prevEntry.setNext(key);
            }
            //insert in between the two nodes
            else
            {
                indexEntry.setPrevious(previous);
                indexEntry.setNext(next);
                prevEntry.setNext(key);
                nextEntry.setPrevious(key);
            }
        }
        if (key > _tail)
        {
            _tail = key;
        }
        
        _evictionIndexEntriesSize -= removeSize;
        _evictionIndexEntriesSize += addSize;
        
        if(incrementKeyCount)
            _keyCount++;        
    }

    public final void Remove(long key, Object value)
    {
        EvictionIndexEntry previousEntry = null;
        EvictionIndexEntry nextEntry = null;

        int addSize = 0;
        int removeSize = 0;
        
        if (_index.containsKey(key))
        {
            EvictionIndexEntry indexEntry = (EvictionIndexEntry) _index.get(key);
            boolean decrementKeyCount = true;
            removeSize = indexEntry.getInMemorySize();
            
            if(!indexEntry.Contains(value))
                decrementKeyCount = false;
            
            if (indexEntry.Remove(value))
            {
                if (indexEntry.getPrevious() != -1)
                {
                    previousEntry = (EvictionIndexEntry) _index.get(indexEntry.getPrevious());
                }
                if (indexEntry.getNext() != -1)
                {
                    nextEntry = (EvictionIndexEntry) _index.get(indexEntry.getNext());
                }

                if (previousEntry != null && nextEntry != null)
                {
                    previousEntry.setNext(indexEntry.getNext());
                    nextEntry.setPrevious(indexEntry.getPrevious());
                }
                else if (previousEntry != null)
                {
                    previousEntry.setNext(indexEntry.getNext());
                    _tail = indexEntry.getPrevious();
                }
                else if (nextEntry != null)
                {
                    nextEntry.setPrevious(indexEntry.getPrevious());
                    _head = indexEntry.getNext();
                }
                else
                {
                    _tail = _head = -1;
                }
                _index.remove(key);
            }
            else{
                addSize = indexEntry.getInMemorySize();
            }
            if(decrementKeyCount)
                _keyCount--;
        }
        
        _evictionIndexEntriesSize -= removeSize;
        _evictionIndexEntriesSize += addSize;
    }

    public final void Remove(long key, Object value, tangible.RefObject<Long> previous, tangible.RefObject<Long> next)
    {
        EvictionIndexEntry previousEntry = null;
        EvictionIndexEntry nextEntry = null;

        previous.argvalue = key;
        
        int addSize = 0, removeSize = 0;

        if (_index.containsKey(key))
        {
            EvictionIndexEntry indexEntry = (EvictionIndexEntry) _index.get(key);

            removeSize = indexEntry.getInMemorySize();
            
            if (indexEntry.getPrevious() != -1)
            {
                previousEntry = (EvictionIndexEntry) _index.get(indexEntry.getPrevious());
            }
            if (indexEntry.getNext() != -1)
            {
                nextEntry = (EvictionIndexEntry) _index.get(indexEntry.getNext());
            }

            next.argvalue = indexEntry.getNext();

            boolean decrementKeyCount = true;
            
            if (!indexEntry.Contains(value)) decrementKeyCount = false;
                
            if (indexEntry.Remove(value))
            {
                previous.argvalue = indexEntry.getPrevious();

                if (previousEntry != null && nextEntry != null)
                {
                    previousEntry.setNext(indexEntry.getNext());
                    nextEntry.setPrevious(indexEntry.getPrevious());
                }
                else if (previousEntry != null)
                {
                    previousEntry.setNext(indexEntry.getNext());
                    _tail = indexEntry.getPrevious();
                }
                else if (nextEntry != null)
                {
                    nextEntry.setPrevious(indexEntry.getPrevious());
                    _head = indexEntry.getNext();
                }
                else
                {
                    _tail = _head = -1;
                }
                _index.remove(key);
            }
            else {
                addSize = indexEntry.getInMemorySize();
            }
            
            if(decrementKeyCount)
                _keyCount--;
        }
        
        _evictionIndexEntriesSize -= removeSize;
        _evictionIndexEntriesSize += addSize;
    }

    public final void Clear()
    {
        _head = _tail = -1;
        _evictionIndexEntriesSize = 0;
        _evictionIndexMaxCount = 0;
        _keyCount = 0;
        
        _index = new HashMap();
    }

    public final java.util.ArrayList GetSelectedKeys(CacheBase cache, long evictSize)
    {
        EvictionIndexEntry entry = null;
        java.util.ArrayList selectedKeys = new java.util.ArrayList();
        int totalSize = 0;
        boolean selectionCompleted = false;
        long index = _head;
        if (_head != -1)
        {
            do
            {
                entry = (EvictionIndexEntry) ((_index.get(index) instanceof EvictionIndexEntry) ? _index.get(index) : null);
                java.util.Collection keys = entry.GetAllKeys();
                for (Iterator it = keys.iterator(); it.hasNext();)
                {
                    Object key = it.next();
                    int itemSize = cache.GetItemSize(key);
                    if (totalSize + itemSize >= evictSize && totalSize > 0)
                    {
                        if (evictSize - totalSize > (itemSize + totalSize) - evictSize)
                        {
                            selectedKeys.add(key);
                        }

                        selectionCompleted = true;
                        break;
                    }
                    else
                    {
                        selectedKeys.add(key);
                        totalSize += itemSize;
                    }
                }
                index = entry.getNext();
            }
            while (!selectionCompleted && index != -1);
        }
        return selectedKeys;
    }

    @Override
    public long getIndexInMemorySize() {
        return _evictionIndexEntriesSize + this.getEvictionIndexSize();
    }
    private long getEvictionIndexSize()
    {
        long temp = 0;
        temp += _evictionIndexMaxCount * MemoryUtil.NetHashtableOverHead;
        return temp;
    }
}
