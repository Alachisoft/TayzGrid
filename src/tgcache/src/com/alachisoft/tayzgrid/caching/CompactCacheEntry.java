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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.common.BitSet;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class CompactCacheEntry implements ICompactSerializable, IRentableObject {

    private Object _key;
    private Object _value;
    private BitSet _flag;
    private ExpirationHint _dependency;
 
    private long _expiration;
    private byte _options;
    private Object _itemRemovedCallback;
    private String _group;
    private String _subgroup;
    private int _rentId;
    private java.util.HashMap _queryInfo;
    private java.util.ArrayList _keysDependingOnMe;
    private Object _lockId;
    private LockAccessType _accessType = getLockAccessType().values()[0];
    private long _version;
    private String _providerName;
    private String _resyncProviderName;

    /**
     *
     *
     * @param key
     * @param value

     * @param expiration
     * @param options
     * @param itemRemovedCallback
     * @param group
     * @param subgroup
     * @param queryInfo
     * @param Flag
     * @param resyncProviderName
     * @param lockId
     * @param version
     * @param providername
     * @param accessType
     */
    public CompactCacheEntry(Object key, Object value, long expiration, byte options, Object itemRemovedCallback, String group, String subgroup, java.util.HashMap queryInfo, BitSet Flag, Object lockId, long version, LockAccessType accessType, String providername, String resyncProviderName) ///#else
    {
        _key = key;
        _flag = Flag;
        _value = value;

       
        _expiration = expiration;
        _options = options;
        _itemRemovedCallback = itemRemovedCallback;
        if (group != null) {
            _group = group;
            if (subgroup != null) {
                _subgroup = subgroup;
            }
        }
        _queryInfo = queryInfo;

        _lockId = lockId;
        _accessType = accessType;
        _version = version;
        _providerName = providername;
        _resyncProviderName = resyncProviderName;
    }

    public CompactCacheEntry() {
    }

    public final Object getKey() {
        return _key;
    }

    public final Object getValue() {
        return _value;
    }

    public final BitSet getFlag() {
        return _flag;
    }

    public final long getExpiration() {
        return _expiration;
    }

    public final Object getLockId() {
        return _lockId;
    }

    public final LockAccessType getLockAccessType() {
        return _accessType;
    }

    public final String getProviderName() {
        return _providerName;
    }

    public final String getResyncProviderName() {
        return _resyncProviderName;
    }

    public final long getVersion() {
        return _version;
    }

    public final ExpirationHint getDependency() {
        return _dependency;
    }

    public final byte getOptions() {
        return _options;
    }

    public final Object getCallback() {
        return _itemRemovedCallback;
    }

    public final String getGroup() {
        return _group;
    }

    public final String getSubGroup() {
        return _subgroup;
    }

    public final java.util.HashMap getQueryInfo() {
        return _queryInfo;
    }

    public final java.util.ArrayList getKeysDependingOnMe() {
        return _keysDependingOnMe;
    }

    public final void setKeysDependingOnMe(java.util.ArrayList value) {
        _keysDependingOnMe = value;
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _key = reader.readObject();
        _value = reader.readObject();
        _expiration = reader.readLong();
        _dependency = ExpirationHint.ReadExpHint(reader); 
        _options = reader.readByte();
        _itemRemovedCallback = reader.readObject();
        _group = (String) reader.readObject();
        _subgroup = (String) reader.readObject();
        _queryInfo = (java.util.HashMap) reader.readObject();
        _keysDependingOnMe = (java.util.ArrayList) reader.readObject();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_key);
        writer.writeObject(_value);
        writer.writeLong(_expiration);
        ExpirationHint.WriteExpHint(writer, _dependency);
        writer.writeByte(_options);
        writer.writeObject(_itemRemovedCallback);
        writer.writeObject(_group);
        writer.writeObject(_subgroup);
        writer.writeObject(_queryInfo);
        writer.writeObject(_keysDependingOnMe);
    }

    @Override
    public final int getRentId() {
        return _rentId;
    }

    @Override
    public final void setRentId(int value) {
        _rentId = value;
    }

    public final void Reset() {
        _key = null;
        _value = null;
        _dependency = null;
        _expiration = 0;
        _options = 0;
        _itemRemovedCallback = null;
        _group = null;
        _subgroup = null;
        _queryInfo = null;

    }
}
