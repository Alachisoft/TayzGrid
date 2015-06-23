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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CacheRegisterationInfo implements InternalCompactSerializable
{
    private CacheServerConfig _updatedCacheConfig;
    private java.util.ArrayList _affectedNodesList;
    private java.util.ArrayList _affectedPartitions;

    public CacheRegisterationInfo() { }
    public CacheRegisterationInfo(CacheServerConfig cacheConfig, java.util.ArrayList nodesList, java.util.ArrayList affectedPartitions)
    {
        _updatedCacheConfig = cacheConfig;
        _affectedNodesList = nodesList;
        _affectedPartitions = affectedPartitions;
    }

    public final CacheServerConfig getUpdatedCacheConfig()
    {
        return _updatedCacheConfig;
    }

    public final java.util.ArrayList getAffectedNodes()
    {
        return _affectedNodesList;
    }

    public final java.util.ArrayList getAffectedPartitions()
    {
        return _affectedPartitions;
    }


        public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
        {
            _updatedCacheConfig = (CacheServerConfig) Common.readAs(reader.ReadObject(),CacheServerConfig.class);
            _affectedNodesList = (ArrayList) Common.readAs(reader.ReadObject(),ArrayList.class);
            _affectedPartitions = (ArrayList) Common.readAs(reader.ReadObject(),ArrayList.class);
        }

        public void Serialize(CompactWriter writer) throws IOException
        {
            writer.WriteObject(_updatedCacheConfig);
            writer.WriteObject(_affectedNodesList);
            writer.WriteObject(_affectedPartitions);
        }
}
