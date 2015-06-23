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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CacheServer implements Cloneable, InternalCompactSerializable
{

    private String _serverName;
    private int _priority;
    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final String getServerName()
    {
        return _serverName;
    }

    @ConfigurationAttributeAnnotation(value = "name", appendText = "")
    public final void setServerName(String value)
    {
        _serverName = value;
    }

    public final int getPriority()
    {
        return _priority;
    }

    public final void setPriority(int value)
    {
        _priority = value;
    }

    @Override
    public String toString()
    {
        return _serverName;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CacheServer)
        {
            CacheServer server = (CacheServer) ((obj instanceof CacheServer) ? obj : null);
            return server.getServerName().toLowerCase().equals(_serverName.toLowerCase());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return _serverName.toLowerCase().hashCode();
    }

    public final Object clone()
    {
        CacheServer server = new CacheServer();
        server._serverName = _serverName;
        server._priority = _priority;

        return server;
    }
    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _serverName = (String) Common.readAs(reader.ReadObject(),String.class);
        _priority = reader.ReadInt32();
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_serverName);
        writer.Write(_priority);
    }
    //</editor-fold>
}
