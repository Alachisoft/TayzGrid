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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheBase;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Iterator;
import java.util.Map;

public class VirtualQueryIndex implements IQueryIndex
{

    private LocalCacheBase _cache;

    public VirtualQueryIndex(LocalCacheBase cache)
    {
        _cache = cache;
    }
        //only needed in case of AttributeIndex
    public final void AddToIndex(Object key, Object value) {  }
    public final void RemoveFromIndex(Object key, Object value)  {  }
    public void RemoveFromIndex(Object key){}
    public final void Clear() { }
    public IndexInformation GetIndexInformation(Object key) { return null; }
    
    public final Iterator GetEnumerator(String typeName, boolean forTag) throws OperationFailedException, LockingException, GeneralFailureException, Exception
    {
        if (_cache == null)
        {
            return null;
        }
        if (typeName.equals("*"))
        {
            return GetEnumerator();
        }
        else
        {
            Iterator tempVar = _cache.GetEnumerator();
            Iterator en = (Iterator) ((tempVar instanceof Iterator) ? tempVar : null);
            java.util.HashMap tbl = new java.util.HashMap();

            while (en.hasNext())
            {
                Map.Entry current = (Map.Entry) en.next();

                Object obj = ((CacheEntry) current.getValue()).DeflattedValue(_cache.getContext().getCacheImpl().getName());

                if (typeName.equals(obj.getClass().getName()))
                {
                    tbl.put(current.getKey(), current.getValue());
                }
            }
            return tbl.entrySet().iterator();
        }
    }
    private Iterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException, CacheException
    {
        if (_cache != null)
        {
            Iterator tempVar = _cache.GetEnumerator();
            return (Iterator) ((tempVar instanceof Iterator) ? tempVar : null);
        }
        return null;
    }

    @Override
    public long getIndexInMemorySize() {
        throw new UnsupportedOperationException("VirtualQueryIndex.getIndexInMemorySize");
    }
}