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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.VerbosityCommand;
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
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.MemTcpClient;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.binaryprotocol.BinaryRequestHeader;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.binaryprotocol.Magic;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;

public class BinaryProtocolParser extends ProtocolParser {

    private BinaryRequestHeader _requestHeader;

    public BinaryProtocolParser(DataStream inputSream, MemTcpClient parent) {
        super(inputSream, parent);
    }

    private int getCommandDataSize(BinaryRequestHeader header) {
        return header == null ? 0 : header.getTotalBodyLenght();
    }

    @Override
    public void StartParser() {
        try {
            int noOfBytes = 0;
            boolean go = true;

            do {
                synchronized (this) {
                    if (_inputDataStream.getLenght() == 0
                            && this.getState() != ParserState.ReadyToDispatch
                            && !(this.getState() == ParserState.WaitingForData && this.getCommandDataSize(_requestHeader) == 0)) {
                        this.setAlive(false);
                        return;
                    }
                }
                switch (this.getState()) {
                    case Ready:
                        noOfBytes = _inputDataStream.Read(_rawData, _rawDataOffset, 24 - _rawDataOffset);
                        _rawDataOffset += noOfBytes;
                        if (_rawDataOffset == 24) {
                            _rawDataOffset = 0;
                            _requestHeader = this.ParseRequestHeader();

                            boolean valid = isValidHeader(_requestHeader);
                            if (!valid) {
                                _requestHeader = new BinaryRequestHeader();
                                _requestHeader.setOpcode(Opcode.Invalid_Command);
                            }
                            this.setState(ParserState.WaitingForData);
                        }
                        break;
                    case WaitingForData:
                        if (this.getCommandDataSize(_requestHeader) > 0) {
                            noOfBytes = _inputDataStream.Read(_rawData, _rawDataOffset, this.getCommandDataSize(_requestHeader) - _rawDataOffset);
                            _rawDataOffset += noOfBytes;
                        }
                        if (_rawDataOffset >= this.getCommandDataSize(_requestHeader)) {
                            _rawDataOffset = 0;
                            _command = this.BuildBinaryCommand(_requestHeader);
                            this.setState(ParserState.ReadyToDispatch);
                        }
                        break;
                    case ReadyToDispatch:
                        TcpNetworkGateway.s_parseTimeStats.EndSample();
                        TcpNetworkGateway.s_parsingStarted = false;
                        TcpNetworkGateway.s_executionTimeStats.BeginSample();

                        ModuleStatus executionMgrStatus = _commandConsumer.RegisterCommand(_command);
                        this.setState(ParserState.Ready);
                        go = executionMgrStatus == ModuleStatus.Running;
                        break;
                }

            } while (go);
        } catch (RuntimeException e) {
            LogManager.getLogger().Fatal("BinaryProtocolParser", "Exception occured while parsing text command. " + e.getMessage());
            TcpNetworkGateway.DisposeClient(_memTcpClient);
            return;
        }
        this.Dispatch();
    }

    private BinaryRequestHeader ParseRequestHeader() {
        BinaryRequestHeader header = new BinaryRequestHeader(_rawData);
        // this.setState(ParserState.WaitingForData);
        return header;
    }

    private boolean isValidHeader(BinaryRequestHeader requestHeader) {
        boolean valid = true;
        if (requestHeader.getTotalBodyLenght() < requestHeader.getKeyLength() + requestHeader.getExtraLength()) {
            valid = false;
        }
        if (requestHeader.getMagicByte() != Magic.Request) {
            valid = false;
        }
        return valid;


    }

    private AbstractCommand BuildBinaryCommand(BinaryRequestHeader header) {
        AbstractCommand command;
        switch (header.getOpcode()) {
            //Get command
            case Get:
            //GetK command
            case GetK:
                command = CreateGetCommand(header, false);
                break;

            //Set command
            case Set:
            //Add command
            case Add:
            //Replace command
            case Replace:
            //Append command
            case Append:
            //Prepend command
            case Prepend:
                command = CreateStorageCommand(header, false);
                break;

            //Delete command
            case Delete:
                command = CreateDeleteCommand(header, false);
                break;

            //Increment command
            case Increment:
            //Decrement command
            case Decrement:
                MutateCommand mutateCmd = CreateMutateCommand(header, false);
                if(mutateCmd.getDelta()<0)
                {
                    mutateCmd.setDelta(0);
                    mutateCmd.setErrorMessage("CLIENT_ERROR invalid numeric delta argument");
                }
                command=mutateCmd;
                break;

            //Quit command
            case Quit:
                command = new QuitCommand(header.getOpcode());
                break;

            //Flush command
            case Flush:
                command = CreateFlushCommand(header, false);
                break;

            //GetQ command
            case GetQ:
            //GetKQ command
            case GetKQ:
                command = CreateGetCommand(header, true);
                break;

            //No-op command
            case No_op:
                command = new NoOperationCommand();
                break;

            //Version command
            case Version:
                command = CreateVersionCommand();
                break;

            //Stat command
            case Stat:
                command = CreateStatsCommand(header);
                break;

            //SetQ command
            case SetQ:
            //AddQ command
            case AddQ:
            //ReplaceQ command
            case ReplaceQ:
            //AppendQ command
            case AppendQ:
            //PrependQ command
            case PrependQ:
                command = CreateStorageCommand(header, true);
                break;

            //DeleteQ command
            case DeleteQ:
                command = CreateDeleteCommand(header, true);
                break;

            //IncrementQ command
            case IncrementQ:
            //DecrementQ command
            case DecrementQ:
                MutateCommand mutateCmdQ = CreateMutateCommand(header, true);
                if(mutateCmdQ.getDelta()<0)
                {
                    mutateCmdQ.setDelta(0);
                    mutateCmdQ.setErrorMessage("CLIENT_ERROR invalid numeric delta argument");
                }
                command=mutateCmdQ;
                break;

            //QuitQ command
            case QuitQ:
                command = new QuitCommand(header.getOpcode());
                command.setNoReply(true);
                break;

            //FlushQ command
            case FlushQ:
                command = CreateFlushCommand(header, true);
                break;

            default:
                command = CreateInvalidCommand();
                command.setOpcode(Opcode.unknown_command);
                break;
        }

        command.setOpaque(header.getOpaque());
        return command;

    }

