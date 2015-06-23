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

import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheClearedCallback;
import com.alachisoft.tayzgrid.caching.CacheRenderer;
import com.alachisoft.tayzgrid.caching.CustomRemoveCallback;
import com.alachisoft.tayzgrid.caching.CustomUpdateCallback;
import com.alachisoft.tayzgrid.caching.GracefulTimeout;
import com.alachisoft.tayzgrid.caching.ItemAddedCallback;
import com.alachisoft.tayzgrid.caching.ItemRemovedCallback;
import com.alachisoft.tayzgrid.caching.ItemUpdatedCallback;
import com.alachisoft.tayzgrid.caching.LeasedCache;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.EncryptionUtil;
import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.common.enums.CacheStatusOnServer;
import com.alachisoft.tayzgrid.common.enums.CacheStatusOnServerContainer;
import com.alachisoft.tayzgrid.common.enums.CacheTopology;
import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.EventArgs;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.LoggingInfo.LoggingSubsystem;
import com.alachisoft.tayzgrid.common.LoggingInfo.LoggingType;
import com.alachisoft.tayzgrid.common.LoggingInfo.LogsStatus;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServerPlatform;
import com.alachisoft.tayzgrid.common.ServiceConfiguration;
import com.alachisoft.tayzgrid.common.StatusInfo;

import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.EventViewerEvent;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.monitoring.ServerNode;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import com.alachisoft.tayzgrid.common.rpcframework.TargetMethodAttribute;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.util.ManagementUtil.MethodName;
import com.alachisoft.tayzgrid.common.util.ProcessExecutor;
import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.Cluster;
import com.alachisoft.tayzgrid.config.dom.ConfigConverter;

import com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper;
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.EnvPropValue;
import com.alachisoft.tayzgrid.common.PortPoolValidationTask;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList;
import com.alachisoft.tayzgrid.management.clientconfiguration.ClientConfigManager;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.servicecontrol.CacheService;

import java.io.*;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Manages cache start and stop and keeps a named collection of caches
 */
public class CacheServer implements ICacheServer, IDisposable {

    private static CacheServer s_instance;
    private static Object serviceObject = new Object();

    /**
     * A HashMap that contains a list of all the cache objects created so far.
     */
    private static java.util.HashMap s_caches = new java.util.HashMap();

    /**
     * Reader writer lock used to synchronize access to internals.
     */
    public ReaderWriterLock _rwLock = new ReaderWriterLock();
    /**
     * Socket server port
     */
    private static int _socketServerPort;
    private static String _clusterIp;
    private static String _clientserverip;
    private static String _localCacheIp;
    public CacheRenderer _renderer;
    private static Thread _evalWarningTask;
    private TimeScheduler _gcScheduler;
    private static TimeScheduler _portPoolScheduler;
    private static boolean stopEvalWarning = false;

  

    @Override
    public String getPID() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getIsCacheRunning(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        return false;
    }

    public enum CacheStopReason {

        Expired,
        Stoped,
        ForcedStoped
    }

    /**
     * Enumeration specifying type of channel
     */
    public enum Channel {

        /**
         * Bind cluster channel
         */
        Cluster(0),
        /**
         * Bind client server channel
         */
        SocketServer(1);
        private final int intValue;
        private static java.util.HashMap<Integer, Channel> mappings;

        private static java.util.HashMap<Integer, Channel> getMappings() {
            synchronized (Channel.class) {
                if (mappings == null) {
                    mappings = new java.util.HashMap<Integer, Channel>();
                }
            }
            return mappings;
        }

        private Channel(int value) {
            intValue = value;
            Channel.getMappings().put(value, this);
        }

        public int getValue() {
            return intValue;
        }

        public static Channel forValue(int value) {
            return getMappings().get(value);
        }
    }

    /**
     * Gets or sets the cache cacheRenderer.
     *
     * @return
     */
    public CacheRenderer getRenderer() {
        return _renderer;
    }

    public void setRenderer(CacheRenderer value) {
        _renderer = value;
    }

    public static String getObjectUri() {
        return CacheServer.class.getName();
    }

    public String getClusterIP() {
        return _clusterIp;
    }

    public void setClusterIP(String value) {
        _clusterIp = value;
    }

    public final String getLocalCacheIP() {
        return _localCacheIp;
    }

    private final void setLocalCacheIP(String value) {
        _localCacheIp = value;
    }

    public static String getClientserverIp() {
        return CacheServer._clientserverip;
    }

    /**
     * Gets the singlton instance of CacheServer.
     *
     * @return
     */
    public static CacheServer getInstance() {
        return s_instance;
    }

    public static void setInstance(CacheServer value) {
        s_instance = value;
    }

    /**
     * Gets or sets the socket server port.
     *
     * @return
     */
    public static int getSocketServerPort() {
        return _socketServerPort;
    }

    public static void setSocketServerPort(int value) {
        _socketServerPort = value;
    }

    /**
     * returns the collection of cache objects.
     *
     * @return
     */
    public final java.util.Collection getCaches() {
        java.util.ArrayList caches = new java.util.ArrayList();
        _rwLock.AcquireReaderLock();
        try {
            Iterator en = s_caches.entrySet().iterator();
            while (en.hasNext()) {
                Map.Entry current = (Map.Entry) en.next();
                CacheInfo cacheInfo = (CacheInfo) current.getValue();
                {
                    caches.add(cacheInfo.getCache());
                }
            }
            return caches;
        } finally {
            _rwLock.ReleaseReaderLock();
        }
    }

    public CacheServer() {
        if (this._gcScheduler == null) {
            this._gcScheduler = new TimeScheduler();
        }
        this._gcScheduler.Start();

        NetworkData.registerIPToMonitor(ServicePropValues.BIND_ToCLUSTER_IP);
        NetworkData.registerIPToMonitor(ServicePropValues.BIND_toClient_IP);
        this.StartGCTask();
        this.StartPoortPoolValidationTask();

    }

    /**
     * Static constructor
     */
    static {
        try {
            LoadConfiguration();        
            RegisterCompactTypes();
        } catch (Exception e) {
            String msg = String.format("CacheServer failed to load configuration, Error %1$s", e.getMessage());
            EventLogger.LogEvent("TayzGrid",msg, EventType.WARNING,EventCategories.Warning,EventID.CacheStart);
        }
    }


    
    public static void RegisterCompactTypes() throws CacheArgumentException {
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.rpcframework.TargetMethodParameter.class, ((Integer) 165).shortValue());

