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
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.common.protobuf.IsLockedCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.IsLockedResponseProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;

import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import java.io.IOException;

public class IsLockedCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;
        public Object Key;
        public BitSet FlagMap;
        public Object LockId;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Key = this.Key;
            varCopy.FlagMap = this.FlagMap;
            varCopy.LockId = this.LockId;

            return varCopy;
        }
    }

    //PROTOBUF
    @Override
    public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)
    {
        CommandInfo cmdInfo = new CommandInfo();
        ICommandExecuter tempVar = clientManager.getCmdExecuter();
        TayzGrid nCache = (TayzGrid) ((tempVar instanceof TayzGrid) ? tempVar : null);
            
        try
        {
            cmdInfo = ParseCommand(command, clientManager, nCache.getCacheId()).clone();
        }
        catch (IllegalArgumentException arEx)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("IsLockedCommand", "command: " + command + " Error" + arEx);
            }
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(arEx, command.getRequestID()));
            }
            return;
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
            Object lockId = cmdInfo.LockId;
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            java.util.Date lockDate = time.getDate();

            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            boolean res = nCache.getCache().IsLocked(cmdInfo.Key, tempRef_lockId, tempRef_lockDate, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;


            //PROTOBUF:RESPONSE
            ResponseProtocol.Response response = ResponseProtocol.Response.newBuilder().setIsLockedResponse(IsLockedResponseProtocol.IsLockedResponse.newBuilder()
                    .setIsLocked(res)
                    .setLockId(lockId.toString())
                    .setLockTime(new NCDateTime(lockDate).getTicks()))
                    .setRequestId(Long.parseLong(cmdInfo.RequestId))
                    .setResponseType(ResponseProtocol.Response.Type.ISLOCKED).build();

            _serializedResponsePackets.add(ResponseHelper.SerializeResponse(response));

        }
        catch (Exception exc)
        {
            //PROTOBUF:RESPONSE
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
    }

    //PROTOBUF
    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();

        IsLockedCommandProtocol.IsLockedCommand isLockedCommand = command.getIsLockedCommand();

        cmdInfo.Key = CacheKeyUtil.Deserialize(isLockedCommand.getKey(), serializationContext);
        cmdInfo.LockId =  isLockedCommand.getLockId() ;
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();

        return cmdInfo;
    }
}
