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

import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheConfigurationCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetCacheConfigurationResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import java.io.IOException;



public class GetCacheConfigurationCommand extends CommandBase{
    
    private final static class CommandInfo {
        public String RequestId;
        
        public CommandInfo Clone() {
            CommandInfo varCopy = new CommandInfo();
            varCopy.RequestId = this.RequestId;
            return varCopy;
        }
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, CommandProtocol.Command command) throws Exception {
        CommandInfo cmdInfo = new CommandInfo();
        
        byte[] data = null;
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
        
        try {
            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).Clone();
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("GetCacheConfigCmd.Exec", "cmd parsed");
            }
        }
        catch (Exception e) {
            if (!super.immatureId.equals("-2"))
            //PROTOBUF:RESPONSE
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
            }
            return;
        }
        
        try {
            CacheConfigParams configParams = nCache.getCache().getCacheConfiguration();
            
            ResponseProtocol.Response response = 
                    ResponseProtocol.Response.newBuilder().setGetCacheConfigurationResponse(GetCacheConfigurationResponseProtocol.GetCacheConfigurationResponse
                    .newBuilder()
                    .setIsReadThru(configParams.getIsReadThru())
                    .setIsWriteThru(configParams.getIsWriteThru())
                    .setIsStasticsEnabled(configParams.getIsStatisticsEnabled()))
                    .setRequestId(Long.parseLong(cmdInfo.RequestId))
                    .setResponseType(ResponseProtocol.Response.Type.GET_CACHE_CONFIG).build();
            
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (Exception e) {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("GetCacheConfigCmd.Exec", "cmd executed on cache");
        }
    }
    
    //PROTOBUF
    private GetCacheConfigurationCommand.CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException {
        
        CommandInfo cmdInfo = new CommandInfo();
        GetCacheConfigurationCommandProtocol.GetCacheConfigurationCommand getConfigurationCommand =
                command.getGetCacheConfigurationCommand();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        return cmdInfo;
    }
}
