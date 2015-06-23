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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.responsemanagement;

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ICommandConsumer;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.MemTcpClient;

import com.alachisoft.tayzgrid.common.logger.*;
import java.nio.channels.SocketChannel;

public abstract class ResponseManager implements ICommandConsumer {

    protected java.util.LinkedList<AbstractCommand> _responseQueue = new java.util.LinkedList<AbstractCommand>();
    private ILogger _logger;

    public final ILogger getLogger() {
        return _logger;
    }

    public final void setLogger(ILogger value) {
        _logger = value;
    }
    protected SocketChannel _socketChannel;
    protected boolean _alive = false;

    public ResponseManager(SocketChannel channel) {
        _socketChannel = channel;
    }

    public final ModuleStatus RegisterCommand(AbstractCommand command) {
        synchronized (this) {
            _responseQueue.offer(command);
            if (_alive) {
                return ModuleStatus.Running;
            }
            return ModuleStatus.Idle;
        }
    }

    protected MemTcpClient _memTcpClient;

    public final void setMemTcpClient(MemTcpClient value) {
        _memTcpClient = value;
    }
    protected boolean _disposed = false;

    public final boolean getDisposed() {
        return _disposed;
    }

    public final void setDisposed(boolean value) {
        _disposed = value;
    }
}