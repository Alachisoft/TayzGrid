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
import com.alachisoft.tayzgrid.command.AddCommand;
import com.alachisoft.tayzgrid.command.BulkGetCommand;
import com.alachisoft.tayzgrid.command.ClearCommand;
import com.alachisoft.tayzgrid.command.Command;
import com.alachisoft.tayzgrid.command.InsertCommand;
import com.alachisoft.tayzgrid.command.RemoveCommand;
import com.alachisoft.tayzgrid.web.caching.RemoteCache.Broker;
import com.alachisoft.tayzgrid.web.caching.RemoteCacheAsyncEventsListener;

public class AsyncOperationCompletedEventTask implements AsyncProcessor.IAsyncTask {
    
           private boolean _notifyAsync;
        private Command _command;
        private Object _key;
        private Object _asyncOpResult;
        private Broker _parent;
        private UsageStats _stats;

        public AsyncOperationCompletedEventTask(Broker parent, Command command, Object key, Object asyncOpResult, boolean notifyAsync)
        {
            this._parent = parent;
            this._key = key;
            this._command = command;
            this._asyncOpResult = asyncOpResult;
            this._notifyAsync = notifyAsync;
        }

        @Override
        public void Process()
        {
            try
            {
                if (_parent != null && _parent._cache.getAsyncEventsListener() != null)
                {
                    _stats = new UsageStats();
                    _stats.BeginSample();

                    RemoteCacheAsyncEventsListener listener = _parent._cache.getAsyncEventsListener();                    

                    
                    if (_command instanceof AddCommand)
                    {
                        listener.OnAsyncAddCompleted(_key, (short)((AddCommand)_command).AsycItemAddedOpComplete(), _asyncOpResult, _notifyAsync);
                    }
                    else if (_command instanceof InsertCommand)
                    {
                        listener.OnAsyncInsertCompleted(_key, (short)((InsertCommand)_command).AsycItemUpdatedOpComplete(), _asyncOpResult, _notifyAsync);
                    }
                    else if (_command instanceof RemoveCommand)
                    {
                        listener.OnAsyncRemoveCompleted(_key, (short)((RemoveCommand)_command).AsycItemRemovedOpComplete(), _asyncOpResult, _notifyAsync);
                    }
                    else if (_command instanceof ClearCommand)
                    {
                        listener.OnAsyncClearCompleted((short)((ClearCommand)_command).AsyncCacheClearedOpComplete(), _asyncOpResult, _notifyAsync);
                    }
                    else if(_command instanceof BulkGetCommand) {
                        listener.OnJCacheLoadingCompletion((short)((BulkGetCommand)_command).JCacheLoaderOpComplete(), _asyncOpResult, _notifyAsync);
                    }
                }
                _stats.EndSample();

            }
            catch (Exception ex)
            {
                if (_parent != null && _parent.getLogger()!=null && _parent.getLogger().getIsErrorLogsEnabled())
                    if(_parent.getLogger().getCacheLog()!=null)
                    {
                    _parent.getLogger().getCacheLog().Error("Async Operation completed Event Task.Process", ex.getMessage());
                    }
            }
        }
    
}
