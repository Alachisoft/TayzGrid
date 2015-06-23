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

import java.util.Iterator;
import java.util.Map;

public class TypeIndex implements IQueryIndex
{
    private java.util.HashMap _indexTable;
    private boolean _indexForAll;

    public TypeIndex(String type, boolean indexForAll)
    {
        _indexForAll = indexForAll;
        _indexTable = new java.util.HashMap();
        _indexTable.put(type.toLowerCase(), new java.util.HashMap());
    }

    public final void AddToIndex(Object key, Object value)
    {
        if (_indexTable.containsKey(value.getClass().toString().toLowerCase()))
        {
            java.util.HashMap tbl = (java.util.HashMap) ((_indexTable.get(value.getClass().toString().toLowerCase()) instanceof java.util.HashMap) ? _indexTable.get(value.getClass().toString().toLowerCase()) : null);
            tbl.put(key, null);
        }
    }

    public final void RemoveFromIndex(Object key, Object value)
    {
        if (_indexTable.containsKey(value.getClass().toString().toLowerCase()))
        {
            java.util.HashMap tbl = (java.util.HashMap) ((_indexTable.get(value.getClass().toString().toLowerCase()) instanceof java.util.HashMap) ? _indexTable.get(value.getClass().toString().toLowerCase()) : null);
            if (tbl.containsKey(key))
            {
                tbl.remove(key);
            }
        }
    }

    public final void Clear()
    {
        if (_indexForAll)
        {
            _indexTable.clear();
        }
        else
        {
            Iterator e = _indexTable.entrySet().iterator();
            while (e.hasNext())
            {
                Map.Entry current = (Map.Entry) e.next();
                java.util.HashMap tbl = (java.util.HashMap) ((current.getValue() instanceof java.util.HashMap) ? current.getValue() : null);
                tbl.clear();
            }
        }
    }

    public final Iterator GetEnumerator(String typeName, boolean forTag)
    {
        Iterator en = _indexTable.entrySet().iterator();

        while (en.hasNext())
        {
            Map.Entry current = (Map.Entry) en.next();

            java.util.HashMap tbl = (java.util.HashMap) ((current.getValue() instanceof java.util.HashMap) ? current.getValue() : null);
            return tbl.entrySet().iterator();
        }

        return null;
    }
    
        //only needed in case of AttributeIndex
    public IndexInformation GetIndexInformation(Object key) { return null; }

    public void RemoveFromIndex(Object key) { }

    @Override
    public long getIndexInMemorySize() {
        throw new UnsupportedOperationException("TypeIndex.getIndexInMemorySize()");
    }
}
