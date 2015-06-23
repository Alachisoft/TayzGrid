package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetNextRecordCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetTaskEnumeratorCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorPointerProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import java.io.IOException;

/**
 *
 * @author 
 */


public class GetTaskEnumeratorCommand extends Command {

    String taskId;
    short callbackId;
    
    public GetTaskEnumeratorCommand(String taskId, short callbackId)
    {
        this.taskId = taskId;
        this.callbackId = callbackId;
    }
    
    @Override
    public CommandType getCommandType() {
        return CommandType.GET_TASK_ENUMERATOR;
    }

    @Override
    public RequestType getCommandRequestType() {
        return RequestType.BulkRead;
    }

    @Override
    protected void createCommand() throws CommandException {
        try {
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                    .setGetTaskEnumeratorCommand(GetTaskEnumeratorCommandProtocol.GetTaskEnumeratorCommand
                                    .newBuilder().setPointer(TaskEnumeratorPointerProtocol.TaskEnumeratorPointer.newBuilder()
                    .setCallbackId((int)callbackId)
                    .setTaskId(taskId).build()))
                    .setRequestID(this.getRequestId())
                    .setClientLastViewId(this.getClientLastViewId())
                    .setType(CommandProtocol.Command.Type.GET_TASK_ENUMERATOR).build().toByteArray());

        } catch (IOException ex) {
            throw new CommandException(ex.getMessage());
        }
    }

    @Override
    protected boolean parseCommand() {
        return true;
    }

    
}
