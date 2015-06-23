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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CacheTopology implements Cloneable, InternalCompactSerializable
{
	private String topology;
	private Cluster clusterSettings;



	public CacheTopology()
	{
		clusterSettings = new Cluster();
	}

        @ConfigurationSectionAnnotation(value = "cluster-settings")
	public final Cluster getClusterSettings()
	{
		return clusterSettings;
	}
        @ConfigurationSectionAnnotation(value = "cluster-settings")
	public final void setClusterSettings(Cluster value)
	{
		clusterSettings = value;
	}

        @ConfigurationAttributeAnnotation(value = "topology", appendText = "")
	public final String getCacheType()
	{
		return this.topology;
	}
        @ConfigurationAttributeAnnotation(value = "topology", appendText = "")
	public final void setCacheType(String value)
	{
		this.topology = value;
	}

	/** 
	 Get the topology type
	*/
	public final String getTopology()
	{
		String value = this.topology;
		if (value != null)
		{
			value = value.toLowerCase();
			if (value.equals("replicated"))
			{
			return "replicated";
			}
			else if (value.equals("partitioned"))
			{
			return "partitioned";
			}
			else if (value.equals("local"))
			{
			return "local-cache";
			}
		}
		return value;
	}
	public final void setTopology(String value)
	{
		this.topology = value;
	}


        @Override
	public final Object clone() throws CloneNotSupportedException
	{
		CacheTopology config = new CacheTopology();
		config.clusterSettings = getClusterSettings() != null ? (Cluster)getClusterSettings().clone() : null;
		config.topology = this.topology != null ? (String)this.topology: null;

		return config;
	}


    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        topology  = Common.as(reader.ReadObject(), String.class);
        clusterSettings = Common.as(reader.ReadObject(), Cluster.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(topology);
        writer.WriteObject(clusterSettings);
    }


}
