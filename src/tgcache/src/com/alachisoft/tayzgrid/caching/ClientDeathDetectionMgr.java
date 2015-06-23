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
package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.caching.statistics.*;
import com.alachisoft.tayzgrid.caching.topologies.*;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientDeathDetectionMgr implements Runnable {

    /**
     * Cache Impl instance
     */
    private CacheBase _cacheImpl = null;
    /**
     * Cache statistics
     */
    private CacheStatistics _stats = null;
    /**
     * List of suspected clients against their disconnect time
     */
    private java.util.HashMap<String, java.util.Date> _suspectedClients = new java.util.HashMap<String, java.util.Date>();
    /**
     * List of dead clients against their disconnect time
     */
    private java.util.HashMap<String, java.util.Date> _deadClients = new java.util.HashMap<String, java.util.Date>();
    /**
     * Configurable time after which client will be declared dead
     */
    private int _gracePeriod;
    /**
     * The worker thread.
     */
    private Thread _worker = null;

    private Object syncRoot = new Object();

    public ClientDeathDetectionMgr(int gracePeriod) {
        _gracePeriod = gracePeriod * 1000;
    }

    public final CacheBase getCacheImpl() {
        return _cacheImpl;
    }

    public final void setCacheImpl(CacheBase value) {
        _cacheImpl = value;
        _stats = value.getStatistics();

    }

    /**
     * Start monitoring
     */
    public final void StartMonitoringClients() {
        if (this._worker == null) {
            this._worker = new Thread(this);
            this._worker.setDaemon(true);
            this._worker.setName("Client-Death-Detection");
            this._worker.start();
        }
    }
    
    
    @Override
    public void run() {
        while (this._worker != null && this._worker.isAlive()) {
            java.util.ArrayList suspectedClients = new java.util.ArrayList();
            synchronized (syncRoot) {
                if (this._deadClients.size() < 1) {
                    try {
                        Monitor.wait(syncRoot);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ClientDeathDetectionMgr.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                
                //Dictionary<string, DateTime> clone = new Dictionary<string, DateTime>(_deadClients);//shallow clone
                java.util.HashMap<String, java.util.Date> tempList = new java.util.HashMap<String, java.util.Date>(_deadClients);
                java.util.Iterator<java.util.Map.Entry<String, java.util.Date>> ide = tempList.entrySet().iterator();
                while (ide.hasNext()) {
                    java.util.Map.Entry<String, java.util.Date> current = ide.next();
                    
                    
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.setTime(current.getValue());
                    cal.add(Calendar.MILLISECOND, (int) _gracePeriod);//?
                    java.util.Date expireTime = cal.getTime();
                                                                                                                    
                    if (expireTime.compareTo(new java.util.Date()) < 0) {
                        continue;
                    }
                    suspectedClients.add(current.getKey());
                    if (_deadClients.containsKey(current.getKey())) {
                        synchronized (syncRoot) {
                            _deadClients.remove(current.getKey());
                        }
                    }
                }
            }
            if (suspectedClients.isEmpty()) {
                continue;
            }
            java.util.ArrayList deadClients = _cacheImpl.determineClientConnectivity(suspectedClients);
            _cacheImpl.declaredDeadClients(deadClients);
        }
    }

    private void RemoveFromDeadClients(java.util.ArrayList suspectedClients) {
        synchronized (syncRoot) {
            for (Iterator it = suspectedClients.iterator(); it.hasNext();) {
                String client = (String) it.next();
                _deadClients.remove(client);
            }
        }
    }

    public final void ClientDisconnected(String client, java.util.Date dcTime) {
        if (_stats == null) {
            return;
        }

        if (_stats instanceof ClusterCacheStatistics) {
            boolean clientExist = false;
            ClusterCacheStatistics clusterStats = (ClusterCacheStatistics) ((_stats instanceof ClusterCacheStatistics) ? _stats : null);
            for (Iterator it = clusterStats.getNodes().iterator(); it.hasNext();) {
                NodeInfo node = (NodeInfo) it.next();
                if (node.getConnectedClients().contains(client)) {
                    clientExist = true;
                    break;
                }
            }
            synchronized (syncRoot) {
                if (clientExist) {
                    if (!_stats.getClassName().equals("replicated-server")) {
                        _suspectedClients.put(client, dcTime);
                    }
                } else {
                    if (_suspectedClients.containsKey(client)) {
                        _suspectedClients.remove(client);
                    }
                    _deadClients.put(client, dcTime);
                    syncRoot.notify();
                }
            }
        } else {
            synchronized (syncRoot) {
                _deadClients.put(client, dcTime);
                syncRoot.notify();
            }
        }
    }

    public final void ClientConnected(String client) {
        if (_deadClients.containsKey(client)) {
            synchronized (syncRoot) {
                _deadClients.remove(client);
            }
        } else if (_suspectedClients.containsKey(client)) {
            if (_stats instanceof ClusterCacheStatistics) {
                boolean clientExist = true;
                ClusterCacheStatistics clusterStats = (ClusterCacheStatistics) ((_stats instanceof ClusterCacheStatistics) ? _stats : null);
                for (Iterator it = clusterStats.getNodes().iterator(); it.hasNext();) {
                    NodeInfo node = (NodeInfo) it.next();
                    if (!node.getConnectedClients().contains(client)) {
                        clientExist = false;
                        break;
                    }
                }
                if (clientExist) {
                    synchronized (syncRoot) {
                        _suspectedClients.remove(client);
                    }
                }
            }
        }
    }

    public final void dispose() {
        if (this._worker != null && this._worker.isAlive()) {
            this._worker.stop();
            this._worker = null;
        }
    }

    public final void UpdateClientStatus(java.util.ArrayList localNodeClients, java.util.ArrayList updatedNodeClients) {
        String[] tempLocalClient = (String[]) localNodeClients.toArray(new String[0]);
        String[] tempUpdatedClient = (String[]) updatedNodeClients.toArray(new String[0]);

        String[] missingClients = intersection(Arrays.asList(tempLocalClient),Arrays.asList(tempUpdatedClient));
        if (updatedNodeClients.size() > localNodeClients.size()) {
            for (int i = 0; i < missingClients.length; i++) {
                this.ClientConnected(missingClients[i]);
            }
        } else {
            for (int i = 0; i < missingClients.length; i++) {
                this.ClientDisconnected(missingClients[i], new java.util.Date()); //client will be mark as suspected from now on.
            }
        }
    }
    
    public String[] intersection(List<String> list1,List<String> list2) {
        List list = new ArrayList();

        for (String t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return (String[]) list.toArray();
    }

}
