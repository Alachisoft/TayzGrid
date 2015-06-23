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
import com.alachisoft.tayzgrid.common.datastructures.RedBlack;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackEnumerator;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import java.util.Iterator;
public class RBStore implements IIndexStore, ISizableIndex
{
    private RedBlack _rbTree;
    private String _storeDataType = "";

    public RBStore(String cacheName)
    {
        _rbTree = new RedBlack(cacheName);
    }
    
    public RBStore(String cacheName, String storeDataType)
    {
        _rbTree = new RedBlack(cacheName, MemoryUtil.GetAttributeTypeSize(storeDataType));
        this._storeDataType = storeDataType;
        
    }
    
    public final Object Add(Object key, Object value) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException
    {
        Object node = new Object();
        if (_rbTree != null)
        {
            node = _rbTree.Add((java.lang.Comparable) ((key instanceof java.lang.Comparable) ? key : null), value);
        }
        return node;
    }

    public final boolean Remove(Object value, Object indexPosition) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException
    { 
        boolean isNodeRemoved = false;
        if (_rbTree != null)
        {
            isNodeRemoved = _rbTree.Remove(value, indexPosition);
        }
        return isNodeRemoved;
    }

    public final void Clear()
    {
        if (_rbTree != null)
        {
            _rbTree.Clear();
        }
    }

    public final Iterator GetEnumerator()
    {
        if (_rbTree != null)
        {
            return _rbTree.GetEnumerator();
        }

        return new RedBlackEnumerator();
    }

    public final java.util.ArrayList GetData(Object key, ComparisonType comparisonType)
    {
        RedBlack.COMPARE compare = RedBlack.COMPARE.EQ;
        java.util.ArrayList result = new java.util.ArrayList();

        if (_rbTree != null)
        {
            switch (comparisonType)
            {
                case EQUALS:
                    compare = RedBlack.COMPARE.EQ;
                    break;
                case NOT_EQUALS:
                    compare = RedBlack.COMPARE.NE;
                    break;
                case LESS_THAN:
                    compare = RedBlack.COMPARE.LT;
                    break;
                case GREATER_THAN:
                    compare = RedBlack.COMPARE.GT;
                    break;
                case LESS_THAN_EQUALS:
                    compare = RedBlack.COMPARE.LTEQ;
                    break;
                case GREATER_THAN_EQUALS:
                    compare = RedBlack.COMPARE.GTEQ;
                    break;
                case LIKE:
                    compare = RedBlack.COMPARE.REGEX;
                    break;
                case NOT_LIKE:
                    compare = RedBlack.COMPARE.IREGEX;
                    break;
            }

            Object tempVar = _rbTree.GetData((java.lang.Comparable) ((key instanceof java.lang.Comparable) ? key : null), compare);
            result = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
        }
        return result;
    }

    @Override
    public final int getCount()
    {
        return _rbTree != null ? _rbTree.getCount(): 0;
    }

    public String getStoreDataType()
    {
        return _storeDataType;
    }
    
    @Override
    public long getIndexInMemorySize() {

        return this._rbTree.getIndexInMemorySize();

    }
}
