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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.AsyncOpCode;
import com.alachisoft.tayzgrid.common.IDisposable;

public class RemoteCacheAsyncEventsListener implements IDisposable {

 
    CacheAsyncEventsListener _parent;

    /**
     * Constructor.
     *
     * @param parent
     */
    public RemoteCacheAsyncEventsListener(CacheAsyncEventsListener parent) {
        _parent = parent;
    }


    ///#region    /                 --- IDisposable ---           /
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     *
     */
    public final void dispose() {
    }


    ///#endregion
    private Object PackageResult(Object key, short callbackId, Object result) {
        Object[] package_Renamed = new Object[3];
        package_Renamed[0] = key;

        AsyncCallbackInfo cbEntry = new AsyncCallbackInfo(-1, null, callbackId);
        package_Renamed[1] = cbEntry;
        package_Renamed[2] = result;

        return package_Renamed;
    }


    public final void OnAsyncAddCompleted(Object key, short callbackId, Object result, boolean notifyAsync) {
        OnAsyncOperationCompleted(AsyncOpCode.Add, PackageResult(key, callbackId, result), notifyAsync);
    }

    public final void OnAsyncInsertCompleted(Object key, short callbackId, Object result, boolean notifyAsync) {
        OnAsyncOperationCompleted(AsyncOpCode.Update, PackageResult(key, callbackId, result), notifyAsync);
    }

    public final void OnAsyncRemoveCompleted(Object key, short callbackId, Object result, boolean notifyAsync) {
        OnAsyncOperationCompleted(AsyncOpCode.Remove, PackageResult(key, callbackId, result), notifyAsync);
    }

    public final void OnAsyncClearCompleted(short callbackId, Object result, boolean notifyAsync) {
        OnAsyncOperationCompleted(AsyncOpCode.Clear, PackageResult(null, callbackId, result), notifyAsync);
    }

    public final void OnAsyncOperationCompleted(Object opCode, Object result, boolean notifyAsync) {
        _parent.OnAsyncOperationCompleted(opCode, result, notifyAsync);
    }

    public final void OnDataSourceUpdated(short callbackID, java.util.Hashtable result, OpCode operationCode, boolean notifyAsync) {
        _parent.OnDataSourceUpdated(callbackID, result, operationCode, notifyAsync);
    }
    
    public final void OnJCacheLoadingCompletion (short callbackId, Object result, boolean notifyAsync) {
        OnAsyncOperationCompleted(AsyncOpCode.loadAll, PackageResult(null, callbackId, result), notifyAsync);
    }

}
