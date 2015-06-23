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
import com.alachisoft.tayzgrid.common.protobuf.DisposeResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
public class DisposeCommand extends CommandBase {

	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		if (clientManager != null) {
			clientManager._leftGracefully = true;
			clientManager.getCmdExecuter().dispose();

                        ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setDisposeResponse(DisposeResponseProtocol.DisposeResponse.newBuilder())
                                                                                                 .setRequestId(command.getRequestID())
                                                                                                 .setResponseType(ResponseProtocol.Response.Type.DISPOSE).build();

			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		}
	}

}
