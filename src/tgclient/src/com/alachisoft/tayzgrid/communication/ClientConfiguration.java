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

package com.alachisoft.tayzgrid.communication;

import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.util.DirectoryUtil;
import com.alachisoft.tayzgrid.web.caching.CacheInitParams;
import com.alachisoft.tayzgrid.web.caching.CacheServerInfo;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public final class ClientConfiguration
{

    String _configFile = "client.conf";
    String _dirName="Config"; 
    ArrayList _servers = new ArrayList();
    int _currentServer = 0;
    String _cacheId;
    private HashMap _compactTypes = new HashMap();
    private int _serverPort = 9600;//9800;
    private int _timeout = 90;
    private int _connectionRetries = 5;
    private int _retryInterval = 1;
    private int _retryConnectionDelay = 600*1000;
    private int _connectionTimeout = 5*1000;
    private static String _installDir = null;
    private boolean _balanceNodes = true;
    private String _defaultReadThruProvider = null;
    private String _defaultWriteThruProvider = null;
    private String _localServerIP;
    private boolean _loadCacheConfiguration = true;
    public CacheInitParams initParam;
    java.util.Hashtable _mappedServer = new java.util.Hashtable();
    private boolean _loadServersFromConfigFile = true;
    private int _itemSizeThreshHold = 0;
    private boolean _importHashmap = true ;
    int _retries=5;
    private int _jvcServerPort = 9600;
    private int _socketPort = 0;
    private RtContextValue _serverRuntimeContext= RtContextValue.JVCACHE;
    private int _daemonPort;
    
    public static final String OPTIMEOUT = "opTimeOut";
    public static final String CONNECTIONTIMEOUT = "connectionTimeOut";
    public static final String CONNECTIONRETRIES = "connectionRetries";
    public static final String RETRYINTERVAL = "retryInterval";
    public static final String LOADBALANCE = "balanceNodes";
    public static final String PORT = "port";
    public static final String CACHESYNCMODE = "cacheSyncMode";
    public static final String RETRYCONNECTIONDELAY = "retryConnectionDelay"; //[KS: for connection retry delay]
    public static final String DEFAULTREADTHRUPROVIDER = "defaultReadThruProvider";
    public static final String DEFAULTWRITETHRUPROVIDER = "defaultWriteThruProvider";
    public static final String SERVERLIST = "serverlist";
    public static final String BINDIP = "bindIP";
    public static final String SOCKETPORT = "socketPort";
    
    public ClientConfiguration(String cacheId) throws ConfigurationException
    {
        _cacheId = cacheId;
        initParam=null;
    }
    
    public ClientConfiguration(String cacheId,CacheInitParams param)
    {
       _cacheId=cacheId;
       initParam=(CacheInitParams)param.Clone();
       
       
       if(initParam!=null)
       {
           if(initParam.getServerList()!=null && initParam.getServerListSize() > 0)
           {
             for (CacheServerInfo serverInfo : initParam.getServerList())
                  {
		     addServer(serverInfo.getServerInfo());
                  }
             _loadServersFromConfigFile=false;
           }
           else if(initParam.getServer() !=null)
           {
               if(initParam.getClientPort() > 0 )
                   addServer(new RemoteServer(initParam.getServer(),initParam.getClientPort()));
               else
                   addServer(new RemoteServer(initParam.getServer(),initParam.getPort()));
           }
       }
    }

    public int getServerCount()
    {
        return _servers.size();
    }

    public ArrayList getServerList()
    {
        return (ArrayList) this._servers.clone();
    }

    public void addServer(RemoteServer server)
    {
        if (_servers != null && server != null)
        {
            synchronized (_servers)
            {
                if (!_servers.contains(server))
                {
                    RemoteServer rm=getMappedServer(server.getName(),server.getPort());
                    _servers.add(rm);
                }
            }
        }
    }

    public void RemoveServer(RemoteServer server)
    {
        if (_servers != null && server != null)
        {
            synchronized (_servers)
            {

                if (_servers.contains(server))
                {
                    RemoteServer existingServer = (RemoteServer) _servers.get(_servers.indexOf(server));
                    if (!existingServer.isUserProvided())
                    {
                        if (_currentServer == (_servers.size() - 1))
                        {
                            _currentServer--;
                        }
                        _servers.remove(server);
                    }

                }
            }
        }
    }

    private String getConfigPath() throws Exception
    {
        String separator = System.getProperty("file.separator");
        String path = "";

        //Making SetConfigPath property available on Windows.
        path = TayzGrid.getConfigPath();
        if (path != null && path.equalsIgnoreCase("") == false)
        {
            return path.concat(separator + _configFile);
        }

        if (System.getProperty("os.name").toLowerCase().startsWith("win"))
        {
            //<editor-fold defaultstate="collapsed" desc=" Library Path for Windows ">
            //get current execution path
            path = System.getProperty("user.dir");
            if (path != null)
            {
                if (!path.endsWith(separator))
                {
                    path = path.concat(separator);
                }
                path = path.concat(_configFile);

                if (fileExists(path))
                {
                    return path;
                }
            }

            
            path = getInstallDir();
            if (path != null)
            {
                if (path != null)
                {
                    if (!path.endsWith(separator))
                    {
                        path = path.concat(separator);
                    }

                    path = path.concat("config" + separator + _configFile);

                    if (fileExists(path))
                    {
                        return path;
                    }
                }
            }

            //</editor-fold>
        }
        else
        {
            //<editor-fold defaultstate="collapsed" desc=" Library Path for linux ">
            
            path = TayzGrid.getConfigPath();
            if (path != null && path.equalsIgnoreCase("") == false)
            {
                return path.concat(separator + _configFile);
            }
            else
            {
                path = ServicePropValues.getTGHome();
                if (path != null && path.equalsIgnoreCase("") == false)
                {
                    path = path.concat(separator + "config");
                }
                else
                {
                    path = ServicePropValues.INSTALLDIR_DIR;
                    if (path != null && path.equalsIgnoreCase("") == false)
                    {
                        path = path.concat(separator + "config");
                    }
                    //</editor-fold>
                }

                if (path == null || path.equalsIgnoreCase("") == true)
                {
                    path = "/opt/tayzgrid/config/" + _configFile;
                    return path;
                }
            }
        }
        if (path == null)
        {
            throw new Exception("Unable to find " + _configFile + "; please reset Enviorment variables");
        }
        return path.concat(separator + _configFile);
    }

    /**
     * Determine whether file exists at specified path
     *
     * @param path File path to be checked
     * @return True if file exists, false otherwise
     */
    private boolean fileExists(String path)
    {
        File check = new File(path);
        try
        {
            return check.exists();
        }
        catch (SecurityException se)
        {
        }
        return false;
    }

    /**
     * Read windows registry and return ncache installation directory
     *
     * @return Ncache installation directory path
     */
    private String getInstallDir()
    {
        if (_installDir == null)
        {
            _installDir = ServicePropValues.getTGHome();
        }
        return _installDir;
    }
    
    public RemoteServer getMappedServer(String ip,int port)
        {
            RemoteServer mapping=null;
            if (_mappedServer != null || !_mappedServer.isEmpty())
            {
                for (Iterator it = _mappedServer.keySet().iterator(); it.hasNext();) {
                    RemoteServer rm = (RemoteServer) it.next();
                    if (rm.getName().equals(ip))
                    {
                        mapping = new RemoteServer();
                        mapping = (RemoteServer)_mappedServer.get(rm);
                    }
                }
            }

            if (mapping == null)
            {
                mapping = new RemoteServer(ip, port);
            }

            return mapping;

        }
    

    public void LoadConfiguration() throws ConfigurationException
    {
        try
        {
            if (_cacheId == null && _loadCacheConfiguration)
            {
                return;
            }
            String path = getConfigPath();
            File f = new File(path);
            if (!f.exists())
            {
                throw new Exception("'client.conf' not found or does not contain server information");
            }

            Document response = null;

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;

            try
            {
                builder = builderFactory.newDocumentBuilder();
            }
            catch (ParserConfigurationException ex)
            {
            }

            response = builder.parse(f);
            
            NodeList registeredCaches = response.getElementsByTagName("cache");
            boolean cacheExists = false;
            for (int i = 0; i < registeredCaches.getLength(); i++) 
            {
                if(registeredCaches.item(i).getAttributes().getNamedItem("id").getTextContent().equalsIgnoreCase(_cacheId))
                {
                    cacheExists = true;
                    break;
                }
            }
            if(!cacheExists)
            {throw new Exception("'client.conf' not found or does not contain server information");}
            
            boolean serverPortFound = false;
            NodeList serverPortNL = response.getElementsByTagName("cache-server");
            if (serverPortNL != null)
            {
                Node ncacheServerDetails = serverPortNL.item(0);
                if (ncacheServerDetails == null)
                {
                    throw new Exception("cache-server options not found in " + _configFile + ".");
                }

                NamedNodeMap attributes = ncacheServerDetails.getAttributes();
                if (attributes != null)
                {
                    
                    if(initParam!=null && initParam.IsSet(PORT))
                    {
                        _serverPort=initParam.getPort();
                    }
                    else
                    {
                        
                        Node portNode = attributes.getNamedItem("port");
                        
                        if(portNode != null){
                            _daemonPort = Integer.parseInt(portNode.getTextContent());                            
                            setServerPort(_daemonPort);
                        }
                        Node jvcPortNode = attributes.getNamedItem("jvc-port");
                        
                        if(jvcPortNode != null)
                        {
                            setJvcServerPort(Integer.parseInt(jvcPortNode.getTextContent()));
                        }
                    }
                    
                    if(initParam!=null && initParam.IsSet(OPTIMEOUT))
                    {
                        _timeout=initParam.getClientRequestTimeOut(); 
                    }
                    
                    else
                    {
                        Node timeoutNode = attributes.getNamedItem("client-request-timeout");
                        if (timeoutNode != null)
                        {
                            int val=Integer.parseInt(timeoutNode.getTextContent());
                            setTimeout(val); 
                        }
                    }
                    
                    if(initParam != null && initParam.IsSet(CONNECTIONRETRIES))
                    {
                        _retries = initParam.getConnectionRetries();
                    }
                    else
                    {
                        Node connectionRetryNode = attributes.getNamedItem("connection-retries");
                        if (connectionRetryNode != null)
                        {
                            setConnectionRetries(Integer.parseInt(connectionRetryNode.getTextContent()));
                        }
                    }
                    
                    
                    
                    
                    if(initParam != null && initParam.IsSet(RETRYINTERVAL))
                    {
                        _retryInterval=initParam.getRetryInterval(); 
                    }                   
                    else
                    {
                        Node retryIntervalNode = attributes.getNamedItem("retry-interval");
                        if (retryIntervalNode != null)
                        {
                            int val=Integer.parseInt(retryIntervalNode.getTextContent());
                            setRetryInterval(val);
                        }
                    }
                    
                    if(initParam != null && initParam.IsSet(CONNECTIONTIMEOUT))
                    {
                        _connectionTimeout=initParam.getConnectionTimeout()*1000;
                    }
                    
                    else
                    {
                        Node connectionTimeoutNode = attributes.getNamedItem("connection-timeout");
                        if (connectionTimeoutNode != null)
                        {
                            int val=Integer.parseInt(connectionTimeoutNode.getTextContent());
                            val=val*1000;
                            setConnectionTimeout(val);
                        }
                    }

                  if (initParam != null && initParam.IsSet(RETRYCONNECTIONDELAY))
                     {
                          _retryConnectionDelay = initParam.getRetryConnectionDelay()*1000; 
                     }                  
               
                  else
                  {
                        Node retryConnectionDelayNode = attributes.getNamedItem("retry-connection-delay");
                        if (retryConnectionDelayNode != null)
                        {
                            int val=Integer.parseInt(retryConnectionDelayNode.getTextContent());
                            val=val*1000;
                            setRetryConnectionDelay(val);
                        }
                  }
                  
                  

                    Node localServerIPNode = attributes.getNamedItem("local-server-ip");
                    if (localServerIPNode != null)
                    {
                        setLocalServerIP((localServerIPNode.getTextContent()));
                    }
                }
                
                serverPortFound=true;
            }

            if(!_loadCacheConfiguration) return;
            
            
            NodeList cacheNL = response.getElementsByTagName("cache");
            if (cacheNL.getLength() == 0)
            {
                throw new Exception("'client.conf' not found or does not contain server information");
            }
            NodeList serversNL = null;
            Node cacheN = null;

            for (int i = 0; i < cacheNL.getLength(); i++)
            {
                cacheN = cacheNL.item(i);
                if (cacheN.hasAttributes())
                {
                    if (cacheN.getAttributes().getNamedItem("id").getNodeValue().equalsIgnoreCase(_cacheId))
                    {
                        serversNL = cacheN.getChildNodes();
  
                        if(initParam != null && initParam.IsSet(LOADBALANCE))
                        {
                            _balanceNodes=initParam.getLoadBalance();
                        }
                        else
                        {
                            if (cacheN.getAttributes().getNamedItem("load-balance") != null)
                            {
                                _balanceNodes = Boolean.parseBoolean(cacheN.getAttributes().getNamedItem("load-balance").getNodeValue());
                            }
                        }
                        
                        if(initParam != null && initParam.IsSet(SOCKETPORT))
                        {
                            _socketPort = initParam.getClientPort();
                        }
                        else
                        {

                            Node socketPort = cacheN.getAttributes().getNamedItem("client-port");
                            if(socketPort != null)
                            {
                                setSocketPort(Integer.parseInt(cacheN.getAttributes().getNamedItem("client-port").getNodeValue()));
                                
                                if (getSocketPort() > 0){
                                       setServerPort(getSocketPort());
                                    }
                            }
                        }
                        
                        if(initParam != null && initParam.IsSet(DEFAULTREADTHRUPROVIDER))
                        {
                            _defaultReadThruProvider=initParam.getDefaultReadThruProvider();
                        }
                        
                        else
                        {
                            if (cacheN.getAttributes().getNamedItem("default-readthru-provider") != null)
                            {
                                _defaultReadThruProvider = cacheN.getAttributes().getNamedItem("default-readthru-provider").getNodeValue();
                            }
                        }
                        
                        if(initParam !=null && initParam.IsSet(DEFAULTWRITETHRUPROVIDER))
                        {
                            _defaultWriteThruProvider=initParam.getDefaultWriteThruProvider();
                        }
                    
                        else
                        {
                            if (cacheN.getAttributes().getNamedItem("default-writethru-provider") != null)
                            {
                                _defaultWriteThruProvider = cacheN.getAttributes().getNamedItem("default-writethru-provider").getNodeValue();
                            }
                        }

                        if (_defaultReadThruProvider != null && _defaultReadThruProvider.trim().length() > 0)
                        {
                            _defaultReadThruProvider = _defaultReadThruProvider.toLowerCase();
                        }

                        if (_defaultWriteThruProvider != null && _defaultWriteThruProvider.trim().length() > 0)
                        {
                            _defaultWriteThruProvider = _defaultWriteThruProvider.toLowerCase();
                        }

                        _importHashmap=true;
                        serversNL = cacheN.getChildNodes();
                        
                        if (cacheN.getAttributes().getNamedItem("server-runtime-context") != null)
                        {
                            _serverRuntimeContext = cacheN.getAttributes().getNamedItem("server-runtime-context").getNodeValue().equals("JVCACHE") ? RtContextValue.JVCACHE: RtContextValue.NCACHE;
                        }
                        
                        break;
                    }
                }
            }
            
           

            if (serversNL == null)
            {
                return;
            }
            LoadRemoteServerMappingConfig(serversNL);
            LoadRemoteServerConfig(serversNL);
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e.getMessage());
        }
    }
    
      private void LoadRemoteServerMappingConfig(NodeList cacheConfig) {
        try {
            for (int i = 0; i < cacheConfig.getLength(); i++) {
                Node currentConfig = cacheConfig.item(i);

                if (currentConfig.getNodeName().equals("server-ip-mapping")) {
                    NodeList _mappingConfig = currentConfig.getChildNodes();
                    for (int j = 0; j < _mappingConfig.getLength(); j++) {
                        Node mapNodeConfig = _mappingConfig.item(j);
                        if (mapNodeConfig.getNodeName().equals("mapping")) {
                            RemoteServer publicServer = new RemoteServer();
                            RemoteServer privateServer = new RemoteServer();
                            try {
                                privateServer.setName(mapNodeConfig.getAttributes().getNamedItem("private-ip").getNodeValue());
                                privateServer.setPort(Integer.parseInt(mapNodeConfig.getAttributes().getNamedItem("private-port").getNodeValue()));
                                publicServer.setName(mapNodeConfig.getAttributes().getNamedItem("public-ip").getNodeValue());
                                publicServer.setPort(Integer.parseInt(mapNodeConfig.getAttributes().getNamedItem("public-port").getNodeValue()));
                            } catch (RuntimeException e) {
                            }


                            if (!privateServer.getName().isEmpty() || privateServer.getPort()>0) {
                                synchronized (_mappedServer) {
                                    if (!_mappedServer.containsKey(privateServer)) {
                                        _mappedServer.put(privateServer, publicServer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e2) {
        }
    }


    private void LoadRemoteServerConfig(NodeList cacheConfig)
    {
        try
        {
            if(!_loadServersFromConfigFile)
                return;
            int numServers = cacheConfig.getLength();
            int priority = 1;

            for (int i = 0; i < numServers; i++)
            {
                Node serverNode = cacheConfig.item(i);

                if (serverNode.getNodeName().equalsIgnoreCase("server"))
                {

                        RemoteServer remoteServer = new RemoteServer();
                        NamedNodeMap data = serverNode.getAttributes();

                        remoteServer.setName(data.getNamedItem("name").getNodeValue());
                        remoteServer.setPort(this.getServerPort());
                        if (data.getNamedItem("port-range") != null)
                        {
                            remoteServer.setPortRange(Integer.parseInt(data.getNamedItem("port-range").getNodeValue()));
                        }
                        
                        if (data.getNamedItem("node-balance") != null)
                        {
                             boolean _balanceNodes = Boolean.parseBoolean(data.getNamedItem("node-balance").getNodeValue());
                            remoteServer.setNodeBalance(_balanceNodes);
                        }
                        
                        addServer(remoteServer);
                   
                }
            }
            ArrayList rootServers = new ArrayList(_servers.size());
            for(Object obj : _servers){
                RemoteServer server = (RemoteServer)obj;
                RemoteServer rootServer = new RemoteServer(server.getName(), _daemonPort);
                rootServer.setNodeBalance(server.getNodeBalance());
                rootServer.setPortRange(server.getPortRange());
                priority++;
                rootServer.setPriority(priority);
                if(!rootServers.contains(rootServer))
                    rootServers.add(rootServer);
            }
            
            _servers.addAll(rootServers);
            _currentServer = 0;
            Collections.sort(_servers);
        }
        catch (Exception e)
        {
            
        }
        
        
        
        
    }

    public boolean getBalanceNodes()
    {
        if(initParam != null && initParam.IsSet(LOADBALANCE))
        {
            return initParam.getLoadBalance();
        }
        else
        {
            return _balanceNodes;
        }
    }
    
    public void setBalanceNodes(boolean val)
    {
        _balanceNodes=val;
    }

    public RemoteServer getNextServer()
    {
        RemoteServer nextServer = null;
        if (_servers != null && _servers.size() > 0)
        {
            if (_currentServer > _servers.size())
            {
                _currentServer = 0;
            }
            
            nextServer = (RemoteServer) _servers.get(_currentServer);
            _currentServer++;
            if (_currentServer > _servers.size() - 1)
            {
                _currentServer = 0;
            }
        }
        return nextServer;
    }

    public void setServerPort(int port)
    {
        this._serverPort = port;
    }
    
    public void setJvcServerPort(int port)
    {
        this._jvcServerPort = port;
    }

    public int getServerPort()
    {
        if(initParam != null && (initParam.IsSet(PORT) ))
        {
            return initParam.getPort();
        }
        else
        {
            if (this._serverRuntimeContext == RtContextValue.JVCACHE)
                return this._serverPort;
            else                
                return this._jvcServerPort;
        }
    }

    private static String[] getWinSystemDir(String delim, String prop)
    {
        StringTokenizer tokanizer = new StringTokenizer(prop);

        Vector v = new Vector();
        String token = "";
        while (tokanizer.hasMoreTokens())
        {
            token = tokanizer.nextToken(";");
            if (token.indexOf(delim) != -1)
            {
                if (!v.contains(token))
                {
                    v.add(token);
                }
            }
        }
        return (String[]) v.toArray(new String[0]);
    }

    public int getTimeout()
    {
        if(initParam != null && initParam.IsSet(OPTIMEOUT))
        {
            return  initParam.getClientRequestTimeOut();
        }
        else
        {
          return _timeout;
        }
    }

    public void setTimeout(int timeout)
    {
        this._timeout = timeout;
    }

    public int getConnectionRetries()
    {
        if(initParam != null && initParam.IsSet(CONNECTIONRETRIES))
        {
            return initParam.getConnectionRetries();
        }
        else
        {
            return _connectionRetries;
        }
    }

    public void setConnectionRetries(int connectionRetries)
    {
        this._connectionRetries = connectionRetries;
    }

    public int getRetryInterval()
    {
        if(initParam !=null && initParam.IsSet(RETRYINTERVAL))
        {
          return initParam.getRetryInterval();
        }
        else
        {
          return _retryInterval;  
        }
        
    }

    public void setRetryInterval(int retryInterval)
    {
        this._retryInterval = retryInterval;
    }

    public int getConnectionTimeout()
    {
        if(initParam != null && initParam.IsSet(CONNECTIONTIMEOUT))
        {
            return  initParam.getConnectionTimeout()*1000;
        }
        else
        {
            return _connectionTimeout;
        }
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this._connectionTimeout = connectionTimeout;
    }


    public int getRetryConnectionDelay()
    {
        if(initParam != null && initParam.IsSet(RETRYCONNECTIONDELAY))
        {
            return initParam.getRetryConnectionDelay()*1000;
        }
        else
        {
            return _retryConnectionDelay;
        }
    }
    
    public void setRetryConnectionDelay(int val)
    {
            this. _retryConnectionDelay=val;
    }

    public void setLocalServerIP(String ip)
    {
        this._localServerIP = ip;
    }

    public String getLocalServerIP()
    {
        return this._localServerIP;
    }
    public String getDefaultReadThru()
    {
        if(initParam != null && initParam.IsSet(DEFAULTREADTHRUPROVIDER))
        {
            return initParam.getDefaultReadThruProvider();
        }
        else
        {
            return _defaultReadThruProvider;
        }
    }

    public String getDefaultWriteThru()
    {
        if(initParam != null && initParam.IsSet(DEFAULTWRITETHRUPROVIDER))
        {
            return initParam.getDefaultWriteThruProvider();
        }
        else
        {
            return _defaultWriteThruProvider;
        }
    }
    
 
    public void setLoadCacheConfiguration(boolean load){
        _loadCacheConfiguration = load;
    }

    /**
     * @return the _compactTypes
     */
    public HashMap getCompactTypes() {
        return _compactTypes;
    }

    /**
     * @param compactTypes the _compactTypes to set
     */
    public void setCompactTypes(HashMap compactTypes) {
        this._compactTypes = compactTypes;
    }

    /**
     * @return the _itemSizeThreshHold
     */
    public int getItemSizeThreshHold() {
        return _itemSizeThreshHold;
    }

    /**
     * @return the _importHashmap
     */
    public boolean getImportHashmap() {
        return _importHashmap;
    }
    
     public  String getBindIP() 
     {
         
            File file;
            String bindIP="";
            if ((initParam != null && initParam.IsSet(BINDIP)))
            {
                    return initParam.getBindIP();
            }
           
            try{
                   
                   String path = DirectoryUtil.getConfigPath("client.conf");
                   file = new File(path);
                    if (!file.exists())
                    {
                        return "";
                    }
            }
            catch(Exception ex)
            {
                return "";
            }
            Document configuration = null;

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;

            try
            {
                builder = builderFactory.newDocumentBuilder();
            

            configuration = builder.parse(file);
            NodeList cacheList = configuration.getElementsByTagName("jvcache-client");

            for (int i = 0; i < cacheList.getLength(); i++)
            {
                Node cache = cacheList.item(i);
                if (cache.hasAttributes())
                {
                    bindIP = cache.getAttributes().getNamedItem("bind-ip-address").getNodeValue();
                    break;
                }
            }
              
            }
            catch (ParserConfigurationException ex)
            {
            }
            
            catch (SAXException ex) 
            {
            } 
            
            catch (IOException ex) 
            {
            }
           return bindIP; 
     }

    /**
     * @return the _socketPort
     */
    public int getSocketPort() {
        
        if(initParam != null && initParam.IsSet(SOCKETPORT))
        {
            return initParam.getClientPort();
        }
        else
        {            
            return _socketPort;
        }
    }

    /**
     * @param _socketPort the _socketPort to set
     */
    public void setSocketPort(int _socketPort) {
        this._socketPort = _socketPort;
    }

      
    
  
    
}
