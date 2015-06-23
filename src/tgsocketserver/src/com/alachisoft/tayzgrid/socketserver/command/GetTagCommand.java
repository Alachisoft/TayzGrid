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

import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.GetTagResponseBuilder;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.GetTagCommandProtocol;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
public class GetTagCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public String[] Tags;
		public TagComparisonType ComparisonType = TagComparisonType.values()[0];
		public long ClientLastViewId;
		public int CommandVersion;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Tags = this.Tags;
			varCopy.ComparisonType = this.ComparisonType;
			varCopy.ClientLastViewId = this.ClientLastViewId;
			varCopy.CommandVersion = this.CommandVersion;

			return varCopy;
		}
	}

	private OperationResult _getResult = OperationResult.Success;

	@Override
	public OperationResult getOperationResult() {
		return _getResult;
	}

	@Override
	public boolean getCanHaveLargedata() {
		return true;
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		byte[] data = null;

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (IllegalArgumentException arEx) {
			if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error("GetCommand", "command: " + command + " Error" + arEx);
			}
			_getResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2"))
			{
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(arEx, command.getRequestID()));
			}
			return;
		} catch (Exception exc) {
			_getResult = OperationResult.Failure;
			if (!super.immatureId.equals("-2"))
			{
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
			java.util.HashMap result = null;
			OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

			if (cmdInfo.CommandVersion < 1) {
				operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
			}
			else { 
				operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);
			}

			result = nCache.getCache().GetByTag(cmdInfo.Tags, cmdInfo.ComparisonType, operationContext);

			GetTagResponseBuilder.BuildResponse(result, cmdInfo.CommandVersion, cmdInfo.RequestId, _serializedResponsePackets, nCache.getCacheId());

		} catch (Exception exc) {
			_getResult = OperationResult.Failure;
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}



	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		GetTagCommandProtocol.GetTagCommand getTagCommand = command.getGetTagCommand();

		cmdInfo.ComparisonType = TagComparisonType.forValue(getTagCommand.getTagComparisonType());
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.Tags = getTagCommand.getTagsList().toArray(new String[0]);
		cmdInfo.ClientLastViewId = command.getClientLastViewId();
		cmdInfo.CommandVersion = command.getCommandVersion();

		return cmdInfo;
	}

}
