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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.communication.ClientConfiguration;
import com.alachisoft.tayzgrid.communication.RemoteServer;
import java.util.ArrayList;
import java.util.HashMap;


public class CacheInitParams
{

    private CacheMode _mode = CacheMode.Default;
    private String _server = null;
    private int _port = 9600;
    /* no user choice should be given for client optimization i-e multiPartitionConnection will be true by defult */
    private boolean _multiPartitionConnection = true;
    
 
    
    /**
     * 
     * Operation Timeout value
     */
    int _opTimeout = 90;  //90 sec
    /**
     * Connection Timeout value
     */
    int _connectionTimeout = 5000;  //5 sec
    private int _connectionRetries = 5;
    /**
     * interval between retry connection
     */
    int _retryInterval = 1000;   //1 sec
    int _retryConnectionDelay = 600*1000; //600 sec or 10 mins
    private int _socketPort = 0;
    private boolean _balanceNodes = true;
    
    private HashMap _dirtyFlags = new HashMap();
    
    //key for dirty flag hashtable: 
    
    private final String OPTIMEOUT = "opTimeOut";
    private final String CONNECTIONTIMEOUT = "connectionTimeOut";
    private final String CONNECTIONRETRIES = "connectionRetries";
    private final String RETRYINTERVAL = "retryInterval";
    private final String RETRYCONNECTIONDELAY = "retryConnectionDelay";
    private final String LOADBALANCE = "balanceNodes";
    private final String PORT = "port";
    private final String DEFAULTREADTHRUPROVIDER="defaultReadThruProvider";
    private final String DEFAULTWRITETHRUPROVIDER="defaultWriteThruProvider";
    private final String SERVERLIST="serverlist";
    private final String BINDIP="bindIP";
    private final String SOCKETPORT="socketPort";
    
    
    private String _defaultReadThruProvider;
    private String _defaultWriteThruProvider;
    private String _bindIP;

    private ArrayList _serverList = new ArrayList();
        
        
    public final CacheServerInfo[] getServerList()
    {
            CacheServerInfo[] _returnList = new CacheServerInfo[_serverList.size()];
            int _serverCount = 0;

            for (Object temp : _serverList.toArray())
            {
                    _returnList[_serverCount] = new CacheServerInfo();
                    _returnList[_serverCount].setServerInfo((RemoteServer)temp);
                    _serverCount++;
            }

            return _returnList;
    }

    public final void setServerList(CacheServerInfo[] value)
    {
            if (_serverList.size() > 0)
            {
                    _serverList.clear();
            }

            for (CacheServerInfo temp : value)
            {
                    if (!_serverList.contains(temp))
                    {
                            _serverList.add(temp.getServerInfo());
                    }
            }
            _dirtyFlags.put(SERVERLIST, true);

    }    
    /**
     * @return Gets the server clients will connect to.
     */
    public String getServer()
    {
        return _server;
    }
    
    public int getServerListSize()
    {
        return _serverList.size();
    }

    /**
     *
     * @param key
     * @param value
     */
    void add(String key, Object value)
    {
        if (_dirtyFlags.containsKey(key))
        {
            _dirtyFlags.remove(key);
            _dirtyFlags.put(key, value);
        }
        else
        {
            _dirtyFlags.put(key, value);
        }
    }

    /**
     * @param server Sets the server clients will connect to.
     */
    public void setServer(String server)
    {
        _server = server; 
    }

    /**
     * When this flag is set, client tries to connect to the optimum server in terms of number of connected clients. This way almost equal number of clients are connected to every
     * node in the clustered cache and no single node is over burdened.
     *
     * @return boolean true will Load Balance is set to true otherwise false
     */
    public boolean getLoadBalance()
    {
        return this._balanceNodes;
    }

    /**
     * When this flag is set, client tries to connect to the optimum server in terms of number of connected clients. This way almost equal number of clients are connected to every
     * node in the clustered cache and no single node is over burdened.
     *
     * @param balanceNodes if true balance client tries to connect to the optimum server
     */
    public void setLoadBalance(boolean balanceNodes)
    {
        this._balanceNodes = balanceNodes;
        add(LOADBALANCE, true);
    }

    /**
     * *
     * Gets the port on which the clients will connect to a server.
     *
     * @return the port number
     */
    public int getPort()
    {
        return _port;
    }

