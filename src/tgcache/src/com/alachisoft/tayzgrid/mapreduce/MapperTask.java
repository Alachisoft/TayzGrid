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

import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyFilter;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyValuePair;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceInput;
import com.alachisoft.tayzgrid.runtime.mapreduce.OutputMap;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

class MapperTask implements Runnable, Task {

    private Thread mapperThread;
    private MapReduceInput inputProvider;
    private KeyFilter keyFilter;
    private com.alachisoft.tayzgrid.runtime.mapreduce.Mapper mapper;
    private volatile boolean isAlive = true;
    private boolean finishedMap = false;
    private MapReduceTask parent;
    private long mappedCount;
    private java.lang.Class MapperInputKeyType = Object.class;
    private java.lang.Class MapperInputValueType = Object.class;
    private java.lang.Class MapperOutputKeyType = Object.class;
    private java.lang.Class MapperOutputValueType = Object.class;

    public MapperTask(com.alachisoft.tayzgrid.runtime.mapreduce.Mapper m, MapReduceInput input, KeyFilter keyFilter, MapReduceTask p) {
        this.mapper = m;
        inputProvider = input;
        this.keyFilter = keyFilter;
        parent = p;
        for (Type iface : m.getClass().getGenericInterfaces()) {
            ParameterizedType type = null;
            try {
                type = (ParameterizedType) iface;
            } catch (Exception ex) {
            }

            if (type != null && type.getRawType().equals(com.alachisoft.tayzgrid.runtime.mapreduce.Mapper.class)) {
                MapperInputKeyType = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[0];
                MapperInputValueType = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[1];
                MapperOutputKeyType = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[2];
                MapperOutputValueType = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[3];
            }
        }
    }

    @Override
    public void run() {
        if (parent.getContext() != null) {
            if(parent.getContext().getCacheLog().getIsInfoEnabled())
                parent.getContext().getCacheLog().Info("MapperTask(" + parent.getTaskID() + ").Start", "Mapper task is started. input provider: " + inputProvider.hasMoreElements());
        }
        boolean completedSuccessfully = true;
        while (isAlive && inputProvider.hasMoreElements()) {
            try {

                OutputMap output = new OutputMap();
                KeyValuePair element = inputProvider.nextElement();

                if (element != null) {

                    if (keyFilter != null && !keyFilter.filterKey(element.getKey())) {
                        continue;
                    }
                    //Object key = MapperInputKeyType.cast(element.getKey());
                    //Object value = MapperInputValueType.cast(element.getValue());
                    mapper.map(MapperInputKeyType.cast(element.getKey()), MapperInputValueType.cast(element.getValue()), output);
                    this.setMappedCount(getMappedCount() + 1);
                    parent.getContext().PerfStatsColl.incrementMappedPerSecRate();
                    parent.enqueueMapperOutput(output);
                }
            } catch (Exception ex) {
                if (parent.getExceptionCount() < parent.getMaxExceptions()) {
                    parent.getContext().getCacheLog().Error("MapperTask(" + parent.getTaskID() + ").Run", "Exception : " + ex.getMessage());
                    parent.setExceptionCount(parent.getExceptionCount() + 1);
                } else {
                    completedSuccessfully = false;
                    parent.localMapperFailed();
                }
            }
        }

//        if (isAlive) {
//            finishedMap = true;
//        }
        //callback the task to inform about mapper finished
        if (completedSuccessfully && isAlive) {
            parent.localMapperCompleted();
        }
    }

    @Override
    public void startTask() throws OperationFailedException {
        try {
            prepareInputProvider();
            mapperThread = new Thread(this);
            mapperThread.setDaemon(true);
            mapperThread.start();
        } catch (Exception ex) {
            String exception = ex.getMessage();
            if (parent.getContext() != null) {
                parent.getContext().getCacheLog().Error("MapperTask (" + parent.getTaskID() + ").PrepareInput", "Error preparing Input : " + exception);
            }
            //parent.participentSendMapperFailedMessage();
            throw new OperationFailedException("Error preparing Input: " + ex.getMessage());
        }
    }

    @Override
    public void stopTask() {
        if (mapperThread != null) {
            isAlive = false;
            mapperThread.interrupt();
            mapperThread = null;
        }
    }

    private void prepareInputProvider() throws OperationFailedException {
        if (inputProvider == null) {
            throw new IllegalArgumentException("input provider can not be null");
        }
        HashMap providerMap = new HashMap();
        providerMap.put("CacheName", parent.getContext().getCacheRoot().getName());
        providerMap.put("NodeAddress", ((ClusterCacheBase)parent.getContext().getCacheImpl()).getCluster().getLocalAddress().getIpAddress().getHostAddress().toString()+":"+((ClusterCacheBase)parent.getContext().getCacheImpl()).getCluster().getLocalAddress().getPort());
        inputProvider.initialize(providerMap);
        inputProvider.load();
    }

    public long getMappedCount() {
        return mappedCount;
    }

    public void setMappedCount(long mappedCount) {
        this.mappedCount = mappedCount;
    }

}
