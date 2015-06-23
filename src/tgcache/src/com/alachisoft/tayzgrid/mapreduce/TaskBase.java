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

import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallBack;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import java.util.Collections;

/**
 *
 * @author tayzgrid_dev
 */


public abstract class TaskBase implements Task
{
    private String taskID=null;
    private TaskCallBack callBack=null;
    private TaskType type=null;
    private java.util.List callbackListeners = Collections.synchronizedList(new java.util.ArrayList(2));

    
    public String getTaskID()
    {
        return taskID;
    }
    public void setTaskID(String id)
    {
        taskID=id;
    }
    
    public void setTaskCallBack(TaskCallBack cb)
    {
        callBack=cb;
    }
    public TaskCallBack getTaskCallBack()    
    {
        return callBack;
    }

    /**
     * @return the type
     */
    public TaskType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(TaskType type) {
        this.type = type;
    }

    public void addTaskCallBackInfo(TaskCallbackInfo taskCallbackInfo)
    {
         if (getCallbackListeners() != null && !getCallbackListeners().contains(taskCallbackInfo)) {
            getCallbackListeners().add(taskCallbackInfo);
        }
    }
    
    public void removeTaskCallBackInfo(TaskCallbackInfo taskCallbackInfo)
    {
         if (getCallbackListeners() != null && getCallbackListeners().contains(taskCallbackInfo)) {
            getCallbackListeners().remove(taskCallbackInfo);
        }
    }

    /**
     * @return the callbackListeners
     */
    public java.util.List getCallbackListeners() {
        return callbackListeners;
    }
}
