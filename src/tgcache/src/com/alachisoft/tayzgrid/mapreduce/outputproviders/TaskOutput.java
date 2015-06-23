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
package com.alachisoft.tayzgrid.mapreduce.outputproviders;

import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.mapreduce.TaskOutputPair;
import com.alachisoft.tayzgrid.mapreduce.ReducerInput;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.exceptions.InvalidTaskEnumeratorException;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyValuePair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author 
 */
public class TaskOutput {

    private String taskID;
    private ArrayList<TaskOutputPair> output;
    private List listeners;
    private HashMap enumerators;    
    private boolean disposeOutput;

    public TaskOutput(String taskID, ArrayList<TaskOutputPair> output, List listeners) {
        this.taskID = taskID;
        this.output = output;
        this.listeners = listeners;
    }

    /**
     * @return the taskID
     */
    public String getTaskID() {
        return taskID;
    }

    /**
     * @return the output
     */
    public Object getOutput() {
        return output;
    }

    TaskEnumeratorResult getEnumerator(TaskEnumeratorPointer pointer) throws InvalidTaskEnumeratorException {
        
        if(enumerators==null)
        {
            enumerators=new HashMap();
        }
        
        if (enumerators.containsKey(pointer)) {
            throw new InvalidTaskEnumeratorException("Enumerator already exists with specifed pointer");
        }
        if(!isValidPointer(pointer))
            throw new InvalidTaskEnumeratorException("Invalid Enumerator Pointer Specified");


        Iterator it = output.iterator();
        enumerators.put(pointer, it);
        return getNextRecord(pointer);
    }

    TaskEnumeratorResult getNextRecord(TaskEnumeratorPointer pointer) throws InvalidTaskEnumeratorException {
        if (!enumerators.containsKey(pointer)) {
            throw new InvalidTaskEnumeratorException("Enumerator does not exists with specified pointer");
        }

        TaskEnumeratorResult result = new TaskEnumeratorResult();
        result.setPointer(pointer);
        Iterator it = (Iterator) enumerators.get(pointer);
        if (it.hasNext()) {
            TaskOutputPair pair = (TaskOutputPair) it.next();
            result.setRecordSet(new HashMap.SimpleEntry(pair.getKey(), pair.getValue()));
        }

        result.setNodeAddress(null);

        if(!it.hasNext())
        {
            result.setIsLastResult(true);
            enumerators.remove(pointer);
            
            removeFromListeners(pointer);
            
            if(enumerators.isEmpty() && listeners.isEmpty())
                disposeOutput=true;
        }
        return result;
    }

    /**
     * @return the disposeOutput
     */
    public boolean isOutputDisposed() {
        return disposeOutput;
    }
    
    public void RemoveDeadClients(ArrayList clients)
    {
        for(Object client : clients)
        {
            listeners.remove(client);
            Set<TaskEnumeratorPointer> keySet = enumerators.keySet();
            for(TaskEnumeratorPointer pointer : keySet)
            {
                if(pointer.getClientAddress().getIpAddress().toString().equals(client))
                {
                    enumerators.remove(pointer);
                }
            }
        }
        if(enumerators.isEmpty())
            disposeOutput = true;
    }
    
    private boolean isValidPointer(TaskEnumeratorPointer pointer)
    {
        if(listeners!=null)
        {
           Iterator itListeners = listeners.iterator();
           while(itListeners.hasNext())
           {
               TaskCallbackInfo callBackInfo=(TaskCallbackInfo) itListeners.next();
               if(callBackInfo.getClient().equals(pointer.getClientId()) &&((Short)callBackInfo.getCallback()).equals(pointer.getCallbackID()))
               {
                   return true;
               }
           }
        }
        
        return false;
    }
    
     private boolean removeFromListeners(TaskEnumeratorPointer pointer)
    {
        if(listeners!=null)
        {
           Iterator itListeners = listeners.iterator();
           while(itListeners.hasNext())
           {
               TaskCallbackInfo callBackInfo=(TaskCallbackInfo) itListeners.next();
               if(callBackInfo.getClient().equals(pointer.getClientId()) &&((Short)callBackInfo.getCallback()).equals(pointer.getCallbackID()))
               {
                   listeners.remove(callBackInfo);
                   return true;
               }
           }
        }
        
        return false;
    }
}
