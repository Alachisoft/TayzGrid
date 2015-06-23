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
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.DeleteCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StatsCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.GetCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.InvalidCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StorageCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.VersionCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.NoOperationCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.FlushCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.MutateCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.QuitCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.binaryprotocol.BinaryResponseBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BinaryResponseManager extends ResponseManager {

    private DataStream _responseStream = new DataStream(10240);

    public BinaryResponseManager(SocketChannel channel) {
        super(channel);
    }

    @Override
    public void Start() {
        try {
            synchronized (this) {
                if (_alive || _responseQueue.isEmpty()) {
                    return;
                }
                _alive = true;
            }
            Process();
        } catch (RuntimeException e) {
            LogManager.getLogger().Error("BinaryResponseManager", "\tFailed to process response. " + e.getMessage());
            return;
        }
    }

    private void Process() {
        boolean go = false;

        do {
            if (_disposed) {
                return;
            }
            AbstractCommand command = _responseQueue.poll();

            byte[] outpuBuffer = BuildBinaryResponse(command);
            try {
                if (this._disposed) {
                    return;
                }
                if (outpuBuffer != null) {
                    try {
                        _socketChannel.write(ByteBuffer.wrap(outpuBuffer));
               
                    } catch (IOException ex) {
                        //log here
                        return;
                        
                    }
                }
                TcpNetworkGateway.s_responseTimeStats.EndSample();
                if (!TcpNetworkGateway.s_jit) {
                    TcpNetworkGateway.s_executionTimeStats.Reset();
                    TcpNetworkGateway.s_parseTimeStats.Reset();
                    TcpNetworkGateway.s_responseTimeStats.Reset();
                    TcpNetworkGateway.s_jit = true;
                }
                if (command.getOpcode() == Opcode.Quit || command.getOpcode() == Opcode.QuitQ || command.disposeClient()) {
                    TcpNetworkGateway.DisposeClient(_memTcpClient);
                    return;
                }
            } catch (RuntimeException e) {
                LogManager.getLogger().Error("BinaryResponseManager", "\tFailed to send response. " + e.getMessage());
                return;
            }

            synchronized (this) {
                _alive = go = _responseQueue.size() > 0;
            }
        } while (go);
    }


    private byte[] BuildBinaryResponse(AbstractCommand command) {
        switch (command.getOpcode()) {
            case Set:
            case Add:
            case Replace:
            case Append:
            case Prepend:
                _responseStream.Write(BinaryResponseBuilder.BuildStorageResponse((StorageCommand) ((command instanceof StorageCommand) ? command : null)));
                break;
            case SetQ:
            case AddQ:
            case ReplaceQ:
            case AppendQ:
            case PrependQ:
                _responseStream.Write(BinaryResponseBuilder.BuildStorageResponse((StorageCommand) ((command instanceof StorageCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case Get:
            case GetK:
                _responseStream.Write(BinaryResponseBuilder.BuildGetResponse((GetCommand) ((command instanceof GetCommand) ? command : null)));
                break;

            case GetQ:
            case GetKQ:
                _responseStream.Write(BinaryResponseBuilder.BuildGetResponse((GetCommand) ((command instanceof GetCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case No_op:
                _responseStream.Write(BinaryResponseBuilder.BuildNoOpResponse((NoOperationCommand) ((command instanceof NoOperationCommand) ? command : null)));
                break;

            case Delete:
                _responseStream.Write(BinaryResponseBuilder.BuildDeleteResponse((DeleteCommand) ((command instanceof DeleteCommand) ? command : null)));
                break;

            case DeleteQ:
                _responseStream.Write(BinaryResponseBuilder.BuildDeleteResponse((DeleteCommand) ((command instanceof DeleteCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case Flush:
                _responseStream.Write(BinaryResponseBuilder.BuildFlushResponse((FlushCommand) ((command instanceof FlushCommand) ? command : null)));
                break;
            case FlushQ:
                _responseStream.Write(BinaryResponseBuilder.BuildFlushResponse((FlushCommand) ((command instanceof FlushCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case Increment:
            case Decrement:
                _responseStream.Write(BinaryResponseBuilder.BuildCounterResponse((MutateCommand) ((command instanceof MutateCommand) ? command : null)));
                break;
            case IncrementQ:
            case DecrementQ:
                _responseStream.Write(BinaryResponseBuilder.BuildCounterResponse((MutateCommand) ((command instanceof MutateCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case Stat:
                _responseStream.Write(BinaryResponseBuilder.BuildStatsResponse((StatsCommand) ((command instanceof StatsCommand) ? command : null)));
                break;

            case Quit:
                _responseStream.Write(BinaryResponseBuilder.BuildQuitResponse((QuitCommand) ((command instanceof QuitCommand) ? command : null)));
                break;
            case QuitQ:
                _responseStream.Write(BinaryResponseBuilder.BuildQuitResponse((QuitCommand) ((command instanceof QuitCommand) ? command : null)));
                if (!command.getExceptionOccured()) {
                    return null;
                }
                break;

            case Version:
                _responseStream.Write(BinaryResponseBuilder.BuildVersionResponse((VersionCommand) ((command instanceof VersionCommand) ? command : null)));
                break;
            default:
                _responseStream.Write(BinaryResponseBuilder.BuildInvalidResponse((InvalidCommand) ((command instanceof InvalidCommand) ? command : null)));
                break;
        }
        return _responseStream.ReadAll();
    }
}