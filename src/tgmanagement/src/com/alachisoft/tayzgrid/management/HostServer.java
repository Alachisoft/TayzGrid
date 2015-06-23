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
package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheClearedCallback;
import com.alachisoft.tayzgrid.caching.CustomRemoveCallback;
import com.alachisoft.tayzgrid.caching.CustomUpdateCallback;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.ItemAddedCallback;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.ItemRemovedCallback;
import com.alachisoft.tayzgrid.caching.ItemUpdatedCallback;
import com.alachisoft.tayzgrid.caching.LeasedCache;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.event.CacheStopped;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.common.monitoring.ServerNode;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.rpcframework.TargetMethodAttribute;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.util.ManagementUtil.MethodName;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.ConfigConverter;
import com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper;
import com.alachisoft.tayzgrid.management.clientconfiguration.ClientConfigManager;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Manages cache start and stop and keeps a named collection of caches
 */
public class HostServer extends CacheServer {

    private static HostServer classInstance;
    private static Thread evalWarningTask;
    private TimeScheduler gcScheduler;
    private static boolean stopEvalWarning;

    private static String clusterIP;
    private static String clientServerIP;

    private static CacheInfo cacheInfo = null;
    private static CacheStopped cacheStopCallBack = null;
    private Thread portPublisher;

    public HostServer() {
        
    }

    public static String getObjectUri() {
        return HostServer.class.getName();
    }

    public static HostServer getInstance() {
        return classInstance;
    }
    
    public String getHostingCacheName(){
        if(cacheInfo != null && cacheInfo.getCache() != null){
            return cacheInfo.getCache().getName();
        }
        return null;
    }
    
    public static String getClientServerIP() {
        return clientServerIP;
    }

    public static void setClientServerIP(String clientServerIP) {
        HostServer.clientServerIP = clientServerIP;
    }

    public String getClusterIP() {
        return clusterIP;
    }
    
    @TargetMethodAttribute(privateMethod = "GetAllConfiguredCaches", privateOverload = 1)
    @Override
    public ConfiguredCacheInfo[] GetAllConfiguredCaches() {
        return super.GetAllConfiguredCaches();
    }

    public void setClusterIP(String value) {
        clusterIP = value;
    }

    public static void setInstance(HostServer value) {
        classInstance = value;
    }

 

    public void RegisterCacheStopCallBack(CacheStopped callback) {
        if (cacheStopCallBack == null) {
            cacheStopCallBack = callback;
        }
    }

    public void onCacheStopped() {
        if (cacheStopCallBack != null) {
            cacheStopCallBack.onCacheStopped();
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheInfo, privateOverload = 1)
    @Override
    public CacheInfo GetCacheInfo(String cacheId) {
        CacheInfo localCacheInfo = null;
        _rwLock.AcquireReaderLock();
        try {
            if (cacheInfo != null) {
                localCacheInfo = cacheInfo;
            }
        } finally {
            _rwLock.ReleaseReaderLock();
        }
        return localCacheInfo;
    }


    @TargetMethodAttribute(privateMethod = MethodName.ClearCacheContent, privateOverload = 1)
    @Override
    public void ClearCacheContent(String cacheId) throws OperationFailedException {

        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null) {
                cache.Clear();
            }
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.CanApplyHotConfig, privateOverload = 1)
    @Override
    public String CanApplyHotConfiguration(String cacheId, CacheServerConfig config) {
        Exception e = CanApplyHotConfig(config);

        if (e != null) {
            return e.getMessage();
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.BalanceDataloadOnCache, privateOverload = 1)
    @Override
    public void BalanceDataloadOnCache(String cacheId) throws SuspectedException, TimeoutException, GeneralFailureException {
        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null) {
                cache.BalanceDataLoad();
            }
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetStatistics, privateOverload = 1)
    @Override
    public CacheStatistics GetStatistics(String cacheId) {
        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null && cache.getIsRunning()) {
                return cache.getStatistics();
            }
        }
        return null;
    }
    
     /**
     * Gets the list of all the configured cache servers in a clustered cache
     * irrespective of running or stopped.
     *
     * @param cacheId
     * @return
     * @throws java.net.UnknownHostException
     */
@TargetMethodAttribute(privateMethod = MethodName.GetCacheServers, privateOverload = 1)
    @Override
    public final Node[] GetCacheServers(String cacheId) throws UnknownHostException {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }
        java.util.ArrayList<Node> serverNodes = new java.util.ArrayList<Node>();
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            if (cacheInfo.getCacheProps().getCacheType().equals("clustered-cache")) {
                java.util.ArrayList<Address> nodeAddresses = cacheInfo.getCacheProps().getCluster().GetAllConfiguredNodes();
                ServerNode server = null;
                for (Address node : nodeAddresses) {
                    server = new ServerNode();
                    server.setAddress(node);
                    serverNodes.add((Node) server);
                }
            }
        }

        return serverNodes.toArray(new Node[serverNodes.size()]);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetSnmpPorts, privateOverload = 1)
    @Override
    public java.util.HashMap GetSnmpPorts(String cacheid) {
        HashMap snmpPorts = new HashMap();
        Map temp = PortPool.getInstance().getSNMPMap();
        for (Object entryObject : temp.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            snmpPorts.put(entry.getKey(), entry.getValue());
        }

        return snmpPorts;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheStatus, privateOverload = 1)
    @Override
    public StatusInfo GetCacheStatus(String cacheId, String partitionId) {
        StatusInfo status = new StatusInfo();

        if (cacheInfo != null && cacheInfo.getCacheProps() != null) {
            status.setConfigID(cacheInfo.getCacheProps().getConfigID());
        }
        
        if(!cacheInfo.getCache().getName().equalsIgnoreCase(cacheId)){
            status.Status = CacheStatus.Registered;
            return status;
        }   
                  
        
  
            if (cacheInfo != null) {
                LeasedCache cache = cacheInfo.getCache();
                if (cache != null) {
                    status.Status = cache.getIsRunning() ? CacheStatus.Running : CacheStatus.Registered;
                    status.setIsCoordinator(cache.getIsCoordinator());
                    

                }
            }
        

        return status;
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 1)
    @Override
    public void StartCache(String cacheId) throws Exception {
        StartCache(cacheId, null, null, null);
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 2)
    public void StartCache(String cacheId, String partitionId) throws Exception {
        StartCache(cacheId, partitionId, null, null);
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 3)
    public void StartCache(String cacheId, byte[] userId, byte[] password) throws Exception {
        StartCache(cacheId, null, null, null, null, null, null, null, userId, password, false);
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 4)
    public void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws Exception {
        StartCache(cacheId, partitionId, null, null, null, null, null, null, userId, password, twoPhaseInitialization);
    }

    @TargetMethodAttribute(privateMethod = "StartCache", privateOverload = 5)
    public void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password) throws Exception {
        StartCache(cacheId, partitionId, null, null, null, null, null, null, userId, password, false);
    }

    @TargetMethodAttribute(privateMethod = "StartCache", privateOverload = 6)
    public void StartCache(String cacheId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate) throws
            Exception {
        StartCache(cacheId, null, itemAdded, itemRemoved, itemUpdated, cacheCleared, customRemove, customUpdate, null, null, false);
    }

    @TargetMethodAttribute(privateMethod = "StartCache", privateOverload = 7)
    public final void StartCache(String cacheId, String partitionId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws
            Exception {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        LeasedCache cache = null;

        _rwLock.AcquireWriterLock();
        try { ///For a finally {...}
            try {
                LoadConfiguration(cacheId);
            } catch (Exception e) {
                String msg = String.format("HostServer failed to load configuration, Error %1$s", e.getMessage());
                EventLogger.LogEvent(msg, EventType.WARNING);
            }
            if (cacheInfo != null) {
                cache = cacheInfo.getCache();
            }

            try {
                StartCacheInstance(cache, itemAdded, itemRemoved, itemUpdated, cacheCleared, customRemove, customUpdate, userId, password, twoPhaseInitialization);
                EventLogger.LogEvent("TayzGrid", "\"" + cacheId + "\"" + " started successfully.", EventType.INFORMATION, EventCategories.Information, EventID.CacheStart);
                
            } catch (Exception e) {
                EventLogger.LogEvent("TayzGrid", "\"" + cacheId + "\" can not be started.\n" + e.toString(), EventType.ERROR, EventCategories.Error, EventID.CacheStartError);
                throw e;
            }
        } finally {
            _rwLock.ReleaseWriterLock();
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClientNodeStatus, privateOverload = 1)
    @Override
    public ClientNodeStatusWrapper GetClientNodeStatus(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException,
            InstantiationException, IllegalAccessException {
        
        ClientNodeStatus status = ClientConfigManager.GetClientNodeStatus(cacheId);

        if (status == ClientNodeStatus.ClientCacheDisabled) {
            status = ClientNodeStatus.ClientCacheUnavailable;
        }
        ClientNodeStatusWrapper nodeStatus = new ClientNodeStatusWrapper();
        nodeStatus.value = status;
        return nodeStatus;
    }

    @TargetMethodAttribute(privateMethod = MethodName.StopCache, privateOverload = 2)
    public void StopCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean isGraceFulShutDown) throws Exception{
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        LeasedCache cache = null;

        _rwLock.AcquireWriterLock();
        try { ///For a finally {...}

            if (cacheInfo != null) {
                cache = cacheInfo.getCache();
            }
            try {

                StopCacheInstance(cache, CacheStopReason.Stoped, isGraceFulShutDown);
                EventLogger.LogEvent("TayzGrid", "\"" + cacheId + "\"" + " stopped successfully.", EventType.INFORMATION, EventCategories.Information, EventID.CacheStop);

            } catch (Exception e) {
                EventLogger.LogEvent("TayzGrid", "\"" + cacheId + "\" can not be stopped.\n" + e.toString(), EventType.ERROR, EventCategories.Error, EventID.CacheStopError);
                throw e;
            }

        } finally {
            _rwLock.ReleaseWriterLock();
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.IsRunning, privateOverload = 1)
    @Override
    public boolean IsRunning(String cacheId) {

        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null) {
                return cache.getIsRunning() && cache.getName().toLowerCase().equals(cacheId.toLowerCase());
            }
       }
        
        return false;
    }
    
    public  boolean getIsCacheRunning(String cacheId)throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException
    {
        return false;
    }

    @TargetMethodAttribute(privateMethod = MethodName.HotApplyConfiguration, privateOverload = 1)
    @Override
    public void ApplyHotConfiguration(String cacheId, HotConfig hotConfig) throws CacheArgumentException, Exception {
        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null) {
                cache.ApplyHotConfiguration(hotConfig);
            }
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public final void dispose() {
        dispose(true);
    }

    private void StopCache(CacheStopReason reason) throws Exception {

        if (cacheInfo != null) {
            LeasedCache cache = cacheInfo.getCache();
            if (cache != null) {
                StopCacheInstance(cache, reason, false);
            }
        }
    }

    private void StartCacheInstance(final LeasedCache cache, final ItemAddedCallback itemAdded, final ItemRemovedCallback itemRemoved, final ItemUpdatedCallback itemUpdated, final CacheClearedCallback cacheCleared, final CustomRemoveCallback customRemove, final CustomUpdateCallback customUpdate, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws
            ManagementException, Exception {
        if (cache != null) {
            if (itemAdded != null) {
                cache.addItemAddedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        itemAdded.invoke(obj[0], (EventContext) obj[1]);
                        return null;
                    }
                }, null);
            }
            if (itemRemoved != null) {
                cache.addItemRemovedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        itemRemoved.invoke(obj[0], obj[1], (ItemRemoveReason) obj[2], (BitSet) obj[3], (EventContext) obj[4]);
                        return null;
                    }
                }, null);
            }
            if (itemUpdated != null) {
                cache.addItemAddedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        itemUpdated.invoke(obj[0], (EventContext) obj[1]);
                        return null;
                    }
                }, null);
            }
            if (cacheCleared != null) {
                cache.addCacheClearedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        cacheCleared.invoke((EventContext) obj[0]);
                        return null;
                    }
                }, null);

            }
            if (customRemove != null) {
                cache.addCustomRemoveNotifListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        customRemove.invoke(obj[0], obj[1], (ItemRemoveReason) obj[2], (BitSet) obj[3], (EventContext) obj[4]);
                        return null;
                    }
                }, null);
            }
            if (customUpdate != null) {
                cache.addCustomUpdateNotifListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, Exception {
                        customUpdate.invoke(evalWarningTask, evalWarningTask, (EventContext) obj[2]);
                        return null;
                    }
                }, null);
            }

            if (!cache.getIsRunning()) {
                if (cacheInfo != null) {
                    cacheInfo.SyncConfiguration();
                }
                cache.StartInstance(_renderer, Decrypt(userId), Decrypt(password), twoPhaseInitialization);
                    if (com.alachisoft.tayzgrid.caching.Cache.OnCacheStarted != null) {
                        com.alachisoft.tayzgrid.caching.Cache.OnCacheStarted.invoke(cache.getName());
                    }
                                
                startPortPublisher(cache.getName());
            }

        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }
    }

    private void startPortPublisher(final String cacheId){
         if(portPublisher == null){
                    portPublisher = new Thread(new Runnable(){
                         @Override
                        public void run() {
                            while(true){
                                try{
                                     Thread.sleep(5000);
                                     sendSnmpPort(cacheId);
                                }catch(ThreadDeath e){
                                    break;
                                }
                                catch(InterruptedException e){
                                    break;
                                }
                                catch(Exception e){
                                    
                                }
                            }
                        }
                        
                    });
                    
                    portPublisher.setDaemon(false);
                    portPublisher.setName("port_pubisher");
                    portPublisher.start();
                }
    }
    
    private void stopPortPublisher(){
        if(portPublisher != null && portPublisher.isAlive()){
            portPublisher.interrupt();
        }
    }
    private void StopCacheInstance(LeasedCache cache, CacheStopReason reason, Boolean isGracefulShutdown) throws Exception {
        if (cache != null) {
            if (cache.getIsRunning()) {
                if (!cache.VerifyNodeShutdownInProgress(isGracefulShutdown)) {
                    throw new ManagementException("Graceful shutdown is already in progress...");
                }

                if (reason == CacheStopReason.Expired) {
                    EventLogger.LogEvent("TayzGrid","TayzGrid license has expired on this machine. Stopping cache...", EventType.ERROR,EventCategories.Error,EventID.LicensingError);
                }
                cache.StopInstance(isGracefulShutdown);

                if (cacheInfo != null) {
                    cacheInfo.SyncConfiguration();
                }

                //instrumentation Code
                   if (com.alachisoft.tayzgrid.caching.Cache.OnCacheStopped != null) {
                        com.alachisoft.tayzgrid.caching.Cache.OnCacheStopped.invoke(cache.getName());
                    }
                
            }
            onCacheStopped();
            stopPortPublisher();
            
            
        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }

    }

    /**
     * Load all the config sections from the configuration file.
     */
    private static void LoadConfiguration(String cacheName) throws ConfigurationException, ManagementException {
        try {
            CacheServerConfig[] configs = CacheConfigManager.GetConfiguredCaches();
            if (configs != null) {
                for (int i = 0; i < configs.length; i++) {
                    CacheServerConfig config = configs[i];
                    if (config != null && !Common.isNullorEmpty(cacheName) && cacheName.toLowerCase().equals(config.getName().toLowerCase())) {
                        String props = GetProps(config);
                        if (cacheInfo == null) {
                            cacheInfo = new CacheInfo();
                            cacheInfo.setCache(new LeasedCache(props));
                            cacheInfo.setCacheProps(config);
                        } else {
                            cacheInfo.setCacheProps(config);
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Get string props representation of config
     *
     * @param config
     * @return
     */
    private static String GetProps(CacheServerConfig config) {
        java.util.HashMap table = ConfigConverter.ToHashMap(config);
        String props = ConfigReader.ToPropertiesString(table);
        return props;
    }

   


    /**
     * Add garbage collection task to time Scheduler
     */
    private void StartGCTask() {
        boolean enabled = true;
        tangible.RefObject<Boolean> tempRef_enabled = new tangible.RefObject<Boolean>(enabled);
        tempRef_enabled.argvalue = Boolean.getBoolean(ServicePropValues.CacheServer_EnableForcedGC);
        enabled = tempRef_enabled.argvalue;

        if (enabled) {
            int threshold = 0;
            tangible.RefObject<Integer> tempRef_threshold = new tangible.RefObject<Integer>(threshold);
            boolean tempVar = Boolean.getBoolean(ServicePropValues.CacheServer_ForcedGCThreshold);
            threshold = tempRef_threshold.argvalue;
            if (tempVar) {
                this.gcScheduler.AddTask(new TimeScheduler.Task() {
                    @Override
                    public boolean IsCancelled() {
                        return false;
                    }

                    @Override
                    public long GetNextInterval() {
                        return 1000 * 60 * 60 * 12; // 12 hour interval.
                    }

                    @Override
                    public void Run() {
                    }
                });
            }
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     * @param disposing
     */
    private void dispose(boolean disposing) {
        _rwLock.AcquireWriterLock();
        try {
            try {
                if (cacheInfo != null && cacheInfo.getCache() != null) {
                    cacheInfo.getCache().StopInstance(false);
                }
            } catch (Exception e) {
            }
            if (evalWarningTask != null) {
                try {
                    stopEvalWarning = true;
                    evalWarningTask.interrupt();
                } catch (Exception e) {
                }
            }
            if (this.gcScheduler != null) {
                synchronized (this.gcScheduler) {
                    if (this.gcScheduler != null) {
                        try {
                            this.gcScheduler.Stop();
                            this.gcScheduler.dispose();
                        } catch (InterruptedException interruptedException) {
                        }
                    }
                }
            }

        } finally {
            _rwLock.ReleaseWriterLock();
        }
        if (disposing) {
            System.gc();
        }
    }

    private Exception CanApplyHotConfig(CacheServerConfig config) {

        if (cacheInfo != null) {
            Cache cache = cacheInfo.getCache();
            if (cache != null) {
                return cacheInfo.getCache().CanApplyHotConfig(config.getStorage().getSize() * 1024 * 1024);
            }
        }
        return null;
    }
    
    
    private void sendSnmpPort(String cacheId) {
        ICacheServer cs = null;
        try {
            CacheRPCService _ncacheService = new CacheRPCService("");
            _ncacheService.setServerName(ServicePropValues.BIND_ToCLUSTER_IP);
            _ncacheService.setPort(Integer.parseInt(ServicePropValues.CACHE_MANAGEMENT_PORT));
            cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null) {
                cs.setSnmpPort(cacheId, PortPool.getInstance().getSNMPMap());
            }
        } catch (Exception ex) {

        }
        if (cs != null) {
                cs.dispose();
            }
    }
    
    
    @TargetMethodAttribute(privateMethod = MethodName.GetProcessId, privateOverload = 1)
    @Override
    public String getPID() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }
    

}
