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
package com.alachisoft.tayzgrid.common;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;


public class ServicePropValues {

    public final static String LOGS_FOLDER = "log"; //Used by logger

    private static InetAddress StatsServerIp;
    public static String HTTP_PORT;
    public static String CACHE_SERVER_PORT = "9600";
    public static String CACHE_MANAGEMENT_PORT = "8270";
    public static String BIND_ToCLUSTER_IP;
    public static String BIND_toClient_IP;
    public static String INSTALLDIR_DIR;
    public static String CACHE_USER_DIR;
 
    private static String Mapping_Properties;
    private static String TGHome = null;
    public static String AUTO_START_DELAY;
    public static String CACHE_AUTOSTART_USER;
    public static String CACHE_AUTOSTART_PASSWORD;
    public static String STATE_TRANSFER_DATA_SIZE_PER_SEC;
    public static String ENABLE_GC_DURING_STATE_TRANSFER;
    public static String GC_THRESHOLD;
    public final static String SERVICE_PROP_FILE = "server.properties";
    public static String DEBUG_PORT_OFFSET = "4000";

   //<editor-fold desc="Cache Related Properties">
    public static String CacheServer_SendBufferSize = "131072";
    public static String CacheServer_ReceiveBufferSize = "131072";
    public static String CacheServer_BulkItemsToReplicate = "300";
    public static String CacheServer_LogClientEvents;
    public static String CacheServer_EnableSnapshotPoolingCacheSize;
    public static String CacheServer_MinimumSnaphotSizeForPooling = "100000";
    public static String CacheServer_MaxNumOfSnapshotsInPool;
    public static String CacheServer_SnapshotPoolSize;
    public static String CacheServer_NewSnapshotCreationTimeInSec;
    public static String CacheServer_SnapshotCreationThreshold;
    public static String CacheServer_BindToIP;
    public static String CacheServer_EnableCacheLastAccessCount;
    public static String CacheServer_EnableCacheLastAccessCountLogging;
    public static String CacheServer_CacheLastAccessCountInterval;
    public static String CacheServer_CacheLastAccessLogInterval;
    public static String CacheServer_ExpirationBulkRemoveDelay = "0";
    public static String CacheServer_ExpirationBulkRemoveSize = "10";
    public static String CacheServer_EnableGCCollection;
    public static String CacheServer_EvictionBulkRemoveDelay = "0";
    public static String CacheServer_EvictionBulkRemoveSize = "10";
    public static String preparedQueryEvictionPercentage;
    public static String CacheServer_SQLNotificationService;
    public static String CacheServer_SQLNotificationQueue;
    public static String CacheServer_EnumeratorChunkSize;
    public static String preparedQueryTableSize;
    public static String Cache_EnableGCCollection;
    public static String Cache_DisableIndexNotDefinedException;
    public static String SNMP_PORT;
    public static String JMXServer_PORT;
       public static String SNMP_POOL;
    public static String JMX__POOL;
    //</editor-fold>
    //<editor-fold desc="Cluster Related Properties">
    public static String CacheServer_EnableDebuggingCounters = "false";
    public static String CacheServer_EnableDualSocket = "false";
    public static String CacheServer_EnableNagling = "false";
    public static String CacheServer_NaglingSize = "500";
    public static String CacheServer_SimulateSocketClose;
    public static String CacheServer_SocketCloseInterval;
    public static String CacheServer_AllowRequestEnquiry;
    public static String CacheServer_RequestEnquiryInterval;
    public static String CacheServer_RequestEnquiryRetries;
    public static String useAvgStats;
    public static String asyncTcpUpQueue;
    public static String CacheServer_HeartbeatInterval;
    public static String CacheServer_EventsPersistence;
    public static String CacheServer_EventsPersistenceInterval;
    //</editor-fold>
    //<editor-fold desc="Management Related Properties">

