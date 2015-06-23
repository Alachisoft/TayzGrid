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


//import com.alachisoft.jvcache.web.caching.CacheItemRemovedCallback;
//import com.alachisoft.jvcache.web.caching.CacheItemUpdatedCallback;
import com.alachisoft.tayzgrid.web.caching.DataSourceItemsAddedCallback;
import com.alachisoft.tayzgrid.web.caching.CustomEventCallback;
import com.alachisoft.tayzgrid.communication.Request;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;


public abstract class Command
{

    
    /**
     * the command in bytes
     */
    protected byte[] commandBytes = null;
    private CommandResponse result = null;
    private Object[] bulkKeys;




    /**
     * command name
     */
    protected String name;

    public DataSourceItemsAddedCallback dataSourceItemAdded = null;
    
    /**
     * is the command Async
     */
    public boolean isAsync;
    public boolean asyncCallbackSpecified = false;
    protected int asyncCallbackId = -1;
    /**
     * the command id
     */
    private long requestId = -1;
    private long clientLastViewId = -1;
    private String intendedRecipient = "";
    private String _cacheId;

    public String getIntendedRecipient()
    {
        return intendedRecipient;
    }

    public void setIntendedRecipient(String intendedRecipient)
    {
        this.intendedRecipient = intendedRecipient;
    }

    public long getClientLastViewId()
    {
        return clientLastViewId;
    }

    public void setClientLastViewId(long clientLastViewId)
    {
        this.clientLastViewId = clientLastViewId;
    }

    public String getCacheId()
    {
        return _cacheId;
    }

    public void setCacheId(String _cacheId)
    {
        this._cacheId = _cacheId;
    }
    /**
     * The key of the Item.
     *
     * @see #getKey()
     */
    protected Object key;
    /**
     * The value of the current command.
     *
     * @see #getValue()
     */
    protected byte[] value = null;
    protected HashMap hashMap = null;
    /**
     * Ip address to which this command was sent
     */
    private String ip;
    private Request _parent;

    public Request getParent()
    {
        return _parent;
    }

    public void setParent(Request _parent)
    {
        this._parent = _parent;
    }

    public abstract CommandType getCommandType();
    
    public abstract RequestType getCommandRequestType();

    public boolean isAsyncCallbackSpecified()
    {
        return (this.isAsync && this.asyncCallbackId > -1);
    }

    public int getAsyncCallbackId()
    {
        return this.asyncCallbackId;
    }

    public Object[] getBulkKeys()
    {
        return bulkKeys;
    }

    public void setBulkKeys(Object[] bulkKeys)
    {
        this.bulkKeys = bulkKeys;
    }


    /**
     *
     * @param cmdString
     * @param data
     * @throws java.io.UnsupportedEncodingException
     * @return
     */
    protected byte[] constructCommand(String cmdString, byte[] data)
            throws UnsupportedEncodingException
    {

        byte[] command = cmdString.getBytes("UTF-8");
        byte[] buffer = new byte[2 * (CommandOptions.COMMAND_SIZE + CommandOptions.DATA_SIZE) + command.length + data.length];

        byte[] commandSize = String.valueOf(command.length).getBytes("UTF-8");
        byte[] dataSize = String.valueOf(data.length).getBytes("UTF-8");

        System.arraycopy(commandSize, 0, buffer, 0, commandSize.length);
        System.arraycopy(dataSize, 0, buffer, CommandOptions.COMMAND_SIZE,
                dataSize.length);
        // we copy the command size two times to avoid if an corruption occurs.
        System.arraycopy(commandSize, 0, buffer, CommandOptions.TOTAL_SIZE,
                commandSize.length);
        System.arraycopy(dataSize, 0, buffer, CommandOptions.TOTAL_SIZE + CommandOptions.COMMAND_SIZE, dataSize.length);

        System.arraycopy(command, 0, buffer, 2 * CommandOptions.TOTAL_SIZE,
                command.length);
        System.arraycopy(data, 0, buffer, (2 * CommandOptions.TOTAL_SIZE) + command.length, data.length);

        return buffer;
    }

    protected byte[] constructCommand(byte[] serializedCommand)
            throws IOException, UnsupportedEncodingException
    {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try
        {
            ///Copy discarding buffer for server
            byte[] discardingBuffer = new byte[20];
            stream.write(discardingBuffer);

            ///Copy size of data in 10 bytes
            byte[] size = new byte[CommandOptions.COMMAND_SIZE];
            byte[] commandSize = String.valueOf(serializedCommand.length).getBytes("UTF-8");
            System.arraycopy(commandSize, 0, size, 0, commandSize.length);
            stream.write(size);

            ///Copy actual data
            stream.write(serializedCommand);

            ///Return the packet
            return stream.toByteArray();
        }
        finally
        {
            stream.close();
        }
    }

