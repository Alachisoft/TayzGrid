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
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.UnlockCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.UnlockResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.io.IOException;

public class UnlockCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public Object Key;
		public boolean isPreemptive;
		public Object lockId;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Key = this.Key;
			varCopy.isPreemptive = this.isPreemptive;
			varCopy.lockId = this.lockId;

			return varCopy;
		}
	}

	private OperationResult _unlockResult = OperationResult.Success;

	@Override
	public OperationResult getOperationResult() {
		return _unlockResult;
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();
                ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);


		try {
			cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
			if (ServerMonitor.getMonitorActivity()) {
				ServerMonitor.LogClientActivity("UnlockCmd.Exec", "cmd parsed");
			}

		} catch (IllegalArgumentException arEx) {
			if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error("UnlockCommand", "command: " + command + " Error" + arEx);
			}
			_unlockResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2")) {
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(arEx, command.getRequestID()));
			}
			return;
		} catch (Exception exc) {
			_unlockResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
			
			nCache.getCache().Unlock(cmdInfo.Key, cmdInfo.lockId, cmdInfo.isPreemptive, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

			//PROTOBUF:RESPONSE

                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setUnlockResponse(UnlockResponseProtocol.UnlockResponse.newBuilder())
                                                                                                  .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                                                                  .setResponseType(ResponseProtocol.Response.Type.UNLOCK).build();

			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

		} catch (Exception exc) {
			_unlockResult = OperationResult.Failure;
			_serializedResponsePackets.add(clientManager.ReplyPacket(super.ExceptionPacket(exc, cmdInfo.RequestId), super.ExceptionMessage(exc)));
		} finally {
			if (ServerMonitor.getMonitorActivity()) {
				ServerMonitor.LogClientActivity("UnlockCmd.Exec", "cmd executed on cache");
			}

		}
	}


	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
		CommandInfo cmdInfo = new CommandInfo();

		UnlockCommandProtocol.UnlockCommand unlockCommand = command.getUnlockCommand();

		cmdInfo.isPreemptive = unlockCommand.getPreemptive();
		if (!unlockCommand.getPreemptive()) {
			cmdInfo.lockId = unlockCommand.getLockId();
		}
		cmdInfo.Key = CacheKeyUtil.Deserialize(unlockCommand.getKey(), serializationContext);
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

		return cmdInfo;
	}

}
