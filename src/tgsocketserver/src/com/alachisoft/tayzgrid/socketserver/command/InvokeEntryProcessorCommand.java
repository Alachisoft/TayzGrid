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
package com.alachisoft.tayzgrid.socketserver.command;

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.InvokeEntryProcessorProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessor;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.InvokeEntryProcessorResponseBuilder;
import java.io.IOException;

public class InvokeEntryProcessorCommand extends CommandBase {

    protected final static class CommandInfo {

        public String RequestId;
        public Object[] Keys;
        private EntryProcessor entryProcessor;
        private Object[] arguments;
        public String defaultReadThru;
        public String defaultWriteThru;

        public long ClientLastViewId;
        public int CommandVersion;
        public String IntendedRecipient;

        public CommandInfo clone() {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Keys = this.Keys;
            varCopy.ClientLastViewId = this.ClientLastViewId;
            varCopy.CommandVersion = this.CommandVersion;
            varCopy.IntendedRecipient = this.IntendedRecipient;

            return varCopy;
        }
    }

    private OperationResult invokeEntryProcessorResult = OperationResult.Success;

    @Override
    public OperationResult getOperationResult() {
        return invokeEntryProcessorResult;
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
        CommandInfo cmdInfo = new CommandInfo();
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);

        try {
            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId());
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("InvokeEPCmd.Exec", "cmd parsed");
            }

        } catch (Exception exc) {
            invokeEntryProcessorResult = OperationResult.Failure;            
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));           
            return;
        }

        try {
            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);

            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(cmdInfo.IntendedRecipient)) {
                operationContext.Add(OperationContextFieldName.IntendedRecipient, cmdInfo.IntendedRecipient);
            }

            java.util.HashMap getResult = (java.util.HashMap) nCache.getCache().invokeEntryProcessor(cmdInfo.Keys, cmdInfo.entryProcessor, cmdInfo.arguments, cmdInfo.defaultReadThru, cmdInfo.defaultWriteThru, operationContext);

            InvokeEntryProcessorResponseBuilder.BuildResponse(getResult, cmdInfo.CommandVersion, cmdInfo.RequestId, _serializedResponsePackets, cmdInfo.IntendedRecipient, nCache.getCacheId());
        } catch (Exception exc) {
            invokeEntryProcessorResult = OperationResult.Failure;
            //PROTOBUF:RESPONSE
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("InvokeEPCmd.Exec", "cmd executed on cache");
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
        CommandInfo cmdInfo = new CommandInfo();

        InvokeEntryProcessorProtocol.InvokeEntryProcessorCommand invokeEntryProcessorCommand = command.getInvokeEntryProcessorCommand();
        Object[] keys = new Object[invokeEntryProcessorCommand.getKeysList().size()];
        Object[] arguments = null;
        for (int i = 0; i < invokeEntryProcessorCommand.getKeysList().size(); i++) {
            keys[i] = CacheKeyUtil.Deserialize(invokeEntryProcessorCommand.getKeysList().get(i), serializationContext);
        }
        if (invokeEntryProcessorCommand.getArgumentsCount() > 0) {
            arguments = new Object[invokeEntryProcessorCommand.getArgumentsCount()];

            for (int i = 0; i < invokeEntryProcessorCommand.getArgumentsList().size(); i++) {
                arguments[i] = CacheKeyUtil.Deserialize(invokeEntryProcessorCommand.getArgumentsList().get(i), serializationContext);
            }
        }

        cmdInfo.Keys = keys;
        cmdInfo.entryProcessor = (EntryProcessor) CompactBinaryFormatter.fromByteBuffer(invokeEntryProcessorCommand.getEntryprocessor().toByteArray(), serializationContext);
        cmdInfo.arguments = arguments;
        cmdInfo.defaultReadThru = invokeEntryProcessorCommand.getDefaultReadThru();
        cmdInfo.defaultWriteThru = invokeEntryProcessorCommand.getDefaultWriteThru();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        cmdInfo.ClientLastViewId = command.getClientLastViewId();
        cmdInfo.CommandVersion = command.getCommandVersion();

        return cmdInfo;
    }

}
