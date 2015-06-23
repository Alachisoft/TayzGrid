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

import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.GetHashmapCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetHashmapResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePairProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GetHashmapCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;

            return varCopy;
        }
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)
    {
        CommandInfo cmdInfo = new CommandInfo();

        try
        {
            cmdInfo = ParseCommand(command, clientManager).clone();
        }
        catch (Exception exc)
        {
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try
        {
            ICommandExecuter tempVar = clientManager.getCmdExecuter();
            TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
            int bucketSize = 0;
            tangible.RefObject<Integer> tempRef_bucketSize = new tangible.RefObject<Integer>(bucketSize);
            NewHashmap hashmap = nCache.getCache().GetOwnerHashMap(tempRef_bucketSize);
            bucketSize = tempRef_bucketSize.argvalue;
            byte[] buffer = new byte[0];


            
            ResponseProtocol.Response.Builder resBuilder = ResponseProtocol.Response.newBuilder();
            GetHashmapResponseProtocol.GetHashmapResponse.Builder getHashmapResponse = GetHashmapResponseProtocol.GetHashmapResponse.newBuilder();


            if (hashmap != null)
            {
                getHashmapResponse.setViewId(hashmap.getLastViewId());
                getHashmapResponse.setBucketSize(bucketSize);

                for (Iterator it = hashmap.getMembers().iterator(); it.hasNext();)
                {
                    String member = (String) it.next();
                    getHashmapResponse.addMembers(member);
                }
                Map.Entry entry;
                for (Object entryObj : (java.lang.Iterable) hashmap.getMap().entrySet())
                {
                    entry = (Map.Entry) entryObj;
                    KeyValuePairProtocol.KeyValuePair.Builder keyValue = KeyValuePairProtocol.KeyValuePair.newBuilder();
                    keyValue.setKey(entry.getKey().toString());
                    keyValue.setValue(entry.getValue().toString());

                    getHashmapResponse.addKeyValuePair(keyValue.build());
                }
            }


            resBuilder.setGetHashmap(getHashmapResponse.build());
            resBuilder.setResponseType(ResponseProtocol.Response.Type.GET_HASHMAP);
            resBuilder.setRequestId(command.getRequestID());
            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(resBuilder.build()));

        }
        catch (Exception exc)
        {
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager)
    {
        CommandInfo cmdInfo = new CommandInfo();

        GetHashmapCommandProtocol.GetHashmapCommand getHashmapCommand = command.getGetHashmapCommand();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

        return cmdInfo;
    }
}
