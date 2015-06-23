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
package com.alachisoft.tayzgrid.management.rpc;



import com.alachisoft.tayzgrid.caching.CacheClearedCallback;
import com.alachisoft.tayzgrid.caching.CustomRemoveCallback;
import com.alachisoft.tayzgrid.caching.CustomUpdateCallback;
import com.alachisoft.tayzgrid.caching.ItemAddedCallback;
import com.alachisoft.tayzgrid.caching.ItemRemovedCallback;
import com.alachisoft.tayzgrid.caching.ItemUpdatedCallback;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;

import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.communication.IChannelFormatter;
import com.alachisoft.tayzgrid.common.enums.CacheStatusOnServerContainer;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ManagementCommandProtocol.ManagementCommand;
import com.alachisoft.tayzgrid.common.protobuf.ManagementResponseProtocol.ManagementResponse;
import com.alachisoft.tayzgrid.common.util.ManagementUtil;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.BindedIpMap;
import com.alachisoft.tayzgrid.management.CacheInfo;
import com.alachisoft.tayzgrid.management.CacheRegisterationInfo;
import com.alachisoft.tayzgrid.management.CacheServer;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.NewCacheRegisterationInfo;
import com.alachisoft.tayzgrid.management.NodeInfoMap;
import com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteCacheServer extends RemoteServerBase implements ICacheServer {

    private String _user;
    private String _password;

    static {
        try {
            CacheServer.RegisterCompactTypes();
        } catch (NumberFormatException numberFormatException) {
        } catch (CacheArgumentException nCacheArgumentException) {
        }
    }

    public RemoteCacheServer(String server, int port) throws IllegalAccessException, Exception {
        super(server, port);
    }

    public RemoteCacheServer(String server, int port, String bindIp) throws IllegalAccessException, Exception {
        super(server, port, bindIp);
    }

    public RemoteCacheServer(String server, int port, String bindIp, String user, String password) throws IllegalAccessException, Exception {
        super(server, port, bindIp);
        _user = user;
        _password = password;
    }

    @Override
    protected IChannelFormatter GetChannelFormatter() {
        return new ManagementChannelFormatter();
    }

    @Override
    protected boolean InitializeInternal() {
        return super.InitializeInternal();
    }

    private void exceptionOccured(ManagementResponse res)
            throws  ManagementException {

        if (res != null && res.hasException() && res.getException() != null) {
            switch (res.getException().getType()) {

                default:
                    throw new ManagementException(res.getException().getMessage());
            }
        }

    }

    protected final Object ExecuteCommandOnCacehServer(ManagementCommand command) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementResponse response = null;
        if (_requestManager != null) {
            Object tempVar = _requestManager.SendRequest(command);
            if(tempVar != null) {
                response = (ManagementResponse) ((tempVar instanceof ManagementResponse) ? tempVar : null);
            }
            exceptionOccured(response);
        }
        if (response != null) {
            return response.get_returnValue();
        }

        return null;
    }

    private ManagementCommand GetManagementCommand(String method) {
        return GetManagementCommand(method, 1);
    }

    private ManagementCommand GetManagementCommand(String method, int overload) {
        ManagementCommand.Builder build = ManagementCommandProtocol.ManagementCommand.newBuilder();
        build.setMethodName(method);
        build.setOverload(overload);
        build.setObjectName(ManagementUtil.ManagementObjectName.CacheServer);
        build.setSource(ManagementCommand.SourceType.TOOL);
        return build.build();
    }

    public final String GetClusterIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetClusterIP);
        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (String) ((tempVar instanceof String) ? tempVar : null);
    }

    public final String GetLocalCacheIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetLocalCacheIP);
        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (String) ((tempVar instanceof String) ? tempVar : null);
    }

    /**
     * Finalizer for this object.
     */
    protected void finalize() throws Throwable {
        dispose();
    }

    /**
     *
     *
     * @param name Name of the file (assembly)
     * @param buffer
     */
    @Override
    public final void CopyAssemblies(String cacheName, String assemblyFileName, byte[] buffer) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.CopyAssemblies);
        command.getParameters().AddParameter(cacheName);
        command.getParameters().AddParameter(assemblyFileName);
        command.getParameters().AddParameter(buffer);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     *
     * @param bridgeName
     * @param assemblyFileName
     * @param buffer
     */
    public final void CopyBridgeAssemblies(String bridgeName, String assemblyFileName, byte[] buffer) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.CopyBridgeAssemblies);

        command.getParameters().AddParameter(bridgeName);
        command.getParameters().AddParameter(assemblyFileName);
        command.getParameters().AddParameter(buffer);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     *
     * @param cacheName
     * @param fileName
     * @return
     */
    public final byte[] GetAssembly(String cacheName, String fileName) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetAssembly);

        command.getParameters().AddParameter(cacheName);
        command.getParameters().AddParameter(fileName);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
    }

    /**
     * Clear cache
     *
     * @param cacheId
     *
     */
    public final void ClearCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.ClearCache);

        command.getParameters().AddParameter(cacheId);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final boolean Authorize(byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.Authorize);

        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        try {
            return (Boolean) ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Get a list of running caches (local + clustered)
     *
     * @param userId user id
     * @param password password
     * @return list of running caches
     */
    public final java.util.ArrayList GetRunningCaches(String userId, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetRunningCaches);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
    }

    public final java.util.Map GetCacheProps(byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheProps);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (java.util.Map) ((tempVar instanceof java.util.Map) ? tempVar : null);
    }

    /**
     * A collection of the cache info registered with the server.
     *
     *
     * CacheProps are in new format now. Instead of saving the props string, it
     * now saves CacheServerConfig instance:
     *
     * |local-cache-id | CacheServerConfig instance
     * | IDictionary | replica-id |
     * CacheServerConfig instance
     *
     */
    public final java.util.Map CacheProps() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.CacheProps);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (java.util.Map) ((tempVar instanceof java.util.Map) ? tempVar : null);
    }

    @Override
    public final void GarbageCollect() {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GarbageCollect);

        Object tempVar = null;
        try {

            tempVar = ExecuteCommandOnCacehServer(command);

        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public final CacheServerConfig GetCacheConfiguration(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheConfiguration);
        command.getParameters().AddParameter(cacheId);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (CacheServerConfig) ((tempVar instanceof CacheServerConfig) ? tempVar : null);
    }

    public final CacheInfo GetCacheInfo(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheInfo);
        command.getParameters().AddParameter(cacheId);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (CacheInfo) ((tempVar instanceof CacheInfo) ? tempVar : null);
    }

    public final String GetHostName() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetHostName);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (String) ((tempVar instanceof String) ? tempVar : null);
    }

    public final void ReloadSrvcConfig() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.ReloadSrvcConfig);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final int GetSocketServerPort() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        Integer val = null;
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetSocketServerPort);
        try {
            val = (Integer) ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return val;
    }

    public final CacheRegisterationInfo GetUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetUpdatedCacheConfiguration);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partId);
        command.getParameters().AddParameter(newNode);
        command.getParameters().AddParameter(isJoining);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (CacheRegisterationInfo) ((tempVar instanceof CacheRegisterationInfo) ? tempVar : null);
    }

    /**
     * Register cache
     *
     * @param cacheId
     * @param props
     * @param overwrite
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     *
     */
    public final boolean RegisterCache(String cacheId, CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        //overload 1
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.RegisterCache, 1);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(config);
        command.getParameters().AddParameter(partId);
        command.getParameters().AddParameter(overwrite);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(hotApply);
        try {
            return (Boolean) ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public final NodeInfoMap GetNodeInfo() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetNodeInfo);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (NodeInfoMap) Common.readAs(tempVar, NodeInfoMap.class);
    }

    public final String CanApplyHotConfiguration(String cacheId, CacheServerConfig config) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.CanApplyHotConfig);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(config);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(tempVar != null) {
            return (String) ((tempVar instanceof String) ? tempVar : null);
        } else {
            return null;
        }
    }

    /**
     * Gets or sets the socket server port.
     */
    public final void RemoveCacheServerFromClientConfig(String cacheId, String serverName) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.RemoveCacheServerFromClientConfig);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(serverName);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void RemoveCacheFromClientConfig(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.RemoveCacheFromClientConfig);
        command.getParameters().AddParameter(cacheId);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void UpdateClientServersList(String cacheId, CacheServerList serversPriorityList, String serverRuntimeContext, int port) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.UpdateClientServersList);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(serversPriorityList);
        command.getParameters().AddParameter(serverRuntimeContext);
        command.getParameters().AddParameter(port);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void UpdateClientServersList(String cacheId, String[] servers, tangible.RefObject<String> xml, boolean loadBalance, int port) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException, SecurityException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.UpdateClientServersList, 2);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(servers);
        command.getParameters().AddParameter(xml.argvalue);
        command.getParameters().AddParameter(loadBalance);
        command.getParameters().AddParameter(port);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final ClientConfiguration GetClientConfiguration(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetClientConfiguration);
        command.getParameters().AddParameter(cacheId);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (ClientConfiguration) ((tempVar instanceof ClientConfiguration) ? tempVar : null);
    }

    public final void UpdateClientConfiguration(String cacheId, ClientConfiguration configuration) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.UpdateClientConfiguration);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(configuration);
        try {
            ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final String GetBindIP() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetBindIP);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (String) ((tempVar instanceof String) ? tempVar : null);
    }

    public final int GetClientConfigId() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        Integer val = null;
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetClientConfigId);
        try {
            val = (Integer) ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return val;
    }

    @Override
    public final com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper GetClientNodeStatus(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetClientNodeStatus);
        command.getParameters().AddParameter(cacheId);

        return (com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper) ExecuteCommandOnCacehServer(command);
    }

    public final boolean VerifyWindowsUser(String nodeName, String userName, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.VerifyWindowsUser);
        command.getParameters().AddParameter(nodeName);
        command.getParameters().AddParameter(userName);
        command.getParameters().AddParameter(password);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    //Method introduced to check weather a user is a windows administrator or not
    public final boolean VerfyAdministrator(String userName, String password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.VerfyAdministrator);
        command.getParameters().AddParameter(userName);
        command.getParameters().AddParameter(password);
        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Disbale logging
     *
     * @param subsystem Subsystem for which logging will be disabled
     * @param type Type of logging to disable
     */
    public final void DisableLogging(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.DisableLogging);
        command.getParameters().AddParameter(subsystem);
        command.getParameters().AddParameter(type);
        ExecuteCommandOnCacehServer(command);
    }

    public final void SynchronizeClientConfig() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.SynchronizeClientConfig);

        ExecuteCommandOnCacehServer(command);
    }

    /**
     * Update TCP cache settings that includes updated list of TCP members
     *
     * @param cacheId
     * @param props
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    public final boolean ApplyCacheConfiguration(String cacheId, CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.ApplyCacheConfiguration, 1);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(props);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(hotApply);
        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Un-register cache
     *
     * @param cacheId
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    public final void UnregisterCache(String cacheId, String partId, byte[] userId, byte[] password, Boolean isGracefulShutDown, Boolean removeServerOnly) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.UnregisterCache);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partId);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(isGracefulShutDown);
        command.getParameters().AddParameter(removeServerOnly);
        ExecuteCommandOnCacehServer(command);
    }

    public final void StartCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = this.GetManagementCommand(ManagementUtil.MethodName.StartCache);
        command.getParameters().AddParameter(cacheId);
        ExecuteCommandOnCacehServer(command);
    }

    public final void StartCache(String cacheId, String partitionId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        StartCache(cacheId, partitionId, null, null);
    }

    public final void StartCache(String cacheId, byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        StartCache(cacheId, null, null, null, null, null, null, null, userId, password, false);
    }

    public final void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        StartCache(cacheId, partitionId, null, null, null, null, null, null, userId, password, twoPhaseInitialization);
    }

    public final void StartCachePhase2(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand.Builder build = ManagementCommandProtocol.ManagementCommand.newBuilder();
        build.setMethodName(ManagementUtil.MethodName.StartCachePhase2);
        ManagementCommand command = build.build();
        command.getParameters().AddParameter(cacheId);
        ExecuteCommandOnCacehServer(command);
    }

    public final void StartCache(String cacheId, String partitionId, byte[] userId, byte[] password) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        StartCache(cacheId, partitionId, null, null, null, null, null, null, userId, password, false);
    }

    public final void StartCache(String cacheId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
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
    public final void StartCache(String cacheId, String partitionId, ItemAddedCallback itemAdded, ItemRemovedCallback itemRemoved, ItemUpdatedCallback itemUpdated, CacheClearedCallback cacheCleared, CustomRemoveCallback customRemove, CustomUpdateCallback customUpdate, byte[] userId, byte[] password, boolean twoPhaseInitialization) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StartCache, 7);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partitionId);

        command.getParameters().AddParameter(null);
        command.getParameters().AddParameter(null);
        command.getParameters().AddParameter(null);
        command.getParameters().AddParameter(null);
        command.getParameters().AddParameter(null);
        command.getParameters().AddParameter(null);

        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(twoPhaseInitialization);

        ExecuteCommandOnCacehServer(command);
    }

    public final void StopCache(String cacheId, byte[] userId, byte[] password, Boolean isGracefulShutdown) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        StopCache(cacheId, null, userId, password, isGracefulShutdown);
    }
    
    public final void StopAllCaches() throws Exception
    {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StopAllCaches, 1);
        ExecuteCommandOnCacehServer(command);
    }

    public final int GetShutdownTimeout() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetShutdownTimeout);
        return (Integer) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Stop a cache
     *
     * @param cacheId
     * @exception ArgumentNullException cacheId is a null reference (Nothing in
     * Visual Basic).
     */
    public final void StopCache(String cacheId, String partitionId, byte[] userId, byte[] password, boolean isGraceFulNodeShutdown) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException //#else
    {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StopCache, 2);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partitionId);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(isGraceFulNodeShutdown);
        ExecuteCommandOnCacehServer(command);
    }

    /**
     * Detect and return all the available NICs on this machine
     */
    public final java.util.HashMap DetectNICs() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.DetectNICs);

        Object tempVar = ExecuteCommandOnCacehServer(command);
        return (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
    }

    public final void BindToIP(BindedIpMap bindIPMap) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.BindToIP);
        command.getParameters().AddParameter(bindIPMap);
        ExecuteCommandOnCacehServer(command);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public final BindedIpMap BindedIp() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.BindedIp);
        Object tempVar = ExecuteCommandOnCacehServer(command);
        return (BindedIpMap) Common.readAs(tempVar, BindedIpMap.class);
    }

    /**
     * Gets the Max port number, among all the ports of registered caches on
     * this machine
     *
     * @return Max cluster port
     */
    @Override
    public final int GetMaxPort() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetMaxPort);
        return (Integer) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final int GetMaxSocketPort() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetMaxSocketPort);
        return (Integer) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final int GetMaxManagementPort() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetMaxManagementPort);
        return (Integer) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Checks if the current cache is a Cluster cache or not, used in NCache
     * UnReg cache tool as now UnReg is only applicable to cluster caches only
     *
     * @return true if Cluster Cache
     */
    @Override
    public final CacheStatusOnServerContainer IsClusteredCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsClusteredCache);
        command.getParameters().AddParameter(cacheId);

        return (CacheStatusOnServerContainer) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Cluster port
     * @return 'true' if the port is available, otherwise 'flase'
     */
    @Override
    public final boolean PortIsAvailable(int port) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.PortIsAvailable);
        command.getParameters().AddParameter(port);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }
    
    /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Management port
     * @return 'true' if the port is available, otherwise 'flase'
     */
    @Override
    public final boolean IsManagementPortAvailable(int port) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsManagementPortAvailable);
        command.getParameters().AddParameter(port);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

       /**
     * Checks whether the specified port is available (non-conflicting) or not
     *
     * @param port Client port
     * @return 'true' if the port is available, otherwise 'flase'
     */
    @Override
    public final boolean IsClientPortAvailable(int port) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsClientPortAvailable);
        command.getParameters().AddParameter(port);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }
    
    /**
     * Checks whether the newly added node arise port conflict or not
     *
     * @param port Cluster port
     * @return 'true' if the node is allowed, otherwise 'flase'
     */
    @Override
    public final boolean NodeIsAllowed(int port, String id) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.NodeIsAllowed);
        command.getParameters().AddParameter(port);
        command.getParameters().AddParameter(id);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    /**
     * Gets the status of NCache on this node.
     *
     * @param cacheId
     * @param partitionId
     * @return The ServerStatus.
     * @throws ManagementException
     * @throws TimeoutException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    @Override
    public final StatusInfo GetCacheStatus(String cacheId, String partitionId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheStatus);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partitionId);

        Object tempVar = ExecuteCommandOnCacehServer(command);
        return (StatusInfo) ((tempVar instanceof StatusInfo) ? tempVar : null);
    }

    /**
     * Starts monitoring the client activity.
     *
     * @throws ManagementException
     * @throws TimeoutException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    @Override
    public final void StartMonitoringActivity() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StartMonitoringActivity);

        ExecuteCommandOnCacehServer(command);
    }

    /**
     * Stops monitoring client activity.
     *
     * @throws ManagementException
     * @throws TimeoutException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    @Override
    public final void StopMonitoringActivity() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StopMonitoringActivity);

        ExecuteCommandOnCacehServer(command);
    }

    /**
     * Publishes the observed client activity into a file.
     *
     * @throws ManagementException
     * @throws TimeoutException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    @Override
    public final void PublishActivity() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.PublishActivity);

        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final boolean IsBridgeTargetCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsBridgeTargetCache);
        command.getParameters().AddParameter(cacheId);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final void ClearCacheContent(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.ClearCacheContent);
        command.getParameters().AddParameter(cacheId);

        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final boolean IsRunning(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsRunning);
        command.getParameters().AddParameter(cacheId);

        return (Boolean) ExecuteCommandOnCacehServer(command);
    }
    
    public final boolean getIsCacheRunning(String cacheId)throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException
    {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetIsCacheRunning);
        command.getParameters().AddParameter(cacheId);
        
        return (Boolean) ExecuteCommandOnCacehServer(command);
    }
    

    @Override
    public final CacheStatistics GetStatistics(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetStatistics);
        command.getParameters().AddParameter(cacheId);

        Object tempVar = ExecuteCommandOnCacehServer(command);
        return (CacheStatistics) ((tempVar instanceof CacheStatistics) ? tempVar : null);
    }

    @Override
    public final long GetCacheCount(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheCount);
        command.getParameters().AddParameter(cacheId);

        return (Long) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final void SetLocalCacheIP(String ip) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.SetLocalCacheIP);
        command.getParameters().AddParameter(ip);
        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public void BalanceDataloadOnCache(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.BalanceDataloadOnCache);
        command.getParameters().AddParameter(cacheId);
        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public boolean IsCacheRegistered(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.IsCacheRegistered);
        command.getParameters().AddParameter(cacheId);
        return (Boolean) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public final String GetLicenseKey() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetLicenseKey);
        return (String) ExecuteCommandOnCacehServer(command);
    }



    @Override
    public ConfiguredCacheInfo[] GetAllConfiguredCaches() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetAllConfiguredCaches);
        return (ConfiguredCacheInfo[]) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public CacheNodeStatistics[] GetCacheStatistics(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheStatistics);
        command.getParameters().AddParameter(cacheId);
        return (CacheNodeStatistics[]) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public com.alachisoft.tayzgrid.caching.statistics.CacheStatistics GetCacheStatistics2(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheStatistics2);
        command.getParameters().AddParameter(cacheId);
        return (com.alachisoft.tayzgrid.caching.statistics.CacheStatistics) ExecuteCommandOnCacehServer(command);
    }

    public ConfiguredCacheInfo[] GetConfiguredPartitionedReplicaCaches() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetConfiguredPartitionedReplicaCaches);
        return (ConfiguredCacheInfo[]) ExecuteCommandOnCacehServer(command);

    }

    @Override
    public Node[] GetCacheServers(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetCacheServers);
        command.getParameters().AddParameter(cacheId);
        return (Node[]) ExecuteCommandOnCacehServer(command);

    }

    @Override
    public java.util.HashMap GetSnmpPorts(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetSnmpPorts);
        command.getParameters().AddParameter(cacheId);
        return (java.util.HashMap) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public void StopServer() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.StopServer);
        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public String GetServerPlatform() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetServerPlatform);
        return (String) ExecuteCommandOnCacehServer(command);
    }

    @Override
    public com.alachisoft.tayzgrid.config.newdom.CacheServerConfig GetNewConfiguration(String cacheId) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetNewConfiguration);
        command.getParameters().AddParameter(cacheId);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (com.alachisoft.tayzgrid.config.newdom.CacheServerConfig) ((tempVar instanceof com.alachisoft.tayzgrid.config.newdom.CacheServerConfig) ? tempVar : null);

    }

    @Override
    public boolean RegisterCache(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig config, String partId, boolean overwrite, byte[] userId, byte[] password, boolean hotApply) throws ConfigurationException, UnknownHostException, ManagementException, Exception {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.RegisterCache, 2);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(config);
        command.getParameters().AddParameter(partId);
        command.getParameters().AddParameter(overwrite);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(hotApply);
        try {
            return (Boolean) ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;

    }

    @Override
    public boolean ApplyCacheConfiguration(String cacheId, com.alachisoft.tayzgrid.config.newdom.CacheServerConfig props, byte[] userId, byte[] password, boolean hotApply) throws IllegalArgumentException, IllegalAccessException, ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException, Exception {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.ApplyCacheConfiguration, 2);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(props);
        command.getParameters().AddParameter(userId);
        command.getParameters().AddParameter(password);
        command.getParameters().AddParameter(hotApply);
        return (Boolean) ExecuteCommandOnCacehServer(command);

    }

    @Override
    public NewCacheRegisterationInfo GetNewUpdatedCacheConfiguration(String cacheId, String partId, String newNode, boolean isJoining) throws ManagementException, Exception {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetNewUpdatedCacheConfiguration);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(partId);
        command.getParameters().AddParameter(newNode);
        command.getParameters().AddParameter(isJoining);

        Object tempVar = null;
        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (ManagementException ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (NewCacheRegisterationInfo) ((tempVar instanceof NewCacheRegisterationInfo) ? tempVar : null);

    }



    @Override
    public ArrayList<ClientProcessStats> GetClientProcessStats(String cacheId) throws UnknownHostException {

        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetClientProcessStats);
        command.getParameters().AddParameter(cacheId);
        Object tempVar = null;

        try {
            tempVar = ExecuteCommandOnCacehServer(command);
        } catch (Exception ex) {
            Logger.getLogger(RemoteCacheServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (ArrayList<ClientProcessStats>) tempVar;
    }

    @Override
    public void ApplyHotConfiguration(String cacheId, HotConfig hotConfig) throws CacheArgumentException, Exception {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.HotApplyConfiguration);
        command.getParameters().AddParameter(cacheId);
        command.getParameters().AddParameter(hotConfig);
        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public void setSnmpPort(String cacheid, Map<String, ArrayList<Integer>> snmpPort) throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.SetSnmpPort);
        command.getParameters().AddParameter(cacheid);
        command.getParameters().AddParameter(snmpPort);
        ExecuteCommandOnCacehServer(command);
    }

    @Override
    public String getPID() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        ManagementCommand command = GetManagementCommand(ManagementUtil.MethodName.GetProcessId);
        return (String) ExecuteCommandOnCacehServer(command);
    }

}
