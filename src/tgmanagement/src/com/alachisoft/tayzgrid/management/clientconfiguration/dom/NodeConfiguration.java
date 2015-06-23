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

package com.alachisoft.tayzgrid.management.clientconfiguration.dom;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.io.Serializable;

public class NodeConfiguration implements Cloneable, InternalCompactSerializable
{

    private int _connectionRetries = 5;
    private int _retryInterval = 1;
    private int _requestTimeout = 90;
    private int _connectionTimeout = 5;
    private int _serverPort = ServicePropValues.getClientPort();
    private int _configurationId;
    private int _retryConnectionDelay = 0;
    private String _localServerIP;
    private int _jvcServerPort = ServicePropValues.getClientPort();

    
    public final int getConfigurationId()
    {
        return _configurationId;
    }

    public final void setConfigurationId(int value)
    {
        _configurationId = value;
    }


    @ConfigurationAttributeAnnotation(value="connection-retries",appendText="")
    public final int getConnectionRetries()
    {
        return _connectionRetries;
    }

     @ConfigurationAttributeAnnotation(value="connection-retries",appendText="")
    public final void setConnectionRetries(int value)
    {
        _connectionRetries = value;
    }

      @ConfigurationAttributeAnnotation(value="retry-connection-delays",appendText="")
    public final int getRetryConnectionDelay()
    {
        return _retryConnectionDelay;
    }

      @ConfigurationAttributeAnnotation(value="retry-connection-delays",appendText="")
    public final void setRetryConnectionDelay(int value)
    {
        _retryConnectionDelay = value;
    }

      @ConfigurationAttributeAnnotation(value="retry-interval",appendText="")
    public final int getRetryInterval()
    {
        return _retryInterval;
    }

      @ConfigurationAttributeAnnotation(value="retry-interval",appendText="")
    public final void setRetryInterval(int value)
    {
        _retryInterval = value;
    }

      @ConfigurationAttributeAnnotation(value="client-request-timeout",appendText="")
    public final int getRequestTimeout()
    {
        return _requestTimeout;
    }

      @ConfigurationAttributeAnnotation(value="client-request-timeout",appendText="")
    public final void setRequestTimeout(int value)
    {
        _requestTimeout = value;
    }

      @ConfigurationAttributeAnnotation(value="connection-timeout",appendText="")
    public final int getConnectionTimeout()
    {
        return _connectionTimeout;
    }

      @ConfigurationAttributeAnnotation(value="connection-timeout",appendText="")
    public final void setConnectionTimeout(int value)
    {
        _connectionTimeout = value;
    }


      @ConfigurationAttributeAnnotation(value="port",appendText="")
    public final int getServerPort()
    {
        return _serverPort;
    }

       @ConfigurationAttributeAnnotation(value="port",appendText="")
    public final void setServerPort(int value)
    {
        _serverPort = value;
    }
       
    public final int getJvcServerPort()
    {
        return _jvcServerPort;
    }

    public final void setJvcServerPort(int value)
    {
        _jvcServerPort = value;
    }

    @ConfigurationAttributeAnnotation(value="local-server-ip",appendText="")
    public final String getLocalServerIP()
    {
        return _localServerIP;
    }

    @ConfigurationAttributeAnnotation(value="local-server-ip",appendText="")
    public final void setLocalServerIP(String value)
    {
        _localServerIP = value;
    }
       
    public final Object clone()
    {
        NodeConfiguration config = new NodeConfiguration();
        config._configurationId = _configurationId;
        config._connectionRetries = _connectionRetries;
        config._connectionTimeout = _connectionTimeout;
        config._retryInterval = _retryInterval;
        config._serverPort = _serverPort;
        config._requestTimeout = _requestTimeout;
        config._retryConnectionDelay = _retryConnectionDelay;
        config._jvcServerPort = _jvcServerPort;
        return config;
    }
    //<editor-fold defaultstate="collapsed" desc="ISerailizable">
public void Deserialize(CompactReader reader) throws IOException
        {
            _connectionRetries = reader.ReadInt32();
            _retryInterval = reader.ReadInt32();
            _requestTimeout = reader.ReadInt32();
            _connectionTimeout = reader.ReadInt32();
            _serverPort = reader.ReadInt32();
            _configurationId =reader.ReadInt32();
            _retryConnectionDelay = reader.ReadInt32();
            _jvcServerPort = reader.ReadInt32();
        }

        public void Serialize(CompactWriter writer) throws IOException
        {
            writer.Write(_connectionRetries);
            writer.Write(_retryInterval);
            writer.Write(_requestTimeout);
            writer.Write(_connectionTimeout);
            writer.Write(_serverPort);
            writer.Write(_configurationId);
            writer.Write(_retryConnectionDelay);
            writer.Write(_jvcServerPort);            
        }
    //</editor-fold>
}
