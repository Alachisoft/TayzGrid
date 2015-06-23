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

import com.alachisoft.tayzgrid.caching.CacheClearedCallback;
import com.alachisoft.tayzgrid.caching.CustomRemoveCallback;
import com.alachisoft.tayzgrid.caching.CustomUpdateCallback;
import com.alachisoft.tayzgrid.caching.ItemAddedCallback;
import com.alachisoft.tayzgrid.caching.ItemRemovedCallback;
import com.alachisoft.tayzgrid.caching.ItemUpdatedCallback;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.enums.CacheStatusOnServerContainer;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public interface ICacheServer extends IDisposable {

    String GetClusterIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    String GetLocalCacheIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void SetLocalCacheIP(String ip) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     *
     *
     * @param cacheName
     * @param assemblyFileName
     * @param buffer
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws com.alachisoft.tayzgrid.common.exceptions.TimeoutException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     */
    void CopyAssemblies(String cacheName, String assemblyFileName, byte[] buffer) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     *
     *
     * @param bridgeName
     * @param assemblyFileName
     * @param buffer
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws com.alachisoft.tayzgrid.common.exceptions.TimeoutException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException

     */
    void CopyBridgeAssemblies(String bridgeName, String assemblyFileName, byte[] buffer) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     *
     *
     * @param cacheName
     * @param fileName
     * @return
     */
    byte[] GetAssembly(String cacheName, String fileName) throws java.io.IOException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Clear cache
     *
     * @param cacheId
     *
     */
    void ClearCache(String cacheId) throws OperationFailedException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    boolean Authorize(byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Get a list of running caches (local + clustered)
     *
     * @param userId user id
     * @param password password
     * @return list of running caches
     */
    java.util.ArrayList GetRunningCaches(String userId, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    java.util.Map GetCacheProps(byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * A collection of the cache infos registered with the server.
     *
     *
     * CacheProps are in new format now. Instead of saving the props string, it
     * now saves CacheServerConfig instance:
     *
     * |local-cache-id | CacheServerConfig instance
     * |IDictionary | replica-id |
     * CacheServerConfig instance
     *
     * @return 
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws com.alachisoft.tayzgrid.common.exceptions.TimeoutException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException

     */
    java.util.Map CacheProps() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    CacheServerConfig GetCacheConfiguration(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    com.alachisoft.tayzgrid.config.newdom.CacheServerConfig GetNewConfiguration(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException,  Exception;

    CacheInfo GetCacheInfo(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    String GetHostName() throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void ReloadSrvcConfig() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    int GetSocketServerPort() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    CacheRegisterationInfo GetUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, Exception;

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
    com.alachisoft.tayzgrid.management.NewCacheRegisterationInfo GetNewUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, Exception;

    /**
     * Register cache
     *
     * @param cacheId
     * @param config
     * @param partId
     * @param props
     * @param userId
     * @param password
     * @param hotApply
     * @param overwrite
     * @return 
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws java.net.UnknownHostException
     *
     */
    boolean RegisterCache(String cacheId, CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws ConfigurationException, UnknownHostException, ManagementException, java.lang.Exception;

    boolean RegisterCache(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws ConfigurationException, UnknownHostException, ManagementException, java.lang.Exception;

    /**
     * Adds Server Node
     *
     * @param cacheId
     * @param hotConfig
     * @param config
     * @param partId
     * @param overwrite
     * @param userId
     * @param password
     * @param hotApply
     * @param isLocalNode
     * @return
     * @throws com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException
     * @throws com.alachisoft.tayzgrid.common.exceptions.ManagementException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException
     * @throws java.net.UnknownHostException
     * @throws java.lang.IllegalAccessException
     */

   void ApplyHotConfiguration(String cacheId, HotConfig hotConfig) throws CacheArgumentException, Exception ;

    NodeInfoMap GetNodeInfo() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    String CanApplyHotConfiguration(String cacheId, CacheServerConfig config) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void RemoveCacheServerFromClientConfig(String cacheId, String serverName) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, InterruptedException, TimeoutException;

    void RemoveCacheFromClientConfig(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, InterruptedException, TimeoutException;

    void UpdateClientServersList(String cacheId, CacheServerList serversPriorityList, String serverRuntimeContext, int port) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void UpdateClientServersList(String cacheId, String[] servers, tangible.RefObject<String> xml, boolean loadBalance, int port) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException, SecurityException;

    ClientConfiguration GetClientConfiguration(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, InterruptedException, TimeoutException, UnsupportedEncodingException;

    void UpdateClientConfiguration(String cacheId, ClientConfiguration configuration) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    String GetBindIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    int GetClientConfigId() throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper GetClientNodeStatus(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    boolean VerifyWindowsUser(String nodeName, String userName, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    //Method introduced to check weather a user is a windows administrator or not
    boolean VerfyAdministrator(String userName, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void GarbageCollect();

    /**
     * Disbale logging
     *
     * @param subsystem Subsystem for which logging will be disabled
     * @param type Type of logging to disable
     */
    void DisableLogging(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type) throws Exception;

    void SynchronizeClientConfig() throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Update TCP cache settings that includes updated list of TCP members
     *
     * @param cacheId
     * @param props
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    boolean ApplyCacheConfiguration(String cacheId, CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws IllegalArgumentException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    boolean ApplyCacheConfiguration(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws IllegalArgumentException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException,  Exception;

    /**
     * Un-register cache
     *
     * @param cacheId
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    void UnregisterCache(String cacheId, String partId, byte[] userId, byte[] password, Boolean isGracefulShutdown, Boolean removeServerOnly) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, Exception, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void StartCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException, Exception;

    void StartCache(String cacheId, String partitionId) throws Exception;

    void StartCache(String cacheId, byte[] userId, byte[] password) throws Exception;

    void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws  Exception;

    void StartCachePhase2(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password) throws Exception;

    void StartCache(String cacheId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate) throws Exception;

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
    void StartCache(String cacheId, String partitionId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws Exception;

    void StopCache(String cacheId, byte[] userId, byte[] password, Boolean isGracefulShutdown) throws Exception;

    /**
     * Stop a cache
     *
     * @param cacheId
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    void StopCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean isGracefulNodeShutdown) throws Exception;

    /**
     * Stop all running caches
     * @throws java.lang.Exception
     */
    void StopAllCaches() throws Exception;
    
    /**
     * Detect and return all the available NICs on this machine
     */
    java.util.HashMap DetectNICs() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void BindToIP(BindedIpMap bindIPMap) throws Exception;

    /**
     *
     *
     * @return
     */
    BindedIpMap BindedIp() throws Exception;

    /**
     * Gets the Max port number, among all the ports of registered caches on
     * this machine
     *
     * @return Max cluster port
     */
    int GetMaxPort() throws java.net.UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Checks if the current cache is a Cluster cache or not, used in NCache
     * UnReg cache tool as now UnReg is only applicable to cluster caches only
     *
     * @param cacheId
     * @return true if Cluster Cache
     */
    CacheStatusOnServerContainer IsClusteredCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Cluster port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    boolean PortIsAvailable(int port) throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

        /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Management port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    boolean IsManagementPortAvailable(int port) throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;
    
        /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Client port
     * @return 'true' if the port is available, otherwise 'flase'
     * @throws UnknownHostException
     */
    boolean IsClientPortAvailable(int port) throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;
    
    /**
     * Checks whether the newly added node arise port conflict or not
     *
     * @param port Cluster port
     * @return 'true' if the node is allowed, otherwise 'flase'
     */
    //internal bool NodeIsAllowed(int port)
    boolean NodeIsAllowed(int port, String id) throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Gets the status of NCache on this node.
     *
     * @return The ServerStatus.
     */
    StatusInfo GetCacheStatus(String cacheId, String partitionId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Starts monitoring the client activity.
     */
    void StartMonitoringActivity() throws java.lang.Exception;

    /**
     * Stops monitoring client activity.
     */
    void StopMonitoringActivity() throws java.lang.Exception;

    /**
     * Publishes the observed client activity into a file.
     */
    void PublishActivity() throws CloneNotSupportedException, Exception;

    /**
     * Checks if given cache is configured ast bridge target cache
     *
     * @param cache
     * @return
     */
    boolean IsBridgeTargetCache(String cache) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Clears the content of given cache
     *
     * @param cacheId Id of the cache
     */
    void ClearCacheContent(String cacheId) throws OperationFailedException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Returns true if given cache is running
     *
     * @param cacheId
     * @return
     */
    boolean IsRunning(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    /**
     * Gets CacheStatistics for a given cache
     *
     * @param cacheId
     * @return
     */


    com.alachisoft.tayzgrid.caching.statistics.CacheStatistics GetStatistics(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException ;
    


    /**
     * Get count for given cache
     *
     * @param cacheId
     * @return
     */
    long GetCacheCount(String cacheId) throws GeneralFailureException, OperationFailedException, CacheException, ManagementException, InterruptedException, UnsupportedEncodingException;

    void BalanceDataloadOnCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException, GeneralFailureException, SuspectedException;

    boolean IsCacheRegistered(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    Node[] GetCacheServers(String cacheId) throws UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    ConfiguredCacheInfo[] GetAllConfiguredCaches() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    CacheNodeStatistics[] GetCacheStatistics(String cacheId) throws CacheException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    CacheStatistics GetCacheStatistics2(String cacheId) throws ArgumentNullException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

   
    String GetLicenseKey() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    java.util.HashMap GetSnmpPorts(String cacheid) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    void StopServer() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

    public String GetServerPlatform() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;

   
    int GetMaxSocketPort() throws java.net.UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException ;

    int GetMaxManagementPort() throws java.net.UnknownHostException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException ;

    java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> GetClientProcessStats(String cacheId) throws UnknownHostException;

    void setSnmpPort(String cacheid, Map<String, ArrayList<Integer>> snmpPort) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException  ;
    
    String getPID() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException  ;
    
    boolean getIsCacheRunning(String cacheId)throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException;
}
