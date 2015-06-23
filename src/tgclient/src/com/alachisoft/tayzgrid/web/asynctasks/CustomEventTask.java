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
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;

public class CustomEventTask implements AsyncProcessor.IAsyncTask {

    private boolean _notifyAsync;
    private byte[] _key;
    private String _cacheId;
    private byte[] _value;
    private Broker _parent;
    private UsageStats _stats;

    public CustomEventTask(Broker parent, byte[] key, String cacheId, byte[] value, boolean notifyAsync) {
        this._parent = parent;
        this._key = key;
        this._cacheId = cacheId;
        this._value = value;
        this._notifyAsync = notifyAsync;
    }

    public void Process() {
        try {
            if (_parent != null) {
                _stats = new UsageStats();
                _stats.BeginSample();
                _parent._cache.getEventsListener().OnCustomNotification(CompactBinaryFormatter.fromByteBuffer(_key, _cacheId), CompactBinaryFormatter.fromByteBuffer(_value, _cacheId), _notifyAsync);
                _stats.EndSample();
            }
        } catch (Exception ex) {
            if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled() ) {
                _parent.getLogger().getCacheLog().Error("Custome Event Task.Process", ex.getMessage());
            }
        }
    }
}
