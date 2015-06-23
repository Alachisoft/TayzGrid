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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ClientNodes implements Cloneable, InternalCompactSerializable
{

    private java.util.ArrayList<ClientNode> nodesList ;
    
    public ClientNodes()
    {
        nodesList = new ArrayList<ClientNode>();
    }

    @ConfigurationSectionAnnotation(value = "client-node") //Changes for Dom node
    public final ClientNode[] getNodes()
    {
        if (nodesList != null)
        {
            return nodesList.toArray(new ClientNode[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = "client-node") //Changes for Dom node
    public final void setNodes(Object value)
    {
         if (nodesList == null)
        {
            nodesList = new java.util.ArrayList<ClientNode>();
        }

        nodesList.clear();
        if (value != null)
        {
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++)
            {
                nodesList.add((ClientNode) objs[i]);
            }
        }
    }

    public final java.util.ArrayList<ClientNode> getNodesList()
    {
        return nodesList;
    }

    public final void setNodesList(java.util.ArrayList<ClientNode> value)
    {
        nodesList = value;
    }

    @Override
    public final Object clone()
    {
        ClientNodes clientNodes = new ClientNodes();
        clientNodes.setNodes(getNodes() != null ? (ClientNode[]) getNodes().clone() : null);
        return clientNodes;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        setNodes(Common.as(reader.ReadObject(), ClientNode[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(getNodes());
    }
}
