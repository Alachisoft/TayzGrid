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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway;

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.responsemanagement.TextResponseManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.responsemanagement.BinaryResponseManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.responsemanagement.ResponseManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.parsing.ProtocolParser;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.parsing.TextProtocolParser;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.parsing.BinaryProtocolParser;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.executionmanagement.ExecutionManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.executionmanagement.SequentialExecutionManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.IThreadPoolTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MemTcpClient implements IThreadPoolTask {

    private final ProtocolParser _parser;
    private ResponseManager _responseManager;
    protected ExecutionManager _executionManager;
    protected SocketChannel _socketChannel;
    protected ByteBuffer _inputBuffer;
    protected static final int MAX_BUFFER_SIZE = 10240;
    protected DataStream _inputDataStream;
    private boolean _disposed = false;
    private ProtocolType _protocol;

    public final ProtocolType getProtocol() {
        return _protocol;
    }

    /**
     * Initialize and start new MemTcpClient
     *
     * @param socketChannel SocketChannel of new connected client
     */
    public MemTcpClient(SocketChannel socketChannel, ProtocolType protocol) {
        LogManager.getLogger().Info("MemTcpClient", "\tInitializing new " + protocol.toString() + " client.");

        _socketChannel = socketChannel;
        _inputBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        _inputDataStream = new DataStream();
        _protocol = protocol;

        if (protocol == ProtocolType.Text) {
            _parser = new TextProtocolParser(_inputDataStream, this);
            _responseManager = new TextResponseManager(_socketChannel);
        } else {
            _parser = new BinaryProtocolParser(_inputDataStream, this);
            _responseManager = new BinaryResponseManager(_socketChannel);
        }
        _executionManager = new SequentialExecutionManager();

        _responseManager.setMemTcpClient(this);
        _parser.setCommandConsumer(_executionManager);
        _executionManager.setCommandConsumer(_responseManager);
    }

    public final ModuleStatus ReadDataFromSocket() {
        try {

            if (!TcpNetworkGateway.s_parsingStarted) {
                TcpNetworkGateway.s_parseTimeStats.BeginSample();
                TcpNetworkGateway.s_parsingStarted = true;
            }
            if (_disposed) {
                return ModuleStatus.Finished;
            }

            int bytesRecieved = 0;
            try {
                _inputBuffer.clear();
                bytesRecieved = _socketChannel.read(_inputBuffer);
                if (bytesRecieved <= 0) {
                    TcpNetworkGateway.DisposeClient(this);
                    return ModuleStatus.Finished;
                }
            } catch (IOException ex) {
                LogManager.getLogger().Warn("MemTcpClient", "Failed to read data from client. Exception: " + ex);
                TcpNetworkGateway.DisposeClient(this);
                return ModuleStatus.Finished;
            }
            _inputDataStream.Write(_inputBuffer.array(), 0, bytesRecieved);
            synchronized (_parser) {
                if (_parser.isAlive()) {
                    return ModuleStatus.Running;
                }
                return ModuleStatus.Idle;
            }
        } catch (RuntimeException e) {
            if(!_disposed)
            {
                LogManager.getLogger().Error("MemTcpClient", "\tError in client handler. Exception: " + e);
                TcpNetworkGateway.DisposeClient(this);
            }
            return ModuleStatus.Finished;
        }
    }

    public final void dispose() {
        _disposed = true;
        _parser.setDisposed(true);
        _executionManager.setDisposed(true);
        _responseManager.setDisposed(true);

        if (_socketChannel != null) {
            try {
                _socketChannel.close();
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public void run(Object obj) {
        run();
    }

    @Override
    public void run() {
        synchronized (_parser) {
            if (_parser.isAlive()) {
                return;
            }
            _parser.setAlive(true);
        }
        _parser.StartParser();
    }
}