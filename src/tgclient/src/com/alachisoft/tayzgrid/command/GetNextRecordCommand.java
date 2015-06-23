package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CountCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetNextRecordCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorPointerProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;

/**
 *
 * @author 
 */


public class GetNextRecordCommand extends Command {

    String taskId;
    short callbackId;
    String clientId;
    Address clientAddress;
    Address clusterAddress;
    String intendedRecipient = "";
    
    public GetNextRecordCommand(String clientId, String taskId, short callbackId, Address _clientAddress, Address _clusterAddress)
    {
        this.taskId = taskId;
        this.callbackId = callbackId;
        this.clientId = clientId;
        this.clientAddress = _clientAddress;
        this.clusterAddress = _clusterAddress;
        this.intendedRecipient = intendedRecipient;
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.GET_NEXT_RECORD;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.BulkRead;
    }

    @Override
    protected void createCommand() throws CommandException {
         try {
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                    .setGetNextRecordCommand(GetNextRecordCommandProtocol.GetNextRecordCommand
                                    .newBuilder().setPointer(TaskEnumeratorPointerProtocol.TaskEnumeratorPointer.newBuilder()
                                            .setCallbackId((int)callbackId)
                                            .setTaskId(taskId)
                                            .setClientId(clientId)
                                            .setClientIp(clientAddress.getIpAddress().getHostAddress())
                                            .setClientPort(clientAddress.getPort())
                                            .setClusterIp(clusterAddress.getIpAddress().getHostAddress())
                                            .setClusterPort(clusterAddress.getPort()).build())
                                    .setIntendedRecipient(this.intendedRecipient))
                    .setRequestID(this.getRequestId())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setType(CommandProtocol.Command.Type.GET_NEXT_RECORD).build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }
    
}