    public static String IPC_PortName;
    public static String CacheServer_EnableForcedGC;
    public static String CacheServer_ForcedGCThreshold;
    public static String UninstallInProgress = "false";
    //</editor-fold>
    //<editor-fold desc="Socket Server Related Properties">
    public static String Cache_MaxPendingConnections;
    public static String Cache_EnableServerCounters;
    public static String CacheServer_ResponseDataSize;
    public static String Enable_Logs;
    public static String Enable_Detailed_Logs;
    public static String Cache_GracefullShutdownTimeout;
    public static String Cache_BlockingActivityTimeout;

    public static String Enable_Client_Death_Detection;
    public static String Client_Death_Detection_Grace_Period;
    //</editor-fold>

    public static void initialize() {
        SNMP_PORT = System.getProperty("JmxSnmp.Port");
        JMXServer_PORT = System.getProperty("JmxServer.Port");
        if (System.getProperty("MappingProperties.Path") != null) {
            Mapping_Properties = System.getProperty("MappingProperties.Path");
        }

        HTTP_PORT = System.getProperty("Http.Port");

        String serverPort = System.getProperty("CacheServer.Port");
        if (serverPort != null && serverPort.length() > 0) {
            CACHE_SERVER_PORT = serverPort;
        }

        String managementPort = System.getProperty("CacheManagementServer.Port");
        if (managementPort != null && managementPort.length() > 0) {
            CACHE_MANAGEMENT_PORT = managementPort;
        }

        String sendBufferSize = System.getProperty("CacheServer.SendBufferSize");
        if (sendBufferSize != null && sendBufferSize.length() > 0) {
            CacheServer_SendBufferSize = sendBufferSize;
        }

        String reveiveBufferSize = System.getProperty("ReceiveBufferSize");
        if (reveiveBufferSize != null && reveiveBufferSize.length() > 0) {
            CacheServer_ReceiveBufferSize = reveiveBufferSize;
        }

        BIND_ToCLUSTER_IP = System.getProperty("CacheServer.BindToClusterIP");
        BIND_toClient_IP = System.getProperty("CacheServer.BindToClientServerIP");

        INSTALLDIR_DIR = System.getProperty("Cache.InstallDir");
        CACHE_USER_DIR = System.getProperty("Cache.user.directory");

        SNMP_POOL = System.getProperty("SNMP.Pool");
        JMX__POOL = System.getProperty("JMX.Pool");

        AUTO_START_DELAY = System.getProperty("AutoStartDelay");
        CACHE_AUTOSTART_USER = System.getProperty("CacheAutoStart.User");
        CACHE_AUTOSTART_PASSWORD = System.getProperty("CacheAutoStart.Password");

        //<editor-fold desc="Cache Related Properties">
        String bulkItemsToReplicate = System.getProperty("CacheServer.BulkItemsToReplicate");
        if (bulkItemsToReplicate != null && bulkItemsToReplicate.length() > 0) {
            CacheServer_BulkItemsToReplicate = bulkItemsToReplicate;
        }

        CacheServer_LogClientEvents = System.getProperty("CacheServer.LogClientEvents");
        CacheServer_EnableSnapshotPoolingCacheSize = System.getProperty("CacheServer.EnableSnapshotPoolingCacheSize");
        CacheServer_MinimumSnaphotSizeForPooling = System.getProperty("CacheServer.EnableSnapshotPoolingCacheSize");
        CacheServer_MaxNumOfSnapshotsInPool = System.getProperty("CacheServer.MaxNumOfSnapshotsInPool");
        CacheServer_SnapshotPoolSize = System.getProperty("CacheServer.SnapshotPoolSize");
        CacheServer_NewSnapshotCreationTimeInSec = System.getProperty("CacheServer.NewSnapshotCreationTimeInSec");
        CacheServer_SnapshotCreationThreshold = System.getProperty("CacheServer.SnapshotCreationThreshold");
        CacheServer_BindToIP = System.getProperty("CacheServer.BindToIP");
        CacheServer_EnableCacheLastAccessCount = System.getProperty("CacheServer.EnableCacheLastAccessCount");
        CacheServer_EnableCacheLastAccessCountLogging = System.getProperty("CacheServer.EnableCacheLastAccessCountLogging");
        CacheServer_CacheLastAccessCountInterval = System.getProperty("CacheServer.CacheLastAccessCountInterval");
        CacheServer_CacheLastAccessLogInterval = System.getProperty("CacheServer.CacheLastAccessLogInterval");

        String expiraionBulkRemoveDelay = System.getProperty("CacheServer.ExpirationBulkRemoveDelay");
        if (expiraionBulkRemoveDelay != null && expiraionBulkRemoveDelay.length() > 0) {
            CacheServer_ExpirationBulkRemoveDelay = expiraionBulkRemoveDelay;
        }

        String expirationBulkRemoveSize = System.getProperty("CacheServer.ExpirationBulkRemoveSize");
        if (expirationBulkRemoveSize != null && expirationBulkRemoveSize.length() > 0) {
            CacheServer_ExpirationBulkRemoveSize = expirationBulkRemoveSize;
        }

        CacheServer_EnableGCCollection = System.getProperty("CacheServer.EnableGCCollection");

        String evictionBulkRemoveDelay = System.getProperty("CacheServer.EvictionBulkRemoveDelay");
        if (evictionBulkRemoveDelay != null && evictionBulkRemoveDelay.length() > 0) {
            CacheServer_EvictionBulkRemoveDelay = evictionBulkRemoveDelay;
        }

        String evictionBulkRemoveSize = System.getProperty("CacheServer.EvictionBulkRemoveSize");
        if (evictionBulkRemoveSize != null && evictionBulkRemoveSize.length() > 0) {
            CacheServer_EvictionBulkRemoveSize = evictionBulkRemoveSize;
        }

        preparedQueryEvictionPercentage = System.getProperty("preparedQueryEvictionPercentage");
        CacheServer_SQLNotificationService = System.getProperty("CacheServer.SQLNotificationService");
        CacheServer_SQLNotificationQueue = System.getProperty("CacheServer.SQLNotificationQueue");
        CacheServer_EnumeratorChunkSize = System.getProperty("CacheServer.EnumeratorChunkSize");
        preparedQueryTableSize = System.getProperty("preparedQueryTableSize");
        Cache_EnableGCCollection = System.getProperty("Cache.EnableGCCollection");

        String disableIndexNotDefined = System.getProperty("CacheServer.DisableIndexNotDefinedException");
        if (disableIndexNotDefined != null && disableIndexNotDefined.length() > 0) {
            Cache_DisableIndexNotDefinedException = disableIndexNotDefined;
        }
        //</editor-fold>

        //<editor-fold desc="Cluster Related Properties">
        String enableDebuggingCounters = System.getProperty("CacheServer.EnableDebuggingCounters");
        if (enableDebuggingCounters != null && enableDebuggingCounters.length() > 0) {
            CacheServer_EnableDebuggingCounters = enableDebuggingCounters;
        }

        String enableDualSocket = System.getProperty("CacheServer.EnableDualSocket");
        if (enableDualSocket != null && enableDualSocket.length() > 0) {
            CacheServer_EnableDualSocket = enableDualSocket;
        }

        String enableNagling = System.getProperty("CacheServer.EnableNagling");
        if (enableNagling != null && enableNagling.length() > 0) {
            CacheServer_EnableNagling = enableNagling;
        }

        String naglingSize = System.getProperty("CacheServer.NaglingSize");
        if (naglingSize != null && naglingSize.length() > 0) {
            CacheServer_NaglingSize = naglingSize;
        }

        CacheServer_SimulateSocketClose = System.getProperty("CacheServer.SimulateSocketClose");
        CacheServer_SocketCloseInterval = System.getProperty("CacheServer.SocketCloseInterval");
        CacheServer_AllowRequestEnquiry = System.getProperty("CacheServer.AllowRequestEnquiry");
        CacheServer_RequestEnquiryInterval = System.getProperty("CacheServer.RequestEnquiryInterval");
        CacheServer_RequestEnquiryRetries = System.getProperty("CacheServer.RequestEnquiryRetries");
        useAvgStats = System.getProperty("useAvgStats");
        asyncTcpUpQueue = System.getProperty("asyncTcpUpQueue");
        CacheServer_HeartbeatInterval = System.getProperty("CacheServer.HeartbeatInterval");

        String eventPersistance = System.getProperty("CacheServer_EventsPersistence");
        if (eventPersistance != null && eventPersistance.length() > 0) {
            CacheServer_EventsPersistence = eventPersistance;
        }
        String eventPersistanceInterval = System.getProperty("CacheServer_EventsPersistenceInterval");
        if (eventPersistanceInterval != null) {
            CacheServer_EventsPersistenceInterval = eventPersistanceInterval;
        }
        //</editor-fold>

    
        IPC_PortName = System.getProperty("IPC.PortName");

       

        CacheServer_EnableForcedGC = System.getProperty("CacheServer.EnableForcedGC");
        CacheServer_ForcedGCThreshold = System.getProperty("CacheServer.ForcedGCThreshold");

        //</editor-fold>
        //<editor-fold desc="Socket Server Related Properties">
        Cache_MaxPendingConnections = System.getProperty("Cache.MaxPendingConnections");
        Cache_EnableServerCounters = System.getProperty("Cache.EnableServerCounters");
        CacheServer_ResponseDataSize = System.getProperty("CacheServer.ResponseDataSize");
        Enable_Logs = System.getProperty("EnableLogs");
        Enable_Detailed_Logs = System.getProperty("EnableDetailedLogs");
        Cache_GracefullShutdownTimeout = System.getProperty("CacheServer.GracefullShutdownTimeout");
        Cache_BlockingActivityTimeout = System.getProperty("CacheServer.BlockingActivityTimeout");

        Enable_Client_Death_Detection = System.getProperty("CacheServer.EnableClientDeathDetection");
        Client_Death_Detection_Grace_Period = System.getProperty("CacheServer.ClientDeathDetectionGracePeriod");
        if (System.getProperty("CacheServer.DebugPortOffset") != null) {
            DEBUG_PORT_OFFSET = System.getProperty("CacheServer.DebugPortOffset");
        }
        //</editor-fold>

        if (System.getProperty("UninstallInProgress") != null) {
            UninstallInProgress = System.getProperty("UninstallInProgress");
        }
    }

