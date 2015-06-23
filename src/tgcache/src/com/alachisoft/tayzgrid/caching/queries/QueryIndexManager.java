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
import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.datastructures.RedBlackException;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor.IAsyncTask;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class QueryIndexManager implements ISizableIndex {

    String TAG_INDEX_KEY = "$Tag$";
    String NAMED_TAG_PREFIX = "$NamedTagAttribute$";
    private static boolean _disableException = false;
    //in case of DisableException is true, exception will not be thrown, and return new attribute index. 

    public static final boolean getDisableException() {
        if (ServicePropValues.Cache_DisableIndexNotDefinedException != null) {
            _disableException = Boolean.parseBoolean(ServicePropValues.Cache_DisableIndexNotDefinedException);
        }
        return _disableException;
    }

    private static class IndexAddTask implements IAsyncTask {

        private Object _key;
        private CacheEntry _entry;
        private QueryIndexManager _indexManager;

        public IndexAddTask(QueryIndexManager indexManager, Object key, CacheEntry value) {
            _key = key;
            _entry = value;
            _indexManager = indexManager;
        }

        public void Process() {
            try {
                _indexManager.AddToIndex(_key, _entry);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static class IndexRemoveTask implements IAsyncTask {

        private Object _key;
        private CacheEntry _entry;
        private QueryIndexManager _indexManager;

        public IndexRemoveTask(QueryIndexManager indexManager, Object key, CacheEntry value) {
            _key = key;
            _entry = value;
            _indexManager = indexManager;
        }

        public void Process() {
            try {
                _indexManager.RemoveFromIndex(_key, _entry.getObjectType());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
    private AsyncProcessor _asyncProcessor;
    private boolean _indexForAll;
    private IndexedLocalCache _cache;
    private java.util.Map _props;
    protected String _cacheName;
    protected TypeInfoMap _typeMap;
    protected java.util.HashMap _indexMap;
    protected java.util.HashMap _dataSharingTypesMap;
    protected java.util.HashMap<String, AttributeIndex> _sharedAttributeIndex;
    protected long _queryIndexMemorySize;

    public QueryIndexManager(java.util.Map props, IndexedLocalCache cache, String cacheName, java.util.HashMap dataSharingKnownTypes) {
        _indexMap = new java.util.HashMap();
        _cache = cache;
        _props = props;
        _cacheName = cacheName;
        _dataSharingTypesMap = dataSharingKnownTypes;
        if (_dataSharingTypesMap != null && _dataSharingTypesMap.size() > 0) {
            _sharedAttributeIndex = new HashMap<String, AttributeIndex>();
        }
    }

    public final TypeInfoMap getTypeInfoMap() {
        return _typeMap;
    }

    public final AsyncProcessor getAsyncProcessor() {
        return _asyncProcessor;
    }

    public final boolean getIndexForAll() {
        return _indexForAll;
    }

    public final java.util.HashMap getIndexMap() {
        return _indexMap;
    }

    public final void createSharedTypeAttributeIndex(HashMap knownSharedClasses, HashMap indexedClasses) {
        HashMap commonRBStore = new HashMap();
        HashMap<String, AttributeIndex> sharedAttributeIndexMap = new HashMap<String, AttributeIndex>();
        Iterator iteouterSharedTypes = knownSharedClasses.entrySet().iterator();

        commonRBStore.put(AttributeIndex.TAG_INDEX_KEY, new RBStore(_cacheName, MemoryUtil.Net_System_String));

        while (iteouterSharedTypes.hasNext()) {
            Map.Entry outerEntry = (Map.Entry) iteouterSharedTypes.next();
            HashMap outerEntryValue = (HashMap) outerEntry.getValue();
            String name = (String) outerEntryValue.get("name");
            String[] temp = name.split(":");
            String outerTypeName = temp[0];
            //Create Attribute Index even if not queryindexed
            sharedAttributeIndexMap.put(outerTypeName, new AttributeIndex(_cacheName, outerTypeName));

            if (indexedClasses.size() > 0 && isQueryindexed(outerTypeName, indexedClasses)) {
                HashMap outerTypeAttributes = (java.util.HashMap) outerEntryValue.get("attribute");
                if (outerTypeAttributes != null) {
                    Iterator iteOuterTypeAttribute = outerTypeAttributes.entrySet().iterator();
                    while (iteOuterTypeAttribute.hasNext()) {
                        Map.Entry tempEntry = (Map.Entry) iteOuterTypeAttribute.next();
                        HashMap outerAttributeMeta = (HashMap) tempEntry.getValue();

                        String outerOrderNo = (String) outerAttributeMeta.get("order");
                        String outerAttributeName = (String) outerAttributeMeta.get("name");
                        if (isQueryindexedAttribute(outerTypeName, outerAttributeName, indexedClasses)) {
                            Iterator iteInnerSharedTypes = knownSharedClasses.entrySet().iterator();
                            while (iteInnerSharedTypes.hasNext()) {
                                Map.Entry innerEntry = (Map.Entry) iteInnerSharedTypes.next();

                                HashMap innerEntryValue = (HashMap) innerEntry.getValue();
                                String name1 = (String) innerEntryValue.get("name");
                                String[] temp1 = name1.split(":");
                                String innerTypeName = temp1[0];
                                if (!outerTypeName.equals(innerTypeName) && isQueryindexed(innerTypeName, indexedClasses)) {
                                    HashMap innerTypeAttributes = (java.util.HashMap) ((java.util.HashMap) innerEntry.getValue()).get("attribute");

                                    Iterator iteInnerTypeAttribute = innerTypeAttributes.entrySet().iterator();
                                    while (iteInnerTypeAttribute.hasNext()) {

                                        Map.Entry tempEntry1 = (Map.Entry) iteInnerTypeAttribute.next();
                                        HashMap innerAttributeMeta = (HashMap) tempEntry1.getValue();

                                        String innerorderNo = (String) innerAttributeMeta.get("order");
                                        String innerAttributeName = (String) innerAttributeMeta.get("name");
                                        if (innerorderNo.equals(outerOrderNo) && isQueryindexedAttribute(innerTypeName, innerAttributeName, indexedClasses)) {
                                            if (commonRBStore.containsKey(outerTypeName + ":" + outerAttributeName)) {
                                                RBStore commonRB = (RBStore) commonRBStore.get(outerTypeName + ":" + outerAttributeName);
                                                commonRBStore.put(innerTypeName + ":" + innerAttributeName, commonRB);
                                                break;
                                            } else {
                                                String storeDataType = _typeMap.getAttributeType(innerTypeName, innerAttributeName);
                                                RBStore commonRB = new RBStore(_cacheName, storeDataType);
                                                commonRBStore.put(innerTypeName + ":" + innerAttributeName, commonRB);
                                                commonRBStore.put(outerTypeName + ":" + outerAttributeName, commonRB);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sharedAttributeIndexMap.size() > 0) {
            Iterator iteSharedIndexMap = sharedAttributeIndexMap.entrySet().iterator();
            while (iteSharedIndexMap.hasNext()) {
                ArrayList<AttributeIndex> sharedTypes = new ArrayList<AttributeIndex>();
                Map.Entry outerEntry = (Map.Entry) iteSharedIndexMap.next();
                String outerTypeName = (String) outerEntry.getKey();
                AttributeIndex outerSharedIndex = (AttributeIndex) outerEntry.getValue();
                for (Map.Entry innerEntry : sharedAttributeIndexMap.entrySet()) {
                    String innerTypeName = (String) innerEntry.getKey();
                    if (!innerTypeName.equals(outerTypeName)) {
                        AttributeIndex innerSharedIndex = (AttributeIndex) innerEntry.getValue();
                        sharedTypes.add(innerSharedIndex);
                    }
                }
                outerSharedIndex.setCommonRBStores(commonRBStore);
                outerSharedIndex.setSharedTypes(sharedTypes);
                _sharedAttributeIndex.put(outerTypeName, outerSharedIndex);
            }
        }
    }

    public final boolean isQueryindexed(String typeName, HashMap indexedClasses) {
        Iterator ie = indexedClasses.entrySet().iterator();
        while (ie.hasNext()) {
            Map.Entry current_1 = (Map.Entry) ie.next();
            java.util.HashMap innerProps = (java.util.HashMap) ((current_1.getValue() instanceof java.util.HashMap) ? current_1.getValue() : null);
            String queryIndexedTypename = "";

            if (innerProps != null) {
                queryIndexedTypename = (String) innerProps.get("id");
                if (typeName.equals(queryIndexedTypename)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final boolean isQueryindexedAttribute(String typeName, String attributeName, HashMap indexedClasses) {
        Iterator ie = indexedClasses.entrySet().iterator();
        while (ie.hasNext()) {
            Map.Entry current_1 = (Map.Entry) ie.next();
            java.util.HashMap innerProps = (java.util.HashMap) ((current_1.getValue() instanceof java.util.HashMap) ? current_1.getValue() : null);
            String queryIndexedTypeName = "";
            if (innerProps != null) {
                queryIndexedTypeName = (String) innerProps.get("id");
                if (typeName.equals(queryIndexedTypeName)) {
                    java.util.ArrayList attribList = new java.util.ArrayList();
                    Iterator en = innerProps.entrySet().iterator();
                    while (en.hasNext()) {
                        Map.Entry current_2 = (Map.Entry) en.next();
                        java.util.HashMap attribs = (java.util.HashMap) ((current_2.getValue() instanceof java.util.HashMap) ? current_2.getValue() : null);
                        if (attribs != null) {
                            Iterator ide = attribs.entrySet().iterator();
                            while (ide.hasNext()) {
                                Map.Entry current_3 = (Map.Entry) ide.next();
                                java.util.HashMap attrib = (java.util.HashMap) ((current_3.getValue() instanceof java.util.HashMap) ? current_3.getValue() : null);
                                if (attrib != null) {
                                    String tempAttrib = (String) attrib.get("id");
                                    if (attributeName.equals(tempAttrib)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public final boolean checkForSameClassNames(java.util.HashMap knownSharedClasses) {
        java.util.Collection coll = knownSharedClasses.values();

        HashMap[] values = new HashMap[coll.size()];

        values = (HashMap[]) coll.toArray(values);

        for (int outer = 0; outer < values.length - 1; outer++) {
            java.util.HashMap outerValue = (java.util.HashMap) values[outer];

            String name = (String) outerValue.get("name");
            String[] temp = name.split("[:]", -1);
            String outerTypeName = temp[0];

            for (int inner = outer + 1; inner < values.length; inner++) {
                java.util.HashMap innerValue = (java.util.HashMap) values[inner];

                String name1 = (String) innerValue.get("name");
                String[] temp1 = name1.split("[:]", -1);
                String innerTypeName = temp1[0];

                if (outerTypeName.equals(innerTypeName)) {
                    return false;
                }
            }
        }
        return true;
    }

    public final void manipulateDataSharing(java.util.HashMap indexClasses) {
        if (_dataSharingTypesMap != null && _dataSharingTypesMap.size() > 0 && indexClasses != null) {
            Iterator ieDataSharingTypeMap = _dataSharingTypesMap.entrySet().iterator();
            while (ieDataSharingTypeMap.hasNext()) {
                Map.Entry current_1 = (Map.Entry) ieDataSharingTypeMap.next();
                java.util.HashMap typeHashMap = (java.util.HashMap) ((current_1.getValue() instanceof java.util.HashMap) ? current_1.getValue() : null);
                if (typeHashMap != null && typeHashMap.containsKey("known-classes")) {
                    java.util.HashMap knownSharedClasses = (java.util.HashMap) typeHashMap.get("known-classes");
                    if (checkForSameClassNames(knownSharedClasses)) {
                        createSharedTypeAttributeIndex(knownSharedClasses, indexClasses);
                    }
                }
            }
        }
    }

    public boolean Initialize() {
        boolean indexedDefined = false;
        if (_props != null) {
            if (_props.containsKey("index-for-all")) {
                _indexForAll = (Boolean) (_props.get("index-for-all"));
                indexedDefined = _indexForAll;
            }

            if (_props.containsKey("index-classes")) {
                java.util.HashMap indexClasses = (java.util.HashMap) ((_props.get("index-classes") instanceof java.util.HashMap) ? _props.get("index-classes") : null);
                _typeMap = new TypeInfoMap(indexClasses);
                manipulateDataSharing(indexClasses);

                Iterator ie = indexClasses.entrySet().iterator();
                while (ie.hasNext()) {
                    Map.Entry current_1 = (Map.Entry) ie.next();
                    java.util.HashMap innerProps = (java.util.HashMap) ((current_1.getValue() instanceof java.util.HashMap) ? current_1.getValue() : null);
                    String typename = "";

                    if (innerProps != null) {
                        typename = (String) innerProps.get("id");
                        java.util.ArrayList attribList = new java.util.ArrayList();
                        Iterator en = innerProps.entrySet().iterator();
                        while (en.hasNext()) {
                            Map.Entry current_2 = (Map.Entry) en.next();
                            java.util.HashMap attribs = (java.util.HashMap) ((current_2.getValue() instanceof java.util.HashMap) ? current_2.getValue() : null);
                            if (attribs != null) {
                                Iterator ide = attribs.entrySet().iterator();
                                while (ide.hasNext()) {
                                    Map.Entry current_3 = (Map.Entry) ide.next();
                                    java.util.HashMap attrib = (java.util.HashMap) ((current_3.getValue() instanceof java.util.HashMap) ? current_3.getValue() : null);
                                    if (attrib != null) {
                                        attribList.add((String) ((attrib.get("id") instanceof String) ? attrib.get("id") : null));
                                    }
                                }
                            }
                        }

                        //attrib level index.
                        if (attribList.size() > 0) {
                            if (_sharedAttributeIndex != null && _sharedAttributeIndex.containsKey(typename)) {
                                AttributeIndex attribIndex = _sharedAttributeIndex.get(typename);
                                attribIndex.Initialize(attribList);
                                _indexMap.put(typename, attribIndex);
                            } else {
                                _indexMap.put(typename, new AttributeIndex(attribList, _cacheName, typename, _typeMap));
                            }
                        } //just a key level index.
                        else {
                            _indexMap.put(typename, new TypeIndex(typename, _indexForAll));
                        }
                        indexedDefined = true;
                    }
                }
            }
        } else {
            _indexMap.put("default", new VirtualQueryIndex(_cache));
            manipulateDataSharing(new java.util.HashMap());
        }
        if (indexedDefined) {
            _asyncProcessor = new AsyncProcessor(_cache.getContext().getCacheLog());
            _asyncProcessor.Start();
        }
        return indexedDefined;
    }

    public final void dispose() {
        if (_asyncProcessor != null) {
            _asyncProcessor.Stop();
            _asyncProcessor = null;
        }
        if (_indexMap != null) {
            _indexMap.clear();
            _indexMap = null;
        }
        _cache = null;
    }

    public void AddToIndex(Object key, Object value) throws ClassNotFoundException, RedBlackException {

        CacheEntry entry = (CacheEntry) value;
        Object tempVar = entry.getQueryInfo().get("query-info");
        java.util.HashMap queryInfo = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
        if (queryInfo == null) {
            return;
        }

        synchronized (_indexMap) {
            Iterator queryInfoEnumerator = queryInfo.entrySet().iterator();

            if (_typeMap != null) {
                while (queryInfoEnumerator.hasNext()) {
                    Map.Entry current = (Map.Entry) queryInfoEnumerator.next();
                    int handleId = (Integer) current.getKey();

                    String type = _typeMap.getTypeName(handleId);
                    if (_indexMap.containsKey(type)) {
                        java.util.HashMap indexAttribs = new java.util.HashMap();
                        java.util.HashMap metaInfoAttribs = new java.util.HashMap();

                        java.util.ArrayList values = (java.util.ArrayList) current.getValue();

                        java.util.ArrayList attribList = _typeMap.getAttribList(handleId);
                        for (int i = 0; i < attribList.size(); i++) {
                            String attribute = attribList.get(i).toString();
                            String val = (String) ((_typeMap.getAttributes(handleId).get(attribList.get(i)) instanceof String) ? _typeMap.getAttributes(handleId).get(attribList.get(i)) : null);
                            java.lang.Class t1 = ConvertCLRtoJavaMapping(val);
                            Object obj = null;

                            if (values.get(i) != null) {
                                try {
                                    if ((t1 == java.util.Date.class) && !(((String) values.get(i)).equals("NCNULL"))) {
                                        NCDateTime ncd = new NCDateTime(Long.parseLong((String) values.get(i)));
                                        obj = ncd.getDate();
                                    } else {
                                        obj = new ConfigurationBuilder().ConvertToPrimitive(t1, values.get(i).toString(), "");
                                    }
                                } catch (Exception e) {
                                    throw new ClassNotFoundException("Cannot convert '" + values.get(i) + "' to " + t1.toString());
                                }
                                indexAttribs.put(attribute, obj);
                            } else {
                                indexAttribs.put(attribute, null);
                            }

                            metaInfoAttribs.put(attribute, obj);
                        }

                        entry.setMetaInformation(new MetaInformation(metaInfoAttribs));
                        entry.getMetaInformation().setCacheKey(key);
                        entry.getMetaInformation().setType(_typeMap.getTypeName(handleId));
                        entry.setObjectType(_typeMap.getTypeName(handleId));

                        IQueryIndex index = (IQueryIndex) _indexMap.get(type);
                        
                        long prevSize = index.getIndexInMemorySize();
                        index.AddToIndex(key, indexAttribs);
                        
                        this._queryIndexMemorySize += index.getIndexInMemorySize() - prevSize;
                    }
                }
            }
        }
    }

    public final Class ConvertCLRtoJavaMapping(String dataType) throws ClassNotFoundException {
        try {
            if (dataType.startsWith("System")) {
                if (dataType.equals("System.String")) {
                    return Class.forName("java.lang.String");
                } else if (dataType.equals("System.Boolean")) {
                    return Class.forName("java.lang.Boolean");
                } else if (dataType.equals("System.Char")) {
                    return Class.forName("java.lang.Character");
                } else if (dataType.equals("System.Double")) {
                    return Class.forName("java.lang.Double");
                } else if (dataType.equals("System.Single")) {
                    return Class.forName("java.lang.Float");
                } else if (dataType.equals("System.Int16")) {
                    return Class.forName("java.lang.Short");
                } else if (dataType.equals("System.Int32")) {
                    return Class.forName("java.lang.Integer");
                } else if (dataType.equals("System.Int64")) {
                    return Class.forName("java.lang.Long");
                } else if (dataType.equals("System.Byte")) {
                    return Class.forName("java.lang.Byte");
                } else if (dataType.equals("System.SByte")) {
                    return Class.forName("java.lang.Byte");
                } else if (dataType.equals("System.DateTime")) {
                    return Class.forName("java.util.Date");
                } else if (dataType.equals("System.UInt16")) {
                    return Class.forName("java.lang.Integer");
                } else if (dataType.equals("System.UInt32")) {
                    return Class.forName("java.lang.Long");
                } else if (dataType.equals("System.UInt64")) {
                    return Class.forName("java.math.BigInteger");
                } else if (dataType.equals("System.Decimal")) {
                    return Class.forName("java.math.BigDecimal");
                } else {
                    return Class.forName(dataType);
                }
            } else {
                try {
                    return Class.forName(dataType);
                } catch (ClassNotFoundException classNotFoundException1) {
                    throw classNotFoundException1;
                }
            }
        } catch (ClassNotFoundException classNotFoundException) {
            try {
                return Class.forName(dataType);
            } catch (ClassNotFoundException classNotFoundException1) {
                throw classNotFoundException;
            }
        }
    }

    public final void AsyncAddToIndex(Object key, CacheEntry value) {
        synchronized (_asyncProcessor) {
            _asyncProcessor.Enqueue(new IndexAddTask(this, key, value));
        }
    }

    public void RemoveFromIndex(Object key, String value) throws com.alachisoft.tayzgrid.common.datastructures.RedBlackException {
        if (value == null) {
            return;
        }
        synchronized (_indexMap) {
            String type = value.toString();
            if (_indexMap.containsKey(type)) {
                IQueryIndex index = (IQueryIndex) _indexMap.get(type);
                
                long prevSize = index.getIndexInMemorySize();
                index.RemoveFromIndex(key);
                
                this._queryIndexMemorySize += index.getIndexInMemorySize() - prevSize;
            }
        }
    }

    public void RemoveFromIndex(Object key, Object value) throws RedBlackException, ClassNotFoundException {
        if (value == null) {
            return;
        }
        synchronized (_indexMap) {
            Iterator queryInfoDic = ((java.util.HashMap) value).entrySet().iterator();
            while (queryInfoDic.hasNext()) {
                Map.Entry current = (Map.Entry) queryInfoDic.next();
                int handleId = (Integer) current.getKey();
                String type = _typeMap.getTypeName(handleId);
                if (_indexMap.containsKey(type)) {
                    java.util.HashMap attribs = new java.util.HashMap();
                    java.util.ArrayList values = (java.util.ArrayList) current.getValue();

                    java.util.ArrayList attribList = _typeMap.getAttribList(handleId);

                    for (int i = 0; i < attribList.size(); i++) {
                        String val = (String) ((_typeMap.getAttributes(handleId).get(attribList.get(i)) instanceof String) ? _typeMap.getAttributes(handleId).get(attribList.get(i)) : null);

                        java.lang.Class t1 = ConvertCLRtoJavaMapping(val);
                        Object obj = null;
                        if (values.get(i) != null) {
                            try {
                                if ((t1 == java.util.Date.class) && !(((String) values.get(i)).equals("NCNULL"))) {
                                    NCDateTime ncd = new NCDateTime(Long.parseLong(values.get(i).toString()));
                                    obj = ncd.getDate();
                                } else {
                                    obj = new ConfigurationBuilder().ConvertToPrimitive(t1, values.get(i).toString(), "");
                                }
                            } catch (Exception e) {
                                throw new ClassNotFoundException("Cannot convert '" + values.get(i) + "' to " + t1.toString());
                            }

                            String attribute = attribList.get(i).toString();

                            if (obj != null && obj instanceof String) {
                                attribs.put(attribute, ((String) obj).toLowerCase());
                            } else {
                                attribs.put(attribute, obj);
                            }
                        }
                    }

                    IQueryIndex index = (IQueryIndex) _indexMap.get(type);
                    
                    long prevSize = index.getIndexInMemorySize();
                    index.RemoveFromIndex(key, attribs);
                    
                    this._queryIndexMemorySize += index.getIndexInMemorySize() - prevSize;
                }
            }
        }
    }

    public final void AsyncRemoveFromIndex(Object key, CacheEntry value) {
        synchronized (_asyncProcessor) {
            _asyncProcessor.Enqueue(new IndexRemoveTask(this, key, value));
        }
    }

    public final void Clear() {
        if (_indexMap != null) {
            synchronized (_indexMap) {
                Iterator e = _indexMap.entrySet().iterator();
                while (e.hasNext()) {
                    Map.Entry current = (Map.Entry) e.next();
                    IQueryIndex index = (IQueryIndex) ((current.getValue() instanceof IQueryIndex) ? current.getValue() : null);
                    index.Clear();
                }
                this._queryIndexMemorySize = 0;
            }
        }
    }

    public java.util.HashMap GetQueryInfo(Object key, Object value) {
        java.util.HashMap queryInfo = new java.util.HashMap();
        java.util.HashMap queryIndex = new java.util.HashMap();
        CacheEntry entry = (CacheEntry) value;
        if (entry.getObjectType() == null) {
            return queryInfo;
        }
        IQueryIndex index = (IQueryIndex) _indexMap.get(entry.getObjectType());
        IndexInformation indexInformation = index.GetIndexInformation(key);

        if (_typeMap != null) {
            int handleId = _typeMap.getHandleId(entry.getObjectType());
            if (handleId > -1) {
                java.util.ArrayList attributes = _typeMap.getAttribList(handleId);

                java.util.ArrayList attributeValues = new java.util.ArrayList();

                for (int i = 0; i < attributes.size(); i++) {
                    for (IndexStoreInformation indexStoreInfo : indexInformation.getIndexStoreInformations()) {

                        if (attributes.get(i).toString().equals(indexStoreInfo.getStoreName())) {
                            if (indexStoreInfo.getIndexPosition() == null) {
                                attributeValues.add(null);
                            } else {
                                Object val = indexStoreInfo.getIndexPosition().getRBReference().getKey();

                                String objValue = null;

                                if (val instanceof java.util.Date) {
                                    long ticks = HelperFxn.getUTCTicks((java.util.Date) val);
                                    objValue = Long.toString(ticks);
                                } else {
                                    objValue = val.toString();
                                }

                                attributeValues.add(objValue);
                            }
                            break;
                        }
                    }

                }
                queryIndex.put(handleId, attributeValues);
                queryInfo.put("query-info", queryIndex);
            }
        }

        java.util.HashMap namedTagInfo = new java.util.HashMap();
        java.util.HashMap namedTagsList = new java.util.HashMap();

        java.util.HashMap tagInfo = new java.util.HashMap();
        java.util.ArrayList tagsList = new java.util.ArrayList();

        for (IndexStoreInformation indexStoreinfo : indexInformation.getIndexStoreInformations()) {
            if (AttributeIndex.IsNamedTagKey(indexStoreinfo.getStoreName())) {
                if (indexStoreinfo.getIndexPosition() != null) {
                    namedTagsList.put(ConvertToNamedTag(indexStoreinfo.getStoreName().toString()), indexStoreinfo.getIndexPosition().getRBReference().getKey());
                }
            } else if (indexStoreinfo.getStoreName().equals(TAG_INDEX_KEY)) {
                if (indexStoreinfo.getIndexPosition() != null) {
                    tagsList.add(indexStoreinfo.getIndexPosition().getRBReference().getKey());
                }
            }
        }

        namedTagInfo.put("type", entry.getObjectType());
        namedTagInfo.put("named-tags-list", namedTagsList);
        queryInfo.put("named-tag-info", namedTagInfo);

        tagInfo.put("type", entry.getObjectType());
        tagInfo.put("tags-list", tagsList);
        queryInfo.put("tag-info", tagInfo);

        return queryInfo;

    }

    public final String ConvertToNamedTag(String indexKey) {
        String namedTagKey = indexKey.replace(NAMED_TAG_PREFIX, "");
        return namedTagKey;
    }
        
    @Override
    public long getIndexInMemorySize() {
        return _queryIndexMemorySize;
    }
}