        //<editor-fold defaultstate="collapsed" desc="[Register Cache Server Assemblies]">
        FormatterServices.getDefault().registerKnownTypes(CacheServerConfig.class, ((Integer) 177).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Log.class, ((Integer) 178).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PerfCounters.class, ((Integer) 179).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.QueryIndex.class, ((Integer) 181).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Class.class, ((Integer) 182).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Type.class, ((Integer) 184).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CompactClass.class, ((Integer) 186).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.BackingSource.class, ((Integer) 187).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Readthru.class, ((Integer) 188).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Writethru.class, ((Integer) 189).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Provider.class, ((Integer) 190).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheLoader.class, ((Integer) 191).shortValue());
        

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Notifications.class, ((Integer) 192).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Cleanup.class, ((Integer) 194).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Storage.class, ((Integer) 195).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.EvictionPolicy.class, ((Integer) 196).shortValue());
        FormatterServices.getDefault().registerKnownTypes(Cluster.class, ((Integer) 197).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.dom.Channel.class, ((Integer) 198).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NodeIdentity.class, ((Integer) 199).shortValue());
        FormatterServices.getDefault().registerKnownTypes(StatusInfo.class, ((Integer) 200).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing.class, ((Integer) 203).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNodes.class, ((Integer) 204).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNode.class, ((Integer) 205).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AlertsNotifications.class, ((Integer) 206).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.EmailNotifications.class, ((Integer) 207).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AlertsTypes.class, ((Integer) 208).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient.class, ((Integer) 209).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig.class, ((Integer) 210).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.TaskConfiguration.class, ((Integer) 365).shortValue());
        FormatterServices.getDefault().registerKnownTypes(CacheRegisterationInfo.class, ((Integer) 212).shortValue());
        FormatterServices.getDefault().registerKnownTypes(CacheStatusOnServerContainer.class, ((Integer) 213).shortValue());
        FormatterServices.getDefault().registerKnownTypes(CacheStatistics.class, ((Integer) 65).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClusterCacheStatistics.class, ((Integer) 66).shortValue());
        FormatterServices.getDefault().registerKnownTypes(NodeInfo.class, ((Integer) 67).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration.class, ((Integer) 214).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.clientconfiguration.dom.NodeConfiguration.class, ((Integer) 215).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration.class, ((Integer) 216).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer.class, ((Integer) 219).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.CacheInfo.class, ((Integer) 220).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Class[].class, ((Integer) 249).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper.class, ((Integer) 250).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Attrib.class, ((Integer) 251).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Type[].class, ((Integer) 252).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableClass.class, ((Integer) 253).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableClass[].class, ((Integer) 254).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AttributeListUnion.class, ((Integer) 255).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableAttribute.class, ((Integer) 256).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableAttribute[].class, ((Integer) 257).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.management.NewCacheRegisterationInfo.class, ((Integer) 258).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.enums.RtContextValue.class, ((Integer) 300).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheDeployment.class, ((Integer) 264).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class, ((Integer) 265).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Cluster.class, ((Integer) 266).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheServerConfigSetting.class, ((Integer) 267).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheTopology.class, ((Integer) 268).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Channel.class, ((Integer) 269).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ServerNode.class, ((Integer) 270).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ServersNodes.class, ((Integer) 271).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.WriteBehind.class, ((Integer) 276).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.BatchConfig.class, ((Integer) 277).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.NewHashmap.class, ((Integer) 346).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.caching.util.HotConfig.class, ((Integer) 349).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy.class, ((Integer) 348).shortValue());

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="[Monitor Server Assemblies]">
        FormatterServices.getDefault().registerKnownTypes(CacheNodeStatistics.class, ((Integer) 221).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ServerNode.class, ((Integer) 222).shortValue());

        FormatterServices.getDefault().registerKnownTypes(EventViewerEvent.class, ((Integer) 223).shortValue());

        FormatterServices.getDefault().registerKnownTypes(Node.class, ((Integer) 224).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.net.Address.class, ((Integer) 110).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.monitoring.ClientNode.class, ((Integer) 226).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ConfiguredCacheInfo.class, ((Integer) 227).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClientProcessStats.class, ((Integer) 228).shortValue());

        FormatterServices.getDefault().registerKnownTypes(CacheNodeStatistics[].class, ((Integer) 229).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ServerNode[].class, ((Integer) 230).shortValue());
        FormatterServices.getDefault().registerKnownTypes(EventViewerEvent[].class, ((Integer) 231).shortValue());

        FormatterServices.getDefault().registerKnownTypes(Node[].class, ((Integer) 232).shortValue());
        FormatterServices.getDefault().registerKnownTypes(Address[].class, ((Integer) 233).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.monitoring.ClientNode[].class, ((Integer) 234).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ConfiguredCacheInfo[].class, ((Integer) 235).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClientProcessStats[].class, ((Integer) 236).shortValue());
        FormatterServices.getDefault().registerKnownTypes(BindedIpMap.class, ((Integer) 237).shortValue());
        FormatterServices.getDefault().registerKnownTypes(NodeInfoMap.class, ((Integer) 238).shortValue());
        FormatterServices.getDefault().registerKnownTypes(CacheServerList.class, ((Integer) 239).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.User.class, ((Integer) 240).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.User[].class, ((Integer) 241).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[].class, ((Integer) 242).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CompactClass[].class, ((Integer) 243).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Parameter.class, ((Integer) 244).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Parameter[].class, ((Integer) 245).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ProviderAssembly.class, ((Integer) 246).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNode[].class, ((Integer) 247).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Provider[].class, ((Integer) 248).shortValue());

        //</editor-fold>
    }

    



    @TargetMethodAttribute(privateMethod = "GetLicenseKey", privateOverload = 1)
    public final String GetLicenseKey() {
        return new String();
    }

    /**
     *
     *
     * @param cacheName
     * @param assemblyFileName
     * @param name Name of the file (assembly)
     * @param buffer
     */
    @TargetMethodAttribute(privateMethod = "CopyAssemblies", privateOverload = 1)
    @Override
    public final void CopyAssemblies(String cacheName, String assemblyFileName, byte[] buffer) {
        //********************** Newly merged code **********************************/
        if (AppUtil.getInstallDir() != null) {
            FileOutputStream fs = null;
            try {
                String path = DirectoryUtil.getDeployedAssemblyFolder();
                (new java.io.File(path)).mkdir();
                String currentCacheFolderPath = Common.combinePath(path, cacheName.toLowerCase());
                (new java.io.File(currentCacheFolderPath.trim())).mkdir();
                fs = new FileOutputStream(Common.combinePath(currentCacheFolderPath, assemblyFileName));
                fs.write(buffer, 0, buffer.length);
                fs.flush();
            } catch (Exception e) {
            } finally {
                if (fs != null) {
                    fs = null;
                }
            }
        }
        //****************************************************************************/
    }

    /**
     *
     *
     * @param bridgeName
     * @param assemblyFileName
     * @param buffer
     */
    @TargetMethodAttribute(privateMethod = "CopyBridgeAssemblies", privateOverload = 1)
    @Override
    public final void CopyBridgeAssemblies(String bridgeName, String assemblyFileName, byte[] buffer) {
    }

    /**
     *
     *
     * @param cacheName
     * @param fileName
     * @return
     */
    @TargetMethodAttribute(privateMethod = "GetAssembly", privateOverload = 1)
    public final byte[] GetAssembly(String cacheName, String fileName) throws java.io.IOException {
        byte[] asmData = null;
        if (AppUtil.getInstallDir() != null) {
            InputStream fs = null;
            try {
                String path = AppUtil.DeployedAssemblyDir + cacheName.toLowerCase() + "\\" + fileName;
                fs = new FileInputStream(path);
                asmData = new byte[fs.available()];
                fs.read(asmData, 0, asmData.length);

            } catch (Exception e) {
                return asmData;
            } finally {
                if (fs != null) {
                    fs.close();
                    fs = null;
                }
            }
        }
        return asmData;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheInfo, privateOverload = 1)
    @Override
    public CacheInfo GetCacheInfo(String cacheId) {
        CacheInfo cacheInfo = null;
        _rwLock.AcquireReaderLock();
        try {
            if (s_caches.containsKey(cacheId.toLowerCase())) {
                cacheInfo = (CacheInfo) ((s_caches.get(cacheId.toLowerCase()) instanceof CacheInfo) ? s_caches.get(cacheId.toLowerCase()) : null);
            }
        } finally {
            _rwLock.ReleaseReaderLock();
        }
        return cacheInfo;
    }

    /**
     * finds and returns a cache object, that was previously created.
     *
     * @param cacheId
     * @return
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    public final Cache getItem(String cacheId) {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            return cacheInfo.getCache();
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = "GetCacheInstance", privateOverload = 1)
    public final Cache GetCacheInstance(String cacheId, String partitionId) {
        if (partitionId == null || partitionId.equals("")) {
            return this.getItem(cacheId);
        } else {
            if (cacheId == null) {
                throw new IllegalArgumentException("cacheId");
            }
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = "GetCacheInstance", privateOverload = 2)
    public final Cache GetCacheInstance(String cacheId, byte[] userId, byte[] password) {
        return this.getItem(cacheId);
    }

    @TargetMethodAttribute(privateMethod = "GetCacheInstance", privateOverload = 3)
    public final Cache GetCacheInstance(String cacheId, String partitionId, byte[] userId, byte[] password) {
        Cache returnCache = null;
        if (partitionId == null || partitionId.equals("")) {
            returnCache = this.getItem(cacheId);
        } else {
            if (cacheId == null) {
                throw new IllegalArgumentException("cacheId");
            }
        }

        return returnCache;
    }

    /**
     * Gets the cache instance ignoring the backup/replica id. For e.g. if we
     * have two instances of the same cache por_test (master node id) and
     * por_test_bk_node_node1 a replica of the same cache. Now we try to first
     * connect the master id and if it is not available or running then we try
     * to connect to its backup although its id is different. UserId and
     * password are in encryped form.
     *
     * @param cacheId Id of the cache
     * @param userId Encrypted user id
     * @param password Encrypted password
     * @return Cache Instance
     * @throws ArgumentNullException
     */
    @TargetMethodAttribute(privateMethod = "GetCacheInstanceIgnoreReplica", privateOverload = 1)
    public Cache GetCacheInstanceIgnoreReplica(String cacheId, byte[] userId, byte[] password) throws ArgumentNullException {
        return GetCacheInstanceIgnoreReplica(cacheId, Decrypt(userId), Decrypt(password));
    }

    /**
     * Gets the cache instance ignoring the backup/replica id. For e.g. if we
     * have two instances of the same cache por_test (master node id) and
     * por_test_bk_node_node1 a replica of the same cache. Now we try to first
     * connect the master id and if it is not available or running then we try
     * to connect to its backup although its id is different.
     *
     * @param cacheId Id of the cache
     * @param userId user id
     * @param password password
     * @return Cache Instance
     * @throws ArgumentNullException
     */
    @TargetMethodAttribute(privateMethod = "GetCacheInstanceIgnoreReplica", privateOverload = 2)
    public Cache GetCacheInstanceIgnoreReplica(String cacheId, String userId, String password) throws ArgumentNullException {

        if (userId != null && userId.equals("")) {
            userId = null;
        }
        if (password != null && password.equals("")) {
            password = null;
        }
        if (cacheId == null) {
            throw new ArgumentNullException("Cache ID can not be null");
        }
        cacheId = cacheId.toLowerCase();

        Boolean isLockAcquired = false;
        if (!_rwLock.IsWriterLockHeld()) {
            _rwLock.AcquireReaderLock();
            isLockAcquired = true;
        }
        Cache cache = null;
        try {
            cache = getItem(cacheId);
            if (cache != null && cache.getIsRunning()) {
                return cache;
            }
        } finally {
            if (isLockAcquired) {
                _rwLock.ReleaseReaderLock();
            }
        }
        return cache;
    }

    /**
     *
     * @param userId
     * @param password
     * @return
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheProps, privateOverload = 1)
    @Override
    public final java.util.Map GetCacheProps(byte[] userId, byte[] password){
        return getCacheProps();
    }

    /**
     * CacheProps are in new format now. Instead of saving the props string, it
     * now saves CacheServerConfig instance:
     *
     * |local-cache-id | CacheServerConfig instance
     * | IDictionary | replica-id |
     * CacheServerConfig instance
     *
     * @return A collection of the cache info registered with the server.
     */
    private final java.util.Map getCacheProps() {
        java.util.Map cacheProps = new java.util.HashMap();
        _rwLock.AcquireReaderLock();
        try {
            Iterator en = s_caches.entrySet().iterator();
            while (en.hasNext()) {
                Map.Entry current = (Map.Entry) en.next();
                CacheInfo cacheInfo = (CacheInfo) current.getValue();
                cacheProps.put(cacheInfo.getCache().getName(), cacheInfo.getCacheProps());
            }
            return cacheProps;
        } finally {
            _rwLock.ReleaseReaderLock();
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheConfiguration, privateOverload = 1)
    public final CacheServerConfig GetCacheConfiguration(String cacheId) {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        CacheServerConfig config = null;

        if (cacheInfo != null) {
            config = cacheInfo.getCacheProps();
        }

        return config;
    }

    /**
     *
     * @param cacheId
     * @return
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetNewConfiguration, privateOverload = 1)
    @Override
    public final com.alachisoft.tayzgrid.config.newdom.CacheServerConfig GetNewConfiguration(String cacheId) throws Exception {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        CacheServerConfig config = null;

        if (cacheInfo != null) {
            config = cacheInfo.getCacheProps();
            return com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToNewDom(config);
        }

        return null;

    }

    @TargetMethodAttribute(privateMethod = MethodName.GetHostName, privateOverload = 1)
    @Override
    public final String GetHostName() throws UnknownHostException {
        String localhost = java.net.InetAddress.getLocalHost().getHostName();
        return localhost;
    }

    /**
     *
     * @param cacheId
     * @param partId
     * @param newNode
     * @param isJoining
     * @return
     * @throws ManagementException
     * @throws Exception
     */
    @TargetMethodAttribute(privateMethod = "GetUpdatedCacheConfiguration", privateOverload = 1)
    @Override
    public final CacheRegisterationInfo GetUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, Exception {
        CacheServerConfig config = null;
        java.util.ArrayList affectedNodes = new java.util.ArrayList();
        java.util.ArrayList affectedPartitions = new java.util.ArrayList();
        try {
            tangible.RefObject<java.util.ArrayList> tempRef_affectedNodes = new tangible.RefObject<java.util.ArrayList>(affectedNodes);
            config = CacheConfigManager.GetUpdatedCacheConfig(cacheId, partId, newNode, tempRef_affectedNodes, isJoining);
            affectedNodes = tempRef_affectedNodes.argvalue;
        } catch (Exception ex) {
            throw ex;
        }

        return new CacheRegisterationInfo(config, affectedNodes, affectedPartitions);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetNewUpdatedCacheConfiguration, privateOverload = 1)
    @Override
    public NewCacheRegisterationInfo GetNewUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, Exception {

        com.alachisoft.tayzgrid.management.CacheRegisterationInfo oldCacheInfo = GetUpdatedCacheConfiguration(cacheId, partId, newNode, isJoining);

        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig newDom = com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToNewDom(oldCacheInfo.getUpdatedCacheConfig());
        com.alachisoft.tayzgrid.management.NewCacheRegisterationInfo newCacheInfo = new NewCacheRegisterationInfo(newDom, oldCacheInfo.getAffectedNodes(), oldCacheInfo.getAffectedPartitions());

        return newCacheInfo;

    }

    @TargetMethodAttribute(privateMethod = "GetCacheRenderer", privateOverload = 1)
    public final CacheRenderer GetCacheRenderer() {
        return _renderer;

    }

    /**
     * Clear cache
     *
     * @param cacheId
     * @throws OperationFailedException
     *
     */
    @TargetMethodAttribute(privateMethod = MethodName.ClearCache, privateOverload = 1)
    @Override
    public void ClearCache(String cacheId) throws OperationFailedException {
        ClearCacheContent(cacheId);
    }

    @TargetMethodAttribute(privateMethod = MethodName.Authorize, privateOverload = 1)
    public final boolean Authorize(byte[] userId, byte[] password) {
        return true;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetTayzGridServer, privateOverload = 1)
    public final boolean IsTayzGridServer() {
        return false;
    }

    /**
     * Get a list of running caches (local + clustered)
     *
     * @param userId user id
     * @param password password
     * @return list of running caches
     */
    @TargetMethodAttribute(privateMethod = "GetRunningCaches", privateOverload = 2)
    public final java.util.ArrayList GetRunningCaches(String userId, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException,
            InterruptedException {
        return GetRunningCaches(EncryptionUtil.Encrypt(userId), EncryptionUtil.Encrypt(password));
    }

    /**
     * Returns a list of running caches
     *
     * @param userId user id
     * @param password password
     * @return list of running caches
     */
    @TargetMethodAttribute(privateMethod = "GetRunningCaches", privateOverload = 3)
    private java.util.ArrayList GetRunningCaches(byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException
            {
        java.util.ArrayList runningCache = new java.util.ArrayList(5);
        java.util.Map coll = GetCacheProps(userId, password);
        for (Iterator it = coll.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            if (entry.getValue() instanceof CacheServerConfig) {
                CacheServerConfig config = (CacheServerConfig) entry.getValue();
                if (config.getCacheType().equals("local-cache") || config.getCacheType().equals("clustered-cache")) {
                    Cache instance = GetCacheInstance((String) entry.getKey(), userId, password);
                    if (instance != null && IsRunning(instance.getName())) {
                        runningCache.add(entry.getKey());
                    }
                }
            }
            if (entry.getValue() instanceof String) 
            {
                if (((String) entry.getValue()).indexOf("local-cache", 0) != -1 || ((String) entry.getValue()).indexOf("clustered-cache", 0) != -1) {

                    Cache instance = GetCacheInstance((String) entry.getKey(), userId, password);
                    if (instance != null && instance.getIsRunning()) {
                        runningCache.add(entry.getKey());
                    }
                }

            }
//            else if (entry.getValue() instanceof CacheServerConfig)
//            {
//                if(!((CacheServerConfig)entry.getValue()).getInProc())
//                {
//                    if(IsRunning(entry.getKey().toString()))
//                    {
//                        runningCache.add(entry.getKey());
//                    }
//                }
//            }
        }
        return runningCache;
    }

    @TargetMethodAttribute(privateMethod = MethodName.HotApplyConfiguration, privateOverload = 1)
    @Override
    public void ApplyHotConfiguration(String cacheId, HotConfig hotConfig) throws CacheArgumentException, Exception {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            int managementPort = cacheInfo.getManagementPort();
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                if (cacheServer != null) {
                    cacheServer.ApplyHotConfiguration(cacheId, hotConfig);
                } else {
                    throw new ManagementException("Specified cacheId is not started");
                }
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }

    }

    /**
     * Register cache
     *
     * @param cacheId
     * @param config
     * @param props
     * @param partId
     * @param userId
     * @param overwrite
     * @param password
     * @param hotApply
     * @return
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     * @throws java.net.UnknownHostException
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     *
     */
    @TargetMethodAttribute(privateMethod = MethodName.RegisterCache, privateOverload = 1)
    @Override
    public final boolean RegisterCache(String cacheId, com.alachisoft.tayzgrid.config.dom.CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws
            ConfigurationException, UnknownHostException, ManagementException, java.lang.Exception {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        cacheId = cacheId.toLowerCase();
        CacheInfo cacheInfo = null;

        _rwLock.AcquireWriterLock();
        try {
//            if(config.getCluster()!=null)
//            {
//                if (config.getCluster().getChannel().getTcpPort() == 0)
//                    config.getCluster().getChannel().setTcpPort(GetMaxPort());
//                else if (!PortIsAvailable(config.getCluster().getChannel().getTcpPort()))
//                    throw new ConfigurationException("Cluster Port is not available.");
//            }
            if (config.getManagementPort() == 0) {
                config.setManagementPort(GetMaxManagementPort());
            }

//            else if(!IsManagementPortAvailable(config.getManagementPort()))
//                throw new ConfigurationException("Managment Port is not available.");
            if (config.getClientPort() == 0) {
                config.setClientPort(GetMaxSocketPort());
            }
//            else if (!IsClientPortAvailable(config.getClientPort()))
//                throw new ConfigurationException("Client Port is not available.");

            if (s_caches.containsKey(cacheId.toLowerCase())) {
                if (!overwrite) {
                    return false;
                }
                cacheInfo = (CacheInfo) s_caches.get(cacheId.toLowerCase());
                if (cacheInfo != null) {
                    cacheInfo.setPorts(getPorts(config));
                    cacheInfo.setCacheProps(config);
                }
                
            } else {
                /**
                 * [Ata] This is until we change the use of properties in Cache
                 * from props string or HashMap to Dom
                 *
                 */
                String props = GetProps(config);

                ClientConfigManager.setLocalCacheId(this.getLocalCacheIP());
                ClientConfigManager.AddCache(cacheId, config.getClientPort(), config.getRuntimeContext());

                cacheInfo = new CacheInfo();
                cacheInfo.setCache(new LeasedCache(props));
                cacheInfo.setPorts(getPorts(config));
                cacheInfo.setCacheProps(config);
                s_caches.put(cacheId.toLowerCase(), cacheInfo);
            }
               
            if("client-cache".equals(config.getCacheType()))
            {
                ClientConfigManager.setLocalCacheId(this.getLocalCacheIP());
                ClientConfigManager.AddCache(cacheId, config);
            }
            
            if("local-cache".equals(config.getCacheType()))
            {
                ClientConfigManager.setLocalCacheId(this.getLocalCacheIP());
                ClientConfigManager.AddCache(cacheId, config);
            }
            
            if (hotApply && cacheInfo != null && cacheInfo.getCache() != null && IsRunning(cacheId)) {
                CacheConfig cc = CacheConfig.FromConfiguration(config);

                com.alachisoft.tayzgrid.caching.util.HotConfig hotConfig = new com.alachisoft.tayzgrid.caching.util.HotConfig();
                hotConfig.setIsErrorLogsEnabled(cc.getIsErrorLogsEnabled());
                hotConfig.setIsDetailedLogsEnabled(cc.getIsDetailedLogsEnabled());
                hotConfig.setCacheMaxSize(cc.getCacheMaxSize());
                hotConfig.setCleanInterval(cc.getCleanInterval());
                hotConfig.setEvictRatio(cc.getEvictRatio());
                hotConfig.setAlertNotifier(cc.getAlertNotification());

                hotConfig.setBackingSource(cc.getBackingSource());
                ApplyHotConfiguration(cacheId, hotConfig);
            }
            SaveConfiguration();
        } catch (Exception e) {
            throw e;
        } finally {
            _rwLock.ReleaseWriterLock();
        }

        return true;
    }

    @TargetMethodAttribute(privateMethod = MethodName.RegisterCache, privateOverload = 2)
    @Override
    public final boolean RegisterCache(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws
            ConfigurationException, UnknownHostException, ManagementException, java.lang.Exception {

        com.alachisoft.tayzgrid.config.dom.CacheServerConfig oldDom = com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToOldDom(config);

        if (oldDom.getManagementPort() == 0) {
            oldDom.setManagementPort(GetMaxManagementPort());
        }

        if (oldDom.getClientPort() == 0) {
            oldDom.setClientPort(GetMaxSocketPort());
        }

        return RegisterCache(cacheId, oldDom, partId, overwrite, userId, password, hotApply);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetNodeInfo, privateOverload = 1)
    @Override
    public final NodeInfoMap GetNodeInfo() {
        java.util.HashMap nodeInfo = new java.util.HashMap();
        nodeInfo.put(Channel.Cluster, getClusterIP());
        nodeInfo.put(Channel.SocketServer, ClientConfigManager.getBindIP());
        return new NodeInfoMap(nodeInfo);
    }

    @TargetMethodAttribute(privateMethod = MethodName.CanApplyHotConfig, privateOverload = 1)
    @Override
    public String CanApplyHotConfiguration(String cacheId, CacheServerConfig config) {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    try {
                        cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    }
                    catch(Exception exc)
                    {
                        return null;
                    }
                    if (cacheServer != null) {
                        return cacheServer.CanApplyHotConfiguration(cacheId, config);
                    } else {
                        return "Specified cache is not started.";
                    }
                }
            } catch (Exception ex) {
                if (ex != null) {
                    return ex.getMessage();
                }
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        } else {
            return "Specified cache is not registered.";
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.RemoveCacheServerFromClientConfig, privateOverload = 1)
    @Override
    public final void RemoveCacheServerFromClientConfig(String cacheId, String serverName) throws ManagementException, ParserConfigurationException, SAXException, IOException,
            InstantiationException, IllegalAccessException {
        ClientConfigManager.RemoveCacheServer(cacheId, serverName);
    }

    @TargetMethodAttribute(privateMethod = MethodName.RemoveCacheFromClientConfig, privateOverload = 1)
    @Override
    public final void RemoveCacheFromClientConfig(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException,
            IllegalAccessException {
        ClientConfigManager.RemoveCache(cacheId);
    }

    /**
     *
     * @param cacheId
     * @param serversPriorityList
     * @param serverRuntimeContext
     * @throws ManagementException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @TargetMethodAttribute(privateMethod = MethodName.UpdateClientServersList, privateOverload = 1)
    @Override
    public final void UpdateClientServersList(String cacheId, CacheServerList serversPriorityList, String serverRuntimeContext, int port) throws ManagementException, ParserConfigurationException, SAXException,
            IOException, InstantiationException, IllegalAccessException {
        ClientConfigManager.UpdateServerNodes(cacheId, serversPriorityList, serverRuntimeContext.equals("1") ? RtContextValue.JVCACHE : RtContextValue.NCACHE, port);
    }

    /**
     *
     * @param cacheId
     * @param servers
     * @param xml
     * @param loadBalance
     * @throws ManagementException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @TargetMethodAttribute(privateMethod = MethodName.UpdateClientServersList, privateOverload = 2)
    @Override
    public final void UpdateClientServersList(String cacheId, String[] servers, tangible.RefObject<String> xml, boolean loadBalance, int port) throws
            ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        ClientConfigManager.UpdateServerNodes(cacheId, servers, xml, loadBalance,  port);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClientConfiguration, privateOverload = 1)
    public final ClientConfiguration GetClientConfiguration(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException,
            InstantiationException, IllegalAccessException {
        return ClientConfigManager.GetClientConfiguration(cacheId);
    }

    @TargetMethodAttribute(privateMethod = MethodName.UpdateClientConfiguration, privateOverload = 1)
    public final void UpdateClientConfiguration(String cacheId, ClientConfiguration configuration) throws ManagementException, ParserConfigurationException, SAXException,
            IOException, InstantiationException, IllegalAccessException
    {
        //CacheInfo info = GetCacheInfo(cacheId);
        ClientConfigManager.UpdateCacheConfiguration(cacheId, configuration);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetBindIP, privateOverload = 1)
    public final String GetBindIP() {
        return ClientConfigManager.getBindIP();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClientConfigId, privateOverload = 1)
    public final int GetClientConfigId() throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        return ClientConfigManager.GetConfigurationId();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClientNodeStatus, privateOverload = 1)
    public ClientNodeStatusWrapper GetClientNodeStatus(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException,
            InstantiationException, IllegalAccessException {

        ClientNodeStatus status = ClientConfigManager.GetClientNodeStatus(cacheId);

        if (status == ClientNodeStatus.ClientCacheUnavailable) {
            status = ClientNodeStatus.ClientCacheUnavailable;
        } else if (status == ClientNodeStatus.ClientCacheDisabled) {
            status = ClientNodeStatus.ClientCacheUnavailable;
        }
        ClientNodeStatusWrapper nodeStatus = new ClientNodeStatusWrapper();
        nodeStatus.value = status;
        return nodeStatus;
    }

    /**
     * Enable logging for specified subsystem
     *
     * @param subsystem Subsystem for which logging will be enabled
     * @param type Type of logging to enable
     */
    @TargetMethodAttribute(privateMethod = MethodName.EnableLogging, privateOverload = 1)
    public final void EnableLogging(LoggingSubsystem subsystem, LoggingType type) throws Exception {
        try {
            this._renderer.SetLoggingStatus(subsystem, type, LogsStatus.Enable);
            EventLogger.LogEvent("TayzGrid", subsystem.toString() + " logging enabled successfully", EventType.INFORMATION, EventCategories.Information, EventID.LoggingEnabled);
        } catch (Exception exc) {
            EventLogger.LogEvent("TayzGrid", exc.toString(), EventType.ERROR, EventCategories.Error, EventID.GeneralError);
            throw exc;
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.VerifyWindowsUser, privateOverload = 1)
    public final boolean VerifyWindowsUser(String nodeName, String userName, String password) {
        throw new UnsupportedOperationException("Security not supported yet.");

    }

    @TargetMethodAttribute(privateMethod = MethodName.VerfyAdministrator, privateOverload = 1)
    public final boolean VerfyAdministrator(String userName, String password) {
        throw new UnsupportedOperationException("Security not supported yet.");

    }

    /**
     * Disbale logging
     *
     * @param subsystem Subsystem for which logging will be disabled
     * @param type Type of logging to disable
     */
    @TargetMethodAttribute(privateMethod = MethodName.DisableLogging, privateOverload = 1)
    public final void DisableLogging(LoggingInfo.LoggingSubsystem subsystem, LoggingType type) throws Exception {
        try {
            this._renderer.SetLoggingStatus(subsystem, type, LogsStatus.Disable);
            EventLogger.LogEvent("TayzGrid", subsystem.toString() + " logging disabled successfully", EventType.INFORMATION, EventCategories.Information, EventID.LoggingDisabled);
        } catch (Exception exc) {
            EventLogger.LogEvent("TayzGrid", exc.toString(), EventType.ERROR, EventCategories.Error, EventID.GeneralError);
            throw exc;
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.SynchronizeClientConfig, privateOverload = 1)
    public void SynchronizeClientConfig() throws UnknownHostException, ManagementException {
        String bindIP = ServicePropValues.BIND_toClient_IP;
        InetAddress ipAddr = null;

        if (bindIP != null && !bindIP.equals("")) {
            try {
                String[] str = bindIP.split("\\.");
                byte[] bite = new byte[4];
                bite[0] = new Integer(str[0]).byteValue();
                bite[1] = new Integer(str[1]).byteValue();
                bite[2] = new Integer(str[2]).byteValue();
                bite[3] = new Integer(str[3]).byteValue();
                InetAddress.getByAddress(bite);
            } catch (Exception e) {
                bindIP = java.net.InetAddress.getLocalHost().getHostName().toLowerCase();
            }
        } else {
            bindIP = java.net.InetAddress.getLocalHost().getHostName().toLowerCase();
        }
        _clientserverip = bindIP;
        ClientConfigManager.setBindIP(bindIP);
        ClientConfigManager.AvailableNIC(DetectNICs());

        try {
            ClientConfigManager.LoadConfiguration();
        } catch (Exception parserConfigurationException) {
            throw new ManagementException(parserConfigurationException.getMessage());
        }

    }

    /**
     * Update TCP cache settings that includes updated list of TCP members
     *
     * @param cacheId
     * @param props
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    @TargetMethodAttribute(privateMethod = MethodName.ApplyCacheConfiguration, privateOverload = 1)
    public final boolean ApplyCacheConfiguration(String cacheId, CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws ManagementException,
            IllegalArgumentException,
            IllegalAccessException
            {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }
        cacheId = cacheId.toLowerCase();
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            cacheInfo.setCacheProps(props);
            SaveConfiguration();
            return true;
        }

        return false;
    }

    @TargetMethodAttribute(privateMethod = MethodName.ApplyCacheConfiguration, privateOverload = 2)
    @Override
    public final boolean ApplyCacheConfiguration(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws ManagementException,
            IllegalArgumentException,
            IllegalAccessException,
             Exception {
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig oldDom = com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToOldDom(props);
        return ApplyCacheConfiguration(cacheId, oldDom, userId, password, hotApply);

    }

    private boolean RemoveDeployedAssemblies(String cacheId) {
        String path = AppUtil.getInstallDir();
        if (path != null) {
            String deployedAssembliesFolder = new File(AppUtil.DeployedAssemblyDir, cacheId.toLowerCase()).getPath();
            try {
                (new java.io.File(deployedAssembliesFolder)).delete();
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Un-register cache
     *
     * @param cacheId
     * @param partId
     * @param removeServerOnly
     * @param userId
     * @param password
     * @param isGracefulShutdown
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws org.xml.sax.SAXException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.io.IOException
     */
    @TargetMethodAttribute(privateMethod = MethodName.UnregisterCache, privateOverload = 1)
    @Override
    public final void UnregisterCache(String cacheId, String partId, byte[] userId, byte[] password, Boolean isGracefulShutdown, Boolean removeServerOnly) throws ManagementException, ParserConfigurationException, SAXException,
            IOException, InstantiationException, IllegalAccessException, Exception
            {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }
        cacheId = cacheId.toLowerCase();
        _rwLock.AcquireWriterLock();
        try {
            if (s_caches.containsKey(cacheId.toLowerCase())) {
                RemoveClientCacheConfiguration(cacheId.toLowerCase(), removeServerOnly);
                StopCache(cacheId, userId, password, isGracefulShutdown);

                CacheInfo cacheInfo = (CacheInfo) s_caches.get(cacheId.toLowerCase());
                LeasedCache cache = null;

                if (cacheInfo != null) {
                    cache = cacheInfo.getCache();
                }

                if (cache != null) {
                    cache.dispose();
                }

                s_caches.remove(cacheId);

                RemoveDeployedAssemblies(cacheId);

            }
            SaveConfiguration();
        } finally {
            _rwLock.ReleaseWriterLock();
        }
    }

    private void RemoveClientCacheConfiguration(String cacheId, Boolean removeServerOnly) throws ManagementException, ParserConfigurationException, SAXException,
            IOException, InstantiationException, IllegalAccessException, Exception
            {
        if (!removeServerOnly) {
            ClientConfigManager.RemoveCache(cacheId.toLowerCase());
        } else {
            Boolean serverListChanged = false;
            Boolean serverExistsAsClient = false;
            String serverIP = GetBindIP();
            com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration clientConfiguration = ClientConfigManager.GetClientConfiguration(cacheId);
            com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration[] cacheConfigurations = clientConfiguration.getCacheConfigurations();
            for (com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration cc : cacheConfigurations) {
                if (cc.getCacheId() == null ? cacheId == null : cc.getCacheId().equals(cacheId)) {
                    java.util.List<com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> servers = new java.util.ArrayList<com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>();
                    if (servers.size() == 1 && (servers.get(0).getServerName() == null ? serverIP == null : servers.get(0).getServerName().equals(serverIP))) {
                        ClientConfigManager.RemoveCache(cacheId.toLowerCase());
                        break;
                    } else {
                        CacheServerConfig serverConfig = GetCacheConfiguration(cacheId);
                        if (serverConfig != null && serverConfig.getClientNodes() != null && serverConfig.getClientNodes().getNodesList().size() > 0) {
                            for (com.alachisoft.tayzgrid.config.newdom.ClientNode clientNode : serverConfig.getClientNodes().getNodesList()) {
                                if (clientNode.getName().equals(serverIP)) {
                                    serverExistsAsClient = true;
                                    break;
                                }
                            }
                            if (serverExistsAsClient == true) {
                                if (cc.RemoveServer(serverIP)) {
                                    serverListChanged = true;
                                    CacheServerList serversList = new CacheServerList();
                                    serversList.setServersList(UpdateServerPriorityList(cc.getServersPriorityList().getServersList()));
                                    cc.setServersPriorityList(serversList);
                                    clientConfiguration.getCacheConfigurationsMap().put(cacheId, cc);
                                    //CacheInfo info = GetCacheInfo(cacheId);
                                    ClientConfigManager.UpdateCacheConfiguration(cacheId, clientConfiguration);
                                    break;
                                }
                            } else {
                                ClientConfigManager.RemoveCache(cacheId.toLowerCase());
                            }
                        }
                    }
                }
            }
            if (!serverListChanged) {
                ClientConfigManager.RemoveCache(cacheId.toLowerCase());
            }
        }
    }

    private java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> UpdateServerPriorityList(java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> dictionary) {
        java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> newDictionary = new HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>();
        Integer i = 0;
        for (com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer vals : dictionary.values()) {
            newDictionary.put(i++, vals);
        }

        return newDictionary;
    }

    @TargetMethodAttribute(privateMethod = MethodName.ReloadSrvcConfig, privateOverload = 1)
    @Override
    public final void ReloadSrvcConfig() {
        ServiceConfiguration.Load();
        ServicePropValues.initialize();
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 1)
    @Override
    public void StartCache(String cacheId) throws Exception {
        StartCache(cacheId, null, null, null);
    }

    /**
     *
     * @param cacheId
     * @param partitionId
     * @throws Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 2)
    @Override
    public void StartCache(String cacheId, String partitionId) throws Exception {
        StartCache(cacheId, partitionId, null, null);
    }

    /**
     *
     * @param cacheId
     * @param userId
     * @param password
     * @throws Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 3)
    @Override
    public void StartCache(String cacheId, byte[] userId, byte[] password) throws Exception {
        StartCache(cacheId, null, null, null, null, null, null, null, userId, password, false);
    }

    /**
     *
     * @param cacheId
     * @param partitionId
     * @param userId
     * @param password
     * @param twoPhaseInitialization
     * @throws Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.StartCache, privateOverload = 4)
    @Override
    public void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws  Exception {
        StartCache(cacheId, partitionId, null, null, null, null, null, null, userId, password, twoPhaseInitialization);
    }

    @TargetMethodAttribute(privateMethod = MethodName.StartCachePhase2, privateOverload = 1)
    @Override
    public void StartCachePhase2(String cacheId) {
        Cache tempVar = GetCacheInstance(cacheId, null);
        LeasedCache cache = (LeasedCache) ((tempVar instanceof LeasedCache) ? tempVar : null);
        if (cache != null) {
            cache.StartInstancePhase2();
        }

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

    /**
     * Start a cache and provide call backs
     *
     * @param cahcheID
     * @param propertyString
     * @param itemAdded
     * @param itemRemoved
     * @param itemUpdated
     * @param cacheCleared
     * @return
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    @TargetMethodAttribute(privateMethod = "StartCache", privateOverload = 7)
    public void StartCache(String cacheId, String partitionId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws Exception {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            int socketServerPort = cacheInfo.getSocketServerPort();
            if (socketServerPort <= 0) {
                throw new ManagementException("Client port is not provided for cache" + cacheId + " .");
            }
            //int managementPort = cacheInfo.getManagementPort(); Waleed
            int managementPort = PortCalculator.getManagementPort(socketServerPort);
            if (managementPort <= 0) {
                throw new ManagementException("Management port is not provided for cache" + cacheId + " .");
            }
            if (!IsRunning(cacheId)) {
                
                EnvPropValue.loadEnvPropFromTGHOME();
                ArrayList<String> paramsArray = new ArrayList<String>();

                String javaPath = "";
                RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
                if (currentOS == RuntimeUtil.OS.Linux) {
                    javaPath = Common.combinePath(System.getProperty("java.home"), "/bin/java");
                    paramsArray.add(javaPath);
                } else if (currentOS == RuntimeUtil.OS.Windows) {
                    javaPath = Common.combinePath(System.getProperty("java.home"), "\\bin\\java");
                    paramsArray.add('"' + javaPath + '"');
                }

                //paramsArray.add("-Xdebug");
                int Port = EnvPropValue.DEBUG_PORT_OFFSET.isEmpty() ? Integer.parseInt(ServicePropValues.DEBUG_PORT_OFFSET) + managementPort : managementPort + Integer.parseInt(EnvPropValue.DEBUG_PORT_OFFSET) ;
                String vmArg = "-Xrunjdwp:server=y,transport=dt_socket,suspend=n,address=" + Port + "";
                paramsArray.add(vmArg);
                //paramsArray.add("-Xmx" + String.valueOf(Helper.getTotalAvailableMemoryInMBs()) + "m");
                //+ Usman
                // Use this flag to disable the JAVA 7 bytecode verifier, which requires the bytecode to keep track of local variable types and 
                // operand stack in the form of Stack Map Frames. These frames are lost when the bytecode is instrumented through javassist and 
                // causes errors for dynamic serialization. Its an experimental flag. Oracle's gone nuts.
                
                //paramsArray.add("-XX:-UseSplitVerifier");                
                //- Usman
                Port = EnvPropValue.JMX_PORT_OFFSET.isEmpty() ? 1501+ managementPort:Integer.parseInt(EnvPropValue.JMX_PORT_OFFSET)+managementPort;
                paramsArray.add("-Dcom.sun.management.jmxremote.port="+Port+"");
                if(EnvPropValue.param.size() > 0)
                    paramsArray.addAll(EnvPropValue.param);
                
                paramsArray.add("-cp");
                String classPath = getClassPath(cacheId);
                if (currentOS == RuntimeUtil.OS.Windows) {
                    paramsArray.add('"' + classPath + '"');
                } else if (currentOS == RuntimeUtil.OS.Linux) {
                    paramsArray.add(classPath);
                }
               
                
                paramsArray.add("com.alachisoft.tayzgrid.cachehost.CacheSeparateHost");
                paramsArray.add("-i");
                paramsArray.add(cacheId);
                paramsArray.add("-p");
                paramsArray.add(Integer.toString(socketServerPort));
                paramsArray.add("-m");
                paramsArray.add(Integer.toString(managementPort));
                paramsArray.add("-F");
                String servicePath = ServicePropValues.getMapping_Properties();
                if (currentOS == RuntimeUtil.OS.Windows) {
                    paramsArray.add('"' + servicePath + '"');
                } else if (currentOS == RuntimeUtil.OS.Linux) {
                    paramsArray.add(servicePath);
                }
//                String str="";
//                for (int i = 0; i < paramsArray.size(); i++) {
//                    str+=paramsArray.get(i);
//                    str+=" ";
//                }
                ProcessExecutor processExecutor = new ProcessExecutor(paramsArray);
                Process process = processExecutor.execute();
                processExecutor.readOutput(process);
                String processId = getPID(managementPort);
                if (!Common.isNullorEmpty(processId)) {
                    cacheInfo.setCacheProcessId(Integer.parseInt(processId));
                }

            } else {
                throw new ManagementException("Specified cacheId is already running");
            }
        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }

    }

    @TargetMethodAttribute(privateMethod = MethodName.StopCache, privateOverload = 1)
    public void StopCache(String cacheId, byte[] userId, byte[] password, Boolean isGracefulShutdown) throws Exception {
        StopCache(cacheId, null, userId, password, isGracefulShutdown);
    }
    
    @TargetMethodAttribute(privateMethod = MethodName.StopAllCaches, privateOverload = 1)
    public void StopAllCaches() throws Exception
    {
        for (Iterator it = s_caches.values().iterator(); it.hasNext();) {
            CacheInfo cacheInfo = (CacheInfo) it.next();
            if (cacheInfo != null) 
            {
                if(IsRunning(cacheInfo.getCache().getName()))
                    StopCache(cacheInfo.getCache().getName(), null, null, null, false);
            }
        }
        //StopAllCaches(CacheStopReason.ForcedStoped);
    }

    /**
     * Stop a cache
     *
     * @param cacheId
     * @param partitionId
     * @param userId
     * @param password
     * @param isGraceFulShutDown
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    @TargetMethodAttribute(privateMethod = MethodName.StopCache, privateOverload = 2)
    @Override
    public void StopCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean isGraceFulShutDown) throws Exception {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            if (IsRunning(cacheId)) {
                CacheService service = null;
                ICacheServer cacheServer = null;
                try {

                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, cacheInfo.getManagementPort());
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        for (int retries = 0; retries < 3; retries++) {
                            try {
                                cacheServer.StopCache(cacheId, null, userId, password, isGraceFulShutDown);
                                break;
                            } catch (Exception e) {
                                if (retries == 2) {
                                    if (cacheInfo.getCacheProcessId() != 0) {
                                        ProcessExecutor.killProcess(cacheInfo.getCacheProcessId());
                                    } else {
                                        ProcessExecutor.killProcess(Integer.parseInt(getPID(cacheInfo.getManagementPort())));
                                    }
                                }
                            }
                        }
                    } else {
                        throw new ManagementException("Specified cacheId is not started");
                    }
                } catch (Exception ex) {
                    throw new ManagementException(ex);
                }
            }
        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetShutdownTimeout, privateOverload = 1)
    public int GetShutdownTimeout() {
        int shutdownTimeout = 180;
        int blockTimeout = 3;

        tangible.RefObject<Integer> tempRef_shutdownTimeout = new tangible.RefObject<Integer>(shutdownTimeout);
        tangible.RefObject<Integer> tempRef_blockTimeout = new tangible.RefObject<Integer>(blockTimeout);
        String expMsg = GracefulTimeout.GetGracefulShutDownTimeout(tempRef_shutdownTimeout, tempRef_blockTimeout);
        shutdownTimeout = tempRef_shutdownTimeout.argvalue;
        blockTimeout = tempRef_blockTimeout.argvalue;
        if (expMsg != null) {
            EventLogger.LogEvent("TayzGrid", expMsg, EventType.WARNING, EventCategories.Warning, EventID.GeneralInformation);
        }

        return shutdownTimeout;
    }

    public void KillAllCaches() {
        //get all cache info
        for (Iterator it = s_caches.values().iterator(); it.hasNext();) {
            CacheInfo cacheInfo = (CacheInfo) it.next();
            if (cacheInfo != null) {
                if (cacheInfo.getCacheProcessId() == 0) {
                    int pid = Integer.parseInt(getPID(cacheInfo.getManagementPort()));
                    cacheInfo.setCacheProcessId(pid);
                }
                ProcessExecutor.killProcess(cacheInfo.getCacheProcessId());
            }
        }

    }

    private void StopAllCaches(CacheStopReason reason) throws Exception {
        ArrayList caches = new ArrayList();
        ArrayList cacheInfos = new ArrayList();

        for (Iterator it = s_caches.values().iterator(); it.hasNext();) {
            CacheInfo cacheInfo = (CacheInfo) it.next();
            if (cacheInfo != null) {
                caches.add(cacheInfo.getCache());
                cacheInfos.add(cacheInfo);
            }
        }
        StopCacheInstance(caches, cacheInfos, reason, null, null, false);
    }

    private void StopCacheInstance(java.util.ArrayList caches, java.util.ArrayList cacheInfos, CacheStopReason reason, byte[] userId, byte[] password, Boolean isGracefulShutdown) throws Exception //#else
    {
        if (caches != null && caches.size() > 0) {
            for (int i = 0; i < caches.size(); i++) {
                LeasedCache cache = (LeasedCache) ((caches.get(i) instanceof LeasedCache) ? caches.get(i) : null);
                CacheInfo cacheInfo = (CacheInfo) ((cacheInfos.get(i) instanceof CacheInfo) ? cacheInfos.get(i) : null);

                if (IsRunning(cache.getName())) {
                    if (!cache.VerifyNodeShutdownInProgress(isGracefulShutdown)) {
                        throw new ManagementException("Graceful shutdown is already in progress...");
                    }

                    if (reason == CacheStopReason.Expired) {
                        EventLogger.LogEvent("TayzGrid","TayzGrid license has expired on this machine. Stopping cache...", EventType.ERROR,EventCategories.Error,EventID.CacheStart);
                    }
                    cache.StopInstance(isGracefulShutdown);

                    cacheInfo.SyncConfiguration();

                    //instrumentation Code
                   
                        if (com.alachisoft.tayzgrid.caching.Cache.OnCacheStopped != null) {
                            com.alachisoft.tayzgrid.caching.Cache.OnCacheStopped.invoke(cache.getName());
                        }
                    
                }
            }
        } else {
            throw new ManagementException("Specified cacheId is not registered");
        }

    }

    /**
     * Load all the config sections from the configuration file.
     */
    private static void LoadConfiguration() throws ConfigurationException, ManagementException {
        CacheInfo cacheInfo = null;
        try {
            CacheServerConfig[] configs = CacheConfigManager.GetConfiguredCaches();
            for (CacheServerConfig config : configs) {
                /**
                 * Until we completely move to using dom based configuration we
                 * have to convert it to string props
                 */
                String props = GetProps(config);
                String cacheId = config.getName().toLowerCase();

                if (!s_caches.containsKey(cacheId)) {
                    cacheInfo = new CacheInfo();
                    cacheInfo.setCache(new LeasedCache(props));
                    cacheInfo.setCacheProps(config);
                    cacheInfo.setPorts(getPorts(config));
                    s_caches.put(cacheId, cacheInfo);
                } else {
                    cacheInfo = (CacheInfo) ((s_caches.get(cacheId) instanceof CacheInfo) ? s_caches.get(cacheId) : null);
                    if (cacheInfo != null) {
                        cacheInfo.setCacheProps(config);
                        cacheInfo.setPorts(getPorts(config));
                    }
                }
            }
        } catch (ManagementException e) {
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Get string props representation of config This is until we change the use
     * of properties in Cache from props string or HashMap to Dom
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
     * Save caches to configuration
     */
    private static void SaveConfiguration() throws ManagementException, IllegalArgumentException, IllegalAccessException {
        try {
            CacheConfigManager.SaveConfiguration(s_caches, null);
        } catch (Exception e) {
            String msg = String.format("CacheServer failed to save configuration information, Error %1$s", e.getMessage() + Arrays.toString(e.getStackTrace()));
            EventLogger.LogEvent("TayzGrid",msg, EventType.WARNING,EventCategories.Warning,EventID.ConfigurationError);
        }
    }

    /**
     * Detect and return all the available NICs on this machine
     *
     * @return
     */
    @TargetMethodAttribute(privateMethod = MethodName.DetectNICs, privateOverload = 1)
    @Override
    public final java.util.HashMap DetectNICs() {
        java.util.HashMap connectedNICs = new java.util.HashMap();

        try {
            // Detecting Network Interface Cards with enabled IPs through WMI:

            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface net = enumeration.nextElement();
                List<InterfaceAddress> list = net.getInterfaceAddresses();
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    InterfaceAddress interf = (InterfaceAddress) it.next();
                    if (interf.getAddress() instanceof Inet6Address) {
                        continue;
                    }
                    connectedNICs.put(interf.getAddress().getHostAddress(), net.getDisplayName());
                }
            }

            return connectedNICs;
        } catch (Exception e) {
        }

        return connectedNICs;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GarbageCollect, privateOverload = 1)
    @Override
    public final void GarbageCollect() {
        Runtime.getRuntime().gc();
    }

    /**
     *
     * @param bindedIpMap
     * @throws Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.BindToIP, privateOverload = 1)
    @Override
    public final void BindToIP(BindedIpMap bindedIpMap) throws java.lang.Exception {
        HashMap bindTable = bindedIpMap.getMap();
        if (bindTable.isEmpty()) {

            return;
        }
        String path = getServiceFilePath();
        if ((new java.io.File(path)).isFile()) {

            String[] elementKeys = {
                "CacheServer.BindToClusterIP",
                "CacheServer.BindToClientServerIP"
            };
            String[] elementValues = {
                bindTable.containsKey(Channel.Cluster) ? (String) bindTable.get(Channel.Cluster) : "",
                bindTable.containsKey(Channel.SocketServer) ? (String) bindTable.get(Channel.SocketServer) : ""
            };
            Properties loadProp = new Properties();
            loadProp.load(new FileInputStream(path));

            Properties alterProps = loadProp;

            String key = "";
            for (int i = 0; i < elementKeys.length; i++) {
                Enumeration enumProps = loadProp.propertyNames();
                while (enumProps.hasMoreElements()) {
                    key = (String) enumProps.nextElement();
                    if (key.equals(elementKeys[i])) {
                        alterProps.setProperty(key, elementValues[i]);
                    }
                }

            }
            File file = new File(path);
            FileOutputStream fileOut = new FileOutputStream(file);
            alterProps.store(fileOut, "TayzGrid Service Properties");
            fileOut.close();

        }

    }

    /**
     *
     *
     * @return @throws Exception
     */
    @Override
    @TargetMethodAttribute(privateMethod = MethodName.BindedIp, privateOverload = 1)
    public final BindedIpMap BindedIp() throws Exception {
        HashMap bindedIps = new java.util.HashMap(2);
        bindedIps.put(Channel.Cluster, ServicePropValues.BIND_ToCLUSTER_IP);
        bindedIps.put(Channel.SocketServer, ServicePropValues.BIND_toClient_IP);
        return new BindedIpMap(bindedIps);
    }

    /**
     * Return service config file path
     */
    private String getServiceFilePath() throws Exception {

        String path = ServicePropValues.INSTALLDIR_DIR + "/config/server.properties";
        if ((new java.io.File(path)).isFile()) {
            return path;
        }

        //else, get the path of config file from Windows Registry:
        path = AppUtil.getInstallDir();
        if (path == null || path.equals("")) {
            throw new Exception("Missing installation folder information");
        }
        return (path + "/bin/service/server.properties");
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetMaxSocketPort, privateOverload = 1)
    @Override
    public final int GetMaxSocketPort() throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg = null;
        int maxPort = 9610, loop = 1;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
            } else if (current.getValue() instanceof java.util.HashMap) {
                Iterator ide = ((java.util.HashMap) current.getValue()).entrySet().iterator();
                while (ide.hasNext()) {
                    cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                    break;
                }
            }
            if (cfg != null) {
                
                if (loop == 1) {
                    if(cfg.getCacheType().equals("local-cache"))
                        maxPort = cfg.getClientPort() + PortCalculator.PORT_JUMP * 1;
                    else
                        maxPort = cfg.getClientPort() + PortCalculator.PORT_JUMP * cfg.getClusterPortRange();
                    loop++;
                } else if (maxPort < cfg.getClientPort() + PortCalculator.PORT_JUMP * cfg.getClusterPortRange()) {
                    if(cfg.getCacheType().equals("local-cache"))
                        maxPort = cfg.getClientPort() + PortCalculator.PORT_JUMP * 1;
                    else
                        maxPort = cfg.getClientPort() + PortCalculator.PORT_JUMP * cfg.getClusterPortRange();
                }
            }

        }

        return maxPort;
    }

    /**
     *
     * @return @throws java.net.UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetMaxManagementPort, privateOverload = 1)
    @Override
    public final int GetMaxManagementPort() throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg = null;
        int maxPort = 8280, loop = 1;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
            } else if (current.getValue() instanceof java.util.HashMap) {
                Iterator ide = ((java.util.HashMap) current.getValue()).entrySet().iterator();
                while (ide.hasNext()) {
                    cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                    break;
                }
            }

            if (cfg != null) {
                if (loop == 1) {
                    maxPort = cfg.getManagementPort() + 1;
                    loop++;
                } else if (maxPort < cfg.getManagementPort() + 1) {
                    maxPort = cfg.getManagementPort() + 1;
                } else if (maxPort < cfg.getManagementPort() + 1) {
                    maxPort = cfg.getManagementPort() + 1;
                }
            }
        }

        return maxPort;
    }

    /**
     * Gets the Max port number, among all the ports of registered caches on
     * this machine
     *
     * @return Max cluster port
     * @throws java.net.UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetMaxPort, privateOverload = 1)
    @Override
    public final int GetMaxPort() throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg = null;
        int maxPort = 0, loop = 1;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
            } else if (current.getValue() instanceof java.util.HashMap) {
                Iterator ide = ((java.util.HashMap) current.getValue()).entrySet().iterator();
                while (ide.hasNext()) {
                    cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                    break;
                }
            }

            if (cfg != null && cfg.getClusterPort() != 0) {
                if (loop == 1) {
                    maxPort = cfg.getClusterPort() + cfg.getClusterPortRange() - 1;
                    loop++;
                } else if (maxPort < (cfg.getClusterPort() + cfg.getClusterPortRange())) {
                    maxPort = cfg.getClusterPort() + cfg.getClusterPortRange() - 1;
                }
            }
        }

        return maxPort;
    }

    /**
     * Checks if the current cache is a Cluster cache or not, used in NCache
     * UnReg cache tool as now UnReg is only applicable to cluster caches only
     *
     * @param cacheId
     * @return true if Cluster Cache
     */
    @TargetMethodAttribute(privateMethod = MethodName.IsClusteredCache, privateOverload = 1)
    @Override
    public final CacheStatusOnServerContainer IsClusteredCache(String cacheId) {
        CacheStatusOnServer result = CacheStatusOnServer.Unregistered;
        CacheStatusOnServerContainer status = new CacheStatusOnServerContainer();

        if (!Common.isNullorEmpty(cacheId)) {
            CacheInfo cacheInfo = GetCacheInfo(cacheId);
            if (cacheInfo != null) {
                if (cacheInfo.getCache().getStatistics().getClassName().equals("replicated-server")
                        || cacheInfo.getCache().getStatistics().getClassName().equals("partitioned-server")) {
                    result = CacheStatusOnServer.ClusteredCache;
                } else {
                    result = CacheStatusOnServer.LocalCache;
                }
            }
            status.cacheStatus = result;
            return status;
        }
        throw new IllegalArgumentException("cacheId");
    }

    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Cluster port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.PortIsAvailable, privateOverload = 1)
    @Override
    public final boolean PortIsAvailable(int port) throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg;
        boolean isAvailable = true;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                if (cfg.getClusterPort() != 0) {
                    for (int i = 0; i < cfg.getClusterPortRange(); i++) {
                        if (port == cfg.getClusterPort() + i) {
                            isAvailable = false;
                        }
                    }
                }
            } else if (current.getValue() instanceof java.util.HashMap) {
                java.util.HashMap partitionedTable = (java.util.HashMap) ((current.getValue() instanceof java.util.HashMap) ? current.getValue() : null);
                if (partitionedTable != null) {
                    Iterator ide = partitionedTable.entrySet().iterator();
                    while (ide.hasNext()) {
                        cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                        if (cfg.getClusterPort() != 0) {
                            for (int i = 0; i < cfg.getClusterPortRange(); i++) {
                                if (port == cfg.getClusterPort() + i) {
                                    isAvailable = false;
                                }
                            }
                        }
                        break;
                    }
                }
            }

        }
        return isAvailable;
    }

    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Management port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.IsManagementPortAvailable, privateOverload = 1)
    @Override
    public final boolean IsManagementPortAvailable(int port) throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg;
        boolean isAvailable = true;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                if (cfg.getManagementPort() != 0) 
                {
                    if (port == cfg.getManagementPort()) 
                    {
                        isAvailable = false;
                    }
                }
            } else if (current.getValue() instanceof java.util.HashMap) {
                java.util.HashMap partitionedTable = (java.util.HashMap) ((current.getValue() instanceof java.util.HashMap) ? current.getValue() : null);
                if (partitionedTable != null) {
                    Iterator ide = partitionedTable.entrySet().iterator();
                    while (ide.hasNext()) {
                        cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                        if (cfg.getManagementPort() != 0) 
                        {
                            if (port == cfg.getManagementPort()) {
                                    isAvailable = false;
                            }
                        }
                        break;
                    }
                }
            }

        }
        return isAvailable;
    }

    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Client port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.IsClientPortAvailable, privateOverload = 1)
    @Override
    public final boolean IsClientPortAvailable(int port) throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();
        java.util.ArrayList list = new ArrayList();
        
            list.add(port);
            list.add(port+1);
            list.add(port+2);
            list.add(port+3);
            list.add(port+4);
            list.add(port+5);
            list.add(port+6);
        
        CacheConfig cfg;
        boolean isAvailable = true;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                if (cfg.getClientPort() != 0) 
                {    
                    for(int i =0; i< 7 ;i ++){
                        if (list.contains(cfg.getClientPort()+i))
                        {
                            isAvailable = false;
                        }
                    }
                }
            } else if (current.getValue() instanceof java.util.HashMap) {
                java.util.HashMap partitionedTable = (java.util.HashMap) ((current.getValue() instanceof java.util.HashMap) ? current.getValue() : null);
                if (partitionedTable != null) {
                    Iterator ide = partitionedTable.entrySet().iterator();
                    while (ide.hasNext()) {
                        cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                        if (cfg.getClientPort() != 0) 
                        {
                            for(int i =0; i< 7 ;i ++){
                                if (list.contains(cfg.getClientPort()+i))
                                {
                                    isAvailable = false;
                                }
                            }
                        }
                        break;
                    }
                }
            }

        }
        return isAvailable;
    }

    /**
     * Checks whether the newly added node arise port conflict or not
     *
     * @param port Cluster port
     * @param id
     * @return 'true' if the node is allowed, otherwise 'flase'
     * @throws java.net.UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.NodeIsAllowed, privateOverload = 1)
    @Override
    public final boolean NodeIsAllowed(int port, String id) throws UnknownHostException {
        java.util.Map regCaches = getCacheProps();
        Iterator ie = regCaches.entrySet().iterator();

        CacheConfig cfg = null;
        boolean isAllowed = true;

        while (ie.hasNext()) {
            Map.Entry current = (Map.Entry) ie.next();
            if (current.getValue() instanceof CacheServerConfig) {
                cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
            } else if (current.getValue() instanceof java.util.HashMap) {
                for (Iterator it = ((java.util.HashMap) current.getValue()).entrySet().iterator(); it.hasNext();) {
                    Map.Entry de = (Map.Entry) it.next();
                    cfg = CacheConfig.FromConfiguration((CacheServerConfig) ((current.getValue() instanceof CacheServerConfig) ? current.getValue() : null));
                    break;
                }
            }
            if (cfg != null) {
                if (cfg.getClusterPort() == port && !cfg.getCacheId().equals(id)) {
                    isAllowed = false;
                }
            }
        }

        return isAllowed;
    }

    public final String Decrypt(byte[] cypherText) {

        return EncryptionUtil.Decrypt(cypherText);

    }

    /**
     * Gets the status of NCache on this node.
     *
     * @param cacheId
     * @param partitionId
     * @return The ServerStatus.
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheStatus, privateOverload = 1)
    @Override
    public StatusInfo GetCacheStatus(String cacheId, String partitionId) {
        StatusInfo status = new StatusInfo();
        if(!Common.isNullorEmpty(cacheId))
        {
            CacheInfo cacheInfo = GetCacheInfo(cacheId);
            CacheService service = null;
            ICacheServer cacheServer = null;
            if (cacheInfo != null) {
                if (cacheInfo.getCacheProps() != null) {
                    status.setConfigID(cacheInfo.getCacheProps().getConfigID());
                }
                try {
                    int managementPort = cacheInfo.getManagementPort();
                    if (managementPort > 0) {
                        service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                        cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                        if (cacheServer != null) {
                            return cacheServer.GetCacheStatus(cacheId, partitionId);
                        }
                    }
                } catch (Exception ex) {
                    //System.out.println(ex.getMessage());
                }
            }
            if (cacheServer == null) {
                status.Status = CacheStatus.Registered;
            }
        }
        else 
            status.Status = CacheStatus.Unavailable;
        return status;
    }

    /**
     * Starts monitoring the client activity.
     *
     * @throws java.lang.Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.StartMonitoringActivity, privateOverload = 1)
    @Override
    public final void StartMonitoringActivity() throws java.lang.Exception {
        try {
            ServerMonitor.StartMonitoring();
            EventLogger.LogEvent("TayzGrid","Activity monitoring is started", EventType.INFORMATION,EventCategories.Information,EventID.GeneralInformation);

        } catch (Exception e) {
            EventLogger.LogEvent("TayzGrid","An error occured while starting activity monitoring " + e.toString(), EventType.ERROR,EventCategories.Information,EventID.GeneralInformation);
            throw e;
        }
    }

    /**
     * Stops monitoring client activity.
     *
     * @throws java.lang.Exception
     */
    @TargetMethodAttribute(privateMethod = MethodName.StopMonitoringActivity, privateOverload = 1)
    @Override
    public final void StopMonitoringActivity() throws java.lang.Exception {
        try {
            ServerMonitor.StopMonitoring();
            EventLogger.LogEvent("Activity monitoring is stopped", EventType.INFORMATION);
        } catch (Exception e) {

            EventLogger.LogEvent("An error occured while stopping activity monitoring " + e.toString(), EventType.ERROR);
            throw e;
        }
    }

    /**
     * Publishes the observed client activity into a file.
     *
     * @throws java.lang.CloneNotSupportedException
     */
    @TargetMethodAttribute(privateMethod = MethodName.PublishActivity, privateOverload = 1)
    @Override
    public final void PublishActivity() throws CloneNotSupportedException, Exception {
    }

    /**
     *
     * @param cacheId
     * @return
     * @throws CacheException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheStatistics, privateOverload = 1)
    @Override
    public final CacheNodeStatistics[] GetCacheStatistics(String cacheId) throws CacheException {
        Cache cache = GetCacheInstance(cacheId, null);
        java.util.ArrayList<CacheNodeStatistics> statistics = null;
        if (cache != null) {
            statistics = cache.GetCacheNodeStatistics();
            return statistics.toArray(new CacheNodeStatistics[statistics.size()]);
        }
        return null;
    }

    /**
     *
     * @param cacheId
     * @return
     * @throws ArgumentNullException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheStatistics2, privateOverload = 1)
    @Override
    public CacheStatistics GetCacheStatistics2(String cacheId) throws ArgumentNullException {

        if (cacheId == null) {
            throw new ArgumentNullException(cacheId);
        }
//        CacheInfo cacheInfo = GetCacheInfo(cacheId);
//        if (cacheInfo != null) {
//            return cacheInfo.getCache().getStatistics();
//        }
        CacheService service = null;
        ICacheServer cacheServer = null;
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        return cacheServer.GetStatistics(cacheId);
                    }
                }
            } catch (Exception ex) {
                //System.out.println(ex.getMessage());
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
    public Node[] GetCacheServers(String cacheId) throws UnknownHostException {
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

    /**
     * Gets the list of servers which are up and are part of a clustered cache.
     *
     * @param cacheId
     * @return
     */
    @TargetMethodAttribute(privateMethod = "GetRunningCacheServers", privateOverload = 1)
    public final java.util.ArrayList<ServerNode> GetRunningCacheServers(String cacheId) {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        java.util.ArrayList<ServerNode> serverNodes = new java.util.ArrayList<ServerNode>();
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            if (cacheInfo.getCache().getIsRunning() && cacheInfo.getCacheProps().getCacheType().equals("clustered-cache")) {
                com.alachisoft.tayzgrid.caching.statistics.CacheStatistics tempVar = cacheInfo.getCache().getStatistics();
                ClusterCacheStatistics stats = (ClusterCacheStatistics) ((tempVar instanceof ClusterCacheStatistics) ? tempVar : null);

                if (stats != null) {
                    for (Iterator it = stats.getNodes().iterator(); it.hasNext();) {
                        NodeInfo node = (NodeInfo) it.next();
                        ServerNode serverNode = new ServerNode();
                        serverNode.setAddress(node.getAddress());
                        serverNode.setIsReplica(node.getIsStartedAsMirror());
                        serverNode.setInProcInstance(node.getIsInproc());
                        if (node.getRendererAddress() != null) {
                            serverNode.setClientPort(node.getRendererAddress().getPort());
                        }
                        if (node.getIsStartedAsMirror() && stats.getNodes().size() > 2) {
                            for (Iterator its = stats.getNodes().iterator(); it.hasNext();) {
                                NodeInfo node2 = (NodeInfo) its.next();
                                if (node2.getSubgroupName().equals(node.getSubgroupName())
                                        && !node2.getAddress().getIpAddress().toString().equals(node.getAddress().getIpAddress().toString())) {
                                    serverNode.setNodeAt(node2.getAddress().getIpAddress().toString());
                                    break;
                                }
                            }
                        } else {
                            serverNode.setNodeAt(node.getAddress().getIpAddress().toString());
                        }
                        serverNodes.add(serverNode);
                    }
                }

            }
        }
        return serverNodes;
    }

    @TargetMethodAttribute(privateMethod = "GetCacheClients", privateOverload = 1)
    public final java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientNode> GetCacheClients(String cacheId) throws UnknownHostException {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientNode> clients = _renderer.GetClientList(cacheId);
        return clients;
    }

    /**
     *
     * @param cacheId
     * @return
     * @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = "GetClientProcessStats", privateOverload = 1)
    @Override
    public final java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> GetClientProcessStats(String cacheId) throws UnknownHostException {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }

        java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> clients = _renderer.GetClientProcessStats(cacheId);
        return clients;
    }

    /**
     * Gets the list of all configured caches on this server.
     *
     * @return
     */
    @TargetMethodAttribute(privateMethod = "GetAllConfiguredCaches", privateOverload = 1)
    @Override
    public  ConfiguredCacheInfo[] GetAllConfiguredCaches() {
        ConfiguredCacheInfo[] configuredCaches = new ConfiguredCacheInfo[s_caches.size()];

        try {
            _rwLock.AcquireReaderLock();
            Iterator ide = s_caches.entrySet().iterator();
            int tmp = 0;
            while (ide.hasNext()) {
                Map.Entry current = (Map.Entry) ide.next();
                CacheInfo cacheInfo = (CacheInfo) ((current.getValue() instanceof CacheInfo) ? current.getValue() : null);
                if (cacheInfo != null) {
                    ConfiguredCacheInfo configuredCache = new ConfiguredCacheInfo();
                    configuredCache.setCacheId(cacheInfo.getCacheProps().getName());
                    if (!cacheInfo.getCacheProps().getInProc()) {
                        configuredCache.setIsRunning(IsRunning(cacheInfo.getCache().getName()));
                    } else {
                        
                        //String pid = getPID(cacheInfo.getManagementPort());
                        configuredCache.setIsRunning(PortPool.getInstance().isPortFound(cacheInfo.getCache().getName()));//+"-"+pid));
                    }
                    if(configuredCache.getIsRunning())
                    {
                        configuredCache.setPID(getPID(cacheInfo.getManagementPort()));
                    }
                    configuredCache.setDataCapacity(cacheInfo.getCacheProps().getStorage().getSize());
                    configuredCache.setCachePropString(_clusterIp);
                    if (cacheInfo.getCacheProps().getCacheType().equals("clustered-cache")) {
                        if (cacheInfo.getCacheProps().getCluster() != null) {
                            String tempVar = cacheInfo.getCacheProps().getCluster().getTopology();
                            if (tempVar.equals("replicated-server")) {
                                configuredCache.setTopology(CacheTopology.Replicated);
                            } else if (tempVar.equals("partitioned-server")) {
                                configuredCache.setTopology(CacheTopology.Partitioned);
                            } 
                        }
                    } else if (cacheInfo.getCacheProps().getCacheType().equals("local-cache")) {
                        configuredCache.setTopology(CacheTopology.Local);
                    }

                    configuredCaches[tmp++] = configuredCache;
                }
            }
        } finally {
            _rwLock.ReleaseReaderLock();
        }
        return configuredCaches;
    }

    /**
     * Gets the basic cache related information for given cache id.
     *
     * @param cacheId Name of the cache.
     * @return
     */
    @TargetMethodAttribute(privateMethod = "GetCacheConfigurationInfo", privateOverload = 1)
    public final ConfiguredCacheInfo GetCacheConfigurationInfo(String cacheId) {
        if (cacheId == null) {
            throw new IllegalArgumentException("cacheId");
        }
        ConfiguredCacheInfo configuredCache = null;
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            configuredCache = new ConfiguredCacheInfo();
            configuredCache.setCacheId(cacheInfo.getCacheProps().getName());
            configuredCache.setIsRunning(cacheInfo.getCache().getIsRunning());
            configuredCache.setDataCapacity(cacheInfo.getCacheProps().getStorage().getSize());

            if (cacheInfo.getCacheProps().getCacheType().equals("clustered-cache")) {
                if (cacheInfo.getCacheProps().getCluster() != null) {
                    String tempVar = cacheInfo.getCacheProps().getCluster().getTopology();
                    if (tempVar.equals("replicated-server")) {
                        configuredCache.setTopology(CacheTopology.Replicated);
                    } else if (tempVar.equals("partitioned-server")) {
                        configuredCache.setTopology(CacheTopology.Partitioned);
                    }
                }
            } else if (cacheInfo.getCacheProps().getCacheType().equals("local-cache")) {
                configuredCache.setTopology(CacheTopology.Local);
            }
        }
        return configuredCache;
    }

    /**
     *
     * @return
     */
    @TargetMethodAttribute(privateMethod = "GetClusterIP", privateOverload = 1)
    @Override
    public final String GetClusterIP() {
        return getClusterIP();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetLocalCacheIP, privateOverload = 1)
    @Override
    public final String GetLocalCacheIP() {
        return getLocalCacheIP();
    }

    @TargetMethodAttribute(privateMethod = MethodName.CacheProps, privateOverload = 1)
    @Override
    public java.util.Map CacheProps() {
        throw new UnsupportedOperationException();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetSocketServerPort, privateOverload = 1)
    @Override
    public final int GetSocketServerPort() {
        return _socketServerPort;
    }

    @TargetMethodAttribute(privateMethod = MethodName.IsBridgeTargetCache, privateOverload = 1)
    @Override
    public final boolean IsBridgeTargetCache(String cacheId) {
      
        return false;
    }

    @TargetMethodAttribute(privateMethod = MethodName.ClearCacheContent, privateOverload = 1)
    @Override
    public void ClearCacheContent(String cacheId) throws OperationFailedException {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        cacheServer.ClearCacheContent(cacheId);
                    }
                }
            } catch (Exception ex) {
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.IsRunning, privateOverload = 1)
    @Override
    public boolean IsRunning(String cacheId) {
        boolean isCacheRunning = false;
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        isCacheRunning = cacheServer.IsRunning(cacheId);
                    }
                }
            } catch (Exception ex) {
                isCacheRunning = false;
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        }
        return isCacheRunning;

    }

    @TargetMethodAttribute(privateMethod = MethodName.GetStatistics, privateOverload = 1)
    @Override
    public CacheStatistics GetStatistics(String cacheId) {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        return cacheServer.GetStatistics(cacheId);
                    }
                }
            } catch (Exception ex) {
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheCount, privateOverload = 1)
    @Override
    public final long GetCacheCount(String cacheId) throws GeneralFailureException, OperationFailedException, CacheException {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        return cacheServer.GetCacheCount(cacheId);
                    }
                }
            } catch (Exception ex) {
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        }
        return 0;
    }

    @TargetMethodAttribute(privateMethod = MethodName.BalanceDataloadOnCache, privateOverload = 1)
    @Override
    public void BalanceDataloadOnCache(String cacheId) throws SuspectedException, TimeoutException, GeneralFailureException {
        CacheInfo cacheInfo = GetCacheInfo(cacheId);
        if (cacheInfo != null) {
            CacheService service = null;
            ICacheServer cacheServer = null;
            try {
                int managementPort = cacheInfo.getManagementPort();
                if (managementPort > 0) {
                    service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, managementPort);
                    cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        cacheServer.BalanceDataloadOnCache(cacheId);
                    }
                }
            } catch (Exception ex) {
            } finally {
                if (service != null) {
                    service.dispose();
                }
            }
        }
    }

    @TargetMethodAttribute(privateMethod = MethodName.IsCacheRegistered, privateOverload = 1)
    @Override
    public boolean IsCacheRegistered(String cacheId) {
        Cache cache = getItem(cacheId);
        if (cache != null) {
            return true;
        }
        return false;
    }

    @TargetMethodAttribute(privateMethod = MethodName.SetLocalCacheIP, privateOverload = 1)
    @Override
    public final void SetLocalCacheIP(String ip) {
        setLocalCacheIP(ip);
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

    @TargetMethodAttribute(privateMethod = MethodName.SetSnmpPort, privateOverload = 1)
    @Override
    public void setSnmpPort(String cacheid, Map<String, ArrayList<Integer>> snmpPort) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {

        CacheInfo info = GetCacheInfo(cacheid);
        //String pid = getPID(info.getManagementPort());
        for (Object entryObject : snmpPort.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            ArrayList<Integer> portList = (ArrayList<Integer>) entry.getValue();
            for (int i = 0; i < portList.size(); i++) {
                PortPool.getInstance().assignPort((String) (entry.getKey()/*+"-"+pid*/), (Integer) portList.get(i));
            }
        }
    }

 

    @TargetMethodAttribute(privateMethod = MethodName.StopServer, privateOverload = 1)
    @Override
    public void StopServer() {
        ReleaseServiceObject();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetServerPlatform, privateOverload = 1)
    @Override
    public String GetServerPlatform() {
        return ServerPlatform.isJava;
    }

    private String getPID(int port) {
        CacheService service = null;
        ICacheServer cacheServer = null;
        try {
            if (port > 0) {
                service = new CacheRPCService(ServicePropValues.BIND_ToCLUSTER_IP, port);
                cacheServer = service.GetCacheServer(new TimeSpan(0, 0, 30));
                if (cacheServer != null) {
                    return cacheServer.getPID();
                }
            }
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    public static void SetWaitOnServiceObject() {
        try {
            synchronized (serviceObject) {
                serviceObject.wait();
            }
        } catch (Exception ex) {
        }
    }

    private static void ReleaseServiceObject() {
        try {
            synchronized (serviceObject) {
                serviceObject.notifyAll();
            }
        } catch (Exception ex) {
        }
    }

    public static HashMap GetBinding(String cacheId) {
        HashMap binding = new HashMap();
        binding.put("ip", ServicePropValues.BIND_ToCLUSTER_IP);
        CacheServerConfig[] configCaches = null;
        try {
            configCaches = CacheConfigManager.GetConfiguredCaches();
            if (configCaches != null && configCaches.length > 0 && !Common.isNullorEmpty(cacheId)) {
                for (CacheServerConfig cacheServerConfig : configCaches) {
                    if (cacheServerConfig.getName() != null && !cacheServerConfig.getName().isEmpty() && cacheId.toLowerCase().equals(cacheServerConfig.getName().toLowerCase())) {
                        binding.put("port", PortCalculator.getManagementPort(cacheServerConfig.getClientPort()));
                        break;
                    }
                }
            }
        } catch (Exception ex) {
        }
        return binding;
    }

    private static int[] getPorts(CacheServerConfig cacheServerConfig) throws ManagementException {
        int[] portarray = new int[2];
        if (cacheServerConfig != null) {
            portarray[0] = cacheServerConfig.getClientPort();
            //portarray[1] = cacheServerConfig.getManagementPort(); Waleed
            portarray[1] = PortCalculator.getManagementPort(portarray[0]);
                        
        }
        return portarray;
    }

    /**
     * Finalizer for this object.
     */
    protected void finalize() throws Throwable {

       
        dispose(false);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        dispose(true);
    }

    /**
     * Add garbage collection task to time sheduler
     */
    private void StartGCTask() {
        boolean enabled = true;
        tangible.RefObject<Boolean> tempRef_enabled = new tangible.RefObject<Boolean>(enabled);
        tempRef_enabled.argvalue = Boolean.getBoolean(ServicePropValues.CacheServer_EnableForcedGC);
        enabled = tempRef_enabled.argvalue;

        /**
         * Only if CLR is loaded with Server GC, and user asked for forced GC
         */
        if (enabled) {
            int threshold = 0;
            tangible.RefObject<Integer> tempRef_threshold = new tangible.RefObject<Integer>(threshold);
            boolean tempVar = Boolean.getBoolean(ServicePropValues.CacheServer_ForcedGCThreshold);
            threshold = tempRef_threshold.argvalue;
            if (tempVar) {
                this._gcScheduler.AddTask(new TimeScheduler.Task() {
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

    private void StartPoortPoolValidationTask()
    {
//        if(_portPoolScheduler == null){
//            _portPoolScheduler = new TimeScheduler();
//            PortPoolValidationTask portPoolTask = new PortPoolValidationTask();
//            _portPoolScheduler.AddTask(portPoolTask);
//            _portPoolScheduler.Start();
//        }
        
    }
    
    private static String getClassPath(String cacheId) {
        RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
        StringBuilder sb = new StringBuilder();
        File[] pathArr = new File[4];

        char seprator = '.';

        if (currentOS == RuntimeUtil.OS.Linux) {
            seprator = ':';
            pathArr[0] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"libexec"));
            pathArr[1] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"lib",File.separator,"*"));
            pathArr[2] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"lib",File.separator,"resources",File.separator,"*"));
            if (cacheId != null && !cacheId.isEmpty()) {
                pathArr[3] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"deploy",File.separator , cacheId.toLowerCase() ,File.separator, "*"));
            }

            sb.append(pathArr[0].getPath()).append(File.separator).append("tayzgridd.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-bridge.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-socketserver.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-util.jar").append(seprator);
        } else if (currentOS == RuntimeUtil.OS.Windows) {
            seprator = ';';
            pathArr[0] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"libexec"));
            pathArr[1] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"lib",File.separator,"*"));
            pathArr[2] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"lib",File.separator,"resources",File.separator,"*"));
            if (cacheId != null && !cacheId.isEmpty()) {
                pathArr[3] = new File(Common.combinePath(AppUtil.getInstallDir(), File.separator,"deploy",File.separator , cacheId.toLowerCase() ,File.separator, "*"));
            }
            sb.append(pathArr[0].getPath()).append(File.separator).append("tayzgridd.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-bridge.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-socketserver.jar").append(seprator)
                    .append(pathArr[0].getPath()).append(File.separator).append("tg-util.jar").append(seprator);

        }

        for (int i = 1; i < pathArr.length; i++) {
            String val = pathArr[i].getAbsolutePath()+seprator;//getJars(pathArr[i], seprator);
            if (val != null && !val.isEmpty()) {
                sb.append(val);
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    private static String getJars(File path, char seprator) {
        StringBuilder sb = new StringBuilder();
        if (path.exists()) {
            File[] deployedJars = path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (deployedJars != null) {
                for (File jar : deployedJars) {
                    sb.append(jar.getPath());
                    sb.append(seprator);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     * @param disposing
     *
     *
     */
    private void dispose(boolean disposing) {
        _rwLock.AcquireWriterLock();
        try {
            if (_evalWarningTask != null) {
                try {
                    stopEvalWarning = true;
                    _evalWarningTask.interrupt();
                } catch (Exception e) {
                }
            }
            if (this._gcScheduler != null) {
                synchronized (this._gcScheduler) {
                    if (this._gcScheduler != null) {
                        try {
                            this._gcScheduler.Stop();
                            this._gcScheduler.dispose();
                        } catch (InterruptedException interruptedException) {
                        }
                    }
                }
            }
            
            if(this._portPoolScheduler != null){
                synchronized (this._portPoolScheduler){
                    try{
                        this._portPoolScheduler.Stop();
                        this._portPoolScheduler.dispose();
                    }catch(InterruptedException interruptedException){
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

}
