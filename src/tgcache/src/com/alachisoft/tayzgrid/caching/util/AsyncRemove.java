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

import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.AsyncOpResult;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.BitSet;

/**
 * Asynchronous remove operation task.
 */
public class AsyncRemove implements AsyncProcessor.IAsyncTask
{

    /**
     * The cache object
     */
    private Cache _cache;
    /**
     * Key to remove from the cache
     */
    private Object _key;
    private OperationContext _operationContext;

    private ILogger getCacheLog()
    {
        return _cache.getCacheLog();
    }

    /**
     * Constructor
     *
     * @param cache
     * @param key
     */
    public AsyncRemove(Cache cache, Object key, OperationContext operationContext)
    {
        _cache = cache;
        _key = key;
        _operationContext = operationContext;
    }

    /**
     * Implementation of message sending.
     */
    public void Process()
    {
        Object result = null;
        BitSet flagMap = new BitSet();
        CallbackEntry cbEntry = null;
        String providerName = null;
        try
        {
            if (_key instanceof Object[])
            {
                Object[] package_Renamed = (Object[]) _key;
                _key = package_Renamed[0];
                flagMap = (BitSet) ((package_Renamed[1] instanceof BitSet) ? package_Renamed[1] : null);
                if (package_Renamed.length > 2)
                {
                    cbEntry = (CallbackEntry) ((package_Renamed[2] instanceof CallbackEntry) ? package_Renamed[2] : null);
                }
                if (package_Renamed.length == 4)
                {
                    providerName = (String) ((package_Renamed[3] instanceof String) ? package_Renamed[3] : null);
                }
            }
            
             _operationContext.Add(OperationContextFieldName.NoGracefulBlock, true);
            _cache.Remove(_key, flagMap, cbEntry, null, 0, LockAccessType.IGNORE_LOCK, providerName, _operationContext);
            result = AsyncOpResult.Success;
        }
        catch (Exception e)
        {
            if (getCacheLog() != null)
            {
                getCacheLog().Error("AsyncRemove.Process()", e.getMessage());
            }
            result = e;
        }
        finally
        {
            if (cbEntry != null && cbEntry.getAsyncOperationCompleteCallback() != null)
            {
                _cache.OnAsyncOperationCompleted(AsyncOpCode.Remove, new Object[]
                        {
                            _key, cbEntry.getAsyncOperationCompleteCallback(), result
                        });
            }
        }
    }
}
