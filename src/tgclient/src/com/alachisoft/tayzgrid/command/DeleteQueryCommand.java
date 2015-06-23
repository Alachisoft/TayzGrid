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


package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValueProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DeleteQueryCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ValueWithTypeProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public final class DeleteQueryCommand extends Command
{
    private String query;
    private HashMap values;
    private boolean isRemove;
    

    public DeleteQueryCommand(String query, java.util.HashMap values, boolean isRemove)
    {
        this.query = query;
        this.values = values;
        this.isRemove = isRemove;
    }

    private String getValueString(Object value) throws Exception
    {
      
        if (value == null)
        {
            throw new CommandException("NCache query does not support null values");
        }

        if (value instanceof String) //Asif Imam::Catter for case in-sensitive comparison
        {
           value = (Object) (value.toString()).toLowerCase();
        }

        if (value instanceof Date)
        {
            value = HelperFxn.getTicks((Date) value);
        }

        return value.toString();
    }

    @Override
    public CommandType getCommandType()
    {
        return CommandType.DELETEQUERY;
    }

    @Override
    protected void createCommand() throws CommandException
    {      
        DeleteQueryCommandProtocol.DeleteQueryCommand.Builder builder =
                DeleteQueryCommandProtocol.DeleteQueryCommand.newBuilder();
        
        builder = builder.setRequestId(this.getRequestId()).setQuery(this.query).setIsRemove(this.isRemove);        //super._command = new Alachisoft.NCache.Common.Protobuf.Command();
        
        Set keySet = values.keySet();
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext())
        {
            try
            {
                String typeKey = (String) iterator.next();
                KeyValueProtocol.KeyValue.Builder keyValPair = KeyValueProtocol.KeyValue.newBuilder();
                keyValPair = keyValPair.setKey(typeKey);
                   
                if (values.get(typeKey) instanceof ArrayList)
                {
                    ArrayList list = (ArrayList) values.get(typeKey);
                    for (int i = 0; i < list.size(); i++)
                    {
                        Object typeValue = list.get(i);
                        keyValPair = keyValPair.addValue(ValueWithTypeProtocol.ValueWithType.newBuilder()
                                .setValue(getValueString(typeValue)).setType(typeValue.getClass().getName()).build());
                    }
                }
                else
                {
                    Object typeValue = values.get(typeKey);
                    keyValPair = keyValPair.addValue(ValueWithTypeProtocol.ValueWithType.newBuilder()
                            .setValue(getValueString(typeValue)).setType(typeValue.getClass().getName()).build());
                }
                builder = builder.addValues(keyValPair);
            }
            catch(CommandException cmE)
            {
                throw cmE;
            }
            catch (Exception e)
            {
                throw new CommandException("Error parsing values.");
            }
        }
             
        try
        {
           CommandProtocol.Command.Builder commandBuilder = CommandProtocol.Command.newBuilder(); 
           
                 commandBuilder = commandBuilder.setDeleteQueryCommand(builder)
                         .setRequestID(this.getRequestId())
                         .setType(CommandProtocol.Command.Type.DELETEQUERY)
                         .setCommandVersion(1)
                         .setClientLastViewId(this.getClientLastViewId());
                 

                 
              super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());   
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
        
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
        return RequestType.BulkWrite;
    }
}
