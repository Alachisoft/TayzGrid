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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BindedIpMap implements InternalCompactSerializable
{

    private HashMap map;

    public BindedIpMap()
    {
        this.map = new HashMap();
    }

    public BindedIpMap(int size)
    {
        this.map = new HashMap(size);
    }

    public BindedIpMap(HashMap map)
    {
        this.map = map;
    }

    public HashMap getMap()
    {
        if (map == null)
        {
            map = new HashMap();
        }
        return map;
    }

    public void setMap(HashMap map)
    {
        this.map = map;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        try
        {
            int len = reader.ReadInt32();
            while (len-- > 0)
            {
                this.getMap().put(CacheServer.Channel.forValue(reader.ReadInt32()), reader.ReadObject());
            }
        }
        catch (IOException iOException)
        {
            throw iOException;
        }
        catch (ClassNotFoundException classNotFoundException)
        {
            throw classNotFoundException;
        }
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        int len = this.getMap().size();
        try
        {
            writer.Write(len);
            Iterator iE = this.getMap().entrySet().iterator();
            while (iE.hasNext())
            {
                Map.Entry entry = (Map.Entry) iE.next();
                writer.Write(((CacheServer.Channel) entry.getKey()).getValue());
                writer.WriteObject(entry.getValue());
            }
        }
        catch (IOException iOException)
        {
            throw iOException;
        }
    }
}
