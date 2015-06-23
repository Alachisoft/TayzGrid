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

import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.BulkGetCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.BulkGetResponseBuilder;
import com.google.protobuf.ByteString;
import java.io.IOException;


public class BulkGetCommand extends CommandBase {
	protected final static class CommandInfo {
		public String RequestId;
		public Object[] Keys;
		public BitSet FlagMap;
		public String providerName;
		public long ClientLastViewId;
		public int CommandVersion;
		public String IntendedRecipient;
                
                
                public boolean isAsync;
                public short jCacheListenerId;
                public boolean replaceExistingValues;
              

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Keys = this.Keys;
			varCopy.FlagMap = this.FlagMap;
			varCopy.providerName = this.providerName;
			varCopy.ClientLastViewId = this.ClientLastViewId;
			varCopy.CommandVersion = this.CommandVersion;
			varCopy.IntendedRecipient = this.IntendedRecipient;
                        
                        varCopy.isAsync = this.isAsync;
                        varCopy.jCacheListenerId = this.jCacheListenerId;
                        varCopy.replaceExistingValues = this.replaceExistingValues;
                   

			return varCopy;
		}
	}

	private OperationResult _getBulkResult = OperationResult.Success;

	@Override
	public OperationResult getOperationResult() {
		return _getBulkResult;
	}

	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();
		ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
                
		try {
			cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
			if (ServerMonitor.getMonitorActivity()) {
				ServerMonitor.LogClientActivity("BulkGetCmd.Exec", "cmd parsed");
			}

		} catch (Exception exc) {
			_getBulkResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		byte[] data = null;

                if(!cmdInfo.isAsync) {
                    try {
                            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                            operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);
////                            
//                            operationContext.Add(OperationContextFieldName.IsAsync, cmdInfo.isAsync);
//                            operationContext.Add(OperationContextFieldName.JCacheLoadAllNotify, cmdInfo.jCacheListenerId);
//                            operationContext.Add(OperationContextFieldName.ReplaceExistingValues, cmdInfo.replaceExistingValues);
//                     
                            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(cmdInfo.IntendedRecipient)) {
                                    operationContext.Add(OperationContextFieldName.IntendedRecipient, cmdInfo.IntendedRecipient);
                            }

                            java.util.HashMap getResult = (java.util.HashMap)nCache.getCache().GetBulk(cmdInfo.Keys, cmdInfo.FlagMap, cmdInfo.providerName, operationContext);

                            BulkGetResponseBuilder.BuildResponse(getResult, cmdInfo.CommandVersion, cmdInfo.RequestId, _serializedResponsePackets, cmdInfo.IntendedRecipient, nCache.getCacheId());
                    } catch (Exception exc) {
                            _getBulkResult = OperationResult.Failure;
                            //PROTOBUF:RESPONSE
                            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
                    }
                    if (ServerMonitor.getMonitorActivity()) {
                            ServerMonitor.LogClientActivity("BulkGetCmd.Exec", "cmd executed on cache");
                    }
                }
                else {
                    OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                    operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);

                    operationContext.Add(OperationContextFieldName.IsAsync, cmdInfo.isAsync);
                    
                    operationContext.Add(OperationContextFieldName.ReplaceExistingValues, cmdInfo.replaceExistingValues);
                    operationContext.Add(OperationContextFieldName.JCacheLoader, true);
                    
                     if (cmdInfo.jCacheListenerId != -1) {
                            AsyncCallbackInfo onAsyncOperationCompleteCallback = new AsyncCallbackInfo((cmdInfo.RequestId != null) ? Integer.decode(cmdInfo.RequestId) : 0, clientManager.getClientID(), cmdInfo.jCacheListenerId);
                            operationContext.Add(OperationContextFieldName.LoadAllNotificationId, onAsyncOperationCompleteCallback);
                    }
         
                    
                    nCache.getCache().AsyncGetBulk(cmdInfo.Keys, cmdInfo.FlagMap, cmdInfo.providerName, operationContext);

                }

	}


	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
		CommandInfo cmdInfo = new CommandInfo();

		BulkGetCommandProtocol.BulkGetCommand bulkGetCommand = command.getBulkGetCommand();
                Object[] keys = new Object[bulkGetCommand.getKeysList().size()];
                for(int i =0;i<bulkGetCommand.getKeysList().size();i++)
                    keys[i]=CacheKeyUtil.Deserialize(bulkGetCommand.getKeysList().get(i), serializationContext);
		cmdInfo.Keys = keys;
		cmdInfo.providerName = bulkGetCommand.getProviderName();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.FlagMap = new BitSet((byte)(bulkGetCommand.getFlag()));
		cmdInfo.ClientLastViewId = command.getClientLastViewId();
		cmdInfo.CommandVersion = command.getCommandVersion();
                
                cmdInfo.isAsync = bulkGetCommand.getIsAsync();
                cmdInfo.jCacheListenerId = (short) bulkGetCommand.getJCacheListenerId();
                cmdInfo.replaceExistingValues = bulkGetCommand.getReplaceExistingValues();
   
		return cmdInfo;
	}


}