    /**
     * *
     * Set the port on which the clients will connect to a server.
     *
     * @param port specify port number
     */
    public void setPort(int port)
    {
        if (port > 0)
        {
            _port = port;
            add(PORT, true);
        }
    }

    public CacheMode getMode()
    {
        return _mode;
    }

    public void setMode(CacheMode mode)
    {
        _mode = mode;
    }

    /**
     * *
     * Clients operation timeout specified in seconds. Clients wait for the response from the server for this time. If the response is not received within this time, the operation
     * is not successful. Based on the network conditions, OperationTimeout value can be adjusted. The default value is 90 seconds.
     *
     * @return the client request time out value
     */
    public int getClientRequestTimeOut()
    {
        return _opTimeout;
    }

    /**
     * *
     * Clients operation timeout specified in seconds. Clients wait for the response from the server for this time. If the response is not received within this time, the operation
     * is not successful. Based on the network conditions, OperationTimeout value can be adjusted.
     *
     * @param opTimeout Client operation time out value
     */
    public void setClientRequestTimeOut(int opTimeout)
    {
        _opTimeout = opTimeout;
        if (opTimeout >= 0)
        {
            add(OPTIMEOUT, true);
        }
    }

    /**
     * *
     * Get Client's connection timeout specified in seconds.
     *
     * @return Connection time out value
     */
    public int getConnectionTimeout()
    {
        return _connectionTimeout / 1000;
    }

    /**
     * *
     * Set Client's connection timeout specified in seconds.
     *
     * @param connectionTimeout value
     */
    public void setConnectionTimeout(int connectionTimeout)
    {
        _connectionTimeout = connectionTimeout * 1000;
        if (connectionTimeout >= 0)
        {
            add(CONNECTIONTIMEOUT, true);
        }
    }

    /**
     * *
     * Set Client's connection retry delay in seconds.
     *
     * @param retryConnectionDelay value
     */
    public void setRetryConnectionDelay(int retryConnectionDelay)
    {
        _retryConnectionDelay = retryConnectionDelay * 1000;
        if (retryConnectionDelay >= 0)
        {
            add(RETRYCONNECTIONDELAY, true);
        }
    }

    /**
     * *
     * Get Client's connection retry delay in seconds.
     *
     * @return Connection retry delay value
     */
    public int getRetryConnectionDelay()
    {
        return _retryConnectionDelay / 1000;
    }

    /**
     * *
     * Get Number of tries to re-establish a broken connection between client and server.
     *
     * @return value
     */
    public int getConnectionRetries()
    {
        return _connectionRetries;
    }

    /**
     * *
     * Set Number of tries to re-establish a broken connection between client and server.
     *
     * @param connectionRetries value
     */
    public void setConnectionRetries(int connectionRetries)
    {
        _connectionRetries = connectionRetries;
        if (connectionRetries >= 0)
        {
            add(CONNECTIONRETRIES, true);
        }
    }


    /**
     * *
     * Set Time in seconds to wait between two connection retries.
     *
     * @param retryInterval value
     */
    public void setRetryInterval(int retryInterval)
    {
        _retryInterval = retryInterval * 1000;
        add(RETRYINTERVAL, true);
    }

    public boolean IsSet(String paramId)
    {
        if (_dirtyFlags == null || _dirtyFlags.size() == 0)
        {
            return false;
        }

        if (_dirtyFlags.containsKey(paramId))
        {
            boolean val = ((Boolean) _dirtyFlags.get(paramId)).booleanValue();
            return val;
        }

        return false;
    }
    
    /**
     * gets interval value between retry connection in seconds
     *
     * @return
     */
    public int getRetryInterval()
    {
        return _retryInterval / 1000;
    }

  

