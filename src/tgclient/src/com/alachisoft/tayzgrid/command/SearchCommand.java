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
import com.alachisoft.tayzgrid.common.protobuf.SearchCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ValueWithTypeProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @version 1.0
 * 
 */
public final class SearchCommand extends Command
{

    private String query;
    private HashMap values;
    private boolean isSearchEntries;
    private static char Delimitor = '|';

    /**
     * Creates a new instance of SearchCommand
     *
     * @param query
     * @param isSearchEntriesCommand
     * @param async
     * @throws java.io.UnsupportedEncodingException
     * @throws java.io.IOException
     */
    public SearchCommand(String query, HashMap values,
            boolean isSearchEntries, boolean async)
    {
 
        this.query = query;
        this.values = values;
        this.isAsync = async;
        this.isSearchEntries = isSearchEntries;
    }

    public void createCommand() throws CommandException
    {

        SearchCommandProtocol.SearchCommand.Builder builder =
                SearchCommandProtocol.SearchCommand.newBuilder();

        builder = builder.setRequestId(this.getRequestId()).setQuery(this.query).setSearchEntries(this.isSearchEntries);

        int totalValuesCount = 0;

        Set keySet = values.keySet();
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext())
        {
            try
            {
                String typeKey = (String) iterator.next();

                KeyValueProtocol.KeyValue.Builder keyValPair =
                        KeyValueProtocol.KeyValue.newBuilder();
                keyValPair = keyValPair.setKey(typeKey);

                if(values.get(typeKey) == null)
                    throw new ArgumentException("TayzGrid query does not support null values");
                
                if (values.get(typeKey) instanceof ArrayList)
                {
                    ArrayList list = (ArrayList) values.get(typeKey);
                    for (int i = 0; i < list.size(); i++)
                    {
                        Object typeValue = list.get(i);
                        keyValPair = keyValPair.addValue(ValueWithTypeProtocol.ValueWithType.newBuilder().setValue(getValueString(typeValue)).setType(typeValue.getClass().getName()).build());
                    }
                }
                else
                {
                    Object typeValue = values.get(typeKey);
                    keyValPair = keyValPair.addValue(ValueWithTypeProtocol.ValueWithType.newBuilder().setValue(getValueString(typeValue)).setType(typeValue.getClass().getName()).build());
                }
                builder = builder.addValues(keyValPair);
            }
            catch(CommandException cmE)
            {
                throw cmE;
            }
            catch (Exception e){
                throw new CommandException("Error parsing values: " + e.toString());
            }
        }
 

        try
        {
            CommandProtocol.Command.Builder commandBuilder =
                    CommandProtocol.Command.newBuilder();

            commandBuilder = commandBuilder.setSearchCommand(builder)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.SEARCH)
                    .setCommandVersion(2)
                    .setClientLastViewId(this.getClientLastViewId());

            super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
 
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
    }

    /**
     *
     * @return
     */
    protected boolean parseCommand()
    {
        return true;
    }

    private String getValueString(Object value) throws Exception
    {
  

        if (value == null)
        {
            throw new CommandException("NCache query does not support null values");
        }

        if (value instanceof String)
        {
            value = (Object) (value.toString()).toLowerCase();
        }
        if (value instanceof Date)
        {
            value = HelperFxn.getTicks((Date) value);
        }
 
        return value.toString();
    }

    public CommandType getCommandType() {
         return CommandType.SEARCH;
    }
    
    @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.BulkRead;
    }
}
