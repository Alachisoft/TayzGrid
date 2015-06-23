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

import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkDeleteResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.BulkDeleteCommandProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import java.io.IOException;
public class BulkDeleteCommand extends CommandBase {
	protected final static class CommandInfo {
		public String RequestId;
		public Object[] Keys;
		public BitSet FlagMap;
		public short DsItemsRemovedId;
		public String ProviderName;
		public long ClientLastViewId;
		public String IntendedRecipient;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Keys = this.Keys;
			varCopy.FlagMap = this.FlagMap;
			varCopy.DsItemsRemovedId = this.DsItemsRemovedId;
			varCopy.ProviderName = this.ProviderName;
			varCopy.ClientLastViewId = this.ClientLastViewId;
			varCopy.IntendedRecipient = this.IntendedRecipient;

			return varCopy;
		}
	}

	private OperationResult _removeBulkResult = OperationResult.Success;

	@Override
	public OperationResult getOperationResult() {
		return _removeBulkResult;
	}


	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();


		byte[] data = null;

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
                        
                        
                        try {
                            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
                        } catch (Exception exc) {
                                _removeBulkResult = OperationResult.Failure;
                                if (!super.immatureId.equals("-2")) {
                                        //PROTOBUF:RESPONSE
                                        _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
                                }
                                return;
                        }
                        
			CallbackEntry cbEnrty = null;
			if (cmdInfo.DsItemsRemovedId != -1) {
				cbEnrty = new CallbackEntry(clientManager.getClientID(),-1, null, (short)-1, (short)-1, (short)-1, cmdInfo.DsItemsRemovedId, cmdInfo.FlagMap, EventDataFilter.None,EventDataFilter.None);
			}
			OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
			operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(cmdInfo.IntendedRecipient)) {
				operationContext.Add(OperationContextFieldName.IntendedRecipient, cmdInfo.IntendedRecipient);
			}

			nCache.getCache().Delete(cmdInfo.Keys, cmdInfo.FlagMap, cbEnrty, cmdInfo.ProviderName, operationContext);

                        ResponseProtocol.Response.Builder resBuilder=ResponseProtocol.Response.newBuilder();
                        BulkDeleteResponseProtocol.BulkDeleteResponse.Builder bulkDeleteResponse=BulkDeleteResponseProtocol.BulkDeleteResponse.newBuilder();
			resBuilder.setRequestId(Long.parseLong(cmdInfo.RequestId));
			resBuilder.setIntendedRecipient(cmdInfo.IntendedRecipient==null?"":cmdInfo.IntendedRecipient);

			resBuilder.setResponseType(ResponseProtocol.Response.Type.DELETE_BULK);
			resBuilder.setBulkDeleteResponse(bulkDeleteResponse.build());
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(resBuilder.build()));
		} catch (Exception exc) {
			_removeBulkResult = OperationResult.Failure;
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
		CommandInfo cmdInfo = new CommandInfo();

		BulkDeleteCommandProtocol.BulkDeleteCommand bulkRemoveCommand = command.getBulkDeleteCommand();
		Object[] keys = new Object[bulkRemoveCommand.getKeysList().size()];
                for(int i =0;i<bulkRemoveCommand.getKeysList().size();i++)
                    keys[i]=CacheKeyUtil.Deserialize(bulkRemoveCommand.getKeysList().get(i), serializationContext);
		cmdInfo.Keys = keys;
                cmdInfo.DsItemsRemovedId =(short)(bulkRemoveCommand.getDatasourceItemRemovedCallbackId());
		cmdInfo.FlagMap = new BitSet((byte)(bulkRemoveCommand.getFlag()));
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.ProviderName = !tangible.DotNetToJavaStringHelper.isNullOrEmpty(bulkRemoveCommand.getProviderName()) ? bulkRemoveCommand.getProviderName() : null;
		cmdInfo.ClientLastViewId = command.getClientLastViewId();
		return cmdInfo;
	}


}
