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

import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.rpcframework.TargetMethodParameter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.io.IOException;


public abstract class ManagementCommandBase extends CommandBase
{

    @Override
    public void ExecuteCommand(ClientManager clientManager, Command command) throws Exception
    {
    }

    abstract public void ExecuteCommand(ClientManager clientManager, ManagementCommandProtocol.ManagementCommand command);

        protected Object[] GetTargetMethodParameters(byte[] graph)
        {
            Object obj = null;
            try
            {
                obj = CompactBinaryFormatter.fromByteBuffer(graph, "");
            }
            catch (IOException iOException)
            {
                ServerMonitor.LogClientActivity("ManagementCommandBase", iOException.getMessage());
            }
            catch (ClassNotFoundException classNotFoundException)
            {
                ServerMonitor.LogClientActivity("ManagementCommandBase", classNotFoundException.getMessage());
            }
            TargetMethodParameter parameters =  obj instanceof TargetMethodParameter ? (TargetMethodParameter)obj : null;
            return parameters != null ? parameters.getParameterList().toArray() : null;
        }

        protected byte[] SerializeResponse(Object result) throws IOException
        {
            return CompactBinaryFormatter.toByteBuffer(result, "");
        }
}
