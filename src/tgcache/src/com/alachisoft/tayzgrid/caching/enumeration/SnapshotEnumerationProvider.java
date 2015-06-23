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

import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public class SnapshotEnumerationProvider implements IEnumerationProvider
{
    /**
     * The en-wrapped instance of cache.
     */
    private CacheBase _cache;
    /**
     * The en-wrapped instance of enumerator.
     */
    private EnumerationPointer _pointer;
    /**
     * Current list of keys in the cache when the enumerator is taken on cache
     */
    //: Changed Array to Object array
    private Object[] _snapshot;
    /**
     * Size of the chunk to be sent on each next chunk call
     */
    private int _chunkSize = 1000;
    /**
     * Sequence ID of the chunk being send for this particular enumerator. Holds -1 if all the chunks have been sent.
     */
    private int _chunkId = 0;

    public final void Initialize(EnumerationPointer pointer, IndexedLocalCache cache) throws GeneralFailureException, OperationFailedException, CacheException
    {
        _cache = cache;
        _pointer = pointer;

        if (ServicePropValues.CacheServer_EnumeratorChunkSize != null)
        {
            _chunkSize = Integer.decode(ServicePropValues.CacheServer_EnumeratorChunkSize);
        }

        _snapshot = CacheSnapshotPool.getInstance().GetSnaphot(pointer.getId(), cache);
    }

    public final EnumerationDataChunk GetNextChunk(com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer pointer)
    {
        int count = 0;
        EnumerationDataChunk chunk = new EnumerationDataChunk();
        chunk.setData(new java.util.ArrayList());
        int currentIndex = pointer.getChunkId();
        while (currentIndex < _snapshot.length - 1 && count < _chunkSize)
        {
            currentIndex++;
            chunk.getData().add(_snapshot[currentIndex]);
            count++;
        }
        if (currentIndex == _snapshot.length - 1)
        {
            _pointer.setChunkId(-1);
        }
        else
        {
            _pointer.setChunkId(currentIndex); //Set the chunkId to strating index of the next chunk to fetch.
        }

        chunk.setPointer(_pointer);

        return chunk;
    }

    public final void dispose()
    {
        CacheSnapshotPool.getInstance().DiposeSnapshot(_pointer.getId(), _cache); //Disposes the snapshot from pool for this particular pointer
        _cache = null;
        _pointer = null;
        _snapshot = null;
    }
}
