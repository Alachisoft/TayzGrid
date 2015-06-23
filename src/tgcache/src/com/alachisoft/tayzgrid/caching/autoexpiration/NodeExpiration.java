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

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Node expiration based derivative of ExpirationHint.
 */
public class NodeExpiration extends DependencyHint implements java.io.Serializable
{
    /**
     * The node on which this hint depends.
     */
    protected Address _node;

    public NodeExpiration()
    {
        _hintType = ExpirationHintType.NodeExpiration;
    }

    /**
     * Constructor
     *
     * @param node
     */
    public NodeExpiration(Address node)
    {
        _hintType = ExpirationHintType.NodeExpiration;
        _node = node;
    }

    /**
     * virtual method that returns true when the expiration has taken place, returns false otherwise.
     */
    @Override
    public boolean DetermineExpiration(CacheRuntimeContext context)
    {
        if (getHasExpired())
        {
            return true;
        }

        if (context.getIsClusteredImpl())
        {
            if (((ClusterCacheBase) context.getCacheImpl()).getCluster().IsMember(_node) == false)
            {
                this.NotifyExpiration(this, null);
            }
        }

        return getHasExpired();
    }

    public final Address GetNode()
    {
        return _node;
    }

    /**
     * returns false if given node is alive, returns true otherwise.
     */
    @Override
    public boolean getHasChanged()
    {
        return false;
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        if(reader.readBoolean())
            _node = (Address) reader.readObject();
        else
            _node = null;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        if(_node != null)
        {
            writer.writeBoolean(true);
            writer.writeObject(_node);
        }
        else
            writer.writeBoolean(false);
    }
}