    private StorageCommand CreateStorageCommand(BinaryRequestHeader header, boolean noreply) {
        int offset = 0;
        int flags = 0;
        long exp = 0;

        switch (header.getOpcode()) {
            case Append:
            case Prepend:
            case AppendQ:
            case PrependQ:
                break;
            default:
                flags = BinaryConverter.ToInt32(_rawData, offset);
                exp = (long) BinaryConverter.ToInt32(_rawData, offset + 4);
                offset += 8;
                break;
        }
        String key = BinaryConverter.GetString(_rawData, offset, header.getKeyLength());
        byte[] value = new byte[header.getValueLength()];

        System.arraycopy(_rawData, header.getKeyLength() + offset, value, 0, header.getValueLength());

        StorageCommand cmd = new StorageCommand(header.getOpcode(), key, flags, exp, header.getCAS(), header.getValueLength());
        cmd.setDataBlock(value);
        cmd.setNoReply(noreply);
        cmd.setOpaque(header.getOpaque());
        return cmd;
    }

    private GetCommand CreateGetCommand(BinaryRequestHeader header, boolean noreply) {
        String key = BinaryConverter.GetString(_rawData, 0, header.getKeyLength());
        GetCommand cmd = new GetCommand(header.getOpcode());
        cmd.setKeys(new String[]{key});
        cmd.setNoReply(noreply);
        return cmd;

    }

    private DeleteCommand CreateDeleteCommand(BinaryRequestHeader header, boolean noreply) {
        String key = BinaryConverter.GetString(_rawData, 0, header.getKeyLength());
        DeleteCommand cmd = new DeleteCommand(header.getOpcode());
        cmd.setKey(key);
        cmd.setNoReply(noreply);
        cmd.setCAS(header.getCAS());
        return cmd;
    }

    private MutateCommand CreateMutateCommand(BinaryRequestHeader header, boolean noreply) {
        MutateCommand cmd = new MutateCommand(header.getOpcode());
        cmd.setDelta(BinaryConverter.ToInt64(_rawData, 0));
        cmd.setInitialValue(BinaryConverter.ToInt64(_rawData, 8));
        cmd.setExpirationTimeInSeconds(BinaryConverter.ToUInt32(_rawData, 16));
        cmd.setKey(BinaryConverter.GetString(_rawData, 20, header.getKeyLength()));
        cmd.setCAS(header.getCAS());
        cmd.setNoReply(noreply);
        return cmd;
    }

    public final FlushCommand CreateFlushCommand(BinaryRequestHeader header, boolean noreply) {
        int delay = 0;
        if (header.getExtraLength() > 0) {
            delay = ((_rawData[0] << 24) | (_rawData[1] << 16) | (_rawData[2] << 8) | _rawData[3]);
        }
        FlushCommand cmd = new FlushCommand(header.getOpcode(), delay);
        cmd.setNoReply(noreply);
        return cmd;
    }

    public final VersionCommand CreateVersionCommand() {
        return new VersionCommand();
    }

    public final StatsCommand CreateStatsCommand(BinaryRequestHeader header) {
        String argument = null;
        if (header.getKeyLength() > 0) {
            argument = BinaryConverter.GetString(_rawData, 0, header.getKeyLength());
        }
        return new StatsCommand(argument);
    }

    public final VerbosityCommand CreateVerbosityCommand() {
        int level = ((_rawData[0] << 24) | (_rawData[1] << 16) | (_rawData[2] << 8) | _rawData[3]);
        return new VerbosityCommand(level);
    }

    public final InvalidCommand CreateInvalidCommand() {
        return new InvalidCommand();
    }
}