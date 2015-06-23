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
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class ServerNode implements Cloneable, InternalCompactSerializable
{
	private String ip;
	private boolean activeMirrorNode = false;

	public ServerNode()
	{

	}
	public ServerNode(String ip, boolean activeMirrorNode)
	{
		this.ip = ip;
		this.activeMirrorNode = activeMirrorNode;
	}


        @ConfigurationAttributeAnnotation(value = "ip", appendText = "")
	public final String getIP()
	{
		return ip;
	}
        @ConfigurationAttributeAnnotation(value = "ip", appendText = "")
	public final void setIP(String value)
	{
		ip = value;
	}

        @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
	public final boolean getIsActiveMirrorNode()
	{
		return activeMirrorNode;
	}
        @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
	public final void setIsActiveMirrorNode(boolean value)
	{
		activeMirrorNode = value;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ServerNode)
		{
			ServerNode serverNode = (ServerNode)((obj instanceof ServerNode) ? obj : null);
			return serverNode.ip.toLowerCase().compareTo(ip.toLowerCase()) == 0;
		}

		return false;
	}


        @Override
	public final Object clone()
	{
		ServerNode node = new ServerNode();
		node.ip = ip;
		node.activeMirrorNode = activeMirrorNode;
		return node;
	}

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        this.ip=reader.ReadString();     
        this.activeMirrorNode=reader.ReadBoolean();
          
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {

      writer.Write(ip);
      writer.Write(activeMirrorNode);
        
    }


}
