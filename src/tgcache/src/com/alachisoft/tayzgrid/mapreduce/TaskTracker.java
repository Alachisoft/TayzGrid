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
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.mapreduce.inputproviders.CacheInputProvider;
import com.alachisoft.tayzgrid.mapreduce.notification.CallBackResult;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallBack;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskStatus;
import com.alachisoft.tayzgrid.mapreduce.outputproviders.InMemoryOutputProvider;
import com.alachisoft.tayzgrid.mapreduce.outputproviders.TasksOutputStore;
import com.alachisoft.tayzgrid.runtime.exceptions.InvalidTaskEnumeratorException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tayzgrid_dev
 */
public class TaskTracker implements IDisposable, TaskCallBack {

    private int maxTasks = 10;
    private int chunkSize = 100;
    private boolean communicateStats = false;
    private int queueSize = 10;
    private int maxExceptions = 10;
    private CacheRuntimeContext context = null;

    private final HashMap runningTasks;
    private final HashMap submittedTasks;
    private final HashMap waitingTaskMap;

    private final TreeMap taskSequenceQueue;

    private boolean stopped;

    private Long taskSequence = (long) 0;
    private int lastRunningTaskID;
    private TasksOutputStore taskOutPutStore;

    private Address clusterAddress;
    private Address clientAddress;

    private Object lockingObject = new Object();

    public TaskTracker(java.util.Map properties, CacheRuntimeContext context, TasksOutputStore store) {
        if (context.getCacheImpl() != null) {
            try {
                clientAddress = new Address(context.getRender().getIPAddress().getHostAddress(), context.getRender().getPort());
            } catch (Exception ex) {
            }
        }
        this.stopped = false;
        this.runningTasks = new HashMap();
        this.submittedTasks = new HashMap();
        this.waitingTaskMap = new HashMap();
        this.taskSequenceQueue = new TreeMap<Long, String>();
        this.context = context;
        this.taskOutPutStore = store;
        context.PerfStatsColl.setPendingTasksCount(0);
        context.PerfStatsColl.setRunningTasksCount(0);
        Initialize(properties);
    }

    private boolean isSlotAvailable() {
        return runningTasks.size() < maxTasks;
    }

    public TaskExecutionStatus startTask(String taskId, Long sequenceID) throws Exception {
        synchronized (lockingObject) {
            TaskBase task = (TaskBase) submittedTasks.remove(taskId);
            if (isSlotAvailable()) {
                if (sequenceID == lastRunningTaskID + 1) {
                    try {
                        runningTasks.put(task.getTaskID(), task);
                        context.PerfStatsColl.incrementRunningTasks();
                        lastRunningTaskID++;
                        task.startTask();
                    } catch (Exception exx) {
                        task.stopTask();
                        if (runningTasks.containsKey(task.getTaskID())) {
                            runningTasks.remove(task.getTaskID());
                            context.PerfStatsColl.decrementRunningTasks();
                        }
                        throw new OperationFailedException(exx.getMessage());
                    }

                    while (!taskSequenceQueue.isEmpty() && isSlotAvailable()) {
                        Long seqID = (Long) taskSequenceQueue.firstKey();

                        if (seqID != null && seqID == lastRunningTaskID + 1) {
                            TaskBase t = (TaskBase) waitingTaskMap.get(taskSequenceQueue.get(seqID));
                            try {
                                runningTasks.put(t.getTaskID(), t);
                                context.PerfStatsColl.incrementRunningTasks();
                                lastRunningTaskID++;
                                taskSequenceQueue.remove(seqID);
                                waitingTaskMap.remove(t.getTaskID());
                                context.PerfStatsColl.decrementPendingTasks();

                                t.startTask();

                            } catch (Exception exx) {
                                t.stopTask();
                                if (runningTasks.containsKey(t.getTaskID())) {
                                    runningTasks.remove(t.getTaskID());
                                    context.PerfStatsColl.decrementRunningTasks();
                                }
                                throw new OperationFailedException(exx.getMessage());
                            }
                        } else {
                            break;
                        }
                    }
                    return TaskExecutionStatus.Running;
                } else {
                    taskSequenceQueue.put(sequenceID, task.getTaskID());
                    waitingTaskMap.put(task.getTaskID(), task);
                    context.PerfStatsColl.incrementPendingTasks();
                    if(context.getCacheLog().getIsInfoEnabled())
                        context.getCacheLog().Info("TaskTracker.SubmitTask", "MapReduce task with task ID '" + task.getTaskID().toUpperCase() + "' is in the waiting queue.");
                    return TaskExecutionStatus.Waiting;
                }
            } else if (taskSequenceQueue.size() < queueSize) {
                taskSequenceQueue.put(sequenceID, task.getTaskID());
                waitingTaskMap.put(task.getTaskID(), task);
                context.PerfStatsColl.incrementPendingTasks();
                if(context.getCacheLog().getIsInfoEnabled())
                    context.getCacheLog().Info("TaskTracker.SubmitTask", "MapReduce task with task ID '" + task.getTaskID().toUpperCase() + "' is in the waiting queue.");
                return TaskExecutionStatus.Waiting;
            } else {
                throw new Exception("No more task can be submitted");
            }
        }
    }

    public TaskExecutionStatus submitTask(TaskBase task) throws Exception {

        if(context.getCacheLog().getIsInfoEnabled())
            context.getCacheLog().Info("TaskTracker.SubmitTask", "Task with task ID '" + task.getTaskID().toUpperCase() + "' is submitted successfully.");
//        if (context.getCacheLog() != null) {
//            context.getCacheLog().Info("TaskTracker.SubmitTask", "Task with task ID '" + task.getTaskID().toUpperCase() + "' is submitted successfully.");
//        }
        synchronized (lockingObject) {
            if (submittedTasks != null) {
                submittedTasks.put(task.getTaskID(), task);
                return TaskExecutionStatus.Submitted;
            } else {
                return TaskExecutionStatus.Failure;
            }
        }
    }

    /**
     * @return the maxThreads
     */
    public int getMaxTasks() {
        return maxTasks;
    }

    /**
     * @param maxTasks the maxTasks to set
     */
    public void setMaxTasks(int maxTasks) {
        this.maxTasks = maxTasks;
    }

    /**
     * @return the chunkSize
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * @param chunkSize the chunkSize to set
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * @return the communicateStats
     */
    public boolean isCommunicateStats() {
        return communicateStats;
    }

    /**
     * @param communicateStats the communicateStats to set
     */
    public void setCommunicateStats(boolean communicateStats) {
        this.communicateStats = communicateStats;
    }

    /**
     * @return the queueSize
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * @param queueSize the queueSize to set
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    private void Initialize(Map properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        if (properties.containsKey("tasks-config")) {
            java.util.Map taskConfig = (java.util.Map) properties.get("tasks-config");
            if (taskConfig.containsKey("max-tasks")) {
                maxTasks = (Integer.parseInt(taskConfig.get("max-tasks").toString()));
            }
            if (taskConfig.containsKey("chunk-size")) {
                chunkSize = (Integer.parseInt(taskConfig.get("chunk-size").toString()));
            }
            if (taskConfig.containsKey("communicate-stats")) {
                communicateStats = (Boolean.parseBoolean(taskConfig.get("communicate-stats").toString()));
            }
            if (taskConfig.containsKey("queue-size")) {
                queueSize = (Integer.parseInt(taskConfig.get("queue-size").toString()));
            }
            if (taskConfig.containsKey("max-avoidable-exceptions")) {
                maxExceptions = (Integer.parseInt(taskConfig.get("max-avoidable-exceptions").toString()));
            }
        }
    }

    @Override
    public void dispose() {
        cancelAllTasks();
        //dispose all task running and waiting
    }

    /**
     *
     * @param callBackInfo
     */
    @Override
    public void onCallBack(CallBackResult callBackInfo) throws OperationFailedException {
        TaskBase task = callBackInfo.getTask();
        if (runningTasks.containsKey(task.getTaskID())) {
            if (context.getCacheImpl() != null && context.getCacheInternal() != null) {
                EventContext eventContext = new EventContext();
                eventContext.setTaskStatus(callBackInfo.getStatus());
                context.getCacheInternal().NotifyTaskCallback(task.getTaskID(), task.getCallbackListeners(), false, null, eventContext);
            }

            TaskBase runningT = (TaskBase) runningTasks.get(task.getTaskID());
            runningT.stopTask();
            runningTasks.remove(task.getTaskID());
            context.PerfStatsColl.decrementRunningTasks();
            Map.Entry entry = taskSequenceQueue.firstEntry();

            if (entry != null && (Long) entry.getKey() == lastRunningTaskID + 1) {
                TaskBase t = (TaskBase) waitingTaskMap.get(entry.getValue());
                t.startTask();
                runningTasks.put(t.getTaskID(), t);
                context.PerfStatsColl.incrementRunningTasks();
                lastRunningTaskID++;

                taskSequenceQueue.remove(entry.getKey());
                waitingTaskMap.remove(t.getTaskID());
                context.PerfStatsColl.decrementPendingTasks();
            }
        }
    }

    public Object TaskOperationRecieved(MapReduceOperation operation) throws OperationFailedException, InvalidTaskEnumeratorException {
        MapReduceOpCodes opCode = operation.getOpCode();
        switch (opCode) {
            case SubmitMapReduceTask:
                return submitMapReduceTask(operation);

            case CancelTask:
                return cancelTask(operation);

            case CancelAllTasks:
                return cancelAllTasks();

            case MapperCompleted:
                return mapperCompleted(operation);

            case MapperFailed:
                return mapperFailed(operation);

            case SendReducerData:
                return recievedReducerData(operation);

            case ReducerCompleted:
                return reducerCompleted(operation);

            case ReducerFailed:
                return reducerFailed(operation);

            case GetTaskSequence:
                return getTaskSequence(operation);

            case RegisterTaskNotification:
                return registerTaskNotification(operation);

            case UnregisterTaskNotification:
                return unregisterTaskNotification(operation);

            case GetRunningTasks:
                return getRunningTasks();

            case GetTaskStats:
                return getTaskProgress(operation);

            case GetTaskEnumerator:
                return getTaskEnumerator(operation);

            case GetNextRecord:
                return getNextRecord(operation);

            case StartTask:
                return startTask(operation);

            case RemoveFromSubmittedList:
                return removeFromSubmittedList(operation);

            case RemoveFromRunningList:
                return removeFromRunningList(operation);

        }
        return null;
    }

    private Object submitMapReduceTask(MapReduceOperation operation) {
        try {
            ClusterCacheBase p = (ClusterCacheBase) ((context.getCacheImpl() != null && context.getCacheImpl() instanceof ClusterCacheBase) ? context.getCacheImpl() : null);

            com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask userTask = (com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask) operation.getData();
            if (userTask != null) {
                com.alachisoft.tayzgrid.mapreduce.MapReduceTask serverTask = new MapReduceTask(p, this, userTask.getMapper(), userTask.getCombiner(), userTask.getReducer(), userTask.getMapReduceInputProvider() == null ? new CacheInputProvider((CacheBase) getContext().getCacheInternal(), userTask.getFilter() != null ? userTask.getFilter().getQueryFilter() : null, context) : userTask.getMapReduceInputProvider(), new InMemoryOutputProvider(taskOutPutStore), userTask.getFilter() != null ? userTask.getFilter().getKeyFilter() : null, context, this.chunkSize, this.maxExceptions);

                serverTask.setTaskID((String) (operation.getTaskID()));

                serverTask.addTaskCallBackInfo(operation.getCallbackInfo());
                return this.submitTask(serverTask);
            }

        } catch (Exception ex) {
            return ex;
        }

        return null;
    }

    private Object cancelTask(MapReduceOperation operation) {
        String taskID = (String) operation.getTaskID();
        if (runningTasks.containsKey(taskID)) {
            TaskBase t = (TaskBase) runningTasks.get(taskID);
            t.stopTask();

            if (context.getCacheImpl() != null && context.getCacheInternal() != null) {
                EventContext eventContext = new EventContext();
                eventContext.setTaskStatus(TaskStatus.Cancelled);
                context.getCacheInternal().NotifyTaskCallback(t.getTaskID(), t.getCallbackListeners(), false, null, eventContext);
            }
            runningTasks.remove(taskID);
            context.PerfStatsColl.decrementRunningTasks();
            if(context.getCacheLog().getIsInfoEnabled())
                context.getCacheLog().Info("TaskTracker.CancelTask", "Task with task ID '" + taskID.toUpperCase() + "' has been cancelled.");
        } else {
            context.getCacheLog().Error("TaskTracker.CancelTask", "Task with task ID '" + taskID.toUpperCase() + "' does not exist.");
        }

        return true;
    }

    private Object mapperCompleted(MapReduceOperation operation) throws OperationFailedException {
        synchronized (lockingObject) {
            String taskID = (String) operation.getTaskID();
            if (runningTasks.containsKey(taskID)) {
                MapReduceTask t = (MapReduceTask) runningTasks.get(taskID);
                t.mapperCompleted(operation);
            }
            return true;
        }
    }

    private Object recievedReducerData(MapReduceOperation operation) {
        String taskID = (String) operation.getTaskID();
        if (runningTasks.containsKey(taskID)) {
            MapReduceTask t = (MapReduceTask) runningTasks.get(taskID);
            t.enqueueReducerInput((ReducerInput) operation.getData());
        } else {
            if(context.getCacheLog().getIsInfoEnabled())
                context.getCacheLog().Info("TaskTracker.recievedReducerData", "task does not exist in running tasks " + taskID);
        }
        return true;
    }

    private Object reducerCompleted(MapReduceOperation operation) throws OperationFailedException {
        synchronized (lockingObject) {
            String taskID = (String) operation.getTaskID();
            if (runningTasks.containsKey(taskID)) {
                MapReduceTask t = (MapReduceTask) runningTasks.get(taskID);
                t.reducerCompleted(operation);
            }
            return true;
        }
    }

    private Object mapperFailed(MapReduceOperation operation) throws OperationFailedException {
        String taskID = (String) operation.getTaskID();
        if (runningTasks.containsKey(taskID)) {
            MapReduceTask t = (MapReduceTask) runningTasks.get(taskID);
            t.mapperFailed(operation);
        }
        return true;

    }

    private Object reducerFailed(MapReduceOperation operation) throws OperationFailedException {
        String taskID = (String) operation.getTaskID();
        if (runningTasks.containsKey(taskID)) {
            MapReduceTask t = (MapReduceTask) runningTasks.get(taskID);
            t.reducerFailed(operation);
        }
        return true;

    }

    /**
     * @return the context
     */
    public CacheRuntimeContext getContext() {
        return context;
    }

    private Object getTaskSequence(MapReduceOperation operation) {
        return ++taskSequence;
    }

    public Object cancelAllTasks() {
        //Stop and Remove all running/waiting tasks
        for (Object taskID : runningTasks.keySet()) {
            TaskBase t = (TaskBase) runningTasks.get(taskID);
            if (t != null) {
                t.stopTask();

                if (context.getCacheImpl() != null && context.getCacheInternal() != null) {
                    EventContext eventContext = new EventContext();
                    eventContext.setTaskStatus(TaskStatus.Cancelled);
                    context.getCacheInternal().NotifyTaskCallback(t.getTaskID(), t.getCallbackListeners(), false, null, eventContext);
                }

            }
        }
        
        int sizeRunningTask = runningTasks.size();
        runningTasks.clear();
        context.PerfStatsColl.decrementRunningTasksBy((long)sizeRunningTask);
        
        for (Object taskID : waitingTaskMap.keySet()) {
            TaskBase t = (TaskBase) waitingTaskMap.get(taskID);
            if (t != null) {
                t.stopTask();

                if (context.getCacheImpl() != null && context.getCacheInternal() != null) {
                    EventContext eventContext = new EventContext();
                    eventContext.setTaskStatus(TaskStatus.Cancelled);
                    context.getCacheInternal().NotifyTaskCallback(t.getTaskID(), t.getCallbackListeners(), false, null, eventContext);
                }
            }
        }
        int size = waitingTaskMap.size();
        waitingTaskMap.clear();
        context.PerfStatsColl.decrementPendingTasksBy((long)size);
        //Reset the sequenceID and lastRunningID
        taskSequence = (long) 0;
        lastRunningTaskID = 0;

        return true;
    }

    public Object registerTaskNotification(MapReduceOperation operation) {
        String taskID = (String) operation.getTaskID();
        if (taskID == null || taskID.isEmpty()) {
            return new OperationFailedException("Task can not be null or empty");
        }

        TaskBase t = null;
        if (runningTasks.containsKey(taskID)) {
            t = (TaskBase) runningTasks.get(taskID);
        } else if (waitingTaskMap.containsKey(taskID)) {
            t = (TaskBase) waitingTaskMap.get(taskID);
        } else {
            return new OperationFailedException("Task with Specified Task id does not exists");
        }

        t.addTaskCallBackInfo((TaskCallbackInfo) operation.getCallbackInfo());

        return true;
    }

    public Object unregisterTaskNotification(MapReduceOperation operation) {
        String taskID = (String) operation.getTaskID();
        if (taskID == null || taskID.isEmpty()) {
            return new OperationFailedException("Task can not be null or empty");
        }

        TaskBase t = null;
        if (runningTasks.containsKey(taskID)) {
            t = (TaskBase) runningTasks.get(taskID);
        } else if (waitingTaskMap.containsKey(taskID)) {
            t = (TaskBase) waitingTaskMap.get(taskID);
        } else {
            return new OperationFailedException("Task with Specified Task id does not exists");
        }

        t.removeTaskCallBackInfo((TaskCallbackInfo) operation.getData());

        return true;
    }

    public Object getRunningTasks() {
        return new ArrayList<String>(runningTasks.keySet());
    }

    private Object getTaskProgress(MapReduceOperation operation) throws OperationFailedException {
        String taskId = (String) operation.getTaskID();
        if (runningTasks.containsKey(taskId)) {
            Task t = (Task) runningTasks.get(taskId);
            MapReduceTask mrt = (MapReduceTask) t;
            return mrt.getTaskStatus();
        } else {
            throw new OperationFailedException("Task with Specified task ID does not exist.");
        }
    }

    private Object getTaskEnumerator(MapReduceOperation operation) throws InvalidTaskEnumeratorException {
        TaskEnumeratorPointer pointer = (TaskEnumeratorPointer) operation.getData();
        if (clientAddress == null) {
            try {
                if (context.getRender() != null) {
                    clientAddress = new Address(context.getRender().getIPAddress().getHostAddress(), context.getRender().getPort());
                }
            } catch (Exception ex) {
                throw new InvalidTaskEnumeratorException(ex.getMessage(), ex);
            }
        }
        pointer.setClientAddress(clientAddress);
        if (clusterAddress == null) {
            clusterAddress = ((ClusterCacheBase) context.getCacheImpl()).getCluster().getLocalAddress();
        }
        pointer.setClusterAddress(clusterAddress);

        TaskEnumeratorResult result = null;
        if (taskOutPutStore != null) {
            try {
                result = taskOutPutStore.getTaskEnumerator(pointer);
                //result.setNodeAddress(context.getRender().getIPAddress().toString());
                result.setPointer(pointer);
            } catch (InvalidTaskEnumeratorException ex) {
                context.getCacheLog().Error("TaskTracker.getTaskEnumerator", ex.getMessage());
                throw ex;
            }
        }

        try {
            if(context.getCacheLog().getIsInfoEnabled()) {
                context.getCacheLog().Info("TaskTracker.getTaskEnumerator", "Enumerator provided to client result object" + result != null ? "not null" : "null");
                context.getCacheLog().Info("TaskTracker.getTaskEnumerator", "Enumerator provided to client" + result.getRecordSet().getKey().toString());
            }
        } catch (Exception e) {
        }

        return result;
    }

    private Object getNextRecord(MapReduceOperation operation) throws InvalidTaskEnumeratorException {
        TaskEnumeratorPointer pointer = (TaskEnumeratorPointer) operation.getData();

        TaskEnumeratorResult result = null;
        if (taskOutPutStore != null) {
            try {
                result = taskOutPutStore.getNextRecord(pointer);
                //result.setNodeAddress(context.getRender().getIPAddress().toString());
            } catch (InvalidTaskEnumeratorException ex) {
                context.getCacheLog().Error("TaskTracker.getTaskEnumerator", ex.getMessage());
                throw ex;
            }
        }
        return result;
    }

    public void RemoveDeadClientsTasks(ArrayList clients) {
        //Locking with starttask
        synchronized (lockingObject) {
            //Removing callback entries of dead clients from waiting tasks
            Set<String> waitingTaskList = waitingTaskMap.keySet();
            for (Iterator it = clients.iterator(); it.hasNext();) {
                String client = (String) it.next();
                //if(context.getCacheLog() != null)
                if(context.getCacheLog().getIsInfoEnabled())
                    context.getCacheLog().Info("TaskTracker.RemoveDeadClients", "Removing waiting task listeners for client " + client);
                for (String taskId : waitingTaskList) {
                    TaskBase t = (TaskBase) waitingTaskMap.get(taskId);
                    if (t != null && !t.getCallbackListeners().isEmpty()) {
                        for (Iterator cbit = t.getCallbackListeners().iterator(); cbit.hasNext();) {
                            TaskCallbackInfo taskCallbackInfo = (TaskCallbackInfo) cbit.next();
                            if (taskCallbackInfo.getClient().equals(client)) {
                                t.getCallbackListeners().remove(taskCallbackInfo);
                            }
                        }
                        if (t.getCallbackListeners().isEmpty()) {
                            //if(context.getCacheLog() != null)
                            if(context.getCacheLog().getIsInfoEnabled())
                                context.getCacheLog().Info("TaskTracker.RemoveDeadClients", "No listeners remaining therefore removing waiting task " + t.getTaskID());
                            waitingTaskMap.remove(t.getTaskID());
                            context.PerfStatsColl.decrementPendingTasks();
                        }
                    }
                }
            }
            //Removing callback entries of dead clients from running tasks
            Set<String> runningTaskList = runningTasks.keySet();

            for (Iterator it = clients.iterator(); it.hasNext();) {
                String client = (String) it.next();
                //if(context.getCacheLog() != null)
                if(context.getCacheLog().getIsInfoEnabled())
                    context.getCacheLog().Info("TaskTracker.RemoveDeadClients", "Removing running task listeners for client " + client);
                for (String taskId : runningTaskList) {
                    TaskBase t = (TaskBase) runningTasks.get(taskId);
                    if (t != null && !t.getCallbackListeners().isEmpty()) {
                        for (Iterator cbit = t.getCallbackListeners().iterator(); cbit.hasNext();) {
                            TaskCallbackInfo taskCallbackInfo = (TaskCallbackInfo) cbit.next();
                            if (taskCallbackInfo.getClient().equals(client)) {
                                t.getCallbackListeners().remove(taskCallbackInfo);
                            }
                        }
                        if (t.getCallbackListeners().isEmpty()) {
                            //if(context.getCacheLog() != null)
                            if(context.getCacheLog().getIsInfoEnabled())
                                context.getCacheLog().Info("TaskTracker.RemoveDeadClients", "No listeners remaining therefore removing running task " + t.getTaskID());
                            waitingTaskMap.remove(t.getTaskID());
                            context.PerfStatsColl.decrementPendingTasks();
                        }
                    }
                }
            }
        }
        //Remove Iterators from the task output list
        //Remove Listeners for these clients     
    }

    private Object startTask(MapReduceOperation operation) {
        try {
            return startTask(operation.getTaskID().toString(), new Long(operation.getSequenceID().toString()));
        } catch (Exception exc) {
            return exc;
        }
    }

    private Object removeFromSubmittedList(MapReduceOperation operation) {
        try {
            Object obj = submittedTasks.remove(operation.getTaskID().toString());
            if (obj != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception exc) {
            return exc;
        }
    }

    private Object removeFromRunningList(MapReduceOperation operation) {
        try {
            Object obj = runningTasks.remove(operation.getTaskID().toString());
            context.PerfStatsColl.decrementRunningTasks();
            if (obj != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception exc) {
            return exc;
        }
    }
}
