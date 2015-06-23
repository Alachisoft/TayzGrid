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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class ClientNode implements Cloneable, InternalCompactSerializable
{
    private String name;
    private ClientNodeStatus status = ClientNodeStatus.values()[0];
    private RtContextValue clientRuntimeContext=RtContextValue.JVCACHE;

    @ConfigurationAttributeAnnotation(value = "ip", appendText = "") //Changes for Dom name
    public final String getName()
    {
        return name;
    }
   @ConfigurationAttributeAnnotation(value = "ip", appendText = "") //Changes for Dom name
    public final void setName(String value)
    {
        name = value;
    }
      
   public final RtContextValue getClientRuntimeContext()
   {
       return clientRuntimeContext;
   }   
   
   public final void setClientRuntimeContext(RtContextValue clientRtContext)
   {
       clientRuntimeContext = clientRtContext;
   }
   
   public final String getClientRuntimeContextString()
   {
       if (clientRuntimeContext == RtContextValue.JVCACHE)
           return "JVCACHE";
       else 
           return "NCACHE";               
   }
   
   public final void setClientRuntimeContextString(String value)
   {
       if(value.equals("JVCACHE"))
           clientRuntimeContext = RtContextValue.JVCACHE;
       else if(value.equals("NCACHE"))
           clientRuntimeContext = RtContextValue.NCACHE;
   }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientNode)
        {
            ClientNode clientNode = (ClientNode) ((obj instanceof ClientNode) ? obj : null);
            return clientNode.name.toLowerCase().compareTo(name.toLowerCase()) == 0;
        }

        return false;
    }

    @Override
    public final Object clone()
    {
        ClientNode node = new ClientNode();
        node.name = name;
        node.status = status;
        node.clientRuntimeContext = clientRuntimeContext;
        return node;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        name = (String) Common.readAs(reader.ReadObject(), String.class);
        status = ClientNodeStatus.forValue(reader.ReadInt32());
        clientRuntimeContext = "1".equals(Common.as(reader.ReadObject(), String.class)) ? RtContextValue.JVCACHE : RtContextValue.NCACHE  ;        
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(name);
        writer.Write((int)status.getValue());
        writer.WriteObject(clientRuntimeContext == RtContextValue.JVCACHE ? "1" : "0");
    }
}