    /**
     *
     * @param cacheId
     */
    public void Initialize(String cacheId) throws Exception
    {
        boolean useDefault = false;
        ClientConfiguration config = null;
        int retries = 3;
        setLoadBalance(_balanceNodes);
        while (true)
        {
            try
            {
                config = new ClientConfiguration(cacheId);
                config.LoadConfiguration();
                break;
            }
            catch (Exception ie)
            {
                if (--retries == 0)
                {
                    useDefault = true;
                    break;
                }

                try
                {
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                }
            }
        }
        if (!useDefault)
        {
            if (!IsSet(OPTIMEOUT))
            {
                this.setClientRequestTimeOut(config.getTimeout());
            }
            if (!IsSet(CONNECTIONTIMEOUT))
            {
                this.setConnectionTimeout(config.getConnectionTimeout());
            }
            if (!IsSet(CONNECTIONRETRIES))
            {
                this.setConnectionRetries(config.getConnectionRetries());
            }
            if (!IsSet(RETRYINTERVAL))
            {
                this.setRetryInterval(config.getRetryInterval());
            }
            if (!IsSet(RETRYCONNECTIONDELAY))
            {
                this.setRetryConnectionDelay(config.getRetryConnectionDelay());
            }
            
            if(!IsSet(BINDIP))
            {
                this.setBindIP(config.getBindIP());
            }
            
            if(!IsSet(DEFAULTREADTHRUPROVIDER))
            {
                this.setDefaultReadThruProvider(config.getDefaultReadThru());
            }
            
            if(!IsSet(DEFAULTWRITETHRUPROVIDER))
            {
                this.setDefaultWriteThruProvider(config.getDefaultWriteThru());
            }
            
            if(!IsSet(SERVERLIST))
            {
                //this.setServerList((CacheServerInfo [])config.getServerList().clone());
                this._serverList=config.getServerList();
            }
            
            if (!IsSet(LOADBALANCE))
            {
                this.setLoadBalance(config.getBalanceNodes());
            }

            if (!IsSet(PORT))
            {
                this.setPort(config.getServerPort());
            }
            
            this._multiPartitionConnection = true;
        }
    }
    
    /**
     * @return the _defaultReadThruProvider
     */
    public String getDefaultReadThruProvider() {
        return _defaultReadThruProvider;
    }

    /**
     * @param defaultReadThruProvider the _defaultReadThruProvider to set
     */
    public void setDefaultReadThruProvider(String defaultReadThruProvider) {
        this._defaultReadThruProvider = defaultReadThruProvider;
        _dirtyFlags.put(DEFAULTREADTHRUPROVIDER,true);
    }

    /**
     * @return the _defaultWriteThruProvider
     */
    public String getDefaultWriteThruProvider() {
        return _defaultWriteThruProvider;
    }

    /**
     * @param defaultWriteThruProvider the _defaultWriteThruProvider to set
     */
    public void setDefaultWriteThruProvider(String defaultWriteThruProvider) {
        this._defaultWriteThruProvider = defaultWriteThruProvider;
        _dirtyFlags.put(DEFAULTWRITETHRUPROVIDER,true);
    }

    /**
     * @return the _bindIP
     */
    public String getBindIP() {
        return _bindIP;
    }

    /**
     * @param bindIP the _bindIP to set
     */
    public void setBindIP(String bindIP) {
        this._bindIP = bindIP;
        _dirtyFlags.put(BINDIP, true);
    }
    
    public Object Clone()
    {
        CacheInitParams _cloneParam=new CacheInitParams();
        
        synchronized(this)
        {
            _cloneParam._dirtyFlags=(HashMap) this._dirtyFlags.clone();
            _cloneParam.setClientRequestTimeOut(this.getClientRequestTimeOut());
            _cloneParam.setConnectionRetries(this.getConnectionRetries());
            _cloneParam.setConnectionTimeout(this.getConnectionTimeout());
            _cloneParam.setDefaultReadThruProvider(this.getDefaultReadThruProvider());
            _cloneParam.setDefaultWriteThruProvider(this.getDefaultWriteThruProvider());
        
            _cloneParam.setLoadBalance(this.getLoadBalance());
            _cloneParam.setMode(this.getMode());
            _cloneParam.setPort(this.getPort());
            _cloneParam.setBindIP(this.getBindIP());
            _cloneParam.setRetryConnectionDelay(this.getRetryConnectionDelay());
            _cloneParam.setRetryInterval(this.getRetryInterval());
            _cloneParam.setServer(this.getServer());
           _cloneParam.setServerList((CacheServerInfo [])this.getServerList());

        }
        return _cloneParam;
    }

    /**
     * @return the _socketPort
     */
    public int getClientPort() {
        return _socketPort;
    }

    /**
     * @param _socketPort the _socketPort to set
     */
    public void setClientPort(int _socketPort) {
        this._socketPort = _socketPort;
        add(SOCKETPORT,true);
    }

}




abstract class Params
{

    protected boolean _userDefined = false;

    public abstract boolean isUserDefined();
}


        
