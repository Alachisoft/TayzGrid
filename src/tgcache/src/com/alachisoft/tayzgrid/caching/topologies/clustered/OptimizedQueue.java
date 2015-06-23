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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.datastructures.IOptimizedQueueOperation;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public class OptimizedQueue
{
    private java.util.HashMap _queue = new java.util.HashMap(1000);
    private java.util.HashMap<Object, Integer> _keyToIndexMap = new java.util.HashMap<Object, Integer>(1000);
    private java.util.HashMap<Integer, Object> _indexToKeyMap = new java.util.HashMap<Integer, Object>(1000);
    private int _tail = -1;
    private int _head = -1;
    private boolean _tailMaxReached = false;
    private Object _sync_mutex = new Object();
    private long _size;
    private long _count;

    public OptimizedQueue()
    {
    }

    public final long getSize()
    {
        synchronized (_sync_mutex)
        {
            return _size;
        }
    }

    public final long getCount()
    {
        synchronized (_sync_mutex)
        {
            return _count;
        }
    }

    /**
     * Optimized Enqueue opeartion, adds the opeartion at _tail index and removes any previous operations on that key from the queue
     *
     * @param cacheKey
     * @param operation
     * @return 
     */
    public final boolean Enqueue(Object cacheKey, IOptimizedQueueOperation operation) 
    {
        boolean isNewItem = true;
        try
        {
            synchronized (_sync_mutex)
            {
                if (_keyToIndexMap.containsKey(cacheKey))
                {                     
                    int index1 = _keyToIndexMap.get(cacheKey);
                    IOptimizedQueueOperation oldOperation = (IOptimizedQueueOperation)_queue.get(index1) ;
                    _queue.put(index1, operation);
                    isNewItem = false;
                    _size -= oldOperation.getSize(); //subtract old operation size
                    _size += operation.getSize();
                    return isNewItem;
                }

                if (_tail == Integer.MAX_VALUE)
                { //checks if the _tail value has reached the maxvalue of the long data type, so reinitialize it
                    _tail = -1;
                    _tailMaxReached = true;
                }
                
                int index = ++_tail;
                _size += operation.getSize();
                _queue.put(index, operation); //Add new opeartion at the tail of the queue
                _keyToIndexMap.put(cacheKey, index); // update (cache key, queue index) map
                _indexToKeyMap.put(index, cacheKey);
                if (isNewItem)
                {
                    _count++;
                }
            }
        }
        finally
        {
        }
        return isNewItem;
    }

    public final IOptimizedQueueOperation Dequeue() throws OperationFailedException, Exception
    {
        IOptimizedQueueOperation operation = null;
        try
        {
            synchronized (_sync_mutex)
            {
                int index = 0;
                do
                { //fetch the next valid operation from the queue
                    if (_head < _tail || _tailMaxReached)
                    { //or contition checks if the _tail has reached max long value and _head has not yet reached there , so in this case _head<_tail will fail bcz _tail has been reinitialized
                        if (_head == Integer.MAX_VALUE)
                        { //checks if _head has reached the max long value, so reinitialize _head and make _tailMaxReached is set to false as _head<_tail is now again valid
                            _head = -1;
                            _tailMaxReached = false;
                        }

                        index = ++_head;
                        operation = (IOptimizedQueueOperation) ((_queue.get(index) instanceof IOptimizedQueueOperation) ? _queue.get(index) : null); //get key on which the operation is to be performed from the head of the queue

                        if (operation != null)
                        {
                            Object cacheKey = _indexToKeyMap.get(index);
                            _keyToIndexMap.remove(cacheKey); //update map
                            _indexToKeyMap.remove(index);
                            _queue.remove(index); //update queue
                            _size -= operation.getSize();
                            _count--;
                        }
                    }
                    else
                    {
                        break;
                    }
                }
                while (operation == null);
            }
        }
        catch (Exception exp)
        {
            throw exp;
        }
        return operation;
    }

    protected final boolean AllowRemoval(Object cacheKey)
    {
        synchronized (_sync_mutex)
        {
            return !_keyToIndexMap.containsKey(cacheKey);
        }
    }

    /**
     * Clears queue and helping datastructures like map, cache, itemstobereplicated
     */
    public final void Clear()
    {
            synchronized (_sync_mutex)
            {
                _queue.clear();
                _keyToIndexMap.clear();
                _indexToKeyMap.clear();
                _tail = _head = -1;
                _tailMaxReached = false;
                _size = 0;
                _count = 0;
            }
    }

    public final void dispose()
    {
        synchronized (_sync_mutex)
        {
        }
    }
}
