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

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.AsyncOpResult;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.BitSet;

/**
 * Asynchronous clear operation task.
 */
public class AsyncClear implements AsyncProcessor.IAsyncTask
{

    /**
     * The cache object
     */
    private Cache _cache = null;
    private CallbackEntry _cbEntry;
    private BitSet _flagMap;
    private OperationContext _operationContext;

    private ILogger getCacheLog()
    {
        return _cache.getCacheLog();
    }

    /**
     * Constructor
     *
     * @param cache
     */
    public AsyncClear(Cache cache, CallbackEntry cbEntry, BitSet flagMap, OperationContext operationContext)
    {
        _cache = cache;
        _cbEntry = cbEntry;
        _flagMap = flagMap;
        _operationContext = operationContext;
    }

    /**
     * Implementation of message sending.
     */
    public void Process()
    {
        Object result = null;
        try
        {
              _operationContext.Add(OperationContextFieldName.NoGracefulBlock, true);
            _cache.Clear(_flagMap, _cbEntry, _operationContext);
            result = AsyncOpResult.Success;
        }
        catch (Exception e)
        {
            if (getCacheLog() != null)
            {
                getCacheLog().Error("AsyncClear.Process()", e.getMessage());
            }
            result = e;
        }
        finally
        {
            if (_cbEntry != null && _cbEntry.getAsyncOperationCompleteCallback() != null)
            {
                _cache.OnAsyncOperationCompleted(AsyncOpCode.Clear, new Object[]
                        {
                            null, _cbEntry.getAsyncOperationCompleteCallback(), result
                        });
            }
        }
    }
}
