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

import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.AsyncOpResult;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.BitSet;

/**
 * Asynchronous Add operation task.
 */
public class AsyncAdd implements AsyncProcessor.IAsyncTask
{

    /**
     * The cache object
     */
    private Cache _cache;
    /**
     * Key and value to add to the cache
     */
    private Object _key, _value;
    /**
     * Expiration hint
     */
    private ExpirationHint _expiryHint;
    /**
     * Eviction hint
     */
    private EvictionHint _evictionHint;
    /**
     * group for data
     */
    private String _group;
    /**
     * sub group of the group
     */
    private String _subGroup;
    private BitSet _flag;
    private java.util.HashMap _queryInfo;

    private OperationContext _operationContext;
    private String _providerName;
    private String _resyncProviderName;
    
    private ILogger getCacheLog()
    {
        return _cache.getCacheLog();
    }

    /**
     * Constructor
     *
     * @param cache
     * @param key
     * @param value
     * @param expiryHint
     * @param evictionHint
     */
    public AsyncAdd(Cache cache, Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, BitSet Flag, java.util.HashMap queryInfo, String providerName, String resyncProviderName, OperationContext operationContext)
    {
        _cache = cache;
        _key = key;
        _value = value;
        _expiryHint = expiryHint;
      
        _evictionHint = evictionHint;
        _group = group;
        _subGroup = subGroup;
        _flag = Flag;
        _queryInfo = queryInfo;
        _providerName=providerName;
        _resyncProviderName=resyncProviderName;
        _operationContext = operationContext;
    }

    /**
     * Implementation of message sending.
     */
    @Override
    public void Process()
    {
        Object result = null;

        try
        {
            _operationContext.Add(OperationContextFieldName.NoGracefulBlock, true);
            _cache.Add(_key, _value, _expiryHint, _evictionHint, _group, _subGroup, _queryInfo, _flag, _providerName, _resyncProviderName, _operationContext, null);
            result = AsyncOpResult.Success;
        }
        catch (Exception e)
        {
            if (getCacheLog() != null)
            {
                getCacheLog().Error("AsyncAdd.Process()", e.getMessage());
            }
            result = e;
        }
        finally
        {
            CallbackEntry cbEntry = (CallbackEntry) ((_value instanceof CallbackEntry) ? _value : null);
            if (cbEntry != null && cbEntry.getAsyncOperationCompleteCallback() != null)
            {
                _cache.OnAsyncOperationCompleted(AsyncOpCode.Add, new Object[]
                        {
                            _key, cbEntry.getAsyncOperationCompleteCallback(), result
                        });
            }
        }
    }
}
