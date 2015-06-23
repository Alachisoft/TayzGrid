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

package com.alachisoft.tayzgrid.common.communication;

import com.alachisoft.tayzgrid.common.protobuf.util.Serializer;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand;
import com.alachisoft.tayzgrid.common.protobuf.ManagementResponseProtocol.ManagementResponse;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtoBuffFormatter implements IChannelFormatter
{

    @Override
    public final byte[] Serialize(Object graph)
    {
        ManagementCommand command = (ManagementCommand) ((graph instanceof ManagementCommand) ? graph : null);

        try
        {
        }
        finally
        {
        }


        try
        {
            byte[] argumentBuffer = null;
            return command.toBuilder().build().toByteArray();
        }
        finally
        {
        }

    }

    public final Object Deserialize(byte[] buffer) throws InvalidProtocolBufferException
    {
        try
        {
            return ManagementResponse.parseFrom(buffer);
        }
        finally
        {
        }
    }
}
