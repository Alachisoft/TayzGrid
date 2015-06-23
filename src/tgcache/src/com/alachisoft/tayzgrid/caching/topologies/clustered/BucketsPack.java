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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.cluster.Global;

public class BucketsPack implements Cloneable
{

    private java.util.ArrayList _bucketIds = new java.util.ArrayList();
    private Address _owner;

    public BucketsPack(java.util.ArrayList buckets, Address owner)
    {
        if (buckets != null)
        {
            _bucketIds = buckets;
        }

        _owner = owner;
    }

    public final java.util.ArrayList getBucketIds()
    {
        synchronized (_bucketIds)
        {
            return _bucketIds;
        }
    }

    public final Address getOwner()
    {
        return _owner;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof BucketsPack)
        {
            return this._owner.compareTo(((BucketsPack) obj)._owner) == 0;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "{ [" + _owner + "] [" + Global.CollectionToString(_bucketIds) + "] }";
    }

    @Override
    public final Object clone()
    {
        BucketsPack pack = new BucketsPack(new java.util.ArrayList(_bucketIds), (Address) _owner.clone());
        return pack;
    }
}
