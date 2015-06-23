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

package com.alachisoft.tayzgrid.caching.datasourceproviders;

import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHintType;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.common.util.LanguageContext;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperationType;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSet;
import java.io.IOException;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHelper;
import com.alachisoft.tayzgrid.caching.cacheloader.JarFileLoader;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DSWriteOperation implements ICompactSerializable {

    /**
     * Key of this operation
     */
    protected Object _key;
    /**
     * value.
     */
    protected Object _value;
    /**
     * item.
     */
    protected CacheEntry _entry;
    /**
     * operation type
     */
    protected OpCode _opCode;
    protected WriteOperation _writeOperation = null;
    /**
     */
    protected String _providerName;
    protected String _cacheId;
    protected int _retryCount = 0;

    static JarFileLoader _loader;

    public DSWriteOperation() {
    }

    public DSWriteOperation(String cacheId, Object key, Object value, CacheEntry entry, OpCode opcode, String providerName) {
        this._key = key;
        this._value = value;
        this._entry = entry;
        this._opCode = opcode;
        this._providerName = providerName;
        this._cacheId = cacheId;
    }

    /**
     * Get key associate with the this task
     */
    public final Object getKey() {
        return _key;
    }

    /**
     * Get key associate with the this task
     */
    public final Object getValue() {
        return _value;
    }

    /**
     * Get cache entry
     */
    public final CacheEntry getEntry() {
        return _entry;
    }

    /**
     * Get operation type
     */
    public final OpCode getOperationCode() {
        return _opCode;
    }

    /**
     * Get provider name
     */
    public final String getProviderName() {
        return _providerName;
    }

    public final void setProviderName(String value) {
        _providerName = value;
    }

    public final int getRetryCount() {
        return _retryCount;
    }

    public final void incrementRetryCount() {
        _retryCount++;
    }

    public final WriteOperation getWriteOperation() {
        return this._writeOperation;
    }

    public final void setWriteOperation(WriteOperation value) {
        this._writeOperation = value;
    }

    static void setClassLoader(JarFileLoader value) {
        _loader = value;
    }

    public final WriteOperation GetWriteOperation(LanguageContext languageContext) throws IOException, ClassNotFoundException {
        if (_writeOperation != null) {
            return new WriteOperation(_writeOperation.getKey(), _writeOperation.getProviderCacheItem(), _writeOperation.getOperationType(), this._retryCount);
        }
        if (_value != null) {
            _value = Deserialize(languageContext, _value, _entry.getFlag());
        } else {
            if (getEntry() != null) {
                if (_entry.getValue() instanceof CallbackEntry) {
                    _value = ((CallbackEntry) _entry.getValue()).getValue();
                } else {
                    _value = getEntry().getValue();
                }

                if (_value != null && _value instanceof UserBinaryObject) {
                    _value = ((UserBinaryObject) _value).GetFullObject();
                    
                    _value = Deserialize(languageContext, _value, _entry.getFlag());
                }
            }
        }

        ProviderCacheItem providerCacheItem = null;
        if (_entry != null) {
            providerCacheItem = GetProviderCacheItemFromCacheEntry(_entry, _value);
        }
        //WriteOperations
        return new WriteOperation(_key, providerCacheItem, SetWriteOperationType(_opCode), _retryCount);
    }

    private WriteOperationType SetWriteOperationType(OpCode opCode) {
        switch (opCode) {
            case Add:
                return WriteOperationType.Add;
            case Update:
                return WriteOperationType.Update;
            case Remove:
                return WriteOperationType.Delete;
        }
        return WriteOperationType.Add;
    }

    private Object Deserialize(LanguageContext languageContext, Object value, BitSet flag) throws IOException, ClassNotFoundException {
        switch (languageContext) {
            case DOTNET: {
                SerializationBitSet tempFlag = new SerializationBitSet();
                if (flag != null) {
                    tempFlag.setData(flag.getData());
                }
                value = SerializationUtil.safeDeserialize(value, _cacheId, tempFlag);
                break;
            }

            case JAVA: {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    if (this._loader != null) {
                        Thread.currentThread().setContextClassLoader(_loader);
                    }
                    SerializationBitSet tempFlag = new SerializationBitSet();
                    if (flag != null) {
                        tempFlag.setData(flag.getData());
                    }

                    value = (Object) SerializationUtil.safeDeserialize(value, _cacheId, tempFlag);
                } finally {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }
                break;
            }
        }
        return value;
    }

    private ProviderCacheItem GetProviderCacheItemFromCacheEntry(CacheEntry cacheEntry, Object value) {

        ProviderCacheItem providerCacheItem = new ProviderCacheItem(value);

        if (cacheEntry.getEvictionHint() != null && cacheEntry.getEvictionHint()._hintType == EvictionHintType.PriorityEvictionHint) {

            providerCacheItem.setItemPriority(((PriorityEvictionHint) cacheEntry.getEvictionHint()).getPriority());
        } else {
            providerCacheItem.setItemPriority(cacheEntry.getPriority());
        }
        if (cacheEntry.getGroupInfo() != null) {
            providerCacheItem.setGroup(cacheEntry.getGroupInfo().getGroup());
            providerCacheItem.setSubGroup(cacheEntry.getGroupInfo().getSubGroup());
        }

        java.util.Date absoluteExpiration = null;
        TimeSpan slidingExpiration = TimeSpan.ZERO;

        ExpirationHint hint = cacheEntry.getExpirationHint();
        if (hint != null) {
            providerCacheItem.setResyncItemOnExpiration(hint.getNeedsReSync());
            DependencyHelper helper = new DependencyHelper();
            tangible.RefObject<java.util.Date> tempRef_absoluteExpiration = new tangible.RefObject<java.util.Date>(absoluteExpiration);
            tangible.RefObject<TimeSpan> tempRef_slidingExpiration = new tangible.RefObject<TimeSpan>(slidingExpiration);
            helper.GetCacheDependency(hint, tempRef_absoluteExpiration, tempRef_slidingExpiration);
            absoluteExpiration = tempRef_absoluteExpiration.argvalue;
            slidingExpiration = tempRef_slidingExpiration.argvalue;
        }

        if (absoluteExpiration != null) {
            providerCacheItem.setAbsoluteExpiration(absoluteExpiration);
        }
        if (slidingExpiration != TimeSpan.ZERO) {
            providerCacheItem.setSlidingExpiration(slidingExpiration);
        }

        if (cacheEntry.getQueryInfo() != null) {
            if (cacheEntry.getQueryInfo().get("tag-info") != null) {
                HashMap tagInfo = cacheEntry.getQueryInfo().get("tag-info") instanceof HashMap ? (HashMap) cacheEntry.getQueryInfo().get("tag-info") : null;
                if (tagInfo != null) {
                    ArrayList tagsList = tagInfo.get("tags-list") instanceof ArrayList ? (ArrayList) tagInfo.get("tags-list") : null;
                    if (tagsList != null && tagsList.size() > 0) {
                        Tag[] tags = new Tag[tagsList.size()];
                        int i = 0;
                        for (Object tag : tagsList) {
                            tags[i++] = new Tag(tag.toString());
                        }

                        providerCacheItem.setTags(tags);
                    }
                }
            }

            if (cacheEntry.getQueryInfo().get("named-tag-info") != null) {
                HashMap tagInfo = cacheEntry.getQueryInfo().get("named-tag-info") instanceof HashMap ? (HashMap) cacheEntry.getQueryInfo().get("named-tag-info") : null;
                if (tagInfo != null) {
                    HashMap tagsList = tagInfo.get("named-tags-list") instanceof HashMap ? (HashMap) tagInfo.get("named-tags-list") : null;
                    if (tagsList != null) {
                        NamedTagsDictionary namedTags = new NamedTagsDictionary();

                        for (Object tagObj : tagsList.entrySet()) {
                            if (tagObj instanceof Map.Entry) {
                                Map.Entry tag = (Map.Entry) tagObj;
                                Class tagType = tag.getValue().getClass();
                                String tagKey = tag.getKey().toString();
                                try {
                                    if (tag.getValue() instanceof java.lang.Integer) {
                                        namedTags.add(tagKey, (Integer) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.Long) {
                                        namedTags.add(tagKey, (Long) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.Float) {
                                        namedTags.add(tagKey, (Float) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.Double) {
                                        namedTags.add(tagKey, (Double) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.Boolean) {
                                        namedTags.add(tagKey, (Boolean) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.Character) {
                                        namedTags.add(tagKey, (Character) tag.getValue());
                                    } else if (tag.getValue() instanceof java.lang.String) {
                                        namedTags.add(tagKey, (String) tag.getValue());
                                    } else if (tag.getValue() instanceof java.util.Date) {
                                        namedTags.add(tagKey, (Date) tag.getValue());
                                    }
                                } catch (ArgumentException e) {
                                }
                            }
                        }

                        if (namedTags.getCount() > 0) {
                            providerCacheItem.setNamedTags(namedTags);
                        }
                    }
                }
            }
        }
        providerCacheItem.setResyncProviderName(cacheEntry.getResyncProviderName());
        return providerCacheItem;
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_key);
        writer.writeObject(_value);
        writer.writeObject(_entry);
        writer.writeObject(_opCode);
        writer.writeObject(_providerName);
        writer.writeObject(_cacheId);
        writer.writeInt(_retryCount);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        Object tempVar = reader.readObject();
        _key = tempVar;
        _value = reader.readObject();
        Object tempVar2 = reader.readObject();
        _entry = (CacheEntry) ((tempVar2 instanceof CacheEntry) ? tempVar2 : null);
        _opCode = (OpCode) reader.readObject();
        Object tempVar3 = reader.readObject();
        _providerName = (String) ((tempVar3 instanceof String) ? tempVar3 : null);
        Object tempVar4 = reader.readObject();
        _cacheId = (String) ((tempVar4 instanceof String) ? tempVar4 : null);
        _retryCount = reader.readInt();
    }
}
