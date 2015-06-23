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
import com.alachisoft.tayzgrid.common.protobuf.GetGroupCommandProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class GetGroupCommandBase extends CommandBase {
	protected final static class CommandInfo {
		public String RequestId;
		public String Group;
		public String SubGroup;
		public long ClientLastViewId;
		public int CommandVersion;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Group = this.Group;
			varCopy.SubGroup = this.SubGroup;
			varCopy.ClientLastViewId = this.ClientLastViewId;
			varCopy.CommandVersion = this.CommandVersion;

			return varCopy;
		}
	}

	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
	}

	//PROTOBUF
	protected final CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		GetGroupCommandProtocol.GetGroupCommand getGroupCommand = command.getGetGroupCommand();

		cmdInfo.Group = getGroupCommand.getGroup();
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.SubGroup = getGroupCommand.getSubGroup().length() == 0 ? null : getGroupCommand.getSubGroup();
		cmdInfo.ClientLastViewId = command.getClientLastViewId();
		cmdInfo.CommandVersion = command.getCommandVersion();
		return cmdInfo;
	}


}
