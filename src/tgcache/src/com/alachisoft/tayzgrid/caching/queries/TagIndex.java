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

import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import java.util.Iterator;
import java.util.Map;

public class TagIndex implements IQueryIndex
{

    private java.util.HashMap _indexTable;
    public static final String TAG_INDEX_KEY = "$Tag$";

    public TagIndex()
    {
        Initialize();
    }

    /**
     * Gets the size of the attribute index;
     */
    public final int getSize()
    {
        if (_indexTable != null)
        {
            return _indexTable.size();
        }
        else
        {
            return 0;
        }
    }

    private void Initialize()
    {
        _indexTable = new java.util.HashMap();
        IIndexStore store = new HashStore();
        _indexTable.put(TAG_INDEX_KEY, store);
    }

    @Override
    public void AddToIndex(Object key, Object value) throws RedBlackException
    {
        Iterator valuesDic = ((java.util.HashMap) value).entrySet().iterator();

        while (valuesDic.hasNext())
        {
            Map.Entry current = (Map.Entry) valuesDic.next();
            String indexKey = (String) current.getKey();
            IIndexStore store = (IIndexStore) ((_indexTable.get(indexKey) instanceof IIndexStore) ? _indexTable.get(indexKey) : null);

            if (store == null && TAG_INDEX_KEY.equals(indexKey))
            {
                store = new HashStore();
                _indexTable.put(indexKey, store);
            }

            if (store != null)
            {
                Object val = current.getValue();

                if (val != null)
                {
                    store.Add(val, key);
                }
                else
                {
                    store.Add("null", key);
                }
            }
        }
    }

    public void RemoveFromIndex(Object key, Object value) throws RedBlackException
    {
        Iterator valuesDic = ((java.util.HashMap) value).entrySet().iterator();

        while (valuesDic.hasNext())
        {
            Map.Entry current = (Map.Entry) valuesDic.next();

            String indexKey = (String) current.getKey();

            if (_indexTable.containsKey(indexKey))
            {
                IIndexStore store = (IIndexStore) ((_indexTable.get(indexKey) instanceof IIndexStore) ? _indexTable.get(indexKey) : null);
                Object val = current.getValue();

                if (val != null)
                {
                    store.Remove(val, key);
                }
                else
                {
                    store.Remove("null", key);
                }

                if (store.getCount()== 0 && TAG_INDEX_KEY.equals(indexKey))
                {
                    _indexTable.remove(indexKey);
                }
            }
        }
    }

    public final IIndexStore GetIndex(String attrib)
    {
        if (_indexTable.containsKey(attrib))
        {
            return (IIndexStore) ((_indexTable.get(attrib) instanceof IIndexStore) ? _indexTable.get(attrib) : null);
        }

        return null;
    }

    public final void Clear()
    {
        Iterator e = _indexTable.entrySet().iterator();
        while (e.hasNext())
        {
            Map.Entry current = (Map.Entry) e.next();
            IIndexStore index = (IIndexStore) ((current.getValue() instanceof IIndexStore) ? current.getValue() : null);
            index.Clear();
        }
    }

    public final Iterator GetEnumerator(String typeName, boolean forTag)
    {
        if (_indexTable.containsKey(TAG_INDEX_KEY))
        {
            IIndexStore store = (IIndexStore) ((_indexTable.get(TAG_INDEX_KEY) instanceof IIndexStore) ? _indexTable.get(TAG_INDEX_KEY) : null);
            return store.GetEnumerator();
        }

        return null;
    }
    
         //only needed in case of AttributeIndex
    public IndexInformation GetIndexInformation(Object key) { return null; }

    public void RemoveFromIndex(Object key) { }

    @Override
    public long getIndexInMemorySize() {
        throw new UnsupportedOperationException("TagIndex.getIndexInMemorySize");
    }
}
