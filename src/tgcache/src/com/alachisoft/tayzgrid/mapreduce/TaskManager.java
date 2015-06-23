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

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.mapreduce.outputproviders.TasksOutputStore;
import com.alachisoft.tayzgrid.runtime.exceptions.InvalidTaskEnumeratorException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tayzgrid_dev
 */


public class TaskManager implements IDisposable
{
    private TaskTracker taskTracker = null;
    private CacheRuntimeContext context=null;
    private TasksOutputStore taskOutPutStore;

    
    /**
     *
     * @param properties
     * @param context
     */
    public TaskManager(java.util.Map properties, CacheRuntimeContext context)
    {
        this.context=context;
        this.taskOutPutStore=new TasksOutputStore();
        this.taskTracker=new TaskTracker(properties,this.context,taskOutPutStore);            
    }
    
    /**
     * @return the taskTracker
     */
    public TaskTracker getTaskTracker()
    {
        return taskTracker;
    }

    /**
     * @param taskTracker the taskTracker to set
     */
    public void setTaskTracker(TaskTracker taskTracker) 
    {
        this.taskTracker = taskTracker;
    }
    
    @Override
    public void dispose()
    {       
        this.taskTracker.dispose();
    }

    public Object TaskOperationRecieved(MapReduceOperation operation) throws OperationFailedException
    {
        try {
            //In future user may configure different TaskTracker so we will select one and pass the operation
            return taskTracker.TaskOperationRecieved(operation);
        } catch (InvalidTaskEnumeratorException ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }
    
    public void DeadClients(ArrayList clients) 
    {
        if(taskTracker!=null)
            taskTracker.RemoveDeadClientsTasks(clients);
        if(taskOutPutStore != null)
            taskOutPutStore.RemoveDeadClientsIterators(clients);
        //Remove Iterators from the task output list
        //Remove Listeners for these clients     
    }
}
