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

import com.alachisoft.tayzgrid.common.CaseInsensitiveMap;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.runtime.exceptions.InvalidTaskEnumeratorException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author 
 */
public class TasksOutputStore {

    private HashMap tasksOutputMap = new HashMap();
    private Object syncMutext = new Object();

    public void checkinTaskOutput(String taskID, TaskOutput output) {
        synchronized (syncMutext) {
            tasksOutputMap.put(output.getTaskID(), output);
        }
    }

    public TaskEnumeratorResult getTaskEnumerator(TaskEnumeratorPointer pointer) throws InvalidTaskEnumeratorException {
        synchronized (syncMutext) {
            if (isTaskOutputExist(pointer.getTaskID())) {
                TaskOutput output = (TaskOutput) tasksOutputMap.get(pointer.getTaskID());
                TaskEnumeratorResult result=output.getEnumerator(pointer);
                if(output.isOutputDisposed())
                    tasksOutputMap.remove(pointer.getTaskID());                
                return result;
            } else {
                throw new InvalidTaskEnumeratorException("Output with task id : " + pointer.getTaskID() + " does not exist");
            }
        }
    }

    public TaskEnumeratorResult getNextRecord(TaskEnumeratorPointer pointer)throws InvalidTaskEnumeratorException {
        synchronized (syncMutext) {
            if (isTaskOutputExist(pointer.getTaskID())) {
                TaskOutput output = (TaskOutput) tasksOutputMap.get(pointer.getTaskID());
                TaskEnumeratorResult result = output.getNextRecord(pointer);
                
                if(output.isOutputDisposed())
                    tasksOutputMap.remove(pointer.getTaskID());
                
                return result;                
            } else {
                throw new InvalidTaskEnumeratorException("Output with task id : " + pointer.getTaskID() + " does not exist");
            }
        }
    }

    public boolean isTaskOutputExist(String taskID) {
        return tasksOutputMap.containsKey(taskID);
    }
    
    public void RemoveDeadClientsIterators(ArrayList clients)
    {
        Set<String> outputKeyList = tasksOutputMap.keySet();
        for(String key : outputKeyList)
        {
            TaskOutput taskOutput = (TaskOutput)tasksOutputMap.get(key);
            taskOutput.RemoveDeadClients(clients);
            if(taskOutput.isOutputDisposed())
                tasksOutputMap.remove(key);
        }
    }
}
