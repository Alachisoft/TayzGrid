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

package com.alachisoft.tayzgrid.runtime.mapreduce;

import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Enumeration;

public class MapReduceResponse {
    private TaskManagement _cache;

    private Enumeration taskEnumResult = null;
    private TaskCompletionStatus status;
    private String taskId;
    private short uniqueId;
    
    public MapReduceResponse(TaskCompletionStatus status, String taskId, short uniqueId)
    {
        this.status = status;
        this.taskId = taskId;
        this.uniqueId = uniqueId;
    }
    
    /**
     * @return the callbackId
     */
    private short getUniqueId() {
        return uniqueId;
    }
    
    public TaskCompletionStatus getStatus() {
        return this.status;
    }

    public Enumeration getResult() throws OperationFailedException {
        if(taskEnumResult != null) {
            return taskEnumResult;
        } else {
            taskEnumResult = _cache.getTaskEnumerator(taskId, getUniqueId());
            return taskEnumResult;
        }
    }
    
    public void setCache(TaskManagement cache)
    {
        _cache = cache;
    }
}
