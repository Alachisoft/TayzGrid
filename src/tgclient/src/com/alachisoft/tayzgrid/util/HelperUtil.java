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

package com.alachisoft.tayzgrid.util;

import java.io.UnsupportedEncodingException;
import com.alachisoft.tayzgrid.command.CommandOptions;
import com.alachisoft.tayzgrid.command.CommandResponse;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
public class HelperUtil {

    /**
     *
     * @param client
     * @throws java.io.IOException
     * @return
     */
    public static CommandResponse AssureRecieve(Socket client) throws IOException {
        InputStream stream = client.getInputStream();

        byte[] buffer = new byte[CommandOptions.COMMAND_SIZE];
        AssureRecieve(stream, buffer);

        String s = new String(buffer, 0, CommandOptions.COMMAND_SIZE);
        int commandSize = Integer.parseInt(s.trim());

        CommandResponse resultItem = new CommandResponse(false);
        if (commandSize == 0) {
            return resultItem;
        }

        buffer = new byte[commandSize];
        AssureRecieve(stream, buffer);
        resultItem.setRawResult(buffer);

        return resultItem;
    }

    /**
     *
     * @param reader
     * @param charBuffer
     * @throws java.io.IOException
     */
    private static void AssureRecieve(InputStream reader, byte[] buffer) throws IOException {
        int bytesRecieved = 0;

        do{
            bytesRecieved += reader.read(buffer, bytesRecieved, (buffer.length - bytesRecieved));
        }while(bytesRecieved < buffer.length);
    }

    /**
     *
     * @param cmdString
     * @param data
     * @throws java.io.UnsupportedEncodingException
     * @return
     */
    public static byte[] ConstructSendData(String cmdString, byte[] data) throws UnsupportedEncodingException {
        byte[] command = cmdString.getBytes("UTF-8");
        byte[] buffer = new byte[CommandOptions.COMMAND_SIZE + CommandOptions.DATA_SIZE + command.length + data.length];

        byte[] commandSize = String.valueOf(command.length).getBytes("UTF-8");
        byte[] dataSize = String.valueOf(data.length).getBytes("UTF-8");

        System.arraycopy(commandSize, 0, buffer, 0, commandSize.length);
        System.arraycopy(dataSize, 0, buffer, CommandOptions.COMMAND_SIZE, dataSize.length);
        System.arraycopy(command, 0, buffer, CommandOptions.TOTAL_SIZE, command.length);
        System.arraycopy(data, 0, buffer, CommandOptions.TOTAL_SIZE + command.length, data.length);

        return buffer;
    }

    /**
     *
     * @param _client
     * @param buffer
     * @throws java.io.IOException
     */
    public static void AssureSend(Socket _client, byte[] buffer) throws IOException {
        int dataSent = 0, dataLeft = buffer.length;
        Writer writer = new BufferedWriter(new OutputStreamWriter(_client.getOutputStream()));

        writer.write(new String(buffer));
        writer.flush();
    }

    public static CacheItemVersion createCacheItemVersion(long version)
    {
        CacheItemVersion itemVersion = new CacheItemVersion();
        itemVersion.setVersion(version);
        return itemVersion;
    }

}
