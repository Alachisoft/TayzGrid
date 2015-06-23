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
import com.alachisoft.tayzgrid.common.protobuf.GetCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetResponseProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;

public class GetCommand extends CommandBase
{

    private final static class CommandInfo
    {

        public String RequestId;
        public Object Key;
        public String Group;
        public String SubGroup;
        public BitSet FlagMap;
        public LockAccessType LockAccessTypes = LockAccessType.values()[0];
        public Object LockId;
        public TimeSpan LockTimeout = new TimeSpan();
        public long CacheItemVersion;
        public String ProviderName;

        public CommandInfo clone()
        {
            CommandInfo varCopy = new CommandInfo();

            varCopy.RequestId = this.RequestId;
            varCopy.Key = this.Key;
            varCopy.Group = this.Group;
            varCopy.SubGroup = this.SubGroup;
            varCopy.FlagMap = this.FlagMap;
            varCopy.LockAccessTypes = this.LockAccessTypes;
            varCopy.LockId = this.LockId;
            varCopy.LockTimeout = this.LockTimeout;
            varCopy.CacheItemVersion = this.CacheItemVersion;
            varCopy.ProviderName = this.ProviderName;

            return varCopy;
        }
    }
    private OperationResult _getResult = OperationResult.Success;

    @Override
    public OperationResult getOperationResult()
    {
        return _getResult;
    }

    @Override
    public boolean getCanHaveLargedata()
    {
        return true;
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
            if (ServerMonitor.getMonitorActivity())
            {
                ServerMonitor.LogClientActivity("GetCmd.Exec", "cmd parsed");
            }

        }
        catch (IllegalArgumentException arEx)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("GetCommand", "command: " + command + " Error" + arEx);
            }
            _getResult = OperationResult.Failure;
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(arEx, command.getRequestID()));
            }
            return;
        }
        catch (Exception exc)
        {
            _getResult = OperationResult.Failure;
            if (!super.immatureId.equals("-2"))
            {
                _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
            }
            return;
        }

        try
        {
            Object lockId = cmdInfo.LockId;
            long version = cmdInfo.CacheItemVersion;
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            java.util.Date lockDate = time.getDate();

            CompressedValueEntry flagValueEntry = null;

            tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            flagValueEntry = nCache.getCache().GetGroup(cmdInfo.Key, cmdInfo.FlagMap, cmdInfo.Group, cmdInfo.SubGroup, tempRef_version, tempRef_lockId, tempRef_lockDate, cmdInfo.LockTimeout, cmdInfo.LockAccessTypes, cmdInfo.ProviderName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            version = tempRef_version.argvalue;
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;

            UserBinaryObject ubObj = (flagValueEntry == null) ? null : (UserBinaryObject) flagValueEntry.Value;


            ResponseProtocol.Response.Builder resBuilder = ResponseProtocol.Response.newBuilder();
            GetResponseProtocol.GetResponse.Builder getResponse = GetResponseProtocol.GetResponse.newBuilder();
             resBuilder.setRequestId( command.getRequestID());
            resBuilder.setResponseType(ResponseProtocol.Response.Type.GET);
            if (lockId != null)
            {
                getResponse.setLockId(lockId.toString());
            }
            getResponse.setLockTime(new NCDateTime(lockDate).getTicks());
            getResponse.setVersion(version);

            if (ubObj == null)
            {
                resBuilder.setGet(getResponse.build());
                _serializedResponsePackets.add(ResponseHelper.SerializeResponse(resBuilder.build()));
            }
            else
            {
                getResponse.setFlag(flagValueEntry.Flag.getData());
                List list = ubObj.getDataList();
                for (int i = 0; i < list.size(); i++)
                {
                    getResponse.addData(ByteString.copyFrom(ubObj.getDataList().get(i)));
                }
                resBuilder.setGet(getResponse.build());
                _serializedResponsePackets.add(ResponseHelper.SerializeResponse(resBuilder.build()));
            }
        }
        catch (Exception exc)
        {
            _getResult = OperationResult.Failure;
            _serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
        }
        if (ServerMonitor.getMonitorActivity())
        {
            ServerMonitor.LogClientActivity("GetCmd.Exec", "cmd executed on cache");
        }

    }

    private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager, String serializationContext) throws IOException, ClassNotFoundException
    {
        CommandInfo cmdInfo = new CommandInfo();

        GetCommandProtocol.GetCommand getCommand = command.getGetCommand();

        cmdInfo.CacheItemVersion = getCommand.getVersion();
        cmdInfo.FlagMap = new BitSet((byte) getCommand.getFlag());

        cmdInfo.Group = getCommand.getGroup().length() == 0 ? null : getCommand.getGroup();
        cmdInfo.Key = CacheKeyUtil.Deserialize(getCommand.getKey(), serializationContext);
        cmdInfo.LockAccessTypes = LockAccessType.forValue(getCommand.getLockInfo().getLockAccessType());
        cmdInfo.LockId = getCommand.getLockInfo().getLockId();
        cmdInfo.LockTimeout = new TimeSpan(getCommand.getLockInfo().getLockTimeout());
        cmdInfo.ProviderName = getCommand.getProviderName().length() == 0 ? null : getCommand.getProviderName();
        cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
        cmdInfo.SubGroup = getCommand.getSubGroup().length() == 0 ? null : getCommand.getSubGroup();


        return cmdInfo;
    }
}
