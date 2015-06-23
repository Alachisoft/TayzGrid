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

package com.alachisoft.tayzgrid.web.asynctasks;

import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.web.caching.OpCode;
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import java.util.Hashtable;

public class DSUpdateEventTask implements AsyncProcessor.IAsyncTask {

    private boolean _notifyAsync;
    private short _callBackId;
    private Hashtable _result;
    private OpCode _opCode;
    private Broker _parent;
    private UsageStats _stats;

    public DSUpdateEventTask(Broker parent, short callBackId, Hashtable result, OpCode opCode, boolean notifyAsync) {
        this._parent = parent;
        this._callBackId = callBackId;
        this._result = result;
        this._opCode = opCode;
        this._notifyAsync = notifyAsync;
    }

    @Override
    public void Process() {
        try {
            if (_parent != null && _parent._cache.getAsyncEventsListener() != null) {
                _stats = new UsageStats();
                _stats.BeginSample();

                _parent._cache.getAsyncEventsListener().OnDataSourceUpdated(_callBackId, _result, _opCode, _notifyAsync);

                _stats.EndSample();
            }
        } catch (Exception ex) {
            if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled()) {
                _parent.getLogger().getCacheLog().Error("DS Update event Task.Process", ex.getMessage());
            }
        }
    }
}
