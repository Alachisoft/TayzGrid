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

import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.mapreduce.Task;
import com.alachisoft.tayzgrid.runtime.mapreduce.Combiner;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class CombinerTask implements Runnable, Task {

    private Thread combinerThread;
    private LinkedBlockingQueue combinerInputQueue;

    private com.alachisoft.tayzgrid.runtime.mapreduce.CombinerFactory combinerFactory;
    private HashMap combinersMap;
    private volatile boolean isAlive = true;
    private volatile boolean isMapperAlive = true;
    private int totalKeysCombined = 0;

    private MapReduceTask parent;

    public CombinerTask(com.alachisoft.tayzgrid.runtime.mapreduce.CombinerFactory cFactory, MapReduceTask p) {
        combinerFactory = cFactory;
        combinersMap = new HashMap();
        combinerInputQueue = new LinkedBlockingQueue();
        parent = p;
    }

    private void finalizeCombiners() {
        HashMap combinerOutput = new HashMap();
        Iterator it = combinersMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Combiner combiner = (Combiner) entry.getValue();
            combinerOutput.put(entry.getKey(), combiner.finishCombine());
        }
        synchronized(parent.getSendtoReducerQueue()) {
            parent.getSendtoReducerQueue().addAll(combinerOutput.entrySet());
        }
        combinersMap.clear();
    }

//        public void enqueueMapperOutput(Object output)
//        {
//            this.combinerInputQueue.add(output);
//        }
    @Override
    public void run() {
        if (parent.getContext() != null) {
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                parent.getContext().getCacheLog().Info("CombinerTask(" + parent.getTaskID() + ").Start", "Combiner task is started");
        }

        boolean completedSuccessfully = true;

        while (isAlive) {
            try {
                Object obj = null;
                synchronized (getCombinerInputQueue()) {
                    obj = getCombinerInputQueue().poll();
                    if (obj == null && isMapperAlive) {
                        try {
                            Monitor.wait(getCombinerInputQueue());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MapReduceTask.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        obj = getCombinerInputQueue().poll();
                    }
                }

                if (obj != null) {
                    if (obj.getClass().equals(boolean.class) || obj.getClass().equals(Boolean.class)) {
                        boolean finalizeChunk = (Boolean) obj;
                        if (finalizeChunk) {
                            finalizeCombiners();
                            parent.sendToReducers();
                        }
                    } else {
                        Map.Entry entry = (Map.Entry) obj;
                        Object key = entry.getKey();
                        Combiner c = null;
                        if (!combinersMap.containsKey(key)) {
                           
                            c = combinerFactory.getCombiner(key);                           
                            combinersMap.put(key, c);
                        } else {
                            c = (Combiner) combinersMap.get(key);
                        }
                        
                        c.combine(entry.getValue()); 
                        totalKeysCombined++;
                        parent.getContext().PerfStatsColl.incrementCombinedPerSecRate();
                    }
                } else {
                    if (!isMapperAlive) {
                    finalizeCombiners();
                    parent.sendToReducers();
                    if (parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        parent.getContext().getCacheLog().Info("CombinerTask (" + parent.getTaskID() + ").Run", "Combiner Completed, performing further ops");
                    }
                    break;
                }
                }
            } catch (Exception ex) {
                if (parent.getExceptionCount() < parent.getMaxExceptions()) {
                    parent.getContext().getCacheLog().Error("CombinerTask (" + parent.getTaskID() + ").Run", "Exception: " + ex.getMessage());
                    parent.setExceptionCount(parent.getExceptionCount() + 1);
                } else {
                    completedSuccessfully = false;
                    parent.localCombinerFailed();
                }
            }
        }

        if (completedSuccessfully && isAlive) {
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                parent.getContext().getCacheLog().Info("CombinerTask (" + parent.getTaskID() + ").Run", "Total Keys Combined : " + totalKeysCombined);
            parent.localCombinerCompleted();
        }
    }

    @Override
    public void startTask() {
        parent.getContext().PerfStatsColl.setMappedPerSec(0);
        combinerThread = new Thread(this);
        combinerThread.setDaemon(true);
        combinerThread.start();
    }

    @Override
    public void stopTask() {
        if (combinerThread != null) {
            isAlive = false;
            synchronized (getCombinerInputQueue()) {
                Monitor.pulse(getCombinerInputQueue());
            }
            combinerThread.interrupt();
            combinerThread = null;
        }
    }

    /**
     * @return the combinerInputQueue
     */
    public LinkedBlockingQueue getCombinerInputQueue() {
        return combinerInputQueue;
    }

    /**
     * @return the isMapperAlive
     */
    public boolean isIsMapperAlive() {
        return isMapperAlive;
    }

    /**
     * @param isMapperAlive the isMapperAlive to set
     */
    public void setIsMapperAlive(boolean isMapperAlive) {

        synchronized (getCombinerInputQueue()) {
            this.isMapperAlive = isMapperAlive;
            Monitor.pulse(getCombinerInputQueue());
        }
    }
}