    protected byte[] SerializeObject(Object obj) throws CommandException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = null;
        try
        {
            objectStream = new ObjectOutputStream(output);
            objectStream.writeObject(obj);
            return output.toByteArray();
        }
        catch (IOException ex)
        {
            throw new CommandException(ex.getMessage());
        }
        finally
        {
            try
            {
                output.close();
                if (objectStream != null)
                {
                    objectStream.close();
                }
            }
            catch (IOException ex)
            {
                
            }
        }
    }

    protected String rebuildCommandWithQueryInfo(HashMap queryInfo)
    {
        Map.Entry entry;
        StringBuilder cmdString = new StringBuilder();
        Iterator hashmapIt = queryInfo.entrySet().iterator();
        while (hashmapIt.hasNext())
        {
            entry = (Map.Entry) hashmapIt.next();
            cmdString.append(entry.getKey()).append("\"");
            ArrayList values = (ArrayList) entry.getValue();
            cmdString.append(values.size()).append("\"");
            for (int i = 0; i < values.size(); i++)
            {
                cmdString.append(values.get(i)).append("\"");
            }

        }

        return cmdString.toString();
    }

    protected String rebuildCommandWithTagInfo(HashMap tagInfo)
    {
        StringBuilder cmdString = new StringBuilder();
        cmdString.append(String.valueOf(tagInfo.get("type")) + "\"");
        Object obj = tagInfo.get("tags-list");
        if (obj instanceof ArrayList)
        {
            ArrayList tagsList = (ArrayList) obj;
            cmdString.append(String.valueOf(tagsList.size()) + "\"");

            Iterator tagsEnum = tagsList.iterator();
            while (tagsEnum.hasNext())
            {
                Object current = tagsEnum.next();
                if (current != null)
                {
                    cmdString.append(current.toString() + "\"");
                }
                else
                {
                    cmdString.append("NLV\"");
                }
            }
        }
        return cmdString.toString();
    }

    /**
     * Create the Command.
     */
    protected abstract void createCommand() throws CommandException;

    /**
     * parse the command and verify if it is valid
     *
     * @return Returns true if the command is in correct format
     * @Exception CommandException if the command is not in correct format
     */
    protected abstract boolean parseCommand();


    /**
     * Converts the command in the byte array.
     *
     * @return byte[]
     */
    public byte[] toByteArray() throws CommandException
    {
        if (commandBytes == null)
        {
            createCommand();
        }
        return commandBytes;
    }

    // <editor-fold defaultstate="collapsed" desc="--- get/set properties ---">
    /**
     *
     * @param i
     */
    public void setRequestId(long i)
    {
        requestId = i;
    }

    /**
     *
     * @return
     */
    public long getRequestId()
    {
        return requestId;
    }

    /**
     *
     * @param res
     */
    public void setResponse(CommandResponse res)
    {
        this.result = res;
    }

    /**
     *
     * @return
     */
    public CommandResponse getResponse()
    {
        return result;
    }

    public Object getKey()
    {
        return key;
    }

    public Object getValue()
    {
        return value;
    }

    public String getCommandName()
    {
        return name;
    }


    // </editor-fold>
    public void resetBytes()
    {
        commandBytes = null;
    }

    /**
     * Returns the command type and the parameters as a
     * <code>String</code>.
     *
     * @return a string representation of this command.
     */
    @Override
    public String toString()
    {
        return name + " : " + requestId;
    }

    /**
     *
     * @return
     */
    public String getIp()
    {
        return this.ip;
    }

    /**
     *
     * @param ip
     */
    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public static QueryInfoProtocol.QueryInfo getQueryInfoObj(HashMap queryInfoDic)
    {
        if (queryInfoDic == null)
        {
            return null;
        }
        if (queryInfoDic.size() == 0)
        {
            return null;
        }

        QueryInfoProtocol.QueryInfo.Builder builder = QueryInfoProtocol.QueryInfo.newBuilder();

        Iterator queryInfoEnum = queryInfoDic.keySet().iterator();
        while (queryInfoEnum.hasNext())
        {
            builder = builder.setHandleId((Integer) queryInfoEnum.next());

            ArrayList values = (ArrayList) queryInfoDic.get(builder.getHandleId());

            int valuesListSize = values.size();
            for (int i = 0; i < valuesListSize; i++)
            {
                Object val = values.get(i);
                if (val != null)
                {
                    if (val instanceof Date)
                    {
                        val = HelperFxn.getTicks((Date) val);
                    }
                    builder = builder.addAttributes(val.toString());
                }
                else 
                {
                    builder = builder.addAttributes("NCNULL");
                }
            }

        }
        return builder.build();
    }

    public static TagInfoProtocol.TagInfo getTagInfo(HashMap tagInfoDic)
    {
        if (tagInfoDic == null || tagInfoDic.size() == 0)
        {
            return null;
        }

        TagInfoProtocol.TagInfo.Builder builder = TagInfoProtocol.TagInfo.newBuilder();
        builder = builder.setType(String.valueOf(tagInfoDic.get("type")));

        Object obj = tagInfoDic.get("tags-list");
        if (obj instanceof ArrayList)
        {
            Iterator tagsEnum = ((ArrayList) obj).iterator();
            while (tagsEnum.hasNext())
            {
                Object current = tagsEnum.next();
                if (current != null)
                {
                    builder = builder.addTags(current.toString());
                }
            }
        }

        return builder.build();
    }

    public static NamedTagInfoProtocol.NamedTagInfo GetNamedTagInfoObj(HashMap namedTagInfoDic)
    {

        if (namedTagInfoDic == null || namedTagInfoDic.size() == 0)
        {
            return null;
        }

        NamedTagInfoProtocol.NamedTagInfo.Builder builder = NamedTagInfoProtocol.NamedTagInfo.newBuilder();
        builder = builder.setType(String.valueOf(namedTagInfoDic.get("type")));

        HashMap tagsList = (HashMap) namedTagInfoDic.get("named-tags-list");
        Iterator namedTagEnumerator = tagsList.keySet().iterator();
        while (namedTagEnumerator.hasNext())
        {
            Object current = namedTagEnumerator.next();
            if (current != null)
            {
                Object val = tagsList.get(current);
                builder = builder.addNames(current.toString());
                builder = builder.addTypes(val.getClass().getCanonicalName());
                if (val instanceof Date)
                {
                    val = HelperFxn.getTicks((Date) val);
                }
                builder = builder.addVals(val.toString());

            }
        }


        return builder.build();
    }
}
