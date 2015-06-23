package com.alachisoft.tayzgrid.web.mapreduce;

import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;

class TaskPartitionedEnumerator<OutputKey,OutputValue> implements Enumeration<Map.Entry<OutputKey,OutputValue>> {
    
        private TaskEnumeratorPointer pointer = null;
        private Map.Entry recordSet = null;
        private String nodeAddress = null;
        private boolean IsLastResult = false;
        private TaskEnumeratorCache _cache;
        private short callbackId;
        private int localRecordSetCount = 0;
        
        private boolean isValid;
        
        public TaskPartitionedEnumerator(TaskEnumeratorCache cache, TaskEnumeratorPointer pointer, Map.Entry recordSet, String nodeAddress, boolean isLastResult)
        {
            _cache = cache;
            this.pointer = pointer;
            this.recordSet = recordSet;
            this.nodeAddress = nodeAddress;
            this.IsLastResult = isLastResult;
            this.callbackId = pointer.getCallbackID();
            
            if(recordSet != null)
                isValid = true;
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

    /**
     * @return the nodeAddress
     */
    public String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * @param nodeAddress the nodeAddress to set
     */
    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    /**
     * @return the IsLastResult
     */
    public boolean IsLastResult() {
        return IsLastResult;
    }

    /**
     * @param IsLastResult the IsLastResult to set
     */
    public void setIsLastResult(boolean IsLastResult) {
        this.IsLastResult = IsLastResult;
    }

    @Override
    public boolean hasMoreElements() {
        return isValid();
    }

    @Override
    public Map.Entry<OutputKey,OutputValue> nextElement() {
        Map.Entry<OutputKey, OutputValue> current = recordSet;
        localRecordSetCount++;
        if(!IsLastResult)
        {
            try {
                TaskEnumeratorResult enumeratorResultSet = _cache.getNextRecord(pointer.getClusterAddress().getIpAddress().getHostAddress(),pointer);
                if(enumeratorResultSet != null) {
                    recordSet = enumeratorResultSet.getRecordSet();
                    
                    IsLastResult = enumeratorResultSet.getIsLastResult();
                    isValid = true;
                }
            }
            catch (OperationFailedException ex) 
            {
                isValid = false;
                throw  new NoSuchElementException("Output corrupted on node : " + pointer.getClientAddress().toString());
            }
        }
        else
        {
            isValid = false;
        }
        
        return current;
    }

    /**
     * @return the isValid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * @param isValid the isValid to set
     */
    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
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
}
