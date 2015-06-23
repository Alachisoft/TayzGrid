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

package com.alachisoft.tayzgrid.socketserver;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.ClientNode;
import com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats;
import com.alachisoft.tayzgrid.common.logger.LoggerNames;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.caching.CacheRenderer;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.socketserver.statistics.PerfStatsCollector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import com.alachisoft.tayzgrid.util.Logs;
import java.io.IOException;

/**
 * An object of this class is called when Ncache service starts and stops.
 */
public final class SocketServer extends CacheRenderer
{

    private int _serverPort;
    private int _sendBuffer;
    private int _recieveBuffer;
    public static final int DEFAULT_SOCK_SERVER_PORT = 9800;
    public static final int DEFAULT_SOCK_BUFFER_SIZE = 32768;
    //Size is specified in bytes (.net stream if grow more then 1.9GB will give memory exception that's why
    //we send multiple responses each containing objects of specified size.
    // 1 GB = 1 * 1024 * 1024 * 1024 = 1073741824
    // 1 GB = 1 * MB * KB * Bytes = 1073741824
    // If not specified in Service config then we will consider 1GB packeting
    public static long CHUNK_SIZE_FOR_OBJECT = 1073741824;
    private static Logs _logger = new Logs();
    private static boolean _enableCacheServerCounters = true;
    private static PerfStatsCollector _perfStatsColl = null;
    private static LoggingInfo _serverLoggingInfo = new LoggingInfo();
    /**
     * The garbage collection timer class.
     */
    private GarbageCollectionTimer gcTimer;
    private ConnectionManager _conManager;
    LoggerNames _loggerName;

    /**
     * Initializes the socket server with the given port.
     *
     * @param port
     * @param sendBufferSize
     * @param recieveBufferSize
     */
    public SocketServer(int port, int sendBufferSize, int recieveBufferSize)
    {
        _serverPort = port;
        _sendBuffer = sendBufferSize;
        _recieveBuffer = recieveBufferSize;
        if (ServicePropValues.CacheServer_ResponseDataSize != null)
        {
            CHUNK_SIZE_FOR_OBJECT = Long.parseLong(ServicePropValues.CacheServer_ResponseDataSize);
            CHUNK_SIZE_FOR_OBJECT = CHUNK_SIZE_FOR_OBJECT < 0 ? 1024 : CHUNK_SIZE_FOR_OBJECT; //If less then zero is specified switch to default else pick the specified value
            CHUNK_SIZE_FOR_OBJECT = CHUNK_SIZE_FOR_OBJECT * 1024 * 1024; //Convert size from MB's to bytes
        }
    }

    /**
     * Initializes the socket server with the given port.
     *
     * @param port
     * @param sendBufferSize
     * @param recieveBufferSize
     */
    public SocketServer(int sendBufferSize, int recieveBufferSize)
    {
        _sendBuffer = sendBufferSize;
        _recieveBuffer = recieveBufferSize;
        if (ServicePropValues.CacheServer_ResponseDataSize != null)
        {
            CHUNK_SIZE_FOR_OBJECT = Long.parseLong(ServicePropValues.CacheServer_ResponseDataSize);
            CHUNK_SIZE_FOR_OBJECT = CHUNK_SIZE_FOR_OBJECT < 0 ? 1024 : CHUNK_SIZE_FOR_OBJECT; //If less then zero is specified switch to default else pick the specified value
            CHUNK_SIZE_FOR_OBJECT = CHUNK_SIZE_FOR_OBJECT * 1024 * 1024; //Convert size from MB's to bytes
        }
    }

    public static PerfStatsCollector getPerfStatsColl()
    {
        return _perfStatsColl;
    }

    /**
     * Gets the socket server port.
     */
    public int getServerPort()
    {
        return _serverPort;
    }

    public void setServerPort(int value)
    {
        _serverPort = value < 0 ? DEFAULT_SOCK_SERVER_PORT : value;
    }


    /**
     * Gets the send buffer size of connected client socket.
     */
    public int getSendBufferSize()
    {
        return _sendBuffer;
    }

