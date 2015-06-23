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


package com.alachisoft.tayzgrid.management.clientconfiguration;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class CacheServerList implements InternalCompactSerializable
{
    private HashMap<Integer, CacheServer> _serversList = new HashMap<Integer, CacheServer>();

    public HashMap<Integer, CacheServer> getServersList()
    {
        return this._serversList;
    }

    public void setServersList(HashMap<Integer, CacheServer> _serversList)
    {
        this._serversList = _serversList;
    }
    public void setServersList(int key,  CacheServer cacheServer)
    {
        _serversList.put(key, cacheServer);
    }

    public CacheServerList() { }

    public CacheServerList(HashMap<Integer, CacheServer> serversList)
    {
            this._serversList = serversList;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        this._serversList = new HashMap<Integer, CacheServer>();
        int length = reader.ReadInt32();
        for (int i = 0; i < length; i++)
        {
            int key = reader.ReadInt32();
            CacheServer value = (CacheServer) Common.readAs(reader.ReadObject(), CacheServer.class);
            _serversList.put(key, value);
        }
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(this._serversList.size());
        Map.Entry current = null;
        Iterator ite = this._serversList.entrySet().iterator();
        while (ite.hasNext())
        {
            current = (Entry) ite.next();
            writer.Write(((Integer) current.getKey()).intValue());
            writer.WriteObject(current.getValue());
        }
    }
    //</editor-fold>
}
