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
package com.alachisoft.tayzgrid.web.mapreduce;

import com.alachisoft.tayzgrid.runtime.mapreduce.TrackableTask;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceListener;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceResponse;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskCompletionStatus;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskManagement;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import java.util.Enumeration;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapReduceTaskResult<OutputKey extends Object, OutputValue extends Object>
        implements MapReduceListener, TrackableTask {

    TaskManagement _cache = null;

    private String taskId = null;
    private TaskEnumerator mrTaskEnumerator = null;
    private MapReduceListener mapReduceCallback = null;
    private Object mutext = new Object();
    private boolean asyncGet = false;
    private boolean syncGet = false;
    private boolean callbackReceived = false;
    private TaskCompletionStatus taskStatus = null;
    private MapReduceResponse _response;

    public MapReduceTaskResult(TaskManagement cache, String taskId) {
        this._cache = cache;
        this.taskId = taskId;
//        mrTaskOutput = new MRResultIterator<OutputKey, OutputValue>(new ArrayList<String>());

    }

    @Override
    public Enumeration getResult() throws OperationFailedException{
        return getResult(Long.MAX_VALUE);
    }

    @Override
    public Enumeration getResult(Long timeout) throws OperationFailedException{
        if (timeout == null) {
            throw new IllegalArgumentException("timeout cannot be null.");
        }
        if (!asyncGet) {
            syncGet = true;
            synchronized (mutext) {
                if (_response == null) {
                    try {
                        Monitor.wait(mutext, timeout);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MapReduceTaskResult.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            
                if (!callbackReceived) {
                    throw new OperationFailedException("getResult request timed out.");
                }
                if (_response != null){
                        if (_response.getStatus() == TaskCompletionStatus.Cancelled) {
                            throw new OperationFailedException("Task was cancelled.");
                        } else if (_response.getStatus() == TaskCompletionStatus.Failure){
                            throw new OperationFailedException("Task was failed.");
                        }
                    }
                return mrTaskEnumerator;
            }
        } else {
            throw new OperationFailedException("You have already registered callback (async method for get result)");
        }
    }

    /**
     * @return the taskId
     */
    @Override
    public String getTaskId() {
        return taskId;
    }

    /**
     * @param mapReduceCallback the mapReduceCallback to set
     */
    @Override
    public void setTaskCallback(MapReduceListener mapReduceCallback) throws OperationFailedException {
        if (!syncGet) {
            this.mapReduceCallback = mapReduceCallback;
            if(_response!=null)
                this.mapReduceCallback.onTaskResult(_response);
            asyncGet = true;
        } else {
            throw new OperationFailedException("You have already made synchronous call to get result)");
        }
    }

    // <editor-fold desc=" ------- Task Management -------- ">
    @Override
    public void cancelTask() throws OperationFailedException {
        if (this.getTaskId() != null || !this.getTaskId().equals("")) {
            _cache.cancelTask(this.getTaskId());
        } else {
            throw new OperationFailedException();
        }
    }

    @Override
    public TaskStatus getTaskStatus() throws GeneralFailureException {
            TaskStatus taskStatus = null;
        if (callbackReceived) {
            switch(this.taskStatus)
            {
                case Cancelled:
                    taskStatus = new TaskStatus(TaskStatus.Status.Cancelled, 0);
                    break;
                case Failure:
                    taskStatus = new TaskStatus(TaskStatus.Status.Failed, 0);
                    break;
                case Success:
                    taskStatus = new TaskStatus(TaskStatus.Status.Completed, 0);
                    break;
            }
        }
        else {
            if (this.getTaskId() != null || !this.getTaskId().equals("")) {
                try {
                    taskStatus = _cache.getTaskProgress(this.getTaskId());
                } catch (Exception ex) {
                    throw new GeneralFailureException(ex.getMessage());
                }
            } else {
                throw new GeneralFailureException();
            }

            if (taskStatus != null) {
                return taskStatus;
            }
        }
        return taskStatus;
    }

    // </editor-fold>
    // <editor-fold desc="  ------------   Task Listener  -----------   ">
    @Override
    public void onTaskResult(MapReduceResponse response) {
        synchronized (mutext) {
            try {
                taskStatus = response.getStatus();
                response.setCache(_cache);
                _response = response;
                if (taskStatus == TaskCompletionStatus.Success) {
                    mrTaskEnumerator = (TaskEnumerator) _response.getResult();
                }

                callbackReceived = true;
                Monitor.pulse(mutext);
            } catch (OperationFailedException ex) {
                ex.printStackTrace();
            }
        }

        if (this.mapReduceCallback != null) {
            this.mapReduceCallback.onTaskResult(response);
        }

    }
    // </editor-fold>

}
