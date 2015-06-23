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

package com.alachisoft.tayzgrid.common.util;

public class ManagementUtil
{

    public final static class MethodName
    {

        //<editor-fold defaultstate="collapsed" desc="CacheServer">
        public static final String StopServer = "StopServer";
        public static final String StartCache = "StartCache";
        public static final String GetClusterIP = "GetClusterIP";
        public static final String GetLocalCacheIP = "GetLocalCacheIP";
        public static final String CopyAssemblies = "CopyAssemblies";
        public static final String CopyBridgeAssemblies = "CopyBridgeAssemblies";
        public static final String GetAssembly = "GetAssembly";
        public static final String ClearCache = "ClearCache";
        public static final String Authorize = "Authorize";
        public static final String IsSecurityEnabled = "IsSecurityEnabled";
        public static final String GetRunningCaches = "GetRunningCaches";
        public static final String GetCacheProps = "GetCacheProps";
        public static final String CacheProps = "CacheProps";
        public static final String GetCacheConfiguration = "GetCacheConfiguration";
        public static final String GetCacheInfo = "GetCacheInfo";
        public static final String GetHostName = "GetHostName";
        public static final String ReloadSrvcConfig = "ReloadSrvcConfig";
        public static final String GetSocketServerPort = "GetSocketServerPort";
        public static final String GetUpdatedCacheConfiguration = "GetUpdatedCacheConfiguration";
        public static final String RegisterCache = "RegisterCache";
        public static final String GetNodeInfo = "GetNodeInfo";
        public static final String CanApplyHotConfig = "CanApplyHotConfig";
        public static final String RemoveCacheServerFromClientConfig = "RemoveCacheServerFromClientConfig";
        public static final String RemoveCacheFromClientConfig = "RemoveCacheFromClientConfig";
        public static final String UpdateClientServersList = "UpdateClientServersList";
        public static final String UpdateSecurity = "UpdateSecurity";
        public static final String UpdateUserSecurityCredentials = "UpdateUserSecurityCredentials";
        public static final String GetUserSecurityCredentials = "GetUserSecurityCredentials";
        public static final String GetClientConfiguration = "GetClientConfiguration";
        public static final String UpdateClientConfiguration = "UpdateClientConfiguration";
        public static final String GetBindIP = "GetBindIP";
        public static final String GetClientConfigId = "GetClientConfigId";
        public static final String GetClientNodeStatus = "GetClientNodeStatus";
        public static final String VerifyWindowsUser = "VerifyWindowsUser";
        public static final String VerfyAdministrator = "VerfyAdministrator";
        public static final String VerifyNodeAdministrator = "VerifyNodeAdministrator";
        public static final String DisableLogging = "DisableLogging";
        public static final String SynchronizeClientConfig = "SynchronizeClientConfig";
        public static final String ApplyCacheConfiguration = "ApplyCacheConfiguration";
        public static final String UnregisterCache = "UnregisterCache";
        public static final String StartCachePhase2 = "StartCachePhase2";
        public static final String StopCache = "StopCache";
        public static final String StopAllCaches = "StopAllCaches";
        public static final String DetectNICs = "DetectNICs";
        public static final String BindToIP = "BindToIP";
        public static final String BindedIp = "BindedIp";
        public static final String GetMaxPort = "GetMaxPort";
        public static final String IsClusteredCache = "IsClusteredCache";
        public static final String PortIsAvailable = "PortIsAvailable";
        public static final String IsManagementPortAvailable = "IsManagementPortAvailable";
        public static final String IsClientPortAvailable = "IsClientPortAvailable";
        public static final String NodeIsAllowed = "NodeIsAllowed";
        public static final String Decrypt = "Decrypt";
        public static final String GetCacheStatus = "GetCacheStatus";
        public static final String StartMonitoringActivity = "StartMonitoringActivity";
        public static final String StopMonitoringActivity = "StopMonitoringActivity";
        public static final String PublishActivity = "PublishActivity";
        public static final String GetCacheStatistics = "GetCacheStatistics";
        public static final String GetCacheStatistics2 = "GetCacheStatistics2";
        public static final String GetCacheServers = "GetCacheServers";
        public static final String GetRunningCacheServers = "GetRunningCacheServers";
        public static final String GetCacheClients = "GetCacheClients";
        public static final String GetClientProcessStats = "GetClientProcessStats";
        public static final String GetAllConfiguredCaches = "GetAllConfiguredCaches";
        public static final String GetCacheConfigurationInfo = "GetCacheConfigurationInfo";
        public static final String EnableLogging = "EnableLogging";
        public static final String GetPartitionedReplicaCaches = "getPartitionedReplicaCaches";
        public static final String GetCaches = "getCaches";
        public static final String GetCacheRenderer = "GetCacheRenderer";
        public static final String GetCacheInstanceIgnoreReplica = "GetCacheInstanceIgnoreReplica";
        public static final String GetCacheInstance = "GetCacheInstance";
        public static final String GetSecurityMap = "GetSecurityMap";
        public static final String GetLicenseKey = "GetLicenseKey";
        public static final String GetLicenses = "GetLicenses";
        public static final String GetLicenseLogger = "GetLicenseLogger";
        public static final String IsBridgeTargetCache = "IsBridgeTargetCache";
        public static final String ClearCacheContent = "ClearCacheContent";
        public static final String IsRunning = "IsRunning";
        public static final String GetStatistics = "GetStatistics";
        public static final String GetCacheCount = "GetCacheCount";
        public static final String GetClusterNIC = "GetClusterNIC";
        public static final String GetSocketServerNIC = "GetSocketServerNIC";
        public static final String GetNICForIP = "GetNICForIP";
        public static final String GetUpdatedCacheServers = "GetUpdatedCacheServers";
        public static final String GetUpdatedRunningCacheServers = "GetUpdatedRunningCacheServers";
        public static final String GetUpdatedCacheClients = "GetUpdatedCacheClients";
        public static final String GetPercentageCPUUsage = "GetPercentageCPUUsage";
        public static final String RegisterEventViewerEvents = "RegisterEventViewerEvents";
        public static final String UnRegisterEventViewerEvents = "UnRegisterEventViewerEvents";
        public static final String GetLatestEvents = "GetLatestEvents";
        public static final String SetLocalCacheIP = "SetLocalCacheIP";
        public static final String BalanceDataloadOnCache = "BalanceDataloadOnCache";
        public static final String IsCacheRegistered = "IsCacheRegistered";
        public static final String InitializeMonitor = "InitializeMonitor";
        public static final String GetConfiguredPartitionedReplicaCaches = "GetConfiguredPartitionedReplicaCaches";
        public static final String GetSnmpPorts = "GetSnmpPorts";
        public static final String GetServerPlatform = "GetServerPlatform";
        public static final String GetNewConfiguration="GetNewConfiguration";
        public static final String GetNewUpdatedCacheConfiguration="GetNewUpdatedCacheConfiguration";
        public static final String GetServerLicenseInfo= "GetServerLicenseInfo";
        public static final String GetTayzGridServer = "GetTayzGridServer";
        public static final String GetShutdownTimeout = "GetShutdownTimeout";
        public static final String GarbageCollect = "GarbageCollect";
        public static final String GetMaxSocketPort = "GetMaxSocketPort";
        public static final String GetMaxManagementPort = "GetMaxManagementPort";
        public static final String HotApplyConfiguration= "HotApplyConfiguration";
        public static final String GetCacheBinding = "GetCacheBinding";
        public static final String SetSnmpPort = "SetSnmpPort";
        public static final String GetProcessId = "GetProcessId";
        public static final String GetIsCacheRunning = "GetIsCacheRunnings";
        
       
        //</editor-fold>

    }

    public final static class ManagementObjectName
    {

        public static final String MonitorServer = "MonitorServer";
        public static final String CacheServer = "CacheServer";
       
    }
}
