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

package com.alachisoft.tayzgrid.socketserver;


import com.alachisoft.tayzgrid.socketserver.command.AddAttributeCommand;
import com.alachisoft.tayzgrid.socketserver.command.AddCommand;

import com.alachisoft.tayzgrid.socketserver.command.BulkAddCommand;
import com.alachisoft.tayzgrid.socketserver.command.BulkDeleteCommand;
import com.alachisoft.tayzgrid.socketserver.command.BulkGetCommand;
import com.alachisoft.tayzgrid.socketserver.command.BulkInsertCommand;
import com.alachisoft.tayzgrid.socketserver.command.BulkRemoveCommand;
import com.alachisoft.tayzgrid.socketserver.command.ClearCommand;
import com.alachisoft.tayzgrid.socketserver.command.CommandBase;
import com.alachisoft.tayzgrid.socketserver.command.ContainsCommand;
import com.alachisoft.tayzgrid.socketserver.command.CountCommand;
import com.alachisoft.tayzgrid.socketserver.command.DeleteCommand;
import com.alachisoft.tayzgrid.socketserver.command.DeleteQueryCommand;
import com.alachisoft.tayzgrid.socketserver.command.DisposeCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetCacheItemCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetEnumeratorCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetGroupData;
import com.alachisoft.tayzgrid.socketserver.command.GetGroupKeys;
import com.alachisoft.tayzgrid.socketserver.command.GetGroupNextChunkCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetHashmapCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetKeysByTagCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetLogginInfoCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetNextChunkCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetOptimalServerCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetRunningServersCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetRunningTasksCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetTagCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetTaskEnumeratorCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetTypeInfoMap;

import com.alachisoft.tayzgrid.socketserver.command.InitSecondarySocketCommand;
import com.alachisoft.tayzgrid.socketserver.command.InitializeCommand;
import com.alachisoft.tayzgrid.socketserver.command.InsertCommand;
import com.alachisoft.tayzgrid.socketserver.command.IsLockedCommand;
import com.alachisoft.tayzgrid.socketserver.command.LockCommand;
import com.alachisoft.tayzgrid.socketserver.command.MapReduceCommand;
import com.alachisoft.tayzgrid.socketserver.command.TaskCancelCommand;
import com.alachisoft.tayzgrid.socketserver.command.NotificationRegistered;
import com.alachisoft.tayzgrid.socketserver.command.OperationResult;
import com.alachisoft.tayzgrid.socketserver.command.RaiseCustomNotifCommand;
import com.alachisoft.tayzgrid.socketserver.command.RegisterBulkKeyNotifcationCommand;
import com.alachisoft.tayzgrid.socketserver.command.RegisterKeyNotifcationCommand;
import com.alachisoft.tayzgrid.socketserver.command.RegisterTaskCallbackCommand;
import com.alachisoft.tayzgrid.socketserver.command.RemoveByTagCommand;
import com.alachisoft.tayzgrid.socketserver.command.RemoveCommand;
import com.alachisoft.tayzgrid.socketserver.command.RemoveGroupCommand;
import com.alachisoft.tayzgrid.socketserver.command.RemoveQueryCommand;
import com.alachisoft.tayzgrid.socketserver.command.SearchCommand;
import com.alachisoft.tayzgrid.socketserver.command.SearchEnteriesCommand;
import com.alachisoft.tayzgrid.socketserver.command.SyncEventCommand;
import com.alachisoft.tayzgrid.socketserver.command.TaskProgressCommand;
import com.alachisoft.tayzgrid.socketserver.command.UnRegisterKeyNoticationCommand;
import com.alachisoft.tayzgrid.socketserver.command.UnRegsisterBulkKeyNotification;
import com.alachisoft.tayzgrid.socketserver.command.UnlockCommand;
import com.alachisoft.tayzgrid.socketserver.command.VerifyLockCommand;

import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.ICommandManager;
import com.alachisoft.tayzgrid.socketserver.SocketServer;
import com.alachisoft.tayzgrid.socketserver.ConnectionManager;

import com.alachisoft.tayzgrid.common.protobuf.GetGroupCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DeleteQueryCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InitCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.SearchCommandProtocol;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.caching.SocketServerStats;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.socketserver.command.GetCacheBindingCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetCacheConfigurationCommand;
import com.alachisoft.tayzgrid.socketserver.command.GetTaskNextRecordCommand;
import com.alachisoft.tayzgrid.socketserver.command.InvokeEntryProcessorCommand;
import com.google.protobuf.InvalidProtocolBufferException;

public class CommandManager implements ICommandManager {

    @Override
    public Object Deserialize(byte[] buffer, long CommandSize) {
        Object obj = null;
        try {
            obj = (Object) CommandProtocol.Command.parseFrom(buffer);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
        }
        return obj;
    }

