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

package com.alachisoft.tayzgrid.management.rpc;

import com.alachisoft.tayzgrid.common.communication.IChannelFormatter;
import com.alachisoft.tayzgrid.common.protobuf.util.Serializer;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand.Builder;
import com.alachisoft.tayzgrid.common.protobuf.ManagementResponseProtocol.ManagementResponse;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagementChannelFormatter implements IChannelFormatter
{

    @Override
    public final byte[] Serialize(Object graph) throws IOException
    {
        byte[] bufffer = null;
        ManagementCommand command = (ManagementCommand) ((graph instanceof ManagementCommand) ? graph : null);
        Builder builder = command.toBuilder();
        builder.setArguments(ByteString.copyFrom(CompactBinaryFormatter.toByteBuffer(command.getParameters(), "")));
        builder.setRequestId(command.getRequestId());
        bufffer = builder.build().toByteArray();
        return bufffer;
    }

    public final Object Deserialize(byte[] buffer) throws InvalidProtocolBufferException
    {
        ManagementResponse response = null;
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);

        response = ManagementResponse.parseFrom(buffer);
        if (response != null)
        {
            if (response.getReturnVal() != null)
            {
                try
                {

                    response.toBuilder();
                    if (response.getReturnVal().toByteArray().length != 0)
                    {
                        response.set_returnValue(CompactBinaryFormatter.fromByteBuffer(response.getReturnVal().toByteArray(), ""));
                    }
                    return response;
                }
                catch (ClassNotFoundException ce)
                {
                    Logger.getLogger(ManagementChannelFormatter.class.getName()).log(Level.SEVERE, null, ce);
                }
                catch (IOException ex)
                {
                    Logger.getLogger(ManagementChannelFormatter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;//Unalbe to build buidler
    }
}