    public void setSendBufferSize(int value)
    {
        _sendBuffer = value < 0 ? DEFAULT_SOCK_BUFFER_SIZE : value;
    }

    public static Logs getLogger()
    {
        return _logger;
    }

    /**
     * Gets the receive buffer size of connected client socket.
     */
    public int getReceiveBufferSize()
    {
        return _recieveBuffer;
    }

    public void setReceiveBufferSize(int value)
    {
        _recieveBuffer = value < 0 ? DEFAULT_SOCK_BUFFER_SIZE : value;
    }

    /**
     * Gets a value indicating whether Cache Server counters are enabled or not.
     */
    public static boolean getIsServerCounterEnabled()
    {
        return _enableCacheServerCounters;
    }

    public static void setIsServerCounterEnabled(boolean value)
    {
        _enableCacheServerCounters = value;
    }

    /**
     * Gets the PerfStatsCollector instance for this Cache Servers' counters.
     */
    /**
     * Starts the socket server.It registers some types with compact Framework, enables simple logs as well as DetailedLogs, then it checks Ncache licence information. starts
     * connection manager and perfmon counters.
     *
     * @param bindIP
     */
    public void Start(InetAddress bindIP, LoggerNames loggerName, String perfStatColInstanceName, CommandManagerType cmdMgrType) throws CacheArgumentException, Exception
    {
       
        
        FormatterServices impl = FormatterServices.getDefault();
        impl.SetCacheContext("");
        
        
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.OperationContext.class, (short) 153);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer.class, (short) 161);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk.class, (short) 162);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult.class, (short) 399);





        if (loggerName == null)
        {
            _loggerName = LoggerNames.SocketServerLogs;
        }
        else
        {
            _loggerName = loggerName;
        }

        InitializeLogging();

        _conManager = new ConnectionManager();
        _conManager.Start(bindIP, _serverPort, _sendBuffer, _recieveBuffer, _logger.getCacheLog(), cmdMgrType);
        if (_perfStatsColl == null)
        {
            _perfStatsColl = new PerfStatsCollector(perfStatColInstanceName, _serverPort, bindIP, _logger.getCacheLog());
            _perfStatsColl.InitializePerfCounters();
        }
    }

    /**
     * Starts the socket server.It registers some types with compact Framework, enables simple logs as well as DetailedLogs, then it checks Ncache licence information. starts
     * connection manager and perfmon counters.
     *
     * @param bindIP
     */
    public void Start(InetSocketAddress bindIP) throws CacheArgumentException, Exception
    {
        FormatterServices impl = FormatterServices.getDefault();
       
        
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.OperationContext.class, (short) 153);


        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer.class, (short) 161);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk.class, (short) 162);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult.class, (short) 399);

        InitializeLogging();

        _conManager = new ConnectionManager();
        _conManager.Start(bindIP, _sendBuffer, _recieveBuffer);
    }

    /**
     * Initialize logging by reading setting from application configuration file
     */
    public void InitializeLogging() throws Exception
    {
        boolean enable_logs = false;
        boolean detailed_logs = false;

        try
        {
            if (ServicePropValues.Enable_Logs != null)
            {
                enable_logs = Boolean.parseBoolean(ServicePropValues.Enable_Logs);
            }

            if (ServicePropValues.Enable_Detailed_Logs != null)
            {
                detailed_logs = Boolean.parseBoolean(ServicePropValues.Enable_Detailed_Logs);
            }

            this.InitializeLogging(enable_logs, detailed_logs);
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    /**
     * Initialize logging
     *
     * @param enable Enable error logging only
     * @param detailed Enable detailed logging
     */
    public void InitializeLogging(boolean errorOnly, boolean detailed) throws Exception
    {
        
        
        Logs localLogger = new Logs();
        try
        {
            if (errorOnly || detailed)
            {
                localLogger.setNCacheLog(new JLogger());
                localLogger.getCacheLog().Initialize(_loggerName);
                if (detailed)
                {
                    localLogger.getCacheLog().SetLevel("ALL");
                    localLogger.setIsErrorLogsEnabled(true);
                }
                else
                {
                    localLogger.getCacheLog().SetLevel("INFO");
                }
                localLogger.getCacheLog().Info("SocketServer.Start", "server started successfully");
                /**
                 * Set logging status
                 */
                if (errorOnly)
                {
                    _serverLoggingInfo.SetStatus(LoggingInfo.LoggingType.Error, LoggingInfo.LogsStatus.Enable);
                }
                if (detailed)
                {
                    _serverLoggingInfo.SetStatus(LoggingInfo.LoggingType.Detailed, (detailed ? LoggingInfo.LogsStatus.Enable : LoggingInfo.LogsStatus.Disable));
                }
                //Log4net.Initialize(NCacheLog.LoggerNames.SocketServerLogs);
                localLogger.setIsDetailedLogsEnabled(detailed);
                localLogger.setIsErrorLogsEnabled(errorOnly);
            }
            else
            {
                if (_logger.getCacheLog() != null)
                {
                    _logger.getCacheLog().Flush();
                    _logger.getCacheLog().SetLevel("OFF");
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            _logger = localLogger;
        }
    }

    /**
     * Stops the socket server. Stops connection manager.
     */
    public void Stop() throws IOException
    {
        if (_conManager != null)
        {
            _conManager.Stop();
        }
        _perfStatsColl.dispose();
    }

    public void StopListening() throws IOException
    {
        if (_conManager != null)
        {
            _conManager.StopListening();
        }
    }

    protected void finalize() throws Throwable
    {
        if (_conManager != null)
        {
            _conManager.Stop();
            _conManager = null;
        }
    }

    /**
     * Gets Server Port.
     */
    @Override
    public int getPort()
    {
        return getServerPort();
    }

    /**
     * Converts server InetAddress string to IPAddress instance and return that.
     */
    @Override
    public InetAddress getIPAddress()
    {
        try
        {
            return InetAddress.getByName(ConnectionManager.getServerIpAddress());
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    /**
     * Get current logging status for specified type
     *
     * @param subsystem
     * @param type
     * @return
     */
    @Override
    public LoggingInfo.LogsStatus GetLoggingStatus(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type)
    {
        switch (subsystem)
        {
            case Server:
                synchronized (_serverLoggingInfo)
                {
                    return _serverLoggingInfo.GetStatus(type);
                }
            case Client:
                return ConnectionManager.GetClientLoggingInfo(type);
            default:
                return LoggingInfo.LogsStatus.Disable;
        }
    }

    /**
     * Set and apply logging status
     *
     * @param subsystem
     * @param type
     * @param status
     */
    @Override
    public void SetLoggingStatus(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type, LoggingInfo.LogsStatus status) throws Exception
    {
        if (subsystem == LoggingInfo.LoggingSubsystem.Client)
        {
            boolean updateClient = false;
            if (type == type.Error || type == type.Detailed)
            {
                updateClient = ConnectionManager.SetClientLoggingInfo(type, status);
            }
            else if (type == type.Error && type == type.Detailed)
            {
                boolean updateErrorLogs = ConnectionManager.SetClientLoggingInfo(LoggingInfo.LoggingType.Error, status);
                boolean updateDetailedLogs = ConnectionManager.SetClientLoggingInfo(LoggingInfo.LoggingType.Detailed, status);
                updateClient = (updateErrorLogs || updateDetailedLogs);
            }
            if (updateClient)
            {
                ConnectionManager.UpdateClients();
            }
        }
        else if (subsystem == LoggingInfo.LoggingSubsystem.Server)
        {
            switch (status)
            {
                case Disable:

                    /**
                     * Logger not initialized yet
                     */
                    /**
                     * If error logs are disabled, then disable both
                     */
                    if (type == LoggingInfo.LoggingType.Error || type.getValue() == (LoggingInfo.LoggingType.Error.getValue() | LoggingInfo.LoggingType.Detailed.getValue()))
                    {
                        this.InitializeLogging(false, false);
                    }
                    else if (type == LoggingInfo.LoggingType.Detailed)
                    {
                        this.InitializeLogging(getLogger().getIsErrorLogsEnabled(), false);
                    }

                    break;

                case Enable:

                    boolean error = getLogger().getIsErrorLogsEnabled();
                    boolean detailed = getLogger().getIsDetailedLogsEnabled();

                    if (type == LoggingInfo.LoggingType.Error)
                    {
                        error = true;
                        detailed = false;
                    }
                    else if (type.getValue() == LoggingInfo.LoggingType.Detailed.getValue() | type.getValue() == (LoggingInfo.LoggingType.Error.getValue()
                            | LoggingInfo.LoggingType.Detailed.getValue()))
                    {
                        error = true;
                        detailed = true;
                    }

                    this.InitializeLogging(error, detailed);

                    break;
            }
        }
    }

    /**
     * Start the gc timer to collect GEN#2 after specified intervals.
     *
     * @param dueTime Time to wait (in minutes) before first collection.
     * @param period Time between two consective GEN#2 collections.
     */
    public void StartGCTimer(int dueTime, int period)
    {
        try
        {
        }
        catch (Exception e)
        {
            if (SocketServer.getLogger().getIsErrorLogsEnabled())
            {
                SocketServer.getLogger().getCacheLog().Error("SocketServer.StartGCTimer", e.toString());
            }
        }
    }

    @Override
    public java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientNode> GetClientList(String cacheId) throws UnknownHostException
    {
        java.util.ArrayList<ClientNode> clients = new java.util.ArrayList<ClientNode>();

        synchronized (ConnectionManager.ConnectionTable)
        {
            Iterator ide = ConnectionManager.ConnectionTable.entrySet().iterator();
            Map.Entry pair;
            while (ide.hasNext())
            {
                pair = (Map.Entry) ide.next();
                ClientManager clientManager = (ClientManager) ((pair.getValue() instanceof ClientManager) ? pair.getValue() : null);

                if (!clientManager.getIsDisposed() && clientManager.getCmdExecuter() != null && clientManager.getCmdExecuter().getID().toLowerCase().equals(cacheId.toLowerCase()))
                {
                    ClientNode client = new ClientNode();
                    //InetSocketAddress endPoint = (InetSocketAddress)((clientManager.getClientSocket().RemoteEndPoint instanceof InetSocketAddress) ? clientManager.getClientSocket().RemoteEndPoint : null);
                    client.setAddress(new Address(clientManager.getClientSocket().getInetAddress().getHostAddress(), clientManager.getClientSocket().getPort()));
                    client.setClientID(clientManager.getClientID());
                    if(clientManager.IsDotNetClient)
                    {
                        client.setClientContext(RtContextValue.NCACHE);
                    }
                    else
                    {
                        client.setClientContext(RtContextValue.JVCACHE);
                    }
                    clients.add(client);
                }
            }
        }
        return clients;
    }

    //CLIENTSTATS : Add method in CacheRenderer as virtual
    @Override
    public java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> GetClientProcessStats(String cacheId) throws UnknownHostException
    {
        java.util.ArrayList<ClientProcessStats> clientProcessStats = new java.util.ArrayList<ClientProcessStats>();

        synchronized (ConnectionManager.ConnectionTable)
        {
            Iterator ide = ConnectionManager.ConnectionTable.entrySet().iterator();
            Map.Entry pair;
            while (ide.hasNext())
            {
                pair = (Map.Entry) ide.next();
                ClientManager clientManager = (ClientManager) ((pair.getValue() instanceof ClientManager) ? pair.getValue() : null);
                if (clientManager.getCmdExecuter() != null && clientManager.getCmdExecuter().getID().toLowerCase().equals(cacheId.toLowerCase()))
                {
                    //InetSocketAddress endPoint = (InetSocketAddress)((clientManager.getClientSocket().RemoteEndPoint instanceof InetAddress) ? clientManager.getClientSocket().RemoteEndPoint : null);
                   Address address = new Address(clientManager.getClientSocket().getInetAddress().getHostAddress(), clientManager.getClientSocket().getPort());

                    ClientProcessStats cpStats = new ClientProcessStats(clientManager.getClientID(), address, clientManager.getClientsBytesSent(), clientManager.getClientsBytesRecieved(), ConnectionManager.getServerIpAddress());

                    clientProcessStats.add(cpStats);
                }
            }
        }
        return clientProcessStats;
    }
}
