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
package com.alachisoft.tayzgrid.socketserver.command.responsebuilders;

import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.protobuf.GetTaskEnumeratorResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSet;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author 
 */
public class TaskResponseBuilder extends ResponseBuilderBase {

    public static void BuildGetTaskEnumeratorResponse(List<com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult> resultSetList, int commandVersion, String RequestId, java.util.List<byte[]> _serializedResponse) throws IOException {
        if (resultSetList == null) {
            return;
        }
        
        long requestId = Long.parseLong(RequestId);
        Response.Builder response =Response.newBuilder();
        response.setRequestId(requestId);
        response.setResponseType(Response.Type.GET_TASK_ENUMERATOR);

        GetTaskEnumeratorResponseProtocol.GetTaskEnumeratorResponse.Builder getTaskEnumeratorResponse =GetTaskEnumeratorResponseProtocol.GetTaskEnumeratorResponse.newBuilder();
        for(int index=0;index<resultSetList.size();index++)
        {
            com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult.Builder resultSet=ToProtobufTaskEnumeratorResult(resultSetList.get(index));
            getTaskEnumeratorResponse.addTaskEnumeratorResult(resultSet);                
        }

        response.setGetTaskEnumeratorResponse(getTaskEnumeratorResponse);
        _serializedResponse.add(com.alachisoft.tayzgrid.common.util.ResponseHelper.SerializeResponse(response.build()));

    }

    public static void BuildGetTaskNextRecordResponse(com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult result, String RequestId, List<byte[]> _serializedResponse) throws IOException {

        long requestId = Long.parseLong(RequestId);

        Response.Builder response =Response.newBuilder();
        response.setRequestId(requestId);
        response.setResponseType(Response.Type.GET_NEXT_RECORD);
       
        com.alachisoft.tayzgrid.common.protobuf.GetNextRecordResponseProtocol.GetNextRecordResponse.Builder nexRecordResponse=com.alachisoft.tayzgrid.common.protobuf.GetNextRecordResponseProtocol.GetNextRecordResponse.newBuilder();
        nexRecordResponse.setTaskEnumeratorResult(ToProtobufTaskEnumeratorResult(result));
        response.setGetNextRecordResponse(nexRecordResponse);

        _serializedResponse.add(com.alachisoft.tayzgrid.common.util.ResponseHelper.SerializeResponse(response.build()));

    }
    
    public static com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult.Builder ToProtobufTaskEnumeratorResult(TaskEnumeratorResult result) throws IOException
        {
            if (result == null)
                return null;
            com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult.Builder resultSetProto =com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult.newBuilder();
            //resultSetProto.setNodeAddress(result.getNodeAddress());
            resultSetProto.setIsLastResult(result.getIsLastResult());
            
            if(result.getPointer()!=null)
            {
                com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorPointerProtocol.TaskEnumeratorPointer.Builder pointer=com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorPointerProtocol.TaskEnumeratorPointer.newBuilder();
                //pointer.setClientId(result.getPointer().getClientID());
                pointer.setTaskId(result.getPointer().getTaskID());            
                pointer.setCallbackId(result.getPointer().getCallbackID());  
                pointer.setClientId(result.getPointer().getClientId());
                pointer.setClientIp(result.getPointer().getClientAddress().getIpAddress().getHostAddress());
                pointer.setClientPort(result.getPointer().getClientAddress().getPort());
                pointer.setClusterIp(result.getPointer().getClusterAddress().getIpAddress().getHostAddress());
                pointer.setClusterPort(result.getPointer().getClusterAddress().getPort());
                resultSetProto.setPointer(pointer);
            }
                                   
            if(result.getRecordSet()!=null)
            {
                com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorEntryProtocol.TaskEnumeratorEntry.Builder entry=com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorEntryProtocol.TaskEnumeratorEntry.newBuilder();
                entry.setKey(ByteString.copyFrom(SerializationUtil.safeSerialize(result.getRecordSet().getKey(),"",new SerializationBitSet())));
                entry.setValue(ByteString.copyFrom(SerializationUtil.safeSerialize(result.getRecordSet().getValue(),"",new SerializationBitSet())));
                resultSetProto.setEntry(entry);
            }           
            return resultSetProto;          
        }

}
