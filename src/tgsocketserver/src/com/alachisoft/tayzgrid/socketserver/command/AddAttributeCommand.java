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
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.AddAttributeResponseProtocol;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.io.IOException;

public class AddAttributeCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public Object Key;
		public ExpirationHint ExpHint;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Key = this.Key;
			varCopy.ExpHint = this.ExpHint;

			return varCopy;
		}
	}

	private byte[] _resultPacket = null;
	protected String serializationContext;


	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws Exception {
		CommandInfo cmdInfo = new CommandInfo();

		byte[] data = null;

		ICommandExecuter tempVar = clientManager.getCmdExecuter();
		TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
		try {
			serializationContext = nCache.getCacheId();
			cmdInfo = ParseCommand(command).clone();
		} catch (Exception exc) {
			if (!super.immatureId.equals("-2")) {
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		data = new byte[1];
		try {
			//PROTOBUF:RESPONSE
			boolean result = nCache.getCache().AddExpirationHint(cmdInfo.Key, cmdInfo.ExpHint, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
			//PROTOBUF:RESPONSE
                        ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setAddAttributeResponse(AddAttributeResponseProtocol.AddAttributeResponse.newBuilder()
                                                                                                            .setSuccess(result).build())
                                                                                                            .setRequestId(Long.parseLong(cmdInfo.RequestId))
                                                                                                            .setResponseType(ResponseProtocol.Response.Type.ADD_ATTRIBUTE).build();
			_serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
		} catch (Exception exc) {
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) throws IOException, ClassNotFoundException
    {

        CommandInfo cmdInfo = new CommandInfo();

        com.alachisoft.tayzgrid.common.protobuf.AddAttributeProtocol.AddAttributeCommand addAttributeCommand = command.getAddAttributeCommand();
        try
        {
            cmdInfo.ExpHint = com.alachisoft.tayzgrid.caching.util.ProtobufHelper.GetExpirationHintObj( addAttributeCommand.getAbsExpiration(), 0, false, serializationContext);
        }
        catch (Exception e)
        {
        }
        cmdInfo.Key = CacheKeyUtil.Deserialize(addAttributeCommand.getKey(), serializationContext);
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

        return cmdInfo;

    }

}
