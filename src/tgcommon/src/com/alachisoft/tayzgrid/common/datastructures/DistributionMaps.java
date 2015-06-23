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

package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

public class DistributionMaps implements Cloneable, ICompactSerializable, Serializable
{

    private java.util.ArrayList _hashmap;
    private java.util.HashMap _bucketsOwnershipMap;
    private BalancingResult _result = BalancingResult.Default;
    /**
     * Normally buckets are owned by main partition nodes and other main partition nodes as well as replica nodes transfer those buckets from owner partition nodes. But
     * their are exceptional cases when we inform other nodes to transfer those buckets from replica nodes. One very obvious use case is when all the nodes left the cluster except
     * one and this last node need to transfer data from the backup available on the same node. This table is used to maintain the bucket ids for which we are not following the
     * rule but exception. Against bucket ids we store backup node address from where other nodes will transfer the bucket.
     */
    private java.util.HashMap _specialBucketOwners = new java.util.HashMap();

    public DistributionMaps(java.util.ArrayList hashmap, java.util.HashMap bucketsOwnershipMap)
    {
        _hashmap = hashmap;
        _bucketsOwnershipMap = bucketsOwnershipMap;
    }

    /**
     *
     * @deprecated used only for ICompactSerializable
     */
    @Deprecated
    public DistributionMaps()
    {
    }

    public DistributionMaps(BalancingResult balResult)
    {
        _hashmap = null;
        _bucketsOwnershipMap = null;
        _result = balResult;
    }

    public final java.util.ArrayList getHashmap()
    {
        return _hashmap;
    }

    public final void setHashmap(java.util.ArrayList value)
    {
        _hashmap = value;
    }

    public final java.util.HashMap getBucketsOwnershipMap()
    {
        return _bucketsOwnershipMap;
    }

    public final void setBucketsOwnershipMap(java.util.HashMap value)
    {
        _bucketsOwnershipMap = value;
    }

    public final java.util.HashMap getSpecialBucketOwners()
    {
        return _specialBucketOwners;
    }

    public final void setSpecialBucketOwners(java.util.HashMap value)
    {
        _specialBucketOwners = value;
    }

    public final BalancingResult getBalancingResult()
    {
        return _result;
    }

    public final void setBalancingResult(BalancingResult value)
    {
        _result = value;
    }

    @Override
    public String toString()
    {
        Iterator idict = _bucketsOwnershipMap.entrySet().iterator();
        StringBuilder sb = new StringBuilder();
        while (idict.hasNext())
        {
            Map.Entry pair = (Map.Entry) idict.next();
            sb.append("Key: ").append(pair.getKey().toString()).append("Bucket Count: ").append(((java.util.ArrayList) ((pair.getValue() instanceof java.util.ArrayList) ? pair.getValue() : null)).size()).append("\n");
            java.util.ArrayList values = (java.util.ArrayList) ((pair.getValue() instanceof java.util.ArrayList) ? pair.getValue() : null);
        }

        return sb.toString();
    }

    public final Object clone()
    {
        DistributionMaps maps = new DistributionMaps(_result);
        Object tempVar = null;
        Object tempVar2 = null;
        Object tempVar3 = null;
        if (_hashmap != null)
        {
            tempVar = _hashmap.clone();
        }

        maps.setHashmap((java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));

        if (_bucketsOwnershipMap != null)
        {
            tempVar2 = _bucketsOwnershipMap.clone();
        }

        maps.setBucketsOwnershipMap((java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null));

        if (_specialBucketOwners != null)
        {
            tempVar3 = _specialBucketOwners.clone();
        }

        maps.setSpecialBucketOwners((java.util.HashMap) ((tempVar3 instanceof java.util.HashMap) ? tempVar3 : null));


        return maps;
    }

    public final void deserialize(CacheObjectInput reader)
    {
        try
        {
            _result = (BalancingResult) reader.readObject();
            _hashmap = (java.util.ArrayList) reader.readObject();
            _bucketsOwnershipMap = (java.util.HashMap) reader.readObject();
            _specialBucketOwners = (java.util.HashMap) reader.readObject();
        }
        catch (Exception ex)
        {
        }
    }

    public final void serialize(CacheObjectOutput writer)
    {
        try
        {
            writer.writeObject(_result);
            writer.writeObject(_hashmap);
            writer.writeObject(_bucketsOwnershipMap);
            writer.writeObject(_specialBucketOwners);
        }
        catch (Exception ex)
        {
        }
    }
}
