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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackNodeReference;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;

 public class IndexInformation implements ISizableIndex
 {
    private java.util.ArrayList<IndexStoreInformation> _nodeInfo;
    
    private int _indexInformationSize;
    private int _nodeInfoMaxCount;

    public IndexInformation()
    {
        _nodeInfo = new java.util.ArrayList<IndexStoreInformation>();
    }

    public final java.util.ArrayList<IndexStoreInformation> getIndexStoreInformations()
    {
        return _nodeInfo;
    }
    public final void setIndexStoreInformations(java.util.ArrayList<IndexStoreInformation> value)
    {
        _nodeInfo = value;
    }

    public final void Add(String storeName,IIndexStore store, RedBlackNodeReference node)
    {
        IndexStoreInformation ni = new IndexStoreInformation(storeName,store, node);
        _nodeInfo.add(ni);
        
        if(_nodeInfo.size() > _nodeInfoMaxCount)
            _nodeInfoMaxCount = _nodeInfo.size();
    }

    @Override
    public long getIndexInMemorySize() {
        return _indexInformationSize + (_nodeInfoMaxCount * MemoryUtil.NetListOverHead);
    }
 }
