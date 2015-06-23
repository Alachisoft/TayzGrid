
package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
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
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.io.IOException;


public class AsyncBulkGet implements AsyncProcessor.IAsyncTask{

    private Cache _cache;
    private Object[] _keys;
    private BitSet _flag;
    private OperationContext _operationContext;
    private String _providerName;
    
    private ILogger getCacheLog()
    {
        return _cache.getCacheLog();
    }
    
    public AsyncBulkGet(Cache cache, Object[] keys, BitSet flagMap, String providerName, OperationContext operationContext) {
        _keys = keys;
        _flag = flagMap;
        _providerName = providerName;
        _operationContext = operationContext;
        _cache = cache;
        
    }
    
    @Override
    public void Process() throws OperationFailedException, IOException, CacheException, LockingException  {
        Object result = null;
        try {
            _operationContext.Add(OperationContextFieldName.NoGracefulBlock, true);
            _cache.GetBulk(_keys, _flag, _providerName, _operationContext);
            result = AsyncOpResult.Success;
        }
        catch (Exception e) {
            if (getCacheLog() != null)
            {
                getCacheLog().Error("AsyncBulkGet.Process()", e.getMessage());
            }
            result = e;
        }
        
        finally {

           Object callback = _operationContext.GetValueByField(OperationContextFieldName.LoadAllNotificationId);
           AsyncCallbackInfo notificationCallback = callback instanceof AsyncCallbackInfo? (AsyncCallbackInfo) callback: null;
           if(notificationCallback != null) {
               _cache.OnAsyncOperationCompleted(AsyncOpCode.loadAll, new Object[]
               {null, notificationCallback, result}
          );
           }
        }
    }
    
}
