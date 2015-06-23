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
import com.alachisoft.tayzgrid.common.protobuf.UnRegisterBulkKeyNotifCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.UnregisterBulkKeyNotifResponseProtocol;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import java.io.IOException;

public class UnRegsisterBulkKeyNotification extends CommandBase {
	protected final static class CommandInfo {
		public int PackageSize;

		public String RequestId;
		public Object[] Keys;
		public short RemoveCallbackId;
		public short UpdateCallbackId;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.PackageSize = this.PackageSize;
			varCopy.RequestId = this.RequestId;
			varCopy.Keys = this.Keys;
			varCopy.RemoveCallbackId = this.RemoveCallbackId;
			varCopy.UpdateCallbackId = this.UpdateCallbackId;

			return varCopy;
		}
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();
                ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
			
		try {
			cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
		} catch (Exception exc) {
			if (!super.immatureId.equals("-2")) {
				_serializedResponsePackets.add(clientManager.ReplyPacket(super.ExceptionPacket(exc, super.immatureId), super.ParsingExceptionMessage(exc)));
			}
			return;
		}

		try {
			nCache.getCache().UnregisterKeyNotificationCallback(cmdInfo.Keys, new CallbackInfo(clientManager.getClientID(), cmdInfo.UpdateCallbackId,EventDataFilter.None), new CallbackInfo(clientManager.getClientID(), cmdInfo.RemoveCallbackId,EventDataFilter.None), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setUnregisterBulkKeyNotifResponse(UnregisterBulkKeyNotifResponseProtocol.UnregisterBulkKeyNotifResponse.newBuilder())
                                                                                                  .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                                                                  .setResponseType(ResponseProtocol.Response.Type.UNREGISTER_BULK_KEY_NOTIF).build();
                        
                        _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));                        
		} catch (Exception exc) {
			_serializedResponsePackets.add(clientManager.ReplyPacket(super.ExceptionPacket(exc, cmdInfo.RequestId), super.ExceptionMessage(exc)));
		}
	}


	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
		CommandInfo cmdInfo = new CommandInfo();

                UnRegisterBulkKeyNotifCommandProtocol.UnRegisterBulkKeyNotifCommand.Builder unRegisterBulkKeyNotifCommandBuilder = 
                        UnRegisterBulkKeyNotifCommandProtocol.UnRegisterBulkKeyNotifCommand.newBuilder();
                
                UnRegisterBulkKeyNotifCommandProtocol.UnRegisterBulkKeyNotifCommand unRegisterBulkKeyNotifCommand = 
                        unRegisterBulkKeyNotifCommandBuilder.build();
		unRegisterBulkKeyNotifCommand = command.getUnRegisterBulkKeyNotifCommand();

		Object[] keys = new Object[unRegisterBulkKeyNotifCommand.getKeysList().size()];
                for(int i =0;i<unRegisterBulkKeyNotifCommand.getKeysList().size();i++)
                    keys[i]=CacheKeyUtil.Deserialize(unRegisterBulkKeyNotifCommand.getKeysList().get(i), serializationContext);
                cmdInfo.Keys = keys;
                cmdInfo.RemoveCallbackId = (short)unRegisterBulkKeyNotifCommand.getRemoveCallbackId();
                cmdInfo.UpdateCallbackId = (short)unRegisterBulkKeyNotifCommand.getUpdateCallbackId();
                cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
                

		return cmdInfo;
	}

}
