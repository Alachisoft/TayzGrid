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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.parsing;

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.IThreadPoolTask;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.ThreadPool;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ICommandConsumer;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.MemTcpClient;

public abstract class ProtocolParser implements IThreadPoolTask {

    /**
     * Current state of parser
     */
    protected ParserState _state = ParserState.Ready;
    /**
     * Data Stream shared b/w ClientHandler and parser
     */
    protected DataStream _inputDataStream;
    /**
     * Raw data buffer used by parser to store raw data until complete command
     * received
     */
    protected byte[] _rawData = new byte[102400];
    protected int _rawDataOffset = 0;
    /**
     * Currently under-process command
     */
    protected AbstractCommand _command;
    protected MemTcpClient _memTcpClient;
    
    
    protected ICommandConsumer _commandConsumer;

    public ProtocolParser(DataStream inputStream, MemTcpClient parent) {
        _inputDataStream = inputStream;
        _memTcpClient = parent;
    }
    /**
     * Gets or sets state of parsers activity
     */
    private boolean isAlive;

    public final boolean isAlive() {
        return isAlive;
    }

    public final void setAlive(boolean value) {
        isAlive = value;
    }

    /**
     * Current state of parser
     */
    public final ParserState getState() {
        return _state;
    }

    protected final void setState(ParserState value) {
        _state = value;
    }
    protected boolean _disposed;

    public final boolean isDisposed() {
        return _disposed;
    }

    public final void setDisposed(boolean value) {
        _disposed = value;
    }

    /**
     * Starts parsing command from inputStream.
     */
    public abstract void StartParser();

    public final ICommandConsumer getCommandConsumer() {
        return _commandConsumer;
    }

    public final void setCommandConsumer(ICommandConsumer value) {
        _commandConsumer = value;
    }

    /**
     * Dispatch created command for execution.
     */
    public final void Dispatch() {
        try {
            synchronized (this) {
                this.setState(ParserState.Ready);
                if (_inputDataStream.getLenght() > 0) {
                    ThreadPool.ExecuteTask(this);
                } else {
                    this.setAlive(false);
                }
            }
            if (_commandConsumer != null) {
                _commandConsumer.Start();
            }
        } catch (RuntimeException e) {
            LogManager.getLogger().Error("ProtocolParser", " Failed to dispatch parsed command. Exception: " + e.getMessage());
        }
    }

    private void StartParser(Object obj) {
        this.StartParser();
    }

    @Override
    public final void run() {
        this.StartParser();
    }

    @Override
    public final void run(Object obj) {
        this.run();
    }
}