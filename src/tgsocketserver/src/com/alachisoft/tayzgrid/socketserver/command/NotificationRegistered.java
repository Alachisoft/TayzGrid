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

import com.alachisoft.tayzgrid.socketserver.NotificationsType;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterNotifCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RegisterNotifResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;

public class NotificationRegistered extends CommandBase {

	private final static class CommandInfo {
		public String RequestId;
		public int RegNotifs;
                public int datafilter;
                public int sequence;


		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.RegNotifs = this.RegNotifs;
                        varCopy.datafilter = this.datafilter;
                        varCopy.sequence = this.sequence;
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
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		try {
                    
                        TayzGrid nCache = (TayzGrid)clientManager.getCmdExecuter() ;
                        NotificationsType notif = NotificationsType.forValue(cmdInfo.RegNotifs);
                        //Will only register those which are == null i.e. not initialized
                        nCache.RegisterNotification(notif);

                        //Execute only if successfull
                        if ((cmdInfo.RegNotifs & NotificationsType.RegAddNotif.getValue()) != 0 || (cmdInfo.RegNotifs & NotificationsType.RegUpdateNotif.getValue()) != 0 || (cmdInfo.RegNotifs & NotificationsType.RegRemoveNotif.getValue()) != 0) {
                            nCache.MaxEventRequirement(EventDataFilter.forValue(cmdInfo.datafilter), notif, (short)cmdInfo.sequence);
                        } 

                	//PROTOBUF:RESPONSE
                        ResponseProtocol.Response response= ResponseProtocol.Response.newBuilder().setRegisterNotifResponse(RegisterNotifResponseProtocol.RegisterNotifResponse.newBuilder())
			                                                                          .setRequestId(Long.parseLong(cmdInfo.RequestId))
			                                                                          .setResponseType(ResponseProtocol.Response.Type.REGISTER_NOTIF).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}



	//PROTOBUF
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();
		RegisterNotifCommandProtocol.RegisterNotifCommand registerNotifCommand = command.getRegisterNotifCommand();
		cmdInfo.RegNotifs = registerNotifCommand.getNotifMask();
		cmdInfo.RequestId = (String.valueOf(command.getRequestID()));
                cmdInfo.datafilter = registerNotifCommand.getDatafilter();
                cmdInfo.sequence = registerNotifCommand.getSequence();

		return cmdInfo;
	}

}
