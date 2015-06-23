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
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import com.alachisoft.tayzgrid.common.datastructures.SRTree;
import java.util.Iterator;
import java.util.Map;

public class TagIndexManager extends QueryIndexManager {

    public TagIndexManager(java.util.Map props, IndexedLocalCache cache, String cacheName, java.util.HashMap dataSharingKnownTypes) {
        super(props, cache, cacheName, dataSharingKnownTypes);
    }

    public boolean Initialize() {
        if (super.Initialize()) {
            return true;
        }

        //initialize the index map to put the tags information on runtime.
        _indexMap = new java.util.HashMap();
        return true;
    }

    private void AddTag(Object key, java.util.HashMap value) throws ClassNotFoundException, RedBlackException {
        if (value != null) {
            String type = (String) ((value.get("type") instanceof String) ? value.get("type") : null);
            java.util.ArrayList tagsList = (java.util.ArrayList) ((value.get("tags-list") instanceof java.util.ArrayList) ? value.get("tags-list") : null);

            if (!_indexMap.containsKey(type)) {
                _indexMap.put(type, new AttributeIndex(null, _cacheName, type));
            }

            IQueryIndex index = (IQueryIndex) ((_indexMap.get(type) instanceof IQueryIndex) ? _indexMap.get(type) : null);
            for (Iterator it = tagsList.iterator(); it.hasNext();) {
                String tag = (String) it.next();
                java.util.HashMap tmp = new java.util.HashMap();
                tmp.put("$Tag$", tag);
                index.AddToIndex(key, tmp);
            }
        }
    }

    private void RemoveTag(Object key, java.util.HashMap value) throws RedBlackException {
        if (value != null) {
            String type = (String) ((value.get("type") instanceof String) ? value.get("type") : null);
            java.util.ArrayList tagsList = (java.util.ArrayList) ((value.get("tags-list") instanceof java.util.ArrayList) ? value.get("tags-list") : null);

            if (_indexMap.containsKey(type)) {
                IQueryIndex index = (IQueryIndex) ((_indexMap.get(type) instanceof IQueryIndex) ? _indexMap.get(type) : null);
                for (Iterator it = tagsList.iterator(); it.hasNext();) {
                    String tag = (String) it.next();
                    java.util.HashMap tmp = new java.util.HashMap();
                    tmp.put("$Tag$", tag.toLowerCase());
                    index.RemoveFromIndex(key, tmp);
                }
                if (((AttributeIndex) index).getCount() == 0) {
                    _indexMap.remove(type);
                }
            }
        }
    }

    @Override
    public void AddToIndex(Object key, Object value) throws ClassNotFoundException, RedBlackException {
        CacheEntry entry = (CacheEntry) value;
        java.util.HashMap queryInfo = entry.getQueryInfo();

        if (queryInfo.containsKey("query-info")) {
            super.AddToIndex(key, entry);
        }
        if (queryInfo.containsKey("tag-info")) {
            /* For clustered and outproc cache tag info is converted in HashMap command parsing.
             But for inproc caches it is populated in Hashtable. For handling both cases here,
             * overloads of AddTag and RemoveTag and these checks are added.
             */
            if (queryInfo.get("tag-info") instanceof java.util.HashMap) {
                AddTag(key, (java.util.HashMap) (queryInfo.get("tag-info")));
                if (queryInfo.get("tag-info") != null) {
                    entry.setObjectType(((java.util.HashMap) (queryInfo.get("tag-info"))).get("type").toString());
                }

            }
        }
    }

    @Override
    public void RemoveFromIndex(Object key, Object value) throws RedBlackException, ClassNotFoundException {
        if (((java.util.HashMap) value).containsKey("query-info")) {
            super.RemoveFromIndex(key, ((java.util.HashMap) value).get("query-info"));
        }
        if (((java.util.HashMap) value).containsKey("tag-info")) {
            if ((((java.util.HashMap) value).get("tag-info") instanceof java.util.HashMap)) {
                RemoveTag(key, (java.util.HashMap) (((java.util.HashMap) value).get("tag-info")));
            }
        }
    }

    private java.util.ArrayList GetCombinedKeysFromEveryType(String tag) {
        if (_indexMap == null) {
            return null;
        }

        java.util.ArrayList keys = null;
        java.util.HashMap finalTable = new java.util.HashMap();

        Iterator typeEnumerator = _indexMap.entrySet().iterator();

        while (typeEnumerator.hasNext()) {
            Map.Entry current = (Map.Entry) typeEnumerator.next();
            SRTree tree = new SRTree();
            AttributeIndex index = (AttributeIndex) ((current.getValue() instanceof AttributeIndex) ? current.getValue() : null);
            IIndexStore store = index.GetStore("$Tag$");

            if (store != null) {
                keys = store.GetData(tag.toLowerCase(), ComparisonType.EQUALS);

                if (keys != null && keys.size() > 0) {
                    for (int i = 0; i < keys.size(); i++) {
                        finalTable.put(keys.get(i), null);
                    }
                }
            }
        }

        return new java.util.ArrayList(finalTable.keySet());
    }

    public final java.util.ArrayList GetAllMatchingTags(String[] tags) {
        java.util.HashMap finalTable = new java.util.HashMap();
        java.util.ArrayList keys = GetCombinedKeysFromEveryType(tags[0]);

        for (int i = 0; i < keys.size(); i++) {
            finalTable.put(keys.get(i), null);
        }

        for (int i = 1; i < tags.length; i++) {
            java.util.HashMap shiftTable = new java.util.HashMap();

            keys = GetCombinedKeysFromEveryType(tags[i]);

            if (keys != null) {
                for (int j = 0; j < keys.size(); j++) {
                    Object key = keys.get(j);

                    if (finalTable.containsKey(key)) {
                        shiftTable.put(key, null);
                    }
                }
            }
            finalTable = shiftTable;
        }
        return new java.util.ArrayList(finalTable.keySet());
    }

    public final java.util.ArrayList GetByTag(String tag) {
        return GetCombinedKeysFromEveryType(tag);
    }

    public final java.util.ArrayList GetAnyMatchingTag(String[] tags) {
        java.util.ArrayList finalKeys = GetCombinedKeysFromEveryType(tags[0]);
        java.util.HashMap finalTable = new java.util.HashMap();

        for (int i = 0; i < finalKeys.size(); i++) {
            finalTable.put(finalKeys.get(i), null);
        }

        for (int i = 1; i < tags.length; i++) {
            java.util.ArrayList keys = GetCombinedKeysFromEveryType(tags[i]);

            if (keys != null && keys.size() > 0) {
                for (int j = 0; j < keys.size(); j++) {
                    finalTable.put(keys.get(j), null);
                }
            }
        }
        return new java.util.ArrayList(finalTable.keySet());
    }
}
