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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.IThreadPoolTask;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.ThreadPool;

import com.alachisoft.tayzgrid.common.stats.*;
import com.alachisoft.tayzgrid.integrations.memcached.provider.CacheFactory;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.MemConfiguration;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TcpNetworkGateway extends NetworkGateway implements IThreadPoolTask {

    ServerSocketChannel _listener;
    private ProtocolType _protocol;
    private Selector _selector;
    private boolean run = true;
    public static HPTimeStats s_parseTimeStats = new HPTimeStats();
    public static HPTimeStats s_executionTimeStats = new HPTimeStats();
    public static HPTimeStats s_responseTimeStats = new HPTimeStats();
    public static boolean s_parsingStarted;
    public static boolean s_jit;
    private boolean _started = false;

    public TcpNetworkGateway(String hostName, int port, ProtocolType protocol) throws IOException {
        try {
            _listener = ServerSocketChannel.open();
            _listener.socket().bind(new InetSocketAddress(hostName, port));
            _protocol = protocol;
            _selector = Selector.open();
        } catch (IOException ex) {
            LogManager.getLogger().Fatal("TcpNetworkGateway", "Unable to initialize TcpNetworkGateway for protocol: " + protocol + "Exception: " + ex);
            try {
                if (_listener != null) {
                    _listener.close();
                }
                if (_selector != null) {
                    _selector.close();
                }
            } catch (Exception e) {
            }
            throw ex;
        }
    }

    @Override
    public void StartListenForClients() {
        try {
            _listener.configureBlocking(false);
            _listener.register(_selector, SelectionKey.OP_ACCEPT);

            synchronized (this) {
                if (!_started) {
                    ThreadPool.ExecuteTask(this);
                }
                _started = true;
            }

        } catch (IOException ex) {
            LogManager.getLogger().Fatal("TcpNetworkGateway", "Failed to start NetworkGateway. " + ex);
        }
    }

    public void run() {
        try {
            LogManager.getLogger().Info("TcpNetworkGateway", "NetworkGateway started listening for clients for protocol : " + _protocol);

            while (run) {
                int readyChannels = _selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = _selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid() && key.isAcceptable()) {
                        try {
                            SocketChannel channel = _listener.accept();
                            MemTcpClient clientHandler = new MemTcpClient(channel, _protocol);
                            _clients.add(clientHandler);
                            channel.configureBlocking(false);
                            channel.register(_selector, SelectionKey.OP_READ, clientHandler);
                        } catch (IOException e) {
                            LogManager.getLogger().Error("TcpNetworkGateway", "Error while accepting new client. " + e);
                        }
                    } else if (key.isValid() && key.isReadable()) {
                        MemTcpClient clientHandler = (MemTcpClient) key.attachment();
                        ModuleStatus status = clientHandler.ReadDataFromSocket();
                        if (status == ModuleStatus.Idle) {
                            ThreadPool.ExecuteTask(clientHandler);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException ex) {
            LogManager.getLogger().Fatal("TcpNetworkGateway", "Error in TcpNetworkGateway. " + ex);
        } finally {
            _started = false;
        }
    }

    @Override
    public void StopListenForClients() {
        this.run = false;
        _started = false;
    }

    @Override
    public void run(Object obj) {
        run();
    }

    public boolean isStarted() {
        return _started;
    }
    
    public void dispose() throws IOException
    {
        _listener.close();
        _selector.close();
    }
}