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
import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.caching.topologies.clustered.DistributionManager;
import com.alachisoft.tayzgrid.caching.topologies.clustered.PartitionedServerCache;
import com.alachisoft.tayzgrid.common.mapreduce.TaskOutputPair;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.mapreduce.notification.CallBackResult;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskStatus;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallBack;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author tayzgrid_dev
 */
public class MapReduceTask extends TaskBase {

    private static short INMEMORY=1;
    private CacheRuntimeContext _context;
    private ClusterCacheBase parent;
    private MapperTask mapperTask;
    private CombinerTask combinerTask;
    private ReducerTask reducerTask;
    private MapReduceThrottlingManager throttlingManager;
    private boolean isCombinerConfigured;
    private boolean isReducerConfigured;
    private DistributionManager distributionManager = null;
    private TaskCallBack callBack;
    private int maxExceptions = 10;
    private int exceptionCount = 0;
    private HashMap participents = new HashMap();
    private com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceOutput outputProvider;
    private short outputOption=INMEMORY;//for In Memory
    private Type MapperInputKeyType;
    private Type MapperInputValueType;
    private Type MapperOutputKeyType;
    private Type MapperOutputValueType;
    private Object r_mutex=new Object();
    private Object m_mutex=new Object();

    //map will be used in case no reducer has been configured
    private java.util.ArrayList<TaskOutputPair> localOutput=new ArrayList<TaskOutputPair>();

    private LinkedBlockingQueue sendtoReducerQueue = new LinkedBlockingQueue();

    /**
     *
     * @param p
     * @param cb
     * @param m
     * @param cFactory
     * @param rFactory
     * @param input
     * @param output
     * @param chunkSize
     */
    public MapReduceTask(ClusterCacheBase p, TaskCallBack cb, Mapper m, CombinerFactory cFactory, ReducerFactory rFactory, MapReduceInput input, MapReduceOutput output,KeyFilter keyFilter, CacheRuntimeContext context, int chunkSize, int maxExceptions) {
        parent = p;
        callBack=cb;
        this._context = context;
        this.maxExceptions = maxExceptions;
        this.setType(TaskType.MapReduce);
        mapperTask = new MapperTask(m, input,keyFilter, this);
        if (cFactory != null) {
            combinerTask = new CombinerTask(cFactory, this);
            //combinerInputQueue = new LinkedBlockingQueue();
            isCombinerConfigured = true;
        }
        if (rFactory != null) {
            reducerTask = new ReducerTask(rFactory,this);
            isReducerConfigured=true;
        }
        outputProvider=output;

        //reducerInputQueue = new LinkedBlockingQueue();
        throttlingManager = new MapReduceThrottlingManager(chunkSize);

        if (parent != null) 
        {
            List pa = parent.getActiveServers();
            Iterator it = pa.iterator();
            while (it.hasNext()) {
                participents.put(it.next(), new NodeTaskStatus());
            }
            
            if (parent instanceof PartitionedServerCache) {
                distributionManager = ((PartitionedServerCache) parent).getDistributionMgr();
            }
        }
    }

    @Override
    public void startTask() throws OperationFailedException {
        if(_context != null)
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").StartTask", "MapReduce task is starting");
        
        if (mapperTask != null) {
            mapperTask.startTask();
        }
        
        if (combinerTask != null && isCombinerConfigured) {
            combinerTask.startTask();
        }
        if (reducerTask != null && isReducerConfigured) {
            reducerTask.startTask();
        }
        
