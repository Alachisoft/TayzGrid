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
import com.alachisoft.tayzgrid.common.protobuf.DeleteResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DeleteCommandProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
public class DeleteCommand extends CommandBase {
	protected final static class CommandInfo {
		public boolean DoAsync;

		public String RequestId;
		public Object Key;
		public BitSet FlagMap;
		public short DsItemRemovedId;
		public Object LockId;
		public long Version;
		public String ProviderName;
		public LockAccessType LockAccessTypes = LockAccessType.values()[0];
                public DeleteParams deleteParams;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.DoAsync = this.DoAsync;
			varCopy.RequestId = this.RequestId;
			varCopy.Key = this.Key;
			varCopy.FlagMap = this.FlagMap;
			varCopy.DsItemRemovedId = this.DsItemRemovedId;
			varCopy.LockId = this.LockId;
			varCopy.Version = this.Version;
			varCopy.ProviderName = this.ProviderName;
			varCopy.LockAccessTypes = this.LockAccessTypes;
                        varCopy.deleteParams = this.deleteParams;
			return varCopy;
		}
	}

	private OperationResult _removeResult = OperationResult.Success;

	@Override
	public OperationResult getOperationResult() {
		return _removeResult;
	}

	@Override
	public boolean getCanHaveLargedata() {
		return true;
	}

	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();
                ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
		
		try {
			cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
			if (ServerMonitor.getMonitorActivity()) {
				ServerMonitor.LogClientActivity("RemCmd.Exec", "cmd parsed");
			}

		} catch (Exception exc) {
			_removeResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}
		if (!cmdInfo.DoAsync) {
			try {
				CallbackEntry cbEntry = null;
				if (cmdInfo.DsItemRemovedId != -1) {
					cbEntry = new CallbackEntry(clientManager.getClientID(), -1, null,(short) -1,(short) -1,(short) -1, cmdInfo.DsItemRemovedId, cmdInfo.FlagMap, EventDataFilter.None,EventDataFilter.None);
				}

				OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
				
                                if(cmdInfo.deleteParams != null)
                                    operationContext.Add(OperationContextFieldName.DeleteParams, cmdInfo.deleteParams);
                                
				boolean success = nCache.getCache().Delete(cmdInfo.Key, cmdInfo.FlagMap, cbEntry, cmdInfo.LockId, cmdInfo.Version, cmdInfo.LockAccessTypes, cmdInfo.ProviderName, operationContext);


				//PROTOBUF:RESPONSE
                                ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setDeleteResponse(DeleteResponseProtocol.DeleteResponse.newBuilder().setSuccess(success))
                                                                                                         .setResponseType(ResponseProtocol.Response.Type.DELETE)
                                                                                                         .setRequestId(Long.parseLong(cmdInfo.RequestId)).build();
				ResponseProtocol.Response.Builder resBuilder=ResponseProtocol.Response.newBuilder();
				_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

			} catch (Exception exc) {
				_removeResult = OperationResult.Failure;

				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			} finally {
				if (ServerMonitor.getMonitorActivity()) {
					ServerMonitor.LogClientActivity("RemCmd.Exec", "cmd executed on cache");
				}
			}
		}
		else {
			Object[] package_Renamed = null;
			if (!cmdInfo.RequestId.equals("-1") || cmdInfo.DsItemRemovedId != -1) {
				package_Renamed = new Object[] {cmdInfo.Key, cmdInfo.FlagMap, new CallbackEntry(clientManager.getClientID(), Integer.parseInt(cmdInfo.RequestId), null,(short) -1, (short)-1, (short)(cmdInfo.RequestId.equals("-1") ? - 1 : 0), cmdInfo.DsItemRemovedId, cmdInfo.FlagMap, EventDataFilter.None,EventDataFilter.None)};
			} else {
				package_Renamed = new Object[] {cmdInfo.Key, cmdInfo.FlagMap, null, cmdInfo.ProviderName};
			}

			OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
			
			nCache.getCache().RemoveAsync(package_Renamed, operationContext);

		}
	}

	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
		CommandInfo cmdInfo = new CommandInfo();

		DeleteCommandProtocol.DeleteCommand removeCommand = command.getDeleteCommand();
		cmdInfo.DoAsync = removeCommand.getIsAsync();
		cmdInfo.DsItemRemovedId = (short)(removeCommand.getDatasourceItemRemovedCallbackId());
		cmdInfo.FlagMap = new BitSet((byte)(removeCommand.getFlag()));
		cmdInfo.Key = CacheKeyUtil.Deserialize(removeCommand.getKey(), serializationContext);
		cmdInfo.LockAccessTypes = LockAccessType.forValue(removeCommand.getLockAccessType());
		cmdInfo.LockId = removeCommand.getLockId();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.Version = removeCommand.getVersion();
		cmdInfo.ProviderName = !tangible.DotNetToJavaStringHelper.isNullOrEmpty(removeCommand.getProviderName()) ? removeCommand.getProviderName() : null;
                
                if(removeCommand.getCompareOld() && removeCommand.getOldValueCount()> 0)
                {
                    cmdInfo.deleteParams = new DeleteParams();
                    cmdInfo.deleteParams.CompareOldValue = removeCommand.getCompareOld();
                    Object[] p = new Object[removeCommand.getOldValueCount()];
                    List<ByteString> oldValue = removeCommand.getOldValueList();
                    int i=0;
                    for(ByteString bs : oldValue)
                    {
                        p[i++] = bs.toByteArray();
                    }
                    cmdInfo.deleteParams.OldValue = new UserBinaryObject(p);
                    cmdInfo.deleteParams.OldValueFlag = new BitSet((byte)removeCommand.getOldValueFlag());
                }
		return cmdInfo;
	}

}
