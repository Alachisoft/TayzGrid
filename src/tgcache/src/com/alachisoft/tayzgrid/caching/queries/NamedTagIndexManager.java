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
import com.alachisoft.tayzgrid.caching.MetaInformation;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import java.util.ArrayList;
import java.util.Iterator;

public class NamedTagIndexManager extends TagIndexManager {

    public NamedTagIndexManager(java.util.Map props, IndexedLocalCache cache, String cacheName, java.util.HashMap dataSharingKnownTypes) {
        super(props, cache, cacheName, dataSharingKnownTypes);
    }

    @Override
    public boolean Initialize() {
        if (super.Initialize()) {
            return true;
        }
        //initialize the index map to put the tags information on
        _indexMap = new java.util.HashMap();
        return true;
    }

    @Override
    public void AddToIndex(Object key, Object value) throws ClassNotFoundException, RedBlackException {
        super.AddToIndex(key, value);

        CacheEntry entry = (CacheEntry) value;
        java.util.HashMap queryInfo = entry.getQueryInfo();

        if (queryInfo.containsKey("named-tag-info")) {
            AddNamedTag(key, (java.util.HashMap) ((queryInfo.get("named-tag-info") instanceof java.util.HashMap) ? queryInfo.get("named-tag-info") : null), entry);
        }
        entry.setQueryInfo(null);
    }

    @Override
    public void RemoveFromIndex(Object key, Object value) throws RedBlackException, ClassNotFoundException {
        super.RemoveFromIndex(key, value);
        if (((java.util.HashMap) value).containsKey("named-tag-info")) {
            RemoveNamedTag(key, (java.util.HashMap) ((((java.util.HashMap) value).get("named-tag-info") instanceof java.util.HashMap) ? ((java.util.HashMap) value).get("named-tag-info") : null));
        }
    }

    private void AddNamedTag(Object key, java.util.HashMap value, CacheEntry entry) throws ClassNotFoundException, RedBlackException {
        if (value != null) {
            String type = (String) ((value.get("type") instanceof String) ? value.get("type") : null);
            java.util.HashMap tagsList = (java.util.HashMap) value.get("named-tags-list");
            java.util.HashMap metaInfoAttribs = (java.util.HashMap) tagsList.clone();

            java.util.ArrayList<String> modifiedKeys = new java.util.ArrayList<String>();

            if (!_indexMap.containsKey(type)) {
                if (_sharedAttributeIndex != null && _sharedAttributeIndex.containsKey(type)) {
                    AttributeIndex tempAttrib = _sharedAttributeIndex.get(type);
                    tempAttrib.Initialize(new java.util.ArrayList());
                    _indexMap.put(type, tempAttrib);
                    if (tempAttrib._sharedTypes != null && tempAttrib._sharedTypes.size() > 0) {
                        for (AttributeIndex sharedAttrib : tempAttrib._sharedTypes) {
                            sharedAttrib.Initialize(new ArrayList());
                            _indexMap.put(sharedAttrib._type, sharedAttrib);
                        }
                    }
                } else {
                    _indexMap.put(type, new AttributeIndex(null, _cacheName, type));
                }
            }

            IQueryIndex index = (IQueryIndex) ((_indexMap.get(type) instanceof IQueryIndex) ? _indexMap.get(type) : null);

            index.AddToIndex(key, tagsList);

            if (entry.getMetaInformation() != null) {
                entry.getMetaInformation().Add(metaInfoAttribs);
            } else {
                entry.setMetaInformation(new MetaInformation(metaInfoAttribs));
                entry.getMetaInformation().setCacheKey(key);
                entry.getMetaInformation().setType((String) ((value.get("type") instanceof String) ? value.get("type") : null));
                entry.setObjectType((String) ((value.get("type") instanceof String) ? value.get("type") : null));
            }
        }
    }

    protected final void RemoveNamedTag(Object key, java.util.HashMap value) throws RedBlackException {
        if (value != null) {
            String type = (String) ((value.get("type") instanceof String) ? value.get("type") : null);
            java.util.HashMap tagsList = (java.util.HashMap) ((value.get("named-tags-list") instanceof java.util.HashMap) ? value.get("named-tags-list") : null);

            java.util.ArrayList<String> modifiedKeys = new java.util.ArrayList<String>();
            for (Iterator it = tagsList.keySet().iterator(); it.hasNext();) {
                String tagKey = (String) it.next();
                if (tagsList.get(tagKey) instanceof String) {
                    modifiedKeys.add(tagKey);
                }
            }

            for (String tagKey : modifiedKeys) {
                tagsList.put(tagKey, ((String) ((tagsList.get(tagKey) instanceof String) ? tagsList.get(tagKey) : null)).toLowerCase());
            }

            if (_indexMap.containsKey(type)) {
                IQueryIndex index = (IQueryIndex) ((_indexMap.get(type) instanceof IQueryIndex) ? _indexMap.get(type) : null);
                index.RemoveFromIndex(key, tagsList);
            }
        }
    }
}
