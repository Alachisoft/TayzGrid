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

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class EventCacheEntry implements InternalCompactSerializable {

    private String _group;
    private String _subGroup;
    private long _version;
    private int _priority;
    private boolean _resyncExpiredItems;
    private String _reSyncProviderCacheItem;
    private Object _value;
    private BitSet _flags;

    public final String getGroup() {
        return _group;
    }

    public final void setGroup(String value) {
        _group = value;
    }

    public final String getSubGroup() {
        return _subGroup;
    }

    public final void setSubGroup(String value) {
        _subGroup = value;
    }

    public final long getVersion() {
        return _version;
    }

    public final void setVersion(long value) {
        _version = value;
    }

    public final int getPriority() {
        return _priority;
    }

    public final void setPriority(int value) {
        _priority = value;
    }

    public final boolean getReSyncExpiredItems() {
        return _resyncExpiredItems;
    }

    public final void setReSyncExpiredItems(boolean value) {
        _resyncExpiredItems = value;
    }

    public final String getReSyncProviderCacheItem() {
        return _reSyncProviderCacheItem;
    }

    public final void setReSyncProviderCacheItem(String value) {
        _reSyncProviderCacheItem = value;
    }

    public final Object getValue() {
        return _value;
    }

    public final void setValue(Object value) {
        _value = value;
    }

    public final BitSet getFlags() {
        return _flags;
    }

    public final void setFlags(BitSet value) {
        _flags = value;
    }

    public EventCacheEntry(CacheEntry cacheEntry) {
        if (cacheEntry.getGroupInfo() != null) {
            setGroup(cacheEntry.getGroupInfo().getGroup());
            setSubGroup(cacheEntry.getGroupInfo().getSubGroup());
        }
        setVersion(cacheEntry.getVersion());
        setPriority(cacheEntry.getPriority().value());
        if (cacheEntry.getExpirationHint() != null) {
            setReSyncExpiredItems(cacheEntry.getExpirationHint().getNeedsReSync());
        }
        setReSyncProviderCacheItem(cacheEntry.getResyncProviderName());
    }
    
    public EventCacheEntry(){}

    public final void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        Object tempVar = reader.ReadObject();
        setGroup((String) ((tempVar instanceof String) ? tempVar : null));
        Object tempVar2 = reader.ReadObject();
        setSubGroup((String) ((tempVar2 instanceof String) ? tempVar2 : null));
        setVersion(reader.ReadInt64());
        setPriority(reader.ReadInt32());
        setReSyncExpiredItems(reader.ReadBoolean());
        Object tempVar3 = reader.ReadObject();
        setReSyncProviderCacheItem((String) ((tempVar3 instanceof String) ? tempVar3 : null));
        setFlags(new BitSet(reader.ReadByte()));
        setValue(reader.ReadObject());
    }

    public final void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(getGroup());
        writer.WriteObject(getSubGroup());
        writer.Write(getVersion());
        writer.Write(getPriority());
        writer.Write(getReSyncExpiredItems());
        writer.WriteObject(getReSyncProviderCacheItem());
        writer.Write(getFlags().getData());
        writer.WriteObject(getValue());
    }
}
