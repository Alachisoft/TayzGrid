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

import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheBindingCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheBindingResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.CacheInfo;
import com.alachisoft.tayzgrid.socketserver.CacheProvider;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;

public class GetCacheBindingCommand extends CommandBase {

    private final static class CommandInfo {

        public String RequestId;
        public String CacheId;

        public boolean IsDotNetClient;

        @Override
        public CommandInfo clone() {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.CacheId = this.CacheId;

            varCopy.IsDotNetClient = this.IsDotNetClient;

            return varCopy;
        }
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {

        CommandInfo cmdInfo = new CommandInfo();
        try {
            cmdInfo = ParseCommand(command, clientManager).clone();
        } catch (Exception exc) {
            if (!super.immatureId.equals("-2")) {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }
        try {

            int port = 0;
            CacheServerConfig cacheInfo = CacheProvider.getProvider().GetCacheConfiguration(cmdInfo.CacheId); 
            if (cacheInfo != null) {
                port = cacheInfo.getClientPort();
            }
            boolean isRunning = CacheProvider.getProvider().IsRunning(cmdInfo.CacheId);
            String bindIP = ServicePropValues.BIND_toClient_IP;

            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setGetCacheBindingResponse(
                    GetCacheBindingResponseProtocol.GetCacheBindingResponse.newBuilder()
                    .setServer(bindIP)
                    .setPort(port)
                    .setIsRunning(isRunning))
                    .setRequestId(Long.parseLong(cmdInfo.RequestId))
                    .setResponseType(ResponseProtocol.Response.Type.CACHE_BINDING)
                    .build();
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        } catch (Exception ex) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(ex, command.getRequestID()));
        }

    }

    private GetCacheBindingCommand.CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
        CommandInfo cmdInfo = new CommandInfo();
        GetCacheBindingCommandProtocol.GetCacheBindingCommand getCacheBindingCommand = command.getGetCacheBindingCommand();

        cmdInfo.CacheId = getCacheBindingCommand.getCacheId();
        cmdInfo.IsDotNetClient = getCacheBindingCommand.getIsDotnetClient();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

        return cmdInfo;
    }

}
