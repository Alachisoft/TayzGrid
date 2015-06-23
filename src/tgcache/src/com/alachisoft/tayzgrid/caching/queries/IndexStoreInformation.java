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

public class IndexStoreInformation implements ISizableIndex
{
    private IIndexStore _store;
    private RedBlackNodeReference _rbnodes;
    private String _storeName;

    public IndexStoreInformation()
    {
        _rbnodes = new RedBlackNodeReference();
    }

    public IndexStoreInformation(String storeName,IIndexStore store, RedBlackNodeReference node)
    {
        _rbnodes = node;
        _store = store;
        _storeName=storeName;
    }
    
    public final String getStoreName()
    {
        return _storeName;
    }
    
    public final void setStoreName(String storeName)
    {  
        _storeName=storeName;                
    }
    
    public final IIndexStore getStore()
    {
        return _store;
    }
    public final void setStore(IIndexStore value)
    {
        _store = value;
    }

    public final RedBlackNodeReference getIndexPosition()
    {
        return _rbnodes;
    }
    public final void setIndexPosition(RedBlackNodeReference value)
    {
        _rbnodes = value;
    }

    @Override
    public long getIndexInMemorySize() {
        long temp = 0;
        temp += (3 * MemoryUtil.NetReferenceSize); // for _store _rbnodes _storeName refs
        
        return temp;
    }
}
