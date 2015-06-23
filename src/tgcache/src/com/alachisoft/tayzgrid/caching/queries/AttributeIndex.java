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

import com.alachisoft.tayzgrid.common.configuration.Activator;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackNodeReference;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.common.util.StringPool;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AttributeIndex implements IQueryIndex, java.io.Serializable {

    protected java.util.HashMap _indexTable;
    protected String _cacheName;
    protected String _type;
    protected static final String TAG_INDEX_KEY = "$Tag$";
    protected static final String NAMED_TAG_PREFIX = "$NamedTagAttribute$";
    protected java.util.HashMap _keyIndexTable;
    protected java.util.ArrayList<AttributeIndex> _sharedTypes;
    protected java.util.HashMap _commonRBStores;
    
    private long _attributeIndexSize;
    private long _keyIndexInformationSize;
    private long _keyIndexTableMaxCount;
    
    
    private transient TypeInfoMap _typeMap = null;

    public AttributeIndex() {
    }
    
    public AttributeIndex(ArrayList attributeList, String cacheName, String type, TypeInfoMap typeMap)
    {
        _indexTable = new HashMap();
        _cacheName = cacheName;
        _keyIndexTable = new HashMap();
        _type = type;
        _typeMap = typeMap;
        
        Initialize(attributeList);
    }

    public String getTypeName()
    {
        return _type;
    }
    
    public void setSharedTypes(java.util.ArrayList<AttributeIndex> list) {
        _sharedTypes = list;
    }

    public void setCommonRBStores(java.util.HashMap commonRBStores) {
        _commonRBStores = commonRBStores;
    }

    public IIndexStore getRBStore(String attrib) {
        return (IIndexStore) ((_indexTable.get(attrib) instanceof IIndexStore) ? _indexTable.get(attrib) : null);
    }

    public String getCompatibleAttrib(String attribName) {
        String compatibleAttrib = null;
        String storeName = null;
        RBStore store = null;
        if (_sharedTypes != null) {
            for (AttributeIndex attrib : _sharedTypes) {
                if (_commonRBStores.containsKey(attrib._type + ":" + attribName)) {
                    store = (RBStore) _commonRBStores.get(attrib._type + ":" + attribName);
                    storeName = attrib._type + ":" + attribName;
                    break;
                }

            }
            if (store != null && storeName != null) {
                Iterator iteCommonStore = _commonRBStores.entrySet().iterator();
                while (iteCommonStore.hasNext()) {
                    Map.Entry entry = (Map.Entry) iteCommonStore.next();
                    String key = (String) entry.getKey();
                    RBStore entryStore = (RBStore) entry.getValue();
                    if (!storeName.equals(key) && store.equals(entryStore)) {
                        String[] tempName = key.split(":");
                        compatibleAttrib = tempName[1];
                        break;
                    }
                }
            }
        }
        return compatibleAttrib;

    }

    public AttributeIndex(String cacheName, String type) {
        _indexTable = new java.util.HashMap();
        _cacheName = cacheName;
        _type = type;
        _keyIndexTable = new java.util.HashMap();
    }

    public AttributeIndex(java.util.ArrayList attribList, String cacheName, String type) {
        _indexTable = new java.util.HashMap();
        _cacheName = cacheName;
        _keyIndexTable = new java.util.HashMap();
        _type = type;

        Initialize(attribList);
    }

    /**
     * Gets the size of the attribute index;
     */
    public final int getCount() {
        if (_indexTable != null) {
            return _indexTable.size();
        } else {
            return 0;
        }
    }

    public void Initialize(java.util.ArrayList attribList) {
        IIndexStore store = null;
        if (attribList != null && attribList.size() > 0) {

            java.util.Iterator e = attribList.iterator();

            while (e.hasNext()) {
                String attribName = e.next().toString();
                if (_commonRBStores != null && _commonRBStores.containsKey(_type + ":" + attribName)) {
                    RBStore commonStore = (RBStore) _commonRBStores.get(_type + ":" + attribName);
                    _indexTable.put(attribName, commonStore);
                } else {
                    String storeDataType = "";
                    if(this._typeMap != null)
                        storeDataType = _typeMap.getAttributeType(this.getTypeName(), attribName);
                    store = new RBStore(_cacheName, storeDataType);
                    _indexTable.put(attribName, store);
                }
            }
            if (_commonRBStores != null && _commonRBStores.containsKey(TAG_INDEX_KEY)) {
                store = (RBStore) _commonRBStores.get(TAG_INDEX_KEY);
                _indexTable.put(TAG_INDEX_KEY, store);
            } else {
                String storeDataType = "";
                storeDataType = ((RBStore)store).getStoreDataType();
                store = new RBStore(_cacheName, storeDataType);
                _indexTable.put(TAG_INDEX_KEY, store);
            }
        }

        if (!_indexTable.containsKey(TAG_INDEX_KEY) && _commonRBStores != null && _commonRBStores.containsKey(TAG_INDEX_KEY)) {
            store = (RBStore) _commonRBStores.get(TAG_INDEX_KEY);
            _indexTable.put(TAG_INDEX_KEY, store);
        }

    }

    public void AddToIndex(Object key, Object value) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException {
        java.util.HashMap attributeValues = (java.util.HashMap) ((value instanceof java.util.HashMap) ? value : null);
        Iterator valuesDic = attributeValues.entrySet().iterator();
        RedBlackNodeReference keyNode = null;

        while (valuesDic.hasNext()) {
            Map.Entry current = (Map.Entry) valuesDic.next();
            String indexKey = (String) current.getKey();
            String storeName = indexKey;
            IIndexStore store = (IIndexStore) ((_indexTable.get(indexKey) instanceof IIndexStore) ? _indexTable.get(indexKey) : null);
            keyNode = null;

            if (store == null) {
                if (TAG_INDEX_KEY.equals(indexKey)) {                                        
                    store = new RBStore(_cacheName, com.alachisoft.tayzgrid.common.util.MemoryUtil.Java_Lang_String);                        
                    _indexTable.put(indexKey, store);
                    
                } else {
                    String namedTagIndexKey = ConvertToNamedTagKey(indexKey);
                    storeName = namedTagIndexKey;
                    store = (IIndexStore) ((_indexTable.get(namedTagIndexKey) instanceof IIndexStore) ? _indexTable.get(namedTagIndexKey) : null);
                    if (store == null) {
                        if (_sharedTypes != null && _sharedTypes.size() > 0) {
                            for (AttributeIndex attrib : _sharedTypes) {
                                store = attrib.getRBStore(namedTagIndexKey);
                                if (store != null) {
                                    break;
                                }
                            }
                        }
                        if (store == null) {
                            store = new RBStore(_cacheName, com.alachisoft.tayzgrid.common.util.MemoryUtil.Java_Lang_String);
                            _indexTable.put(namedTagIndexKey, store);
                            if (_sharedTypes != null && _sharedTypes.size() > 0) {
                                for (AttributeIndex attrib : _sharedTypes) {
                                    if (!attrib._indexTable.containsKey(namedTagIndexKey)) {
                                        attrib._indexTable.put(namedTagIndexKey, store);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (store != null) {
                long prev = store.getIndexInMemorySize();
                Object val = current.getValue();

                if (val != null) {
                    keyNode = (RedBlackNodeReference) store.Add(val, key);
                }
                _attributeIndexSize += store.getIndexInMemorySize() - prev;
            }

            storeName = StringPool.PoolString(storeName);
            
            IndexInformation info;

            if (_keyIndexTable.containsKey(key)) {
                info = (IndexInformation) ((_keyIndexTable.get(key) instanceof IndexInformation) ? _keyIndexTable.get(key) : null);
            } else {
                info = new IndexInformation();
            }
            
            long prevSize = info.getIndexInMemorySize();
            info.Add(storeName, store, keyNode);
            _keyIndexTable.put(key, info);
            
            this._keyIndexInformationSize += info.getIndexInMemorySize() - prevSize;
            if(_keyIndexTable.size() > _keyIndexTableMaxCount)
                _keyIndexTableMaxCount = _keyIndexTable.size();
        }
    }

    public void RemoveFromIndex(Object key) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException {
        boolean isNodeRemoved = false;
        IndexInformation indexInfo = (IndexInformation) _keyIndexTable.get(key);
        if (indexInfo != null) {
            for (IndexStoreInformation indexStoreInfo : indexInfo.getIndexStoreInformations()) {
                isNodeRemoved = false;
                IIndexStore store = indexStoreInfo.getStore();
                if (indexStoreInfo.getIndexPosition() != null) {
                    long prevSize = store.getIndexInMemorySize();
                    isNodeRemoved = store.Remove(key, indexStoreInfo.getIndexPosition());
                    
                    _attributeIndexSize += store.getIndexInMemorySize() - prevSize;
                }
            }
            _keyIndexInformationSize -= indexInfo.getIndexInMemorySize();
        }

        _keyIndexTable.remove(key);

    }

    public void RemoveFromIndex(Object key, Object value) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException {
        java.util.HashMap attributeValues = (java.util.HashMap) ((value instanceof java.util.HashMap) ? value : null);
        Iterator valuesDic = attributeValues.entrySet().iterator();

        while (valuesDic.hasNext()) {
            Map.Entry current = (Map.Entry) valuesDic.next();

            String indexKey = (String) current.getKey();

            if (_indexTable.containsKey(indexKey) || _indexTable.containsKey(indexKey = ConvertToNamedTagKey(indexKey))) {
                IIndexStore store = (IIndexStore) ((_indexTable.get(indexKey) instanceof IIndexStore) ? _indexTable.get(indexKey) : null);
                Object val = current.getValue();

                long prev = store.getIndexInMemorySize();
                
                if (val != null) {
                    store.Remove(val, key);
                } else {
                    store.Remove("null", key);
                }

                if (store.getCount()== 0) {
                    if (TAG_INDEX_KEY.equals(indexKey) || IsNamedTagKey(indexKey)) {
                        _indexTable.remove(indexKey);
                    } else {
                        String storeDataType = ((RBStore)store).getStoreDataType();
                        _indexTable.put(indexKey, new RBStore(_cacheName, storeDataType));
                    }
                }
                _attributeIndexSize += store.getIndexInMemorySize() - prev;
            }
        }
    }

    public final IIndexStore GetStore(String attrib) {
        boolean disableException = QueryIndexManager.getDisableException();
        IIndexStore store = null;

        if (_indexTable.containsKey(attrib)) {
            store = (IIndexStore) ((_indexTable.get(attrib) instanceof IIndexStore) ? _indexTable.get(attrib) : null);
        } else {
            String namedTagKey = ConvertToNamedTagKey(attrib);

            if (_indexTable.containsKey(namedTagKey)) {
                store = (IIndexStore) ((_indexTable.get(namedTagKey) instanceof IIndexStore) ? _indexTable.get(namedTagKey) : null);
            }

            if (store == null && _sharedTypes != null && _sharedTypes.size() > 0) {
                for (AttributeIndex sharedAttrib : _sharedTypes) {
                    store = sharedAttrib.getRBStore(attrib);
                    if (store != null) {
                        break;
                    }
                }
            }
            if (disableException) {
                if (store == null) {
                    store = new HashStore();
                }
            }
        }

        return store;
    }

    public final void Clear() {
        Iterator e = _indexTable.entrySet().iterator();

        while (e.hasNext()) {
            Map.Entry current = (Map.Entry) e.next();
            IIndexStore store = (IIndexStore) ((current.getValue() instanceof IIndexStore) ? current.getValue() : null);
            store.Clear();
        }
        _keyIndexTable = new HashMap();
        
        _attributeIndexSize = 0;
    }

    public final Iterator GetEnumerator(String type, boolean forTag) {
        Iterator en = _indexTable.entrySet().iterator();

        if (!forTag) {
            while (en.hasNext()) {
                Map.Entry current = (Map.Entry) en.next();
                IIndexStore store = (IIndexStore) ((current.getValue() instanceof IIndexStore) ? current.getValue() : null);
                if (!TAG_INDEX_KEY.equals((String) current.getKey())) {
                    return store.GetEnumerator();
                }
            }
        } else {
            if (_indexTable.containsKey(TAG_INDEX_KEY)) {
                IIndexStore store = (IIndexStore) ((_indexTable.get(TAG_INDEX_KEY) instanceof IIndexStore) ? _indexTable.get(TAG_INDEX_KEY) : null);
                return store.GetEnumerator();
            }
        }

        return null;
    }

    public final IndexInformation GetIndexInformation(Object key) {
        return (IndexInformation) _keyIndexTable.get(key);
    }

    public final Iterator GetEnumerator() {
        return _indexTable.entrySet().iterator();
    }

    protected final String ConvertToNamedTagKey(String indexKey) {
        String namedTagKey = NAMED_TAG_PREFIX + indexKey;
        return namedTagKey;
    }

    public static final boolean IsNamedTagKey(String indexKey) {
        boolean result = indexKey.startsWith(NAMED_TAG_PREFIX);
        return result;
    }

    public final Object GetAttributeValue(Object key, String attributeName) {
        String storeName = attributeName;
        if (!_indexTable.containsKey(storeName) && !_indexTable.containsKey(storeName = ConvertToNamedTagKey(attributeName))) {
            throw new RuntimeException("Index is not defined for attribute '" + attributeName + "'");
        }

        IndexInformation indexInformation = GetIndexInformation(key);

        if (indexInformation == null && _sharedTypes != null) {
            for (AttributeIndex index : _sharedTypes) {
                indexInformation = index.GetIndexInformation(key);
                if (indexInformation != null) {
                    String compatibleName = index.getCompatibleAttrib(storeName);
                    if (compatibleName != null) {
                        storeName = compatibleName;
                    }
                    break;
                }
            }
        }
        java.lang.Comparable value = null;
        for (IndexStoreInformation indexStoreInfo : indexInformation.getIndexStoreInformations()) {
            if (storeName.equals(indexStoreInfo.getStoreName())) {
                if (indexStoreInfo.getIndexPosition() != null) {
                    value = indexStoreInfo.getIndexPosition().getRBReference().getKey();
                } else {
                    return null;
                }
                break;
            }
        }
        return value;
    }

    @Override
    public long getIndexInMemorySize() {
        long temp = 0;
        temp += _keyIndexInformationSize;
        temp += _attributeIndexSize;
        temp += (this._keyIndexTableMaxCount * MemoryUtil.NetHashtableOverHead);
        
        return temp;
    }
}
