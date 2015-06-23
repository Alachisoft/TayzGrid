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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.socketserver.command;

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.util.JavaClrTypeMapping;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command;
import com.alachisoft.tayzgrid.common.protobuf.DeleteQueryResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValueProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ValueWithTypeProtocol;

public class DeleteQueryCommand extends CommandBase
{
    protected final static class CommandInfo
    {
        public String RequestId;
        public String Query;
        public java.util.Map Values;

        public long ClientLastViewId;
        public int CommandVersion;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Query = this.Query;
            varCopy.Values = this.Values;
            varCopy.ClientLastViewId = this.ClientLastViewId;
            varCopy.CommandVersion = this.CommandVersion;

            return varCopy;
        }
}

private OperationResult _deleteQueryResult = getOperationResult().Success;

@Override
public OperationResult getOperationResult()
{
    return _deleteQueryResult;
}

@Override
public void ExecuteCommand(ClientManager clientManager, Command command) throws Exception {
    CommandInfo cmdInfo = new CommandInfo();

        try
        {
            cmdInfo = ParseCommand(command, clientManager).clone();
        }
        catch (RuntimeException exc)
        {
            _deleteQueryResult = getOperationResult().Failure;
            if (!super.immatureId.equals("-2"))
            {
                    //PROTOBUF:RESPONSE
                    _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        byte[] data = null;

        try
        {
            ICommandExecuter tempVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);

            OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
            operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);
            operationContext.Add(OperationContextFieldName.RemoveQueryOperation, false);

            nCache.getCache().DeleteQuery(cmdInfo.Query, cmdInfo.Values, operationContext);

            ResponseProtocol.Response.Builder resBuilder=ResponseProtocol.Response.newBuilder();
            ResponseProtocol.Response response=ResponseProtocol.Response.newBuilder().setDeleteQueryResponse(DeleteQueryResponseProtocol.DeleteQueryResponse.newBuilder())
                    .setResponseType(ResponseProtocol.Response.Type.DELETE_QUERY)
                    .setRequestId(Long.parseLong(cmdInfo.RequestId)).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));
        }
        catch (Exception exc)
        {
            _deleteQueryResult = getOperationResult().Failure;
            //PROTOBUF:RESPONSE
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
}
//PROTOBUF
private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager)
{
        CommandInfo cmdInfo = new CommandInfo();

        com.alachisoft.tayzgrid.common.protobuf.DeleteQueryCommandProtocol.DeleteQueryCommand deleteQueryCommand = command.getDeleteQueryCommand();
        cmdInfo.Query = deleteQueryCommand.getQuery();
               if (clientManager.IsDotNetClient)
        {
            int index = cmdInfo.Query.indexOf("$Text$");
            if (index != -1)
            {
                cmdInfo.Query = cmdInfo.Query.replace("$Text$", "System.String");
            }
            else
            {
                index = cmdInfo.Query.indexOf("$TEXT$");
                if (index != -1)
                {
                    cmdInfo.Query = cmdInfo.Query.replace("$TEXT$", "System.String");
                }
                else
                {
                    index = cmdInfo.Query.indexOf("$text$");
                    if (index != -1)
                    {
                        cmdInfo.Query = cmdInfo.Query.replace("$text$", "System.String");
                    }
                }
            }
        }
        else
        {
            int index = cmdInfo.Query.indexOf("$Text$");
            if (index != -1)
            {
                cmdInfo.Query = cmdInfo.Query.replace("$Text$", "java.lang.String");
            }
            else
            {
                index = cmdInfo.Query.indexOf("$TEXT$");
                if (index != -1)
                {
                    cmdInfo.Query = cmdInfo.Query.replace("$TEXT$", "java.lang.String");
                }
                else
                {
                    index = cmdInfo.Query.indexOf("$text$");
                    if (index != -1)
                    {
                        cmdInfo.Query = cmdInfo.Query.replace("$text$", "java.lang.String");
                    }
                }
            }
        }
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        cmdInfo.ClientLastViewId = command.getClientLastViewId();
        cmdInfo.CommandVersion = command.getCommandVersion();

        {
            cmdInfo.Values = new java.util.HashMap();
            for (KeyValueProtocol.KeyValue deleteValue : deleteQueryCommand.getValuesList())
            {
                String key = deleteValue.getKey();
                java.lang.Class type = null;
                Object value = null;

                for (ValueWithTypeProtocol.ValueWithType valueWithType : deleteValue.getValueList())
                {
                    String typeStr = valueWithType.getType();
                    if (clientManager.IsDotNetClient)
                    {
                        typeStr = JavaClrTypeMapping.ClrToJava(typeStr);
                    }
                    
                     try
                    {
                        type = java.lang.Class.forName(typeStr);

                        if (valueWithType.getValue() != null)
                        {                            
                            if (type == java.util.Date.class)
                            {
                                /**For client we would be sending ticks instead
                                of string representation of Date.
                                */
                                value = HelperFxn.getDateFromTicks(Long.parseLong(valueWithType.getValue()));
                            }
                            else
                            {
                                value = new ConfigurationBuilder().ConvertToPrimitive(type, valueWithType.getValue(), "");
                            }                          
                        }
                    }
                    catch (Exception e)
                    {
                        throw new java.lang.IllegalArgumentException("Cannot convert '" + valueWithType.getValue() + "' to " + type.toString());
                    }

                    if (!cmdInfo.Values.containsKey(key))
                    {
                        cmdInfo.Values.put(key, value);
                    }
                    else
                    {
                        java.util.ArrayList list = (java.util.ArrayList)((cmdInfo.Values.get(key) instanceof java.util.ArrayList) ? cmdInfo.Values.get(key) : null); // the value is not array list
                        if (list == null)
                        {
                            list = new java.util.ArrayList();
                            list.add(cmdInfo.Values.get(key)); // add the already present value in the list
                            cmdInfo.Values.remove(key); // remove the key from hashtable to avoid key already exists exception
                            list.add(value); // add the new value in the list
                            cmdInfo.Values.put(key, list);
                        }
                        else
                        {
                            list.add(value);
                        }
                    }
                }
            }
        }

        return cmdInfo;
    }
}
