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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.MemConfiguration;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StatsCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.GetCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.InvalidCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.VersionCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.TouchCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.SlabsAutomoveCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.FlushCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.MutateCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.QuitCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.SlabsReassignCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.VerbosityCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.DeleteCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StorageCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.MemTcpClient;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;

public class TextProtocolParser extends ProtocolParser {

    public TextProtocolParser(DataStream inputStream, MemTcpClient parent) {
        super(inputStream, parent);
    }

    /**
     * Parses an <see
     * cref="com.alachisoft.integrations.memcachedproxy.commands.AbstractCommand"/>
     * from string
     *
     * @param commandString string commandString to be parsed
     */
    public final AbstractCommand Parse(String stringCommand) {
        String[] commandParts = splitStringIn2IgnoreEmpty(stringCommand);

        if (commandParts == null || commandParts.length == 0) {
            return CreateInvalidCommand();
        }

        AbstractCommand command;

        String arguments = null;
        if (commandParts.length > 1) {
            arguments = commandParts[1];
        }

        if (commandParts[0].equals("get")) {
            command = CreateRetrievalCommand(arguments, Opcode.Get);
        } else if (commandParts[0].equals("gets")) {
            command = CreateRetrievalCommand(arguments, Opcode.Gets);
        } else if (commandParts[0].equals("set")) {
            command = CreateStorageCommand(arguments, Opcode.Set);
        } else if (commandParts[0].equals("add")) {
            command = CreateStorageCommand(arguments, Opcode.Add);
        } else if (commandParts[0].equals("replace")) {
            command = CreateStorageCommand(arguments, Opcode.Replace);
        } else if (commandParts[0].equals("append")) {
            command = CreateStorageCommand(arguments, Opcode.Append);
        } else if (commandParts[0].equals("prepend")) {
            command = CreateStorageCommand(arguments, Opcode.Prepend);
        } else if (commandParts[0].equals("cas")) {
            command = CreateStorageCommand(arguments, Opcode.CAS);
        } else if (commandParts[0].equals("delete")) {
            command = CreateDeleteCommand(arguments);
        } else if (commandParts[0].equals("incr")) {
            command = CreateMutateCommand(arguments, Opcode.Increment);
        } else if (commandParts[0].equals("decr")) {
            command = CreateMutateCommand(arguments, Opcode.Decrement);
        } else if (commandParts[0].equals("touch")) {
            command = CreateTouchCommand(arguments);
        } else if (commandParts[0].equals("flush_all")) {
            command = CreateFlushCommand(arguments);
        } else if (commandParts[0].equals("stats")) {
            command = CreateStatsCommand(arguments);
        } else if (commandParts[0].equals("slabs")) {
            command = CreateSlabsCommand(arguments);
        } else if (commandParts[0].equals("version")) {
            command = CreateVersionCommand(arguments);
        } else if (commandParts[0].equals("verbosity")) {
            command = CreateVerbosityCommand(arguments);
        } else if (commandParts[0].equals("quit")) {
            command = CreateQuitCommand();
        } else {
            command = CreateInvalidCommand();
        }
        if (command == null) {
            return new InvalidCommand("CLIENT_ERROR bad command line format");
        }
        return command;
    }

    private QuitCommand CreateQuitCommand() {
        QuitCommand quitCommand = new QuitCommand(Opcode.Quit);
        this.setState(ParserState.ReadyToDispatch);
        return quitCommand;
    }