    public static InetAddress getStatsServerIp() {
        try {
            if (StatsServerIp == null) {
                if (BIND_toClient_IP != null) {
                    StatsServerIp = InetAddress.getByName(BIND_toClient_IP);
                } else {
                    StatsServerIp = com.alachisoft.tayzgrid.common.net.Helper.getFirstNonLoopbackAddress();
                }
            }
        } catch (UnknownHostException uHE) {
        }
        return StatsServerIp;
    }

    public static String getMapping_Properties() {
        if (Mapping_Properties == null) {
            File file = new File(SERVICE_PROP_FILE);
            if (file.exists()) {
                Mapping_Properties = file.getPath();
            } else {
                Mapping_Properties = Common.combinePath(getTGHome(), "config", SERVICE_PROP_FILE);
            }
        }
        return Mapping_Properties;
    }

    public static String getTGHome() {
        if (TGHome == null) {
            TGHome = Common.getTGHome();
        }
        return TGHome;
    }

    public static boolean loadServiceProp() {
       
            String filePath = getMapping_Properties();
            if(Common.isNullorEmpty(filePath) && !Common.isFileExist(filePath) ) return false;
            Properties props = new Properties();

            try {
                props.load(new FileInputStream(filePath));
            } catch (IOException iOException) {
                return false;
            }

            Enumeration enu = props.keys();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
                System.setProperty(key, props.getProperty(key).trim());
            }
            ServicePropValues.initialize();
            return true;
    }
    
    public static int getClientPort()
    {
        return Integer.parseInt(CACHE_SERVER_PORT);
    }
}
