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

import com.alachisoft.tayzgrid.common.enums.Time;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.rpcframework.dotnetrpc.TargetObject;
import com.alachisoft.tayzgrid.common.rpcframework.RPCService;
import com.alachisoft.tayzgrid.common.util.ManagementUtil;
import com.alachisoft.tayzgrid.management.MonitorServer;
import com.alachisoft.tayzgrid.socketserver.CacheProvider;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ManagementResponseProtocol.ManagementResponse;
import com.google.protobuf.ByteString;
import java.io.FileWriter;
import java.io.IOException;

public class ManagementCommand extends ManagementCommandBase
{

    private void log(String text)
    {
    }

    public void print(String text)
    {
        try
        {
            String filename = "log.txt";
            FileWriter fw = new FileWriter(filename, true); //the true will append the new data
            fw.write(text += "\n");//appends the string to the file
            fw.close();

        }
        catch (IOException ioe)
        {
        }
    }

    @Override
    public void ExecuteCommand(ClientManager clientManager, ManagementCommandProtocol.ManagementCommand command)
    {
        Object result = null;
        try
        {
            if( command.getSource()== com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand.SourceType.MANAGER ||
                            command.getSource()== com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand.SourceType.MONITOR)
            {
                throw new ManagementException("Not Supported For Open Source Edition.");
            }
            if (command.getObjectName().equals(ManagementUtil.ManagementObjectName.CacheServer))
            {
                String method = command.getMethodName();

                log(method);

                int overload = command.getOverload();
                com.google.protobuf.ByteString args = command.getArguments();
                Object[] methodParameters = GetTargetMethodParameters(args.toByteArray());

                long bTime = System.nanoTime();

                result = CacheProvider.getManagementRPCService().InvokeMethodOnTarget(method, overload, methodParameters);

                double time = Time.toMilliSeconds(System.nanoTime() - bTime, Time.nSEC);
                log("Time Taken by Command: " + method + " " + time);

            }
            else if (command.getObjectName().equals(ManagementUtil.ManagementObjectName.MonitorServer))
            {
                if (clientManager.getMonitorRPCService() == null)
                {
                    clientManager.setMonitorRPCService(new RPCService<MonitorServer>(new TargetObject<MonitorServer>(new MonitorServer())));
                }

                result = clientManager.getMonitorRPCService().InvokeMethodOnTarget(command.getMethodName(), command.getOverload(),
                        GetTargetMethodParameters(command.getArguments().toByteArray()));
            }

            ManagementResponse.Builder response = ManagementResponse.newBuilder();
            response.setMethodName(command.getMethodName());
            response.setVersion(command.getCommandVersion());
            response.setRequestId(command.getRequestId());
            response.setReturnVal(ByteString.copyFrom(SerializeResponse(result)));
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (Exception exc)
        {

            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestId()));
        }
    }
}