    private VerbosityCommand CreateVerbosityCommand(String arguments) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            return null;
        }
        String[] argumentsArray = splitStringIgnoreEmpty(arguments);
        if (argumentsArray.length > 2 || argumentsArray.length < 1) {
            return null;
        }

        boolean noreply;

        int level = 0;
        try {
            level = Integer.parseInt(argumentsArray[0]);
            noreply = (argumentsArray.length > 1 && argumentsArray[1].equals("noreply")) ? true : false;
        } catch (RuntimeException e) {
            return null;
        }

        VerbosityCommand verbosiyCommand = new VerbosityCommand(level);
        verbosiyCommand.setNoReply(noreply);
        return verbosiyCommand;
    }

    private VersionCommand CreateVersionCommand(String arguments) {
        if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            return null;
        }

        VersionCommand versionCommand = new VersionCommand();
        return versionCommand;
    }

    private StatsCommand CreateStatsCommand(String arguments) {
        StatsCommand statsCommand;
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            statsCommand = new StatsCommand();
            this.setState(ParserState.ReadyToDispatch);
            return statsCommand;
        }

        String[] argumentsArray = splitStringIgnoreEmpty(arguments);

        String tempVar = argumentsArray[0];
        if (tempVar.equals("settings") || tempVar.equals("items") || tempVar.equals("sizes") || tempVar.equals("slabs")) {
            statsCommand = new StatsCommand(argumentsArray[0]);
        } else {
            return null;
        }
        return statsCommand;
    }

    private AbstractCommand CreateSlabsCommand(String arguments) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            return null;
        }
        String[] argumentsArray = splitStringIgnoreEmpty(arguments);
        if (argumentsArray.length > 3 || argumentsArray.length < 2) {
            return null;
        }
        try {
            boolean noreply;
            String tempVar = argumentsArray[0];
            if (tempVar.equals("reasign")) {
                int sourceClass = Integer.parseInt(argumentsArray[1]);
                int destClass = Integer.parseInt(argumentsArray[2]);
                noreply = argumentsArray.length > 2 && argumentsArray[3].equals("noreply");
                SlabsReassignCommand slabsReassignCommand = new SlabsReassignCommand(sourceClass, destClass);
                slabsReassignCommand.setNoReply(noreply);
                return slabsReassignCommand;
            } else if (tempVar.equals("automove")) {
                int option = Integer.parseInt(argumentsArray[1]);
                noreply = argumentsArray.length > 2 && argumentsArray[2].equals("noreply");
                SlabsAutomoveCommand slabsAutoMoveCommand = new SlabsAutomoveCommand(option);
                slabsAutoMoveCommand.setNoReply(noreply);
                return slabsAutoMoveCommand;
            } else {
                return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private TouchCommand CreateTouchCommand(String arguments) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            return null;
        }

        String[] argumentsArray = splitStringIgnoreEmpty(arguments);
        if (argumentsArray.length > 3 || argumentsArray.length < 2) {
            return null;
        }

        String key;
        long expTime;
        boolean noreply = false;

        try {
            key = argumentsArray[0];
            expTime = Long.parseLong(argumentsArray[1]);
            if (argumentsArray.length > 2 && argumentsArray[2].equals("noreply")) {
                noreply = true;
            }
        } catch (RuntimeException e) {
            return null;
        }

        TouchCommand touchCommand = new TouchCommand(key, expTime);
        touchCommand.setNoReply(noreply);
        return touchCommand;
    }

    private FlushCommand CreateFlushCommand(String arguments) {
        int delay;
        boolean noreply = false;

        String[] argumentsArray;
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            delay = 0;
        } else {
            argumentsArray = splitStringIgnoreEmpty(arguments);
            if (argumentsArray.length > 2) {
                return null;
            }

            try {
                delay = Integer.parseInt(argumentsArray[0]);
                if (argumentsArray.length > 1 && argumentsArray[1].equals("noreply")) {
                    noreply = true;
                }
            } catch (RuntimeException e) {
                return null;
            }
        }

        FlushCommand flushCommand = new FlushCommand(Opcode.Flush, delay);
        flushCommand.setNoReply(noreply);
        return flushCommand;
    }

    private MutateCommand CreateMutateCommand(String arguments, Opcode cmdType) {
        String[] argumentsArray = splitStringIgnoreEmpty(arguments);//= arguments.split(new char[] {' '}, StringSplitOptions.RemoveEmptyEntries);

        if (argumentsArray.length > 3 || argumentsArray.length < 2) {
            return null;
        }

        MutateCommand mutateCommand = new MutateCommand(cmdType);

        try {
            mutateCommand.setKey(argumentsArray[0]);
            mutateCommand.setDelta(Long.parseLong(argumentsArray[1]));
            if (argumentsArray.length > 2 && argumentsArray[2].equals("noreply")) {
                mutateCommand.setNoReply(true);
            }
            if(mutateCommand.getDelta()<0)
            {
                mutateCommand.setDelta(0);
                mutateCommand.setErrorMessage("CLIENT_ERROR invalid numeric delta argument");
            }
        } catch (RuntimeException e) {
            mutateCommand.setErrorMessage("CLIENT_ERROR bad command line format");
        }
        return mutateCommand;
    }

    private DeleteCommand CreateDeleteCommand(String arguments) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(arguments)) {
            return null;
        }

        String[] argumentsArray = splitStringIgnoreEmpty(arguments);
        if (argumentsArray.length > 2 || argumentsArray.length < 1) {
            return null;
        }

        DeleteCommand deleteCommand = new DeleteCommand(Opcode.Delete);
        switch (argumentsArray.length) {
            case 1:
                deleteCommand.setKey(argumentsArray[0]);
                break;
            case 2:
                if (!argumentsArray[1].equals("noreply")) {
                    deleteCommand.setErrorMessage("CLIENT_ERROR bad command line format. Usage: delete <key> [noreply]");
                } else {
                    deleteCommand.setKey(argumentsArray[0]);
                    deleteCommand.setNoReply(true);
                }
                break;
            default:
                deleteCommand.setErrorMessage("CLIENT_ERROR bad command line format. Usage: delete <key> [noreply]");
                break;
        }
        return deleteCommand;
    }

    private GetCommand CreateRetrievalCommand(String arguments, Opcode cmdType) {
        if (arguments == null) {
            return null;
        }

        String[] argumentsArray = splitStringIgnoreEmpty(arguments);//= arguments.split(new char[] {' '}, StringSplitOptions.RemoveEmptyEntries);
        if (argumentsArray.length == 0) {
            return null;
        }
        GetCommand getCommand = new GetCommand(cmdType);
        getCommand.setKeys(argumentsArray);
        return getCommand;
    }

    private StorageCommand CreateStorageCommand(String arguments, Opcode cmdType) {
        if (arguments == null) {
            return null;
        }

        String[] argumentsArray = splitStringIgnoreEmpty(arguments);
        if (argumentsArray.length < 4 || argumentsArray.length > 6) {
            return null;
        } else {
            try {
                String key = argumentsArray[0];
                int flags = Integer.parseInt(argumentsArray[1]);
                long expTime = Long.parseLong(argumentsArray[2]);
                int bytes = Integer.parseInt(argumentsArray[3]);
                if (bytes < 0) {
                    return null;
                }

                StorageCommand storageCommand;
                if (cmdType == Opcode.CAS) {
                    long casUnique = Long.parseLong(argumentsArray[4]);
                    storageCommand = new StorageCommand(cmdType, key, flags, expTime, casUnique, bytes);
                    if (argumentsArray.length > 5 && argumentsArray[5].equals("noreply")) {
                        storageCommand.setNoReply(true);
                    }
                } else {
                    storageCommand = new StorageCommand(cmdType, key, flags, expTime, bytes);
                    if (argumentsArray.length > 4 && argumentsArray[4].equals("noreply")) {
                        storageCommand.setNoReply(true);
                    }
                }
                return storageCommand;
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    private InvalidCommand CreateInvalidCommand(String error) {
        InvalidCommand command = new InvalidCommand(error);
        this.setState(ParserState.ReadyToDispatch);
        return command;
    }

    private InvalidCommand CreateInvalidCommand() {
        return CreateInvalidCommand(null);
    }

    public final void Build(StorageCommand storageCommand, byte[] data) {
        if ((char) data[storageCommand.getDataSize()] != '\r' || (char) data[storageCommand.getDataSize() + 1] != '\n') {
            storageCommand.setErrorMessage("CLIENT_ERROR bad data chunk");
        } else {
            byte[] dataToStore = new byte[storageCommand.getDataSize()];
            System.arraycopy(data, 0, dataToStore, 0, storageCommand.getDataSize());
            storageCommand.setDataBlock((Object) dataToStore);
        }
        this.setState(ParserState.ReadyToDispatch);
    }

    public final int getCommandDataSize(AbstractCommand command) {
        if(command instanceof StorageCommand)
            return ((StorageCommand)command).getDataSize();
        return -2;
    }

    @Override
    public void StartParser() {
        try {
            String commandString;
            int noOfBytesRead;
            boolean go = true;

            do {
                synchronized (this) {
                    if (_inputDataStream.getLenght() == 0 && this.getState() != ParserState.ReadyToDispatch) {
                        this.setAlive(false);
                        return;
                    }
                }
                noOfBytesRead = 0;
                switch (this.getState()) {
                    case Ready:
                        noOfBytesRead = _inputDataStream.Read(_rawData, _rawDataOffset, 1);
                        _rawDataOffset += noOfBytesRead;

                        if (_rawDataOffset > 1 && (char) _rawData[_rawDataOffset - 2] == '\r' && (char) _rawData[_rawDataOffset - 1] == '\n') {
                            commandString = BinaryConverter.GetString(_rawData, 0, (_rawDataOffset - 2));
                            _command = this.Parse(commandString);
                            _rawDataOffset = 0;
                            if (this.getCommandDataSize(_command) < 0) {
                                this.setState(ParserState.ReadyToDispatch);
                            } else {
                                this.setState(ParserState.WaitingForData);
                            }
                        }
                        if (_rawDataOffset == _rawData.length) {
                            byte[] newBuffer = new byte[_rawData.length * 2];
                            System.arraycopy(_rawData, 0, newBuffer, 0, _rawData.length);
                            _rawData = newBuffer;
                        }
                        if (_rawDataOffset > MemConfiguration.getMaximumCommandLength()) {
                            LogManager.getLogger().Fatal("TextProtocolParser", "Command length exceeded maximum allowed command lenght.");
                            TcpNetworkGateway.DisposeClient(_memTcpClient);
                            return;
                        }
                        break;
                    case WaitingForData:
                        noOfBytesRead = _inputDataStream.Read(_rawData, _rawDataOffset, this.getCommandDataSize(_command) + 2 - _rawDataOffset);
                        _rawDataOffset += noOfBytesRead;

                        if (_rawDataOffset == this.getCommandDataSize(_command) + 2) {
                            _rawDataOffset = 0;
                            if (_command instanceof StorageCommand) {
                                this.Build((StorageCommand) _command, _rawData);
                            }
                        }
                        break;
                    case ReadyToDispatch:
                        ModuleStatus executionMgrStatus = _commandConsumer.RegisterCommand(_command);
                        this.setState(ParserState.Ready);
                        go = executionMgrStatus == ModuleStatus.Running;
                        break;
                }
            } while (go);
        } catch (RuntimeException e) {
            LogManager.getLogger().Fatal("TextProtocolParser", "Exception occured while parsing text command. " + e.getMessage());
            TcpNetworkGateway.DisposeClient(_memTcpClient);
            return;
        }
        this.Dispatch();
    }

    private String[] splitStringIn2IgnoreEmpty(String value) {
        String[] result = value.split(" +", 2);
        if (result[0].equals("")) {
            if (result.length > 1) {
                result = result[1].split(" +", 2);
            } else {
                return null;
            }
        }
        return result;
    }

    private String[] splitStringIgnoreEmpty(String value) {
        String[] result = value.split(" +");
        if (result[0].equals("")) {
            String[] newResult = new String[result.length - 1];
            System.arraycopy(result, 1, newResult, 0, result.length - 1);
            result = newResult;
        }
        return result;
    }
}