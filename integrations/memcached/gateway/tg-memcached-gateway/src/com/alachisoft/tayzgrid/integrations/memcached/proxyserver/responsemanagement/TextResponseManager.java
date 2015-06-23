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

import com.alachisoft.tayzgrid.integrations.memcached.provider.Result;
import com.alachisoft.tayzgrid.integrations.memcached.provider.GetOpResult;
import com.alachisoft.tayzgrid.integrations.memcached.provider.MutateOpResult;
import com.alachisoft.tayzgrid.integrations.memcached.provider.OperationResult;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.GetCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;


public class TextResponseManager extends ResponseManager {

    public TextResponseManager(SocketChannel channel) {
        super(channel);

    }

    @Override
    public void Start() {

        synchronized (this) {
            if (_alive || _responseQueue.isEmpty()) {
                return;
            }
            _alive = true;
        }

        boolean go = false;

        do {
            AbstractCommand command = _responseQueue.poll();
            byte[] outpuBuffer = BuildTextResponse(command);

            try {
                if (super._disposed) {
                    return;
                }
                try {
                    _socketChannel.write(ByteBuffer.wrap(outpuBuffer));
                } catch (IOException ex) {
                    //log here
                    return;
                    //Logger.getLogger(TextResponseManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                if(command.disposeClient())
                    TcpNetworkGateway.DisposeClient(_memTcpClient);
            } catch (RuntimeException e) {
                LogManager.getLogger().Error("TextResponseManager", "\tFailed to send response. " + e.getMessage());
                return;
            }

            synchronized (this) {
                _alive = go = _responseQueue.size() > 0;
            }
        } while (go);
    }

    private byte[] BuildTextResponse(AbstractCommand command) {
        if (command.getNoReply()) {
            return new byte[]{};
        }

        if (command.getExceptionOccured()) {
            return BinaryConverter.GetBytes("SERVER_ERROR " + command.getErrorMessage() + "\r\n");
        }
        if (command.getErrorMessage() != null) {
            return BinaryConverter.GetBytes(command.getErrorMessage() + "\r\n");
        }

        DataStream resultStream = new DataStream();

        switch (command.getOpcode()) {
            case Set:
            case Add:
            case Replace:
            case Append:
            case Prepend:
                if (command.getOperationResult().getReturnResult() == Result.SUCCESS) {
                    resultStream.Write(BinaryConverter.GetBytes("STORED\r\n"));
                } else {
                    resultStream.Write(BinaryConverter.GetBytes("NOT_STORED\r\n"));
                }
                break;
            case CAS:
                switch (command.getOperationResult().getReturnResult()) {
                    case SUCCESS:
                        resultStream.Write(BinaryConverter.GetBytes("STORED\r\n"));
                        break;
                    case ITEM_MODIFIED:
                        resultStream.Write(BinaryConverter.GetBytes("EXISTS\r\n"));
                        break;
                    case ITEM_NOT_FOUND:
                        resultStream.Write(BinaryConverter.GetBytes("NOT_FOUND\r\n"));
                        break;
                    default:
                        break;
                }
                break;
            case Get:
            case Gets:
                java.util.List<GetOpResult> results = ((GetCommand)command).getResults();
                for (GetOpResult result : results) {
                    if (result == null) {
                        continue;
                    }
                    Object tempVar = result.getReturnValue();
                    byte[] value = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                    String valueString = null;
                    if (command.getOpcode() == Opcode.Get) {
                        valueString = String.format("VALUE %1$s %2$s %3$s\r\n", result.getKey(), result.getFlag(), value.length);
                    } else {
                        valueString = String.format("VALUE %1$s %2$s %3$s %4$s\r\n", result.getKey(), result.getFlag(), value.length, result.getVersion());
                    }
                    resultStream.Write(BinaryConverter.GetBytes(valueString));
                    resultStream.Write(value);
                    resultStream.Write(BinaryConverter.GetBytes("\r\n"));
                }
                resultStream.Write(BinaryConverter.GetBytes("END\r\n"));
                break;
            case Increment:
            case Decrement:
                switch (command.getOperationResult().getReturnResult()) {
                    case SUCCESS:
                        OperationResult tempVar2 = command.getOperationResult();
                        long value = (long) ((MutateOpResult) ((tempVar2 instanceof MutateOpResult) ? tempVar2 : null)).getMutateResult();
                        resultStream.Write(BinaryConverter.GetBytes((new Long(value)).toString() + "\r\n"));
                        break;
                    case ITEM_TYPE_MISMATCHED:
                        resultStream.Write(BinaryConverter.GetBytes("CLIENT_ERROR cannot increment or decrement non-numeric value\r\n"));
                        break;
                    case ITEM_NOT_FOUND:
                        resultStream.Write(BinaryConverter.GetBytes("NOT_FOUND\r\n"));
                        break;
                    default:
                        resultStream.Write(BinaryConverter.GetBytes("ERROR\r\n"));
                        break;
                }
                break;
            case Delete:
                if (command.getOperationResult().getReturnResult() == Result.SUCCESS) {
                    resultStream.Write(BinaryConverter.GetBytes("DELETED\r\n"));
                } else {
                    resultStream.Write(BinaryConverter.GetBytes("NOT_FOUND\r\n"));
                }
                break;
            case Touch:
                if (command.getOperationResult().getReturnResult() == Result.SUCCESS) {
                    resultStream.Write(BinaryConverter.GetBytes("TOUCHED\r\n"));
                } else {
                    resultStream.Write(BinaryConverter.GetBytes("NOT_FOUND\r\n"));
                }
                break;
            case Flush:
                resultStream.Write(BinaryConverter.GetBytes("OK\r\n"));
                break;
            case Version:
                Object tempVar3 = command.getOperationResult().getReturnValue();
                String version = (String) ((tempVar3 instanceof String) ? tempVar3 : null);
                resultStream.Write(BinaryConverter.GetBytes(version + "\r\n"));
                break;
            case Verbosity:
            case Slabs_Reassign:
            case Slabs_Automove:
                if (command.getOperationResult().getReturnResult() == Result.SUCCESS) {
                    resultStream.Write(BinaryConverter.GetBytes("OK\r\n"));
                } else {
                    resultStream.Write(BinaryConverter.GetBytes("ERROR\r\n"));
                }
                break;
            case Stat:
                Object tempVar4 = command.getOperationResult().getReturnValue();
                java.util.Hashtable stats = (java.util.Hashtable) ((tempVar4 instanceof java.util.Hashtable) ? tempVar4 : null);
                if (stats == null) {
                    resultStream.Write(BinaryConverter.GetBytes("END\r\n"));
                    break;
                }
                java.util.Iterator ie = stats.entrySet().iterator();
                String statString = null;
                while (ie.hasNext()) 
                {
                    Map.Entry e=(Map.Entry)ie.next();
                    statString = String.format("STAT %1$s %2$s\r\n", e.getKey(), e.getValue());
                    resultStream.Write(BinaryConverter.GetBytes(statString));
                }
                resultStream.Write(BinaryConverter.GetBytes("END\r\n"));
                break;
            case Quit:
                TcpNetworkGateway.DisposeClient(_memTcpClient);
                break;
        }

        return resultStream.ReadAll();
    }
}