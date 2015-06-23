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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author 
 */
 public class TaskEnumeratorResult implements InternalCompactSerializable
    {
        private TaskEnumeratorPointer pointer = null;
        private Map.Entry recordSet = null;
        private String nodeAddress = null;
        private boolean isLastResult;

        public TaskEnumeratorResult() 
        {
            
        }
		
    /**
     * @return the recordSet
     */
    public Map.Entry getRecordSet() {
        return recordSet;
    }

    /**
     * @param recordSet the recordSet to set
     */

    public void setRecordSet(Map.Entry recordSet) {
        this.recordSet = recordSet;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public boolean getIsLastResult() {
        return isLastResult;
    }

    public void setIsLastResult(boolean isLastResult) {
        this.isLastResult = isLastResult;
    }

    /**
     * @return the pointer
     */
    public TaskEnumeratorPointer getPointer() {
        return pointer;
    }

    /**
     * @param pointer the pointer to set
     */
    public void setPointer(TaskEnumeratorPointer pointer) {
        this.pointer = pointer;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException 
    {
        setPointer((TaskEnumeratorPointer) reader.ReadObject());
        setRecordSet((Map.Entry) reader.ReadObject());
        setNodeAddress((String) reader.ReadObject());
        setIsLastResult(reader.ReadBoolean());               
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException 
    {
        writer.WriteObject(getPointer());
        writer.WriteObject(getRecordSet());
        writer.WriteObject(getNodeAddress());
        writer.Write(isLastResult);        
    }
 }
