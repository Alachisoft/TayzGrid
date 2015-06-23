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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.executionmanagement;

import com.alachisoft.tayzgrid.integrations.memcached.provider.CacheFactory;
import com.alachisoft.tayzgrid.integrations.memcached.provider.IMemcachedProvider;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ICommandConsumer;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.IThreadPoolTask;


public abstract class ExecutionManager implements IThreadPoolTask, ICommandConsumer {

    protected IMemcachedProvider _cacheProvider = null;
    protected ICommandConsumer _commandConsumer;

    public final ICommandConsumer getCommandConsumer() {
        return _commandConsumer;
    }

    public final void setCommandConsumer(ICommandConsumer value) {
        _commandConsumer = value;
    }
    protected boolean _disposed;

    public final boolean getDisposed() {
        return _disposed;
    }

    public final void setDisposed(boolean value) {
        _disposed = value;
    }

    public abstract void Start();

    public abstract ModuleStatus RegisterCommand(AbstractCommand command);

    public abstract void run();

    public final void run(Object obj) {
        this.run();
    }
}