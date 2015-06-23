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

import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;


public class InitSecondarySocketCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public boolean IsDotNetClient;
		public String ClientID;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.IsDotNetClient = this.IsDotNetClient;
			varCopy.ClientID = this.ClientID;

			return varCopy;
		}
	}

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (RuntimeException exc) {
			if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error("InitializeCommand.Execute", clientManager.getClientSocket().getInetAddress().toString()+":"+clientManager.getClientSocket().getPort()+ " parsing error " + exc.toString());
			}

			if (!super.immatureId.equals("-2")) {
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
			clientManager.setClientID(cmdInfo.ClientID);

			synchronized (ConnectionManager.ConnectionTable) {
				if (ConnectionManager.ConnectionTable.containsKey(clientManager.getClientID())) {
					ClientManager cmgr = (ClientManager)((ConnectionManager.ConnectionTable.get(clientManager.getClientID()) instanceof ClientManager) ? ConnectionManager.ConnectionTable.get(clientManager.getClientID()) : null);
					clientManager.setCmdExecuter(cmgr.getCmdExecuter());
				}
			}

		}

		catch (SecurityException sec) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(sec, command.getRequestID()));
		}
                catch (RuntimeException exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}

	}


	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		return new CommandInfo();
	}

}
