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


import com.alachisoft.tayzgrid.common.enums.RtContextValue;


import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.GenericCopier;

import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatus;
import com.alachisoft.tayzgrid.common.monitoring.ClientNode;
import com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.EventViewerEvent;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.common.monitoring.ServerNode;
import com.alachisoft.tayzgrid.common.rpcframework.TargetMethodAttribute;
import com.alachisoft.tayzgrid.common.util.ManagementUtil.MethodName;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class MonitorServer //extends MarshalByRefObject implements IMonitorServer
{

    private Node[] _totalServerNodes;
    private static java.util.ArrayList<ServerNode> _runningServerNodes = new java.util.ArrayList<ServerNode>();
    private static Object _sync_lock = new Object();
    private static Object _syncLockCPUUsage = new Object();
    private String _cacheId;

    public MonitorServer()
    {
    }

    @TargetMethodAttribute(privateMethod = MethodName.InitializeMonitor, privateOverload = 1)
    public final void Initialize(String cacheId)
    {
        _cacheId = cacheId;
    }

    ///#region IMonitorServer Members
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheStatistics, privateOverload = 1)
    public final CacheNodeStatistics[] GetCacheStatistics() throws GeneralFailureException, OperationFailedException, CacheException
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        return HostServer.getInstance().GetCacheStatistics(_cacheId);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClusterNIC, privateOverload = 1)
    public final String GetClusterNIC() throws GeneralFailureException, OperationFailedException, CacheException
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        CacheNodeStatistics[] statsList = HostServer.getInstance().GetCacheStatistics(_cacheId);
        if (statsList != null && statsList.length > 0)
        {
            CacheNodeStatistics stats = statsList[0];
            if (stats.getStatus() == CacheNodeStatus.Running || stats.getStatus() == CacheNodeStatus.InStateTransfer)
            {
                return GetNICForIP(stats.getNode().getAddress().getIpAddress().toString());
            }
        }
        return null;
    }
    
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheBinding, privateOverload = 1)
    public final HashMap GetCacheBinding(String cacheId)
    {
      
       return CacheServer.GetBinding(cacheId);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetSocketServerNIC, privateOverload = 1)
    public final String GetSocketServerNIC()
    {
        InetAddress ip = null;
        if (CacheServer.getInstance() != null && CacheServer.getInstance().getRenderer() != null)
        {
            ip = CacheServer.getInstance().getRenderer().getIPAddress();
        }

        return ip != null ? GetNICForIP(ip.toString()) : null;
    }

    /**
     *
     *
     * @param ip
     * @return
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetNICForIP, privateOverload = 1)
    public final String GetNICForIP(String ip)
    {
        if (ip == null)
        {
            return null;
        }
        String nic = null;
        try
        {

            // Detecting Network Interface Cards with enabled IPs through WMI:
            NetworkInterface.getByInetAddress(InetAddress.getByName(ip));         
        }
        catch (Exception e)
        {
        }
        return nic;
    }

    /**
     * Will register the EntryWritten event so that can return the event log entry from this point onwards
     *
     * @param sources
     */
    @TargetMethodAttribute(privateMethod = MethodName.RegisterEventViewerEvents, privateOverload = 1)
    public final void RegisterEventViewerEvents(String[] sources)
    {
        UnRegisterEventViewerEvents();
    }

    /**
     * Unregister the EntryWritten event
     *
     * @param sources
     */
    @TargetMethodAttribute(privateMethod = MethodName.UnRegisterEventViewerEvents, privateOverload = 1)
    public final void UnRegisterEventViewerEvents()
    {
        //Not Supported
    }
    @TargetMethodAttribute(privateMethod = MethodName.GetLatestEvents, privateOverload = 1)
    public final EventViewerEvent[] GetLatestEvents() //throws CloneNotSupportedException, UnknownHostException
    {
        EventViewerEvent[] eventList = new EventViewerEvent[EventLogger.getEventList().length];
        eventList = EventLogger.getEventList();
        EventLogger.clearEvents();
        
        return eventList != null ? eventList : null;
    }

    /**
     * Gets the list of configured servers irrespective of their running/stopped status.
     *
     * @param cacheId
     * @return
     * @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetCacheServers, privateOverload = 1)
    public final Node[] GetCacheServers() throws UnknownHostException
    {
        return CacheServer.getInstance().GetCacheServers(_cacheId);
    }

    /**
     *
     * @return @throws UnknownHostException
     */
    @TargetMethodAttribute(privateMethod = MethodName.GetUpdatedCacheServers, privateOverload = 1)
    public final Node[] GetUpdatedCacheServers() throws UnknownHostException
    {        
        Node[] serverNodes = HostServer.getInstance().GetCacheServers(_cacheId);
        synchronized (_sync_lock)
        {
            if (this._totalServerNodes != null && this._totalServerNodes.length != serverNodes.length)
            {
                this._totalServerNodes = serverNodes;
                return serverNodes;
            }
            else if (this._totalServerNodes == null)
            {
                this._totalServerNodes = serverNodes;
                return serverNodes;
            }
            for (Node server : serverNodes)
            {
                if (!(Arrays.asList(this._totalServerNodes).contains(server)))
                {
                    this._totalServerNodes = serverNodes;
                    return serverNodes;
                }
            }
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetRunningCacheServers, privateOverload = 1)
    public final ServerNode[] GetRunningCacheServers()
    {
        if(HostServer.getInstance()!=null)
        {
            System.out.println("hi");
        }else{
            System.out.println("null");
        }
        
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        
        ArrayList<ServerNode> serverNodes = HostServer.getInstance().GetRunningCacheServers(_cacheId);
        return serverNodes != null ? serverNodes.toArray(new ServerNode[serverNodes.size()]) : null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetUpdatedRunningCacheServers, privateOverload = 1)
    public final ServerNode[] GetUpdatedRunningCacheServers()
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        java.util.ArrayList<ServerNode> serverNodes = HostServer.getInstance().GetRunningCacheServers(_cacheId);
        ServerNode[] cacheServers = serverNodes != null ? serverNodes.toArray(new ServerNode[serverNodes.size()]) : null;
        synchronized (_sync_lock)
        {
            if (_runningServerNodes.size() != serverNodes.size())
            {
                _runningServerNodes = serverNodes;
                return cacheServers;
            }
            for (ServerNode server : serverNodes)
            {
                if (!_runningServerNodes.contains(server))
                {
                    _runningServerNodes = serverNodes;
                    return cacheServers;
                }
            }
        }
        return null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheClients, privateOverload = 1)
    public final ClientNode[] GetCacheClients() throws UnknownHostException
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        ArrayList<ClientNode> clientNodes = HostServer.getInstance().GetCacheClients(_cacheId);
        return clientNodes != null ? clientNodes.toArray(new ClientNode[clientNodes.size()]) : null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetUpdatedCacheClients, privateOverload = 1)
    public final ClientNode[] GetUpdatedCacheClients() throws UnknownHostException
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        
        java.util.ArrayList<ClientNode> clientNodes = HostServer.getInstance().GetCacheClients(_cacheId);
        java.util.ArrayList<ClientNode> returnNodes = new java.util.ArrayList<ClientNode>();

        synchronized (_sync_lock)
        {
            java.util.HashMap<String, ClientNode> clientNodePerIP = new java.util.HashMap<String, ClientNode>();

            for (ClientNode node : clientNodes)
            {
                if(node.getClientContext()==RtContextValue.NCACHE)
                {
                String key = node.getAddress().getIpAddress().toString();
                if (!clientNodePerIP.containsKey(key))
                {
                    clientNodePerIP.put(key, node);
                    returnNodes.add(node);
                }
                }
                else
                {
                    returnNodes.add(node);
                }
            }
        }

        return returnNodes != null ? returnNodes.toArray(new ClientNode[returnNodes.size()]) : null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetClientProcessStats, privateOverload = 1)
    public final ClientProcessStats[] GetClientProcessStats() throws UnknownHostException
    {
        if(_cacheId == null){
            _cacheId = HostServer.getInstance().getHostingCacheName();
        }
        ArrayList<ClientProcessStats> clientProcessStats = HostServer.getInstance().GetClientProcessStats(_cacheId);
        return clientProcessStats != null ? clientProcessStats.toArray(new ClientProcessStats[clientProcessStats.size()]) : null;
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetAllConfiguredCaches, privateOverload = 1)
    public final ConfiguredCacheInfo[] GetAllConfiguredCaches()
    {
        if(CacheServer.getInstance() == null)
            return HostServer.getInstance().GetAllConfiguredCaches();
        else
            return CacheServer.getInstance().GetAllConfiguredCaches();
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetCacheConfigurationInfo, privateOverload = 1)
    public final ConfiguredCacheInfo GetCacheConfigurationInfo()
    {
        return CacheServer.getInstance().GetCacheConfigurationInfo(_cacheId);
    }

    @TargetMethodAttribute(privateMethod = MethodName.GetPercentageCPUUsage, privateOverload = 1)
    public final int GetPercentageCPUUsage()
    {
        synchronized (_syncLockCPUUsage)
        {

            try
            {
                return 0;
            }
            catch (Exception exception)
            {
                return -3;
            }
        }
    }
    @TargetMethodAttribute(privateMethod = MethodName.GetSnmpPorts, privateOverload = 1)
    public final java.util.HashMap GetSnmpPort(String cacheid)
    {
        return CacheServer.getInstance().GetSnmpPorts(cacheid);
    }

}