    public final void ProcessCommand(ClientManager clientManager, Object cmd) throws Exception {

        CommandProtocol.Command command = cmd instanceof CommandProtocol.Command ? (CommandProtocol.Command) cmd : null;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CmdMgr.PrsCmd", "enter");
        }
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CmdMgr.PrsCmd", "" + command);
        }

        CommandBase incommingCmd = null;

        switch (command.getType()) {
            case INIT:
                InitCommandProtocol.InitCommand.Builder initCommandBuilder = InitCommandProtocol.InitCommand.newBuilder();
                InitCommandProtocol.InitCommand initCommand = command.getInitCommand();
                initCommandBuilder.setRequestId(command.getRequestID());
                String tempVar = initCommand.getClientEditionId();
               
                if (tempVar.equals("JV-INITOS")||tempVar.equals("INITPROF_NEW")||tempVar.equals("INITPROF"))
                {
                    incommingCmd = new InitializeCommand();
                }

                










                break;

            case ADD:
                incommingCmd = new AddCommand();
                break;

            case ADD_BULK:
                incommingCmd = new BulkAddCommand();
                break;

            case CLEAR:
                incommingCmd = new ClearCommand();
                break;

            case CONTAINS:
                incommingCmd = new ContainsCommand();
                break;
                
            case GET_CACHE_CONFIG:
                incommingCmd = new GetCacheConfigurationCommand();
                break;

            case COUNT:
                incommingCmd = new CountCommand();
                break;

            case DISPOSE:
                incommingCmd = new DisposeCommand();
                break;

            case GET:
                incommingCmd = new GetCommand();
                break;

            case GET_BULK:
                incommingCmd = new BulkGetCommand();
                break;
            case GET_CACHE_ITEM:
                incommingCmd = new GetCacheItemCommand();
                break;

            case GET_ENUMERATOR:
                incommingCmd = new GetEnumeratorCommand();
                break;

            case GET_NEXT_CHUNK:
                incommingCmd = new GetNextChunkCommand();
                break;

            case GET_GROUP_NEXT_CHUNK:
                incommingCmd = new GetGroupNextChunkCommand();
                break;

            case GET_GROUP:
                GetGroupCommandProtocol.GetGroupCommand.Builder getGroupCommandBuilder = GetGroupCommandProtocol.GetGroupCommand.newBuilder();
                getGroupCommandBuilder.setRequestId(command.getRequestID());
                GetGroupCommandProtocol.GetGroupCommand getGroupCommand = getGroupCommandBuilder.build();
                getGroupCommand = command.getGetGroupCommand();
                if (getGroupCommand.getGetGroupKeys()) {
                    incommingCmd = new GetGroupKeys();
                } else {
                    incommingCmd = new GetGroupData();
                }

                break;
            case GET_HASHMAP:
                incommingCmd = new GetHashmapCommand();
                break;

            case GET_LOGGING_INFO:
                incommingCmd = new GetLogginInfoCommand();
                break;
            case GET_OPTIMAL_SERVER:
                incommingCmd = new GetOptimalServerCommand();
                break;
                
            case GET_CACHE_BINDING:
                incommingCmd = new GetCacheBindingCommand();
                break;

            case GET_TAG:
                incommingCmd = new GetTagCommand();
                break;

            case REMOVE_BY_TAG:
                incommingCmd = new RemoveByTagCommand();
                break;

            case GET_KEYS_TAG:
                incommingCmd = new GetKeysByTagCommand();
                break;

            case GET_TYPEINFO_MAP:
                incommingCmd = new GetTypeInfoMap();
                break;

           
         
             

            case INSERT:
                incommingCmd = new InsertCommand();
                break;

            case INSERT_BULK:
                incommingCmd = new BulkInsertCommand();
                break;

            case ISLOCKED:
                incommingCmd = new IsLockedCommand();
                break;

            case LOCK:
                incommingCmd = new LockCommand();
                break;

            case LOCK_VERIFY:
                incommingCmd = new VerifyLockCommand();
                break;

            case RAISE_CUSTOM_EVENT:
                incommingCmd = new RaiseCustomNotifCommand();
                ;
                break;

            case REGISTER_BULK_KEY_NOTIF:
                incommingCmd = new RegisterBulkKeyNotifcationCommand();
                break;

            case REGISTER_KEY_NOTIF:
                incommingCmd = new RegisterKeyNotifcationCommand();
                break;

            case REGISTER_NOTIF:
                incommingCmd = new NotificationRegistered();
                break;

            case REMOVE:
                incommingCmd = new RemoveCommand();
                break;

            case DELETE:
                incommingCmd = new DeleteCommand();
                break;

            case REMOVE_BULK:
                incommingCmd = new BulkRemoveCommand();
                break;

            case DELETE_BULK:
                incommingCmd = new BulkDeleteCommand();
                break;

            case REMOVE_GROUP:
                incommingCmd = new RemoveGroupCommand();
                break;
            case SEARCH:
                SearchCommandProtocol.SearchCommand.Builder searchCommandBuilder = SearchCommandProtocol.SearchCommand.newBuilder();
                SearchCommandProtocol.SearchCommand searchCommand = searchCommandBuilder.build();
                searchCommand = command.getSearchCommand();

                if (searchCommand.getSearchEntries()) {
                    incommingCmd = new SearchEnteriesCommand();
                } else {
                    incommingCmd = new SearchCommand();
                }

                break;

            case DELETEQUERY:
                DeleteQueryCommandProtocol.DeleteQueryCommand deleteQueryCommand = command.getDeleteQueryCommand();
                if (deleteQueryCommand.getIsRemove()) {
                    incommingCmd = new RemoveQueryCommand();
                } else {
                    incommingCmd = new DeleteQueryCommand();
                }

                break;

            case UNLOCK:
                incommingCmd = new UnlockCommand();
                break;

            case UNREGISTER_BULK_KEY_NOTIF:
                incommingCmd = new UnRegsisterBulkKeyNotification();
                break;

            case UNREGISTER_KEY_NOTIF:
                incommingCmd = new UnRegisterKeyNoticationCommand();
                break;


            case ADD_ATTRIBUTE:
                incommingCmd = new AddAttributeCommand();
                break;

            case GET_RUNNING_SERVERS:
                incommingCmd = new GetRunningServersCommand();
                break;

            case SYNC_EVENTS:
                incommingCmd = new SyncEventCommand();
                break;
            case MAP_REDUCE_TASK:
                incommingCmd = new MapReduceCommand();
                break;
            case MAP_REDUCE_TASK_CALLBACK:
                incommingCmd = new RegisterTaskCallbackCommand();
                break;
            case MAP_REDUCE_TASK_CANCEL:
                incommingCmd = new TaskCancelCommand();
                break;
            case GET_RUNNING_TASKS:
                incommingCmd = new GetRunningTasksCommand();
                break;
            case TASK_PROGRESS:
                incommingCmd = new TaskProgressCommand();
                break;
            case GET_TASK_ENUMERATOR:
                incommingCmd = new GetTaskEnumeratorCommand();
                break;
            case GET_NEXT_RECORD:
                incommingCmd = new GetTaskNextRecordCommand();
                break;
            case INVOKE_ENTRYPROCESSOR:
                incommingCmd = new InvokeEntryProcessorCommand();
                break;
        }
        if (SocketServer.getIsServerCounterEnabled()) {
            SocketServer.getPerfStatsColl().mSecPerCacheOperationBeginSample();
        }
        try{
            incommingCmd.ExecuteCommand(clientManager, command);
        }
        catch(Throwable e)
        {
           incommingCmd.getSerializedResponsePackets().add(ResponseHelper.SerializeExceptionResponse(e, command.getRequestID()));
        }
        if (SocketServer.getIsServerCounterEnabled()) {
            SocketServer.getPerfStatsColl().mSecPerCacheOperationEndSample();
            InitCommandProtocol.InitCommand initCommand = command.getInitCommand();
            String port = getCPort(clientManager.getClientID());
            
            if(!port.equals("")){
                SocketServer.getPerfStatsColl().setClientPort(initCommand.getCacheId(),getIPAddress(clientManager.getClientID()),port);
            }
        }

        if (clientManager != null && incommingCmd.getOperationResult() == OperationResult.Success) {
            if (clientManager.getCmdExecuter() != null) {
                clientManager.getCmdExecuter().UpdateSocketServerStats(new SocketServerStats(clientManager.getClientsRequests(), clientManager.getClientsBytesSent(), clientManager.getClientsBytesRecieved()));
            }
        }


        if (clientManager != null && incommingCmd.getSerializedResponsePackets() != null && !clientManager.getIsCacheStopped()) {
            if (SocketServer.getIsServerCounterEnabled()) {
                SocketServer.getPerfStatsColl().incrementResponsesPerSecStats(1);
            }
            for (byte[] reponse : incommingCmd.getSerializedResponsePackets()) {
                ConnectionManager.AssureSend(clientManager, reponse);
            }

        }
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("CmdMgr.PrsCmd", "exit");
        }
    }
    
    private String getCPort(String clientID)
    {
        String port = "";
        if(clientID != null && !clientID.isEmpty())
        {
           String [] clientIDPart = clientID.split(":");
           if(clientIDPart.length > 1)
              port =  clientIDPart[clientIDPart.length - 1];
        }
        return port;
    }
    private String getIPAddress(String clientID)
    {
        String ip="";
        if(clientID != null && !clientID.isEmpty())
        {
           String [] clientIDPart = clientID.split(":");
           if(clientIDPart.length > 2)
              ip =  clientIDPart[clientIDPart.length - 2];
        }
        return ip;
    }
    
    
    public final CommandBase GetCommandObject(String command) {
        CommandBase incommingCmd = null;

        if (command.startsWith("JV-INITOS ")) 
        {
            if (command.startsWith("JV-INIT ")) 
            {
                incommingCmd = new InitializeCommand();
            }
        }

        if (command.startsWith("INITSECONDARY ")) {
            incommingCmd = new InitSecondarySocketCommand();
        } 
        else if (command.startsWith("GETOPTIMALSERVER")) {
            incommingCmd = new GetOptimalServerCommand();
        }
        else if(command.startsWith("GETCACHEBINDING")){
            incommingCmd = new GetCacheBindingCommand();
        }
            
        else if (command.startsWith("ADD ")) {
            incommingCmd = new AddCommand();
        } else if (command.startsWith("INSERT ")) {
            incommingCmd = new InsertCommand();
        } else if (command.startsWith("GET ")) {
            incommingCmd = new GetCommand();
        } else if (command.startsWith("GETTAG ")) {
            incommingCmd = new GetTagCommand();
        } else if (command.startsWith("REMOVE ")) {
            incommingCmd = new RemoveCommand();
        } 
        else if (command.startsWith("REMOVEGROUP ")) {
            incommingCmd = new RemoveGroupCommand();
        } else if (command.startsWith("CONTAINS ")) {
            incommingCmd = new ContainsCommand();
        } else if(command.startsWith("CACHECONFIG")) {
            incommingCmd = new GetCacheConfigurationCommand();
        } else if (command.startsWith("COUNT ")) {
            incommingCmd = new CountCommand();
        } else if (command.startsWith("CLEAR ")) {
            incommingCmd = new ClearCommand();
        } else if (command.startsWith("NOTIF ")) {
            incommingCmd = new NotificationRegistered();
        } else if (command.startsWith("RAISECUSTOMNOTIF ")) {
            incommingCmd = new RaiseCustomNotifCommand();
        } else if (command.startsWith("ADDBULK ")) {
            incommingCmd = new BulkAddCommand();
        } else if (command.startsWith("INSERTBULK ")) {
            incommingCmd = new BulkInsertCommand();
        } else if (command.startsWith("GETBULK ")) {
            incommingCmd = new BulkGetCommand();
        } else if (command.startsWith("REMOVEBULK ")) {
            incommingCmd = new BulkRemoveCommand();
        } 
        else if (command.startsWith("UNLOCK ")) {
            incommingCmd = new UnlockCommand();
        } else if (command.startsWith("LOCK ")) {
            incommingCmd = new LockCommand();
        } else if (command.startsWith("ISLOCKED ")) {
            incommingCmd = new IsLockedCommand();
        } else if (command.startsWith("GETCACHEITEM ")) {
            incommingCmd = new GetCacheItemCommand();
        } else if (command.startsWith("GETGROUPKEYS ")) {
            incommingCmd = new GetGroupKeys();
        } else if (command.startsWith("GETGROUPDATA ")) {
            incommingCmd = new GetGroupData();
        } 
        else if (command.startsWith("GETENUM ")) {
            incommingCmd = new GetEnumeratorCommand();
        } else if (command.startsWith("REGKEYNOTIF ")) {
            incommingCmd = new RegisterKeyNotifcationCommand();
        } else if (command.startsWith("UNREGKEYNOTIF ")) {
            incommingCmd = new UnRegisterKeyNoticationCommand();
        } else if (command.startsWith("GETTYPEINFOMAP ")) {
            incommingCmd = new GetTypeInfoMap();
        } else if (command.startsWith("GETHASHMAP ")) {
            incommingCmd = new GetHashmapCommand();
        } else if (command.startsWith("SEARCH ")) {
            incommingCmd = new SearchCommand();
        } else if (command.startsWith("SEARCHENTERIES ")) {
            incommingCmd = new SearchEnteriesCommand();
        } else if (command.startsWith("BULKREGKEYNOTIF ")) {
            incommingCmd = new RegisterBulkKeyNotifcationCommand();
        } else if (command.startsWith("BULKUNREGKEYNOTIF ")) {
            incommingCmd = new UnRegsisterBulkKeyNotification();
        }else if (command.startsWith("DISPOSE ")) {
            incommingCmd = new DisposeCommand();
        } else if (command.startsWith("GETLOGGINGINFO ")) {
            incommingCmd = new GetLogginInfoCommand();
        }
        return incommingCmd;

    }
}
