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
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;

/**
 * Provides a thread-safe wrapper over the eviction policy.
 */
public class EvictionPolicySyncWrapper implements IEvictionPolicy, ISizableIndex
{

    private IEvictionPolicy _evctPolicy;
    private ReaderWriterLock _sync = new ReaderWriterLock();

    public EvictionPolicySyncWrapper(IEvictionPolicy evictionPolicy)
    {
        _evctPolicy = evictionPolicy;
    }

    public final ReaderWriterLock getSync()
    {
        return _sync;
    }

    public final void Notify(Object key, EvictionHint oldhint, EvictionHint newHint)
    {
        getSync().AcquireWriterLock();
        try
        {
            _evctPolicy.Notify(key, oldhint, newHint);
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public final void Clear()
    {
        getSync().AcquireWriterLock();
        try
        {
            _evctPolicy.Clear();
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public final EvictionHint CompatibleHint(EvictionHint eh)
    {
        return _evctPolicy.CompatibleHint(eh);
    }

    public final void Execute(CacheBase cache, CacheRuntimeContext context, long count)
    {
        getSync().AcquireWriterLock();
        try
        {
            _evctPolicy.Execute(cache, context, count);
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public final void Remove(Object key, EvictionHint hint)
    {
        getSync().AcquireWriterLock();
        try
        {
            _evctPolicy.Remove(key, hint);
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public final float getEvictRatio()
    {
        getSync().AcquireWriterLock();
        try
        {
            return _evctPolicy.getEvictRatio();
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public final void setEvictRatio(float value)
    {
        getSync().AcquireWriterLock();
        try
        {
            _evctPolicy.setEvictRatio(value);
            ;
        }
        finally
        {
            getSync().ReleaseWriterLock();
        }
    }

    public long getIndexInMemorySize() {
        throw new UnsupportedOperationException("EvictionPolicySyncWrapper.getIndexInMemorySize()");
    }
}
