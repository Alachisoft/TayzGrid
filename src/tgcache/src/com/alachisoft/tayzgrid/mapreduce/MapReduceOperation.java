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

package com.alachisoft.tayzgrid.mapreduce;

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class MapReduceOperation implements ICompactSerializable
{

    private MapReduceOpCodes _opCode = MapReduceOpCodes.values()[0];
    private Object _taskID;
    private Object _data;   
    private TaskCallbackInfo callbackInfo;
    private Address _source;
    private Long sequenceID;
    private OperationContext operationContext;
    
    public MapReduceOperation()
    {
    } 

    /**
     * Gets/Sets the Operation code of the operation.
     */
    public final MapReduceOpCodes getOpCode()
    {
        return _opCode;
    }

    public final void setOpCode(MapReduceOpCodes value)
    {
        _opCode = value;
    }
    
    
    /**
     * @return the _taskID
     */
    public Object getTaskID() {
        return _taskID;
    }

    /**
     * @param _taskID the _taskID to set
     */
    public void setTaskID(Object _taskID) {
        this._taskID = _taskID;
    }
    
    public void setCallbackInfo(TaskCallbackInfo value)
    {
        callbackInfo = value;
    }
    public TaskCallbackInfo getCallbackInfo()
    {
        return callbackInfo;
    }

    /**
     * Gets/Sets the data associated with this operation.
     */
    public final Object getData()
    {
        return _data;
    }

    public final void setData(Object value)
    {
        _data = value;
    }

    /**
     * @return the _source
     */
    public Address getSource() {
        return _source;
    }

    /**
     * @param _source the _source to set
     */
    public void setSource(Address _source) {
        this._source = _source;
    }

    /**
     * @return the sequenceID
     */
    public Long getSequenceID() {
        return sequenceID;
    }

    /**
     * @param sequenceID the sequenceID to set
     */
    public void setSequenceID(Long sequenceID) {
        this.sequenceID = sequenceID;
    }

    /**
     * @return the operationContext
     */
    public OperationContext getOperationContext() {
        return operationContext;
    }

    /**
     * @param operationContext the operationContext to set
     */
    public void setOperationContext(OperationContext operationContext) {
        this.operationContext = operationContext;
    }
    
        
    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeByte(_opCode.getValue());    
        writer.writeObject(_taskID);
        writer.writeObject(_data);
        writer.writeObject(_source);
        writer.writeObject(sequenceID);
        writer.writeObject(operationContext);
        writer.writeObject(callbackInfo);
        writer.flush();
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        _opCode = MapReduceOpCodes.forValue(reader.readByte());
        _taskID=reader.readObject();
        _data = reader.readObject();
        _source = (Address) reader.readObject();
        sequenceID = (Long) reader.readObject();
        operationContext=(OperationContext)reader.readObject();
        callbackInfo = (TaskCallbackInfo)reader.readObject();
    }

}
