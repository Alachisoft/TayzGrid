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

import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;

public class EnumerationIndex
{

    private IndexedLocalCache _cache;
    private java.util.HashMap<EnumerationPointer, IEnumerationProvider> _index;

    public EnumerationIndex(IndexedLocalCache cache)
    {
        _cache = cache;
    }

    public final EnumerationDataChunk GetNextChunk(EnumerationPointer pointer) throws Exception
    {
        EnumerationDataChunk nextChunk = null;
        IEnumerationProvider provider = GetProvider(pointer);

        if (pointer.isDisposable() && provider != null)
        {
            provider.dispose();
            if (_index.containsKey(pointer))
            {
                _index.remove(pointer);
            }
            nextChunk = new EnumerationDataChunk();
            nextChunk.setPointer(pointer);
        }
        else if (provider != null)
        {
            nextChunk = provider.GetNextChunk(pointer);
            //Dispose the provider if this is the last chunk for it
            if (nextChunk.isLastChunk())
            {
                provider.dispose();
                if (_index.containsKey(pointer))
                {
                    _index.remove(pointer);
                }
            }
        }

        return nextChunk;
    }

    private IEnumerationProvider GetProvider(EnumerationPointer pointer) throws Exception
    {
        if (_index == null)
        {
            _index = new java.util.HashMap<EnumerationPointer, IEnumerationProvider>();
        }

        IEnumerationProvider provider = null;

        if (_index.containsKey(pointer))
        {
            provider = _index.get(pointer);
        }
        else if (pointer.getChunkId() == -1 && !pointer.isSocketServerDispose() && !pointer.isDisposable())
        {
            provider = new SnapshotEnumerationProvider();
            provider.Initialize(pointer, _cache);
            _index.put(pointer, provider);
        }

        return provider;
    }

    public final boolean Contains(EnumerationPointer pointer)
    {
        if (_index != null)
        {
            return _index.containsKey(pointer);
        }
        else
        {
            return false;
        }
    }
}