         if(_context != null)
             if(parent.getContext().getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").StartTask", "MapReduce task is started");
    }

    @Override
    public void stopTask() {
        if (mapperTask != null) {
            mapperTask.stopTask();
        }

        if (combinerTask != null && isCombinerConfigured) {
            combinerTask.stopTask();
        }
        if (reducerTask != null && isReducerConfigured) {
            reducerTask.stopTask();
        }

    }
        
    void persistFinalOutput(java.util.ArrayList<TaskOutputPair> output) {
        if(outputProvider!=null)
        {
            if (parent.getCacheLog().getIsInfoEnabled())
                    parent.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").PersistFinalOutput", "output persisted. Count: " + ((ArrayList)output).size());
            
            if(outputOption==INMEMORY)
                outputProvider.persist(this.getTaskID(),new com.alachisoft.tayzgrid.mapreduce.outputproviders.TaskOutput(this.getTaskID(), (ArrayList<TaskOutputPair>) output,this.getCallbackListeners()));
        }
    }

    void enqueueMapperOutput(OutputMap output) {
        if (isCombinerConfigured) {
            Iterator it = output.getOutputMap().entrySet().iterator();
            while (it.hasNext()) {
                synchronized (combinerTask.getCombinerInputQueue()) {
                    combinerTask.getCombinerInputQueue().add(it.next());
                    throttlingManager.incrementChunkSize();
                    Monitor.pulse(combinerTask.getCombinerInputQueue());
                    //combinerInputQueue.addAll(output.getOutputMap().entrySet());
                }
            }
        } else {
            synchronized(getSendtoReducerQueue()) {
                getSendtoReducerQueue().addAll(output.getOutputMap().entrySet());
                throttlingManager.incrementChunkSizeBy(output.getOutputMap().entrySet().size());
            }
        }
    }

    void sendToReducers() {
        TaskOutputPair entry = null;
            while (!getSendtoReducerQueue().isEmpty()) {
                try {
                    synchronized(getSendtoReducerQueue()) {
                        Map.Entry ent = (Map.Entry) getSendtoReducerQueue().poll();
                        entry = new TaskOutputPair(ent.getKey(), ent.getValue());
                    }
                    if(entry != null) {
                        if(!isReducerConfigured)
                        {                    
                            localOutput.add(entry);
                            //localOutput.put(entry.getKey(), entry.getValue());
                        } else {
                            ReducerInput input = new ReducerInput();
                            input.setKey(entry.getKey());
                            input.setValue(entry.getValue());
                            if (distributionManager != null) {

                                Address target = distributionManager.SelectNode(entry.getKey());
                                MapReduceOperation op = new MapReduceOperation();
                                op.setData(input);
                                op.setOpCode(MapReduceOpCodes.SendReducerData);
                                op.setTaskID(this.getTaskID());
                                parent.Clustered_SendMapReduceOperation(target, op);
                                }
                            //op.setSource(parent.getCluster().getLocalAddress());

                        }
                    }
                } catch (Exception ex) {
                    parent.getContext().getCacheLog().Error("MapReduceTask(" + this.getTaskID() + ").SendToReducers", "Exception: " + ex.getMessage());
                }
                //Get Target Node check if local enqueue into reducer queue otherwise send to target node
            }
    }

    void enqueueReducerInput(ReducerInput reducerInput) {
        synchronized (reducerTask.getReducerInputQueue()) {
            
            reducerTask.getReducerInputQueue().add(reducerInput);
            Monitor.pulse(reducerTask.getReducerInputQueue());
        }
    }

    void participentSendMapperFailedMessage() {

        MapReduceOperation operation = new MapReduceOperation();
        operation.setTaskID(getTaskID());
        operation.setOpCode(MapReduceOpCodes.MapperFailed);

        try {
            if (parent != null) {
                parent.Clustered_SendMapReduceOperation(new ArrayList(participents.keySet()), operation);
                
                mapperFailed(operation);
            }
        } catch (Exception ex) 
        {
            parent.getContext().getCacheLog().Error("MapReduceTask(" + this.getTaskID() + ").participentSendMapperFailedMessage", "Exception: " + ex.getMessage());
        }
    }

    void participentSendMapperCompleteMessage() {

        MapReduceOperation operation = new MapReduceOperation();
        operation.setTaskID(getTaskID());
        operation.setOpCode(MapReduceOpCodes.MapperCompleted);

        try {
            if (parent != null) {
                parent.Clustered_SendMapReduceOperation(new ArrayList(participents.keySet()), operation);
                
                mapperCompleted(operation);
            }
        } catch (Exception ex) 
        {
            parent.getContext().getCacheLog().Error("MapReduceTask(" + this.getTaskID() + ").participentSendMapperCompleteMessage", "Exception: " + ex.getMessage());
        }
    }

    void participentSendReducerFailedMessage() {

        MapReduceOperation operation = new MapReduceOperation();
        operation.setTaskID(getTaskID());
        operation.setOpCode(MapReduceOpCodes.ReducerFailed);

        try {
            if (parent != null) {
                parent.Clustered_SendMapReduceOperation(new ArrayList(participents.keySet()), operation);
                
                reducerFailed(operation);
            }
        } catch (Exception ex) 
        {
            parent.getContext().getCacheLog().Error("MapReduceTask(" + this.getTaskID() + ").participentSendReducerFailedMessage", "Exception: " + ex.getMessage());
        }
    }

    void participentSendReducerCompleteMessage() {

        MapReduceOperation operation = new MapReduceOperation();
        operation.setTaskID(getTaskID());
        operation.setOpCode(MapReduceOpCodes.ReducerCompleted);

        try {
            if (parent != null) {
                parent.Clustered_SendMapReduceOperation(new ArrayList(participents.keySet()), operation);
                
                reducerCompleted(operation);
            }
        } catch (Exception ex) 
        {
                        parent.getContext().getCacheLog().Error("MapReduceTask(" + this.getTaskID() + ").participentSendReducerCompleteMessage", "Exception: " + ex.getMessage());

        }
    }

    void localMapperCompleted() {

        if (!isCombinerConfigured) {
            sendToReducers();

            if (!isReducerConfigured) {
                persistFinalOutput(localOutput);
                if (parent.getCacheLog().getIsInfoEnabled()) {
                    parent.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").LocalMapperCompleted", "Local Mapper Completed, output persisted.");
                }
            }

            participentSendMapperCompleteMessage();
        } else {
            this.combinerTask.setIsMapperAlive(false);
        }
    }

    void localMapperFailed() {
        //if (isCombinerConfigured) {
          //  combinerTask.stopTask();
        //}

        //mapperTask.stopTask();
        participentSendMapperFailedMessage();
    }

    void localCombinerCompleted() {
        //mapperTask.stopTask();
        //combinerTask.stopTask();
        if(!isReducerConfigured)
        {
            persistFinalOutput(localOutput);                
        }

        participentSendMapperCompleteMessage();
    }

    void localCombinerFailed() {
        //mapperTask.stopTask();
        //combinerTask.stopTask();

        parent.getCacheLog().Error("MapReduceTask (" + this.getTaskID() + ").LocalCombinerFailed", "Combiner is failed.");
        participentSendMapperFailedMessage();
    }

    void localReducerCompleted() {
        participentSendReducerCompleteMessage();            
    }

    void localReducerFailed() {
        //mapperTask.stopTask();
        //if(combinerTask != null)
            //combinerTask.stopTask();

        //participentSendMapperFailedMessage();
        participentSendReducerFailedMessage();
    }

    //Clustered Messages Recieved
    void mapperCompleted(MapReduceOperation operation) throws OperationFailedException{
        synchronized(m_mutex){
        if (participents != null) {
            Address source = operation.getSource();
            NodeTaskStatus status = (NodeTaskStatus) participents.get(source);
            if (status != null) {
                status.setmStatus(MapperStatus.Completed);
                if(_context.getCacheLog().getIsInfoEnabled())
                    _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").MapperCompleted", "Mapped is completed on '" + source.getIpAddress().toString() + "'");
            }
            checkIfMappersCompleted();
            // checkIfAllMappersCompletedThenFinalizeReducers();
        }
        }
    }
    
    void checkIfMappersCompleted() throws OperationFailedException{
        if(participents!=null)
        {
            boolean mCompleted=true;
            Iterator it=participents.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry entry=(Map.Entry) it.next();
                if(((NodeTaskStatus)entry.getValue()).getmStatus()==MapperStatus.Completed)
                    continue;
                else
                {
                    mCompleted=false;
                    break;
                }
            }
            
            if(mCompleted)
            {
                if(isReducerConfigured)
                    reducerTask.setIsMappersCompleted(mCompleted);
                else
                {
                    //this.persistFinalOutput(localOutput);
                    if (parent.getCacheLog().getIsInfoEnabled())
                        parent.getCacheLog().Info("MapReduceTask("+this.getTaskID()+").TaskCompleted", "Task Completed, triggering callback.");
                    this.callBack.onCallBack(new CallBackResult(this, TaskStatus.Success));
                }
            }
        }
    }
    
    void checkIfReducersCompleted() throws OperationFailedException{
        if(participents!=null)
        {
            boolean rCompleted=true;
            Iterator it=participents.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry entry=(Map.Entry) it.next();
                if(((NodeTaskStatus)entry.getValue()).getrStatus()==ReducerStatus.Completed)
                    continue;
                else
                {
                    rCompleted=false;
                    break;
                }
            }
            
            if(rCompleted)
            {
                if (parent.getCacheLog().getIsInfoEnabled())
                    parent.getCacheLog().Info("MapReduceTask("+this.getTaskID()+").TaskCompleted", "Task Completed, triggering callback.");
                    
                this.callBack.onCallBack(new CallBackResult(this, TaskStatus.Success));             
            }
        }
    }
    

    void mapperFailed(MapReduceOperation operation) throws OperationFailedException {
        if (participents != null) {
            Address source = operation.getSource();
            NodeTaskStatus status = (NodeTaskStatus) participents.get(source);
            status.setmStatus(MapperStatus.Failed);
            if(_context.getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").MappedFailed", "Mapper is failed on '" + source.getIpAddress().toString() + "'");
            // notifyClientAboutFailedIfCoordinator();
            this.callBack.onCallBack(new CallBackResult(this, TaskStatus.Failure));
            this.stopTask();
        }
    }

    void reducerCompleted(MapReduceOperation operation) throws OperationFailedException {
        synchronized(r_mutex){
        if (participents != null) {
            Address source = operation.getSource();
            NodeTaskStatus status = (NodeTaskStatus) participents.get(source);
            if(status!=null)
            {
                status.setrStatus(ReducerStatus.Completed);
                if(_context.getCacheLog().getIsInfoEnabled())
                    _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").ReducerCompleted", "Reducer is completed on '" + source.getIpAddress().toString() + "'");
            
                checkIfReducersCompleted();
            }                       
        }
        }
    }

    void reducerFailed(MapReduceOperation operation) throws OperationFailedException {
        if (participents != null) {
            Address source = operation.getSource();
            NodeTaskStatus status = (NodeTaskStatus) participents.get(source);
            status.setrStatus(ReducerStatus.Failed);
            if(_context.getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("MapReduceTask(" + this.getTaskID() + ").ReducerFailed", "Reducer is failed on '" + source.getIpAddress().toString() + "'");
            //notifyClientAboutFailedIfCoordinator();
            this.callBack.onCallBack(new CallBackResult(this, TaskStatus.Failure));
            this.stopTask();
        }
    }

    void throttlingReached() {
        if (isCombinerConfigured) {
            combinerTask.getCombinerInputQueue().add(true);
        } else {
            sendToReducers();
        }
    }

    /**
     * @return the sendtoReducerQueue
     */
    public LinkedBlockingQueue getSendtoReducerQueue() {
        return sendtoReducerQueue;
    }

    //@Override
    public com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus getTaskStatus() 
    {
        
        com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus taskStatus = null;
        Iterator it = participents.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry entry=(Map.Entry) it.next();
            Address address = (Address) entry.getKey();
            NodeTaskStatus status = (NodeTaskStatus) entry.getValue();
            
            if(status.getmStatus().equals(MapperStatus.Running)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.InProgress, mapperTask.getMappedCount());
                break;
            } else if(status.getmStatus().equals(MapperStatus.Failed)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.Failed, mapperTask.getMappedCount());
                break;
            } else if(status.getmStatus().equals(MapperStatus.Completed)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.InProgress, reducerTask.getReducedCount());
            } else if(status.getrStatus().equals(ReducerStatus.Running)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.InProgress, reducerTask.getReducedCount());
                break;
            } else if(status.getrStatus().equals(ReducerStatus.Failed)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.Failed, reducerTask.getReducedCount());
                break;
            } else if(status.getrStatus().equals(ReducerStatus.Completed)) {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.Completed, reducerTask.getReducedCount());
            } else {
                taskStatus = new com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus(com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus.Status.Waiting, 0);
            }
        }
        return taskStatus;
    }

    public CacheRuntimeContext getContext() {
        return _context;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(int exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public int getMaxExceptions() {
        return maxExceptions;
    }

    class MapReduceThrottlingManager {

        private int size;
        private int chunkSize;

        public MapReduceThrottlingManager(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public void incrementChunkSize() {
            if (++size >= chunkSize) {
                throttlingReached();
                this.size = 0;
            }
        }

        public void incrementChunkSizeBy(int size) {
            this.size += size;
            if (this.size >= chunkSize) {
                throttlingReached();
                this.size = 0;
            }
        }
    }
}
