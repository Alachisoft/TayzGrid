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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheBase;
import com.alachisoft.tayzgrid.common.datastructures.SRTree;

public class QueryContext
{
    private LocalCacheBase _cache;
    private AttributeIndex _index;
    private SRTree _tree;
    private String _cacheContext;
    private java.util.Map _values;
    private boolean _populateTree = true;
    private QueryResultSet _resultSet = new QueryResultSet();
    private String _typeName = "";

    public QueryContext(LocalCacheBase cache)
    {
        _cache = cache;
        _tree = new SRTree();
    }

    public final String getTypeName()
    {
        return _typeName;
    }

    public final void setTypeName(String value)
    {
        _typeName = value;
    }

    public final QueryResultSet getResultSet()
    {
        return _resultSet;
    }

    public final void setResultSet(QueryResultSet value)
    {
        _resultSet = value;
    }

    public final java.util.Map getAttributeValues()
    {
        return _values;
    }

    public final void setAttributeValues(java.util.Map value)
    {
        _values = value;
    }

    public final LocalCacheBase getCache()
    {
        return _cache;
    }

    public final String getCacheContext()
    {
        return _cacheContext;
    }

    public final void setCacheContext(String value)
    {
        _cacheContext = value;
    }

    public final AttributeIndex getIndex()
    {
        Object tempVar=null;
	if (_index == null)
        {
            tempVar = getIndexManager().getIndexMap().get(getTypeName());
            _index = (AttributeIndex) ((tempVar instanceof AttributeIndex) ? tempVar : null);
        }

                return _index;
    }

    public final void setIndex(AttributeIndex value)
    {
        _index = value;
    }

    public final SRTree getTree()
    {
        return _tree;
    }

    public final void setTree(SRTree value)
    {
        _tree = value;
    }

    public final boolean getPopulateTree()
    {
        return _populateTree;
    }

    public final void setPopulateTree(boolean value)
    {
        _populateTree = value;
    }

    public final QueryIndexManager getIndexManager()
    {
        return ((IndexedLocalCache) _cache).getIndexManager();
    }

    public final Object Get(Object key, OperationContext operationContext) throws Exception
    {
        CacheEntry entry = getCache().Get(key, operationContext);
        Object obj = entry.DeflattedValue(getCacheContext());

        return obj;
    }
}