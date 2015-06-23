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

import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CacheDeployment implements Cloneable, InternalCompactSerializable
{
	private ClientNodes clientNodes;
	private ServersNodes serverNodes;

	public CacheDeployment()
	{
		serverNodes = new ServersNodes();
	}

        @ConfigurationSectionAnnotation(value = "client-nodes")
	public final ClientNodes getClientNodes()
	{
		return clientNodes;
	}
         @ConfigurationSectionAnnotation(value = "client-nodes")
	public final void setClientNodes(ClientNodes value)
	{
		clientNodes = value;
	}

          @ConfigurationSectionAnnotation(value = "servers")
	public final ServersNodes getServers()
	{
		return serverNodes;
	}
           @ConfigurationSectionAnnotation(value = "servers")
	public final void setServers(ServersNodes value)
	{
		serverNodes = value;
	}

        @Override
	public final Object clone()
	{
		CacheDeployment config = new CacheDeployment();
		Object tempVar = clientNodes.clone();
		config.clientNodes = clientNodes != null ? (ClientNodes)((tempVar instanceof ClientNodes) ? tempVar : null) : null;
		Object tempVar2 = serverNodes;
		config.serverNodes = serverNodes != null ? (ServersNodes)((tempVar2 instanceof ServersNodes) ? tempVar2 : null) : null;
		return config;
	}

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        clientNodes = (ClientNodes) Common.readAs(reader.ReadObject(), ClientNodes.class);
        serverNodes = (ServersNodes) Common.readAs(reader.ReadObject(), ServersNodes.class);	
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        
        writer.WriteObject(clientNodes);
        writer.WriteObject(serverNodes);
        
    }

}
