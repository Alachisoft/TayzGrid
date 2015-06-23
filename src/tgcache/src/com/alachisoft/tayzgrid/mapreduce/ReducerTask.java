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

import com.alachisoft.tayzgrid.common.mapreduce.TaskOutputPair;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyValuePair;
import com.alachisoft.tayzgrid.runtime.mapreduce.Reducer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class ReducerTask implements Runnable, Task {

    private Thread reducerThread;
    private LinkedBlockingQueue reducerInputQueue;
    private com.alachisoft.tayzgrid.runtime.mapreduce.ReducerFactory reducerFactory;
    private volatile boolean isAlive = true;
    private volatile boolean isMappersCompleted;
    private HashMap reducersMap = new HashMap();
    private final MapReduceTask parent;
    private long reducedCount = 0;

    public ReducerTask(com.alachisoft.tayzgrid.runtime.mapreduce.ReducerFactory rFactory, MapReduceTask p) {
        parent = p;
        reducerFactory = rFactory;
        reducerInputQueue = new LinkedBlockingQueue();
    }

    private ArrayList<TaskOutputPair> finalizeReducers() {
        ArrayList<TaskOutputPair> reducersOutput = new ArrayList<TaskOutputPair>();
        Iterator it = reducersMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Reducer reducer = (Reducer) entry.getValue();
            KeyValuePair result = new KeyValuePair();
            reducer.finishReduce(result);
            reducersOutput.add(new TaskOutputPair(result.getKey(), result.getValue()));
        }
        return reducersOutput;
    }

    @Override
    public void run() {
        if (parent.getContext() != null) {
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                parent.getContext().getCacheLog().Info("ReducerTask(" + parent.getTaskID() + ").Start", "Reducer task is started");
        }

        boolean isCompeletedSuccessfully = true;

        while (isAlive) {
            try {
                Object obj = null;
                synchronized (getReducerInputQueue()) {
                    obj = getReducerInputQueue().poll();
                    if (obj == null && !isMappersCompleted) {
                        try {
                            Monitor.wait(getReducerInputQueue());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MapReduceTask.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        obj = getReducerInputQueue().poll();

                    }
                }             

                if(obj!=null)
                {
                    ReducerInput entry = (ReducerInput) obj;
                    Object key = entry.getKey();
                    Reducer r = null;
                    if (!reducersMap.containsKey(key)) {
                        r = reducerFactory.getReducer(key);
                        reducersMap.put(key, r);
                    } else {
                        r = (Reducer) reducersMap.get(key);
                    }
                    r.reduce(entry.getValue());
                    setReducedCount(getReducedCount() + 1); // increment the reducedCount
                    parent.getContext().PerfStatsColl.incrementReducedPerSecRate();
                }
                else
                {
                    if (isMappersCompleted) 
                    {
                        parent.persistFinalOutput(finalizeReducers());
                        if (parent.getContext().getCacheLog().getIsInfoEnabled()) {
                            parent.getContext().getCacheLog().Info("ReducerTask(" + parent.getTaskID() + ").Run", "Reducer Completed, output persisted.");
                        }
                        break;
                    }
                }
                
            } catch (Exception ex) {
                if (parent.getExceptionCount() < parent.getMaxExceptions()) {
                    parent.getContext().getCacheLog().Error("ReducerTask(" + parent.getTaskID() + ").Run", "Exception:" + ex.getMessage());
                    parent.setExceptionCount(parent.getExceptionCount() + 1);
                } else {
                    isCompeletedSuccessfully = false;
                    parent.localReducerFailed();
                }
            }
        }

        if (isCompeletedSuccessfully && isAlive) {
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                parent.getContext().getCacheLog().Info("ReducerTask (" + parent.getTaskID() + ").Run", "Reduced Total Keys : " + this.getReducedCount());
            parent.localReducerCompleted();
        }
    }

    @Override
    public void startTask() {
        reducerThread = new Thread(this);
        reducerThread.setDaemon(true);
        reducerThread.start();
    }

    @Override
    public void stopTask() {
        if (reducerThread != null) {
            isAlive = false;
            synchronized(getReducerInputQueue()) {
                Monitor.pulse(getReducerInputQueue());
            }
            reducerThread.interrupt();
            reducerThread = null;
        }
    }

    /**
     * @return the reducerInputQueue
     */
    public LinkedBlockingQueue getReducerInputQueue() {
        return reducerInputQueue;
    }

    /**
     * @param isAnyMappersAlive the isAnyMappersAlive to set
     */
    public void setIsMappersCompleted(boolean isMappersCompleted) {
        synchronized(getReducerInputQueue()) {
            this.isMappersCompleted = isMappersCompleted;
            Monitor.pulse(getReducerInputQueue());
        }
    }

    /**
     * @return the reducedCount
     */
    public long getReducedCount() {
        return reducedCount;
    }

    /**
     * @param reducedCount the reducedCount to set
     */
    private void setReducedCount(long reducedCount) {
        this.reducedCount = reducedCount;
    }
}
