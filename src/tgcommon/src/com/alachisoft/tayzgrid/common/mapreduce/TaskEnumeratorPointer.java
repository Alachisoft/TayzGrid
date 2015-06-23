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

package com.alachisoft.tayzgrid.common.mapreduce;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

/**
 *
 * @author 
 */
public class TaskEnumeratorPointer implements InternalCompactSerializable
{
    private String taskID;
    private Short callbackID;
    private String clientId;
    private Address clientAddress;
    private Address clusterAddress;
    
    public TaskEnumeratorPointer() {}
             
    public TaskEnumeratorPointer(String clientID, String taskID,short callbackID)
    {
        this.clientId = clientID;
        this.taskID=taskID;
        this.callbackID=callbackID;
    }
    
    @Override
    public boolean equals(Object obj) 
    {        
        if (obj instanceof TaskEnumeratorPointer) 
        {
            TaskEnumeratorPointer other = (TaskEnumeratorPointer) ((obj instanceof TaskEnumeratorPointer) ? obj : null);
            
            if (!other.getClientId().equals(clientId)) 
            {
                return false;
            }
            
            if (!other.getCallbackID().equals(callbackID.shortValue()))
            {
                return false;
            }
            
            return true;
        }
        return false;
    }

    /**
     * @return the taskID
     */
    public String getTaskID() {
        return taskID;
    }

    /**
     * @return the callbackID
     */
    public Short getCallbackID() {
        return callbackID;
    }
    
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        setClientAddress(Common.as(reader.ReadObject(), Address.class));
        setClusterAddress(Common.as(reader.ReadObject(), Address.class));
        clientId = Common.as(reader.ReadObject(), String.class);
        taskID = Common.as(reader.ReadObject(), String.class);
        callbackID = reader.ReadInt16();        
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(getClientAddress());
        writer.WriteObject(getClusterAddress());
        writer.WriteObject(clientId);
        writer.WriteObject(taskID);
        writer.Write(callbackID);
    }
    
    @Override
    public int hashCode()
    {
        return taskID.hashCode() + callbackID.hashCode() + getClientId().hashCode();
    }

    /**
     * @return the clientAddress
     */
    public Address getClientAddress() {
        return clientAddress;
    }

    /**
     * @param clientAddress the clientAddress to set
     */
    public void setClientAddress(Address clientAddress) {
        this.clientAddress = clientAddress;
    }

    /**
     * @return the clusterAddress
     */
    public Address getClusterAddress() {
        return clusterAddress;
    }

    /**
     * @param clusterAddress the clusterAddress to set
     */
    public void setClusterAddress(Address clusterAddress) {
        this.clusterAddress = clusterAddress;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
