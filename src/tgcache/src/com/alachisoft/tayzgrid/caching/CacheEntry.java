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

import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHintType;
import com.alachisoft.tayzgrid.caching.autoexpiration.NodeExpiration;
import com.alachisoft.tayzgrid.common.stats.HPTime;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.locking.LockManager;
import com.alachisoft.tayzgrid.common.datastructures.IStreamItem;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.ICloneable;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.BlockDataOutputStream;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.BlockDataInputStream;
import com.alachisoft.tayzgrid.serialization.core.io.ICustomSerializable;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockMetaInfo;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class CacheEntry extends CacheItemBase implements IDisposable, ICloneable, ICompactSerializable, ICustomSerializable, ISizable, IStreamItem, java.io.Serializable {

    private static final int PrimitiveDTSize = 200;
    //private static final int FLATTENED = 0x01;

    private static byte COMPRESSED = 0x02;

    /**
     * A Provider name for re-synchronization of cache
     */
    private String _resyncProviderName;
    private String _providerName;
    /**
     * The flags for the entry.
     */
    private BitSet _bitset = new BitSet();
    /**
     * The eviction hint to be associated with the object.
     */
    private EvictionHint _evh;
    /**
     * The expiration hint to be associated with the object.
     */
    private ExpirationHint _exh;
    /**
     * The group with which this item is related.
     */
    private GroupInfo _grpInfo = null;
    /**
     * The query information for this item.
     */
    private java.util.HashMap _queryInfo = null;
    
    /**
     * Time at which this item was created.
     */
    private java.util.Date _creationTime = new java.util.Date(0);
    /**
     * Time at which this item was Last modified.
     */
    private java.util.Date _lastModifiedTime = new java.util.Date(0);
    private CacheItemPriority _priorityValue = CacheItemPriority.values()[6];
    private LockMetaInfo _lockMetaInfo = null;
    private long _size = -1;
    private long _version = 1;
    private MetaInformation _metaInformation;
    private String _type = null;

    /**
    * Size of key associated with this cache entry.
    */
    private long _keySize = -1;
    
    public CacheEntry() {
        try {
            NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
            java.util.Date lockDate = time.getDate();
        } catch (Exception e) {
        }
    }

    /**
     * Constructor
     *
     * @param val the object to be added to the cache
     * @param expiryHint expiration hint for the object
     * @param evictionHint eviction hint for the object
     */
    public CacheEntry(Object val, ExpirationHint expiryHint, EvictionHint evictionHint) {
        super(val);
        _exh = expiryHint;
        _evh = evictionHint;

        //if (val is byte[]) _flags.SetBit(FLATTENED);
        //_bitset.SetBit((byte) BitSetConstants.Flattened);

        _creationTime = new java.util.Date();
        _lastModifiedTime = new java.util.Date();
    }

    
    public LockMetaInfo getLockMetaInfo(){
        return _lockMetaInfo;
    }
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        synchronized (this) {
            if (_exh != null) {
                ((IDisposable) _exh).dispose();

            }
            _evh = null;
        }
    }

    public final String getObjectType() {
        return _type;
    }

    public final void setObjectType(String value) {
        _type = value;
    }

    public final boolean getHasQueryInfo() {
        if (_queryInfo != null) {
            if (_queryInfo.get("query-info") != null || _queryInfo.get("tag-info") != null || _queryInfo.get("named-tag-info") != null) {
                return true;
            }
        }

        return false;

    }

    public final MetaInformation getMetaInformation() {
        return _metaInformation;
    }

    public final void setMetaInformation(MetaInformation value) {
        _metaInformation = value;
    }

    public final String getResyncProviderName() {
        return _resyncProviderName;
    }

    public final void setResyncProviderName(String value) {
        _resyncProviderName = value;
    }

    /**
     * Get or set provider name
     */
    public final String getProviderName() {
        return this._providerName;
    }

    public final void setProviderName(String value) {
        this._providerName = value;
    }

    /**
     * Eviction hint for the object.
     */
    public final EvictionHint getEvictionHint() {
        return _evh;
    }

    public final void setEvictionHint(EvictionHint value) {
        synchronized (this) {
            _evh = value;
        }
    }

    public final CacheItemPriority getPriority() {
        return _priorityValue;
    }

    
    @Override
    public int getInMemorySize(){        

        return (int) (PrimitiveDTSize + getInMemoryDataSize() + _keySize);
    }
    
    /**
     *
     * @return
     */
    public int getInMemoryDataSize(){
        
        int size = 0;
        if(getValue() != null){
            if(getValue() instanceof UserBinaryObject){
                size = ((UserBinaryObject)getValue()).getInMemorySize();
            }
            else if(getValue()  instanceof CallbackEntry){
                CallbackEntry entry = (CallbackEntry)getValue();
                if(entry.getValue() != null && entry.getValue() instanceof UserBinaryObject){
                    size = ((UserBinaryObject)entry.getValue()).getInMemorySize();
                }
            }
            else 
            { 
                _size = size;
            }
                
        }
        return size;
    }
    public final void setPriority(CacheItemPriority value) {
        _priorityValue = value;
    }

    /**
     * Expiration hint for the object.
     */
    public final ExpirationHint getExpirationHint() {
        return _exh;
    }

    public final void setExpirationHint(ExpirationHint value) {
        synchronized (this) {
            _exh = value;
        }
    }

    /**
     * The group with which this item is related.
     */
    public final GroupInfo getGroupInfo() {
        return _grpInfo;
    }

    public final void setGroupInfo(GroupInfo value) {
        synchronized (this) {
            _grpInfo = value;
        }
    }

    /**
     * The query information for this item.
     */
    public final java.util.HashMap getQueryInfo() {
        return _queryInfo;
    }

    public final void setQueryInfo(java.util.HashMap value) {
        synchronized (this) {
            _queryInfo = value;
        }
    }

   

    public final Object getLockId() {
        if(_lockMetaInfo != null)
            return _lockMetaInfo.getLockId();
        else
            return null;
    }

    public final void setLockId(Object value) {
        synchronized (this) {
            if(_lockMetaInfo == null){
                _lockMetaInfo = new LockMetaInfo();
            }
            _lockMetaInfo.setLockId(value); 
        }
    }

    public final TimeSpan getLockAge() {
          if(_lockMetaInfo != null){
                return _lockMetaInfo.getLockAge();
            }
          else
              return null;
    }

    public final void setLockAge(TimeSpan value) {
        synchronized (this) {
              if(_lockMetaInfo == null){
                _lockMetaInfo = new LockMetaInfo();
            }
             _lockMetaInfo.setLockAge(value);
        }
    }

    public final java.util.Date getLockDate() {
          if(_lockMetaInfo != null && _lockMetaInfo.getLockDate() != null){
                return _lockMetaInfo.getLockDate();
            }
          else
              return new Date(70,0,1,0,0,0);
    }

    public final void setLockDate(java.util.Date value) {
        synchronized (this) {
              if(_lockMetaInfo == null){
                _lockMetaInfo = new LockMetaInfo();
              }
              _lockMetaInfo.setLockDate(value);
        }
    }

    public final java.util.Date getCreationTime() {
        return _creationTime;
    }

    public final void setCreationTime(java.util.Date value) {
        synchronized (this) {
            _creationTime = value;
        }
    }

    public final java.util.Date getLastModifiedTime() {
        return _lastModifiedTime;
    }

    public final void setLastModifiedTime(java.util.Date value) {
        synchronized (this) {
            _lastModifiedTime = value;
        }
    }

    public final LockAccessType getLockAccessType() {
          if(_lockMetaInfo != null){
                return _lockMetaInfo.getAccessType();
            }
          else
              return null;
    }

    public final void setLockAccessType(LockAccessType value) {
        synchronized (this) {
             if(_lockMetaInfo == null){
                _lockMetaInfo = new LockMetaInfo();
            }
             _lockMetaInfo.setAccessType(value);
        }
    }


    /**
     * Gets the LockManager for this cache entry.
     */
    public final LockManager getRWLockManager() {
        if (_lockMetaInfo == null || _lockMetaInfo.getLockManager() == null) {
            synchronized (this) {
                if (_lockMetaInfo == null) {
                    _lockMetaInfo = new LockMetaInfo();
                }
                if(_lockMetaInfo.getLockManager() == null)
                    _lockMetaInfo.setLockManager(new LockManager());
            }
        }
        return _lockMetaInfo.getLockManager();
    }

    public final LockExpiration getLockExpiration() {
          if(_lockMetaInfo != null){
                return _lockMetaInfo.getLockExpiration();
            }
          else
             return null;
    }

    public final void setLockExpiration(LockExpiration value) {
           if(_lockMetaInfo == null){
                _lockMetaInfo = new LockMetaInfo();
            }
           _lockMetaInfo.setLockExpiration(value);
    }

    public final boolean IsLocked(tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate) {
        synchronized (this) {
            if (this.getFlag().IsAnyBitSet((byte) BitSetConstants.LockItem)) {
                if (this.getLockExpiration() == null || !this.getLockExpiration().HasExpired()) {

                    lockId.argvalue = this.getLockId();
                    lockDate.argvalue = this.getLockDate();
                    return true;
                } else {
                    ReleaseLock();
                    return false;
                }
            }
            return false;
        }
    }

    public final boolean CompareLock(Object lockId) {
        synchronized (this) {
            if (this.getFlag().IsAnyBitSet((byte) BitSetConstants.LockItem)) {
                if (lockId == null) {
                    return false;
                }
                if (this.getLockId().equals(lockId)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Determines whether an item is locked or not.
     *
     * @param lockId
     * @return
     */
    public final boolean IsItemLocked() {
        synchronized (this) {
            if (this.getLockExpiration() == null || !this.getLockExpiration().HasExpired()) {
                return this.getFlag().IsAnyBitSet((byte) BitSetConstants.LockItem);
            }
            return false;
        }
    }

    public final void ReleaseLock() {
        synchronized (this) {
            this.setLockId(null);
            try {
                NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
                this.setLockDate(time.getDate());
            } catch (Exception e) {
            }
            this.getFlag().UnsetBit((byte) BitSetConstants.LockItem);
        }
    }

    public final void CopyLock(Object lockId, java.util.Date lockDate, LockExpiration lockExpiration) {
        synchronized (this) {
            if (lockId != null) {
                this.getFlag().SetBit((byte) BitSetConstants.LockItem);
            } else {
                this.getFlag().UnsetBit((byte) BitSetConstants.LockItem);
            }

            this.setLockId(lockId);
            this.setLockDate(lockDate);
            this.setLockExpiration(lockExpiration);
        }
    }

    public final boolean Lock(LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate) {
        synchronized (this) {
            if (!this.IsLocked(lockId, lockDate)) {
                this.getFlag().SetBit((byte) BitSetConstants.LockItem);
                this.setLockId(lockId.argvalue);
                this.setLockDate(lockDate.argvalue);
                this.setLockExpiration(lockExpiration);
                if (this.getLockExpiration() != null) {
                    this.getLockExpiration().Set();
                }
                return true;
            } else {
                lockId.argvalue = this.getLockId();
                lockDate.argvalue = this.getLockDate();
            }

            return false;
        }
    }

    public final long getVersion() {
        return _version;
    }

    public final void setVersion(long value) {
        _version = value;
    }

    public final void UpdateVersion(CacheEntry entry) {
        synchronized (this) {
            CopyVersion(entry);
            _version++;
        }
    }

    private void CopyVersion(CacheEntry entry) {
        synchronized (this) {
            this._version = entry.getVersion();
        }
    }

    public final void UpdateLastModifiedTime(CacheEntry entry) {
        synchronized (this) {
            this._creationTime = entry.getCreationTime();
        }
    }

    public final boolean IsNewer(long version) {
        synchronized (this) {
            return this.getVersion() > version;
        }
    }

    public final boolean CompareVersion(long version) {
        synchronized (this) {
            return this._version == version;
        }
    }

   

    public final Object[] getUserData() {
        Object[] userData = null;
        if (getValue() != null) {
            UserBinaryObject ubObject = null;
            if (getValue() instanceof CallbackEntry) {
                if (((CallbackEntry) getValue()).getValue() != null) {
                    Object tempVar = ((CallbackEntry) getValue()).getValue();
                    {
                        ubObject = (UserBinaryObject) ((tempVar instanceof UserBinaryObject) ? tempVar : null);
                    }
                }
            } else {
                Object tempVar2 = getValue();
                {
                    ubObject = (UserBinaryObject) ((tempVar2 instanceof UserBinaryObject) ? tempVar2 : null);
                }
            }
        
        if(ubObject != null)
            userData = ubObject.getData();
        }
        return userData;
    }
    
    
    public final byte[] getFullUserData() {
        Object[] userData = null;
        if (getValue() != null) {
            UserBinaryObject ubObject = null;
            if (getValue() instanceof CallbackEntry) {
                if (((CallbackEntry) getValue()).getValue() != null) {
                    Object tempVar = ((CallbackEntry) getValue()).getValue();
                    {
                        ubObject = (UserBinaryObject) ((tempVar instanceof UserBinaryObject) ? tempVar : null);
                    }
                }
            } else {
                Object tempVar2 = getValue();
                {
                    ubObject = (UserBinaryObject) ((tempVar2 instanceof UserBinaryObject) ? tempVar2 : null);
                }
            }
            return ubObject.GetFullObject();
        }
        return null;
    }

    /**
     * The actual object provided by the client application
     * @return     */
    @Override
    public Object getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(Object value) {
        synchronized (this) {
            if (_bitset != null) {
                if (value instanceof byte[] || value instanceof UserBinaryObject) {
                    _bitset.SetBit((byte)BitSetConstants.Flattened);
                } else {
                    _bitset.UnsetBit((byte) BitSetConstants.Flattened);
                }
            }
            
            Object val1 = value;
            if (value instanceof Object[] && !(val1 instanceof byte[])) {
                //val1 = UserBinaryObject.CreateUserBinaryObject((byte[])value ); 
                val1 = new UserBinaryObject((Object[]) value);
                 _bitset.SetBit((byte)BitSetConstants.Flattened);
            }

            if (super.getValue() instanceof CallbackEntry && val1 instanceof UserBinaryObject) {
                Object tempVar = super.getValue();
                CallbackEntry cbEntry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);

                cbEntry.setValue(val1);
            } else {
                super.setValue(val1);
            }
        }
    }

    public final void AddCallbackInfo(CallbackInfo updateCallback, CallbackInfo removeCallback) {
        synchronized (this) {
            CallbackEntry cbEntry;

            if (getValue() instanceof CallbackEntry) {
                Object tempVar = getValue();
                cbEntry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
            } else {
                cbEntry = new CallbackEntry();
                cbEntry.setValue(getValue());
                cbEntry.setFlag(getFlag());
                setValue(cbEntry);
            }

            if (updateCallback != null && updateCallback.getCallback() != null && (Short) updateCallback.getCallback() != -1) {
                cbEntry.AddItemUpdateCallback(updateCallback);
            }
            if (removeCallback != null && removeCallback.getCallback() != null && (Short) removeCallback.getCallback() != -1) {
                cbEntry.AddItemRemoveCallback(removeCallback);
            }
        }
    }

    public final void RemoveCallbackInfo(CallbackInfo updateCallback, CallbackInfo removeCallback) {
        synchronized (this) {
            if (updateCallback != null || removeCallback != null) {
                CallbackEntry cbEntry = null;
                if (getValue() instanceof CallbackEntry) {
                    Object tempVar = getValue();
                    cbEntry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);

                    if (updateCallback != null) {
                        cbEntry.RemoveItemUpdateCallback(updateCallback);
                    }
                    if (removeCallback != null) {
                        cbEntry.RemoveItemRemoveCallback(removeCallback);
                    }

                }
            }
        }
    }

    /**
     * Flat status of the object.
     */
    public final boolean getIsFlattened() {
        return _bitset.IsBitSet((byte) BitSetConstants.Flattened);
    }

    public final boolean getIsCompressed() {
        return _bitset.IsBitSet((byte) COMPRESSED);
    }

    public final void setFlag(BitSet value) {
        synchronized (this) {
            _bitset = value;
        }
    }

    public final BitSet getFlag() {
        return _bitset;
    }
    

    /**
     * Creates a new object that is a copy of the current instance. The value is
     * not copied.
     *
     * @return A new object that is a copy of this instance without value.
     */
    public final CacheEntry CloneWithoutValue() {
        CacheEntry e = new CacheEntry();
        synchronized (this) {
            e._exh = _exh;
            e._evh = _evh;
            if (this._grpInfo != null) {
                e._grpInfo = (GroupInfo) this._grpInfo.clone();
            }
            e._bitset = (BitSet) _bitset.Clone();

            e._queryInfo = _queryInfo;
           
            if(this._lockMetaInfo != null){
                e.setLockId(_lockMetaInfo.getLockId());
                e.setLockDate(_lockMetaInfo.getLockDate());
                e.setLockAge(_lockMetaInfo.getLockAge());
                e.setLockExpiration(_lockMetaInfo.getLockExpiration());
                e.getLockMetaInfo().setLockManager(_lockMetaInfo.getLockManager());
            }
            e._size = _size;            
            e._version = this._version;
            e._creationTime = this._creationTime;
            e._lastModifiedTime = this._lastModifiedTime;
           
            e._resyncProviderName = this._resyncProviderName;
            e._providerName = this._providerName;
            
            if (this.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = (CallbackEntry) this.getValue();
                Object tempVar2 = cbEntry.clone();
                cbEntry = (CallbackEntry) ((tempVar2 instanceof CallbackEntry) ? tempVar2 : null);
                cbEntry.setValue(null);
                e.setValue(cbEntry);
            }
            
            e._type = _type;
            e._keySize = this._keySize;
        }

        return e;

    }

    /**
     * Creates a new object that is a copy of the current instance.
     *
     * @return A new object that is a copy of this instance.
     */
    @Override
    public final Object clone() {
//        super.clone();
        Object value = getValue();
        
        if(value instanceof CallbackEntry){
            value = (CallbackEntry)((CallbackEntry)value).clone();
        }
        
        CacheEntry e = new CacheEntry(value, _exh, _evh);

        synchronized (this) {
            if (this._grpInfo != null) {
                e._grpInfo = (GroupInfo) this._grpInfo.clone();
            }
            e._bitset = (BitSet) _bitset.Clone();
            e.setPriority(getPriority());

       
            e._queryInfo = _queryInfo;
           

             if(this._lockMetaInfo != null){
                e.setLockId(_lockMetaInfo.getLockId());
                e.setLockDate(_lockMetaInfo.getLockDate());
                e.setLockAge(_lockMetaInfo.getLockAge());
                e.setLockExpiration(_lockMetaInfo.getLockExpiration());
                e.getLockMetaInfo().setLockManager(_lockMetaInfo.getLockManager());
               }
            e._size = _size;
    
            e._version = this._version;
            e._creationTime = this._creationTime;
            e._lastModifiedTime = this._lastModifiedTime;
             
            e._resyncProviderName = this._resyncProviderName;
            e._providerName = this._providerName;
           
          
            e._type = this._type;
            e._keySize = this._keySize;
        }
        return e;
    }

    /**
     * Creates a new object that is a copy of the current instance and that is
     * routable as well.
     *
     * @param localAddress
     * @return A routable copy of this instance.
     */
    public final CacheEntry RoutableClone(Address localAddress) {
        synchronized (this) {
            if ( _exh != null) {
                
                //see if expiration hint itself is non-routable then we only need
                //a node expiration to handle both the syncDependency and expiration.
                //otherwise we need a node expiration for syncDependency and also need to
                //maintain the actual routable expiration hint.

                NodeExpiration expiry = null;
                if (localAddress != null) {
                    expiry = new NodeExpiration(localAddress);
                }

             
                    if (_exh != null && _exh.getIsRoutable()) {
                        AggregateExpirationHint aggHint = new AggregateExpirationHint();

                        aggHint.Add(_exh);

                        CacheEntry e = new CacheEntry(getValue(), aggHint, _evh);
                        if (_grpInfo != null) {
                            e._grpInfo = (GroupInfo) _grpInfo.clone();
                        }
                        e._bitset = (BitSet) _bitset.Clone();
                        e._version = this._version;
                        e._creationTime = this._creationTime;
                        e._lastModifiedTime = this._lastModifiedTime;
                        
                        if(_lockMetaInfo != null)
                           e.setLockExpiration(this.getLockExpiration()); 
                        e._resyncProviderName = this._resyncProviderName;
                        e._keySize = this._keySize;
                        e.setPriority(getPriority());
                        return e;
                    } else {
                        CacheEntry e = new CacheEntry(getValue(), expiry, _evh);
                        if (_grpInfo != null) {
                            e._grpInfo = (GroupInfo) _grpInfo.clone();
                        }
                        e._bitset = (BitSet) _bitset.Clone();
                        e._version = this._version;
                        e._creationTime = this._creationTime;
                        e._lastModifiedTime = this._lastModifiedTime;
                        if(_lockMetaInfo != null)
                           e.setLockExpiration(this.getLockExpiration());                         
                        e._resyncProviderName = this._resyncProviderName;
                        e._keySize = this._keySize;
                        e.setPriority(getPriority());
                        return e;
                    }
                
                }
        }
        return (CacheEntry) clone();
    }

    /**
     * Creates a new object that is a copy of the current instance and that is
     * routable as well.
     *
     * @param cacheContext
     * @return A routable copy of this instance.
     */
    public final CacheEntry FlattenedClone(String cacheContext) {
        CacheEntry e = (CacheEntry) clone();
        e.FlattenObject(cacheContext);
        return e;
    }

    /**
     * Falttens, i.e. serializes the object contained in value.
     * @param cacheContext
     * @return 
     */
    public final Object FlattenObject(String cacheContext) {
        //if(!IsFlattened && !(Value is byte[]))
        //{
        //    Value = CompactBinaryFormatter.ToByteBuffer(Value,cacheContext);
        //    _flags.SetBit(FLATTENED);
        //}
        return getValue();
    }

    /**
     * DeFalttens, i.e. deserializes the object contained in value.
     * @param cacheContext
     * @return 
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public final Object DeflattenObject(String cacheContext) throws IOException, ClassNotFoundException {
        synchronized (this) {
            if (getIsFlattened()) {
                // Setting the Value resets the Flat flag!
                Object[] tempVar = getUserData();

                Byte[] bite = (Byte[]) ((tempVar instanceof Byte[]) ? tempVar : null);
                byte[] bit = new byte[bite.length];
                for (int i = 0; i < bite.length; i++) {
                    bit[i] = bite[i];
                }

                setValue(CompactBinaryFormatter.fromByteBuffer(bit, cacheContext));
            }
            return getValue();
        }
    }

    /**
     * Gets the deflatted value of the of the object in the value. It does not
     * deflatten the actual object.
     * @param cacheContext
     * @return 
     * @throws java.lang.Exception
     */
    public final Object DeflattedValue(String cacheContext) throws Exception {
        Object obj = getValue();

        //There is possibility that two threads simultaneously do deserialization; therefore
        //we must deserialize the entry in synchronized fashion.
        synchronized (this) {
            if (getIsFlattened()) {
                // Setting the Value resets the Flat flag!
                UserBinaryObject ub = null;
                CallbackEntry cbEntry = (CallbackEntry) ((obj instanceof CallbackEntry) ? obj : null);
                if (cbEntry != null) {
                    Object tempVar = cbEntry.getValue();
                    ub = (UserBinaryObject) ((tempVar instanceof UserBinaryObject) ? tempVar : null);
                } else {
                    ub = (UserBinaryObject) ((obj instanceof UserBinaryObject) ? obj : null);
                }

                byte[] data = ub.GetFullObject();

                _size = data.length;
                obj = CompactBinaryFormatter.fromByteBuffer(data, cacheContext);
                if (cbEntry != null) {
                    cbEntry.setValue(obj);
                    obj = cbEntry;
                }
            }
        }
        return obj;
    }

    /**
     * muds: in case of local inproc caches, first time the object is accessed
     * we keep the deserialized user object. This way on the upcoming get
     * requests, we save the cost of deserialization every time.
     *
     * @param cacheContext
     * @throws java.lang.Exception
     */
    public final void KeepDeflattedValue(String cacheContext) throws Exception {
        synchronized (this) {
            try {
                if (getIsFlattened()) {
                    setValue(DeflattedValue(cacheContext));
                    _bitset.UnsetBit((byte) BitSetConstants.Flattened);
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public String toString() {
        return "CacheEntry[" + getValue().toString() + "]";
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        synchronized (this) {
            setValue(reader.readObject());
            _bitset = new BitSet(reader.readByte());
            _evh = getEvictionHint().ReadEvcHint(reader);
            _exh = getExpirationHint().ReadExpHint(reader);
            _grpInfo = getGroupInfo().ReadGrpInfo(reader);

            //Object tempVar = reader.readObject();
           
            _queryInfo = (java.util.HashMap) reader.readObject();
           
            _size = reader.readLong();
            setLockId(reader.readObject());

            setLockDate((new NCDateTime(reader.readLong())).getDate());
            //BigInteger to long
            _version = reader.readLong();
            Object tempVar2 = reader.readObject();
            setLockExpiration((LockExpiration) ((tempVar2 instanceof LockExpiration) ? tempVar2 : null));
            _creationTime = (new NCDateTime(reader.readLong())).getDate();
            _lastModifiedTime = (new NCDateTime(reader.readLong())).getDate();
            Object tempVar3 = reader.readObject();
            _resyncProviderName = (String) ((tempVar3 instanceof String) ? tempVar3 : null);

            _priorityValue = CacheItemPriority.forValue(reader.readInt());
            Object tempVar4 = reader.readObject();
           if(_lockMetaInfo != null)
            _lockMetaInfo.setLockManager((LockManager) ((tempVar4 instanceof LockManager) ? tempVar4 : null));
            
           Object tempVar5 = reader.readObject();
            this._providerName = (String) ((tempVar5 instanceof String) ? tempVar5 : null);
           
            Object tempVar7 = reader.readObject();
            _type = (String) ((tempVar7 instanceof String) ? tempVar7 : null);
            Object tempVar8 = reader.readObject();
            _keySize = (Long) ((tempVar8 instanceof Long) ? tempVar8 : -1);
        }
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        synchronized (this) {
            writer.writeObject(getValue());
            writer.writeByte(_bitset.getData());
            getEvictionHint().WriteEvcHint(writer, _evh);
            getExpirationHint().WriteExpHint(writer, _exh);
            
            if(getGroupInfo()!=null)
                getGroupInfo().WriteGrpInfo(writer, _grpInfo);
            else {
                setGroupInfo(new GroupInfo());
                getGroupInfo().WriteGrpInfo(writer, _grpInfo);
            }

            
            writer.writeObject(_queryInfo);
            writer.writeLong(_size);
            writer.writeObject(getLockId());

            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.MILLISECOND, 0);
            c.setTime((Date) getLockDate());
            NCDateTime ncdt = null;
            try {
                ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
            } catch (ArgumentException argumentException) {
            }

            writer.writeLong(ncdt.getTicks());
            writer.writeLong(_version);
            writer.writeObject(getLockExpiration());

            c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.MILLISECOND, 0);
            c.setTime((Date) _creationTime);
            ncdt = null;
            try {
                ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
            } catch (ArgumentException argumentException) {
            }
            writer.writeLong(ncdt.getTicks());

            c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.MILLISECOND, 0);
            c.setTime((Date) _lastModifiedTime);
            ncdt = null;
            try {
                ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
            } catch (ArgumentException argumentException) {
            }
            writer.writeLong(ncdt.getTicks());
            writer.writeObject(_resyncProviderName);
            writer.writeInt(_priorityValue.value());
            writer.writeObject(getRWLockManager());
            writer.writeObject(this._providerName);
         
            writer.writeObject(this._type);
            writer.writeObject(this._keySize);
        }
    }

    @Override
    public final void DeserializeLocal(BlockDataInputStream reader) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    @Override
    public final void SerializeLocal(BlockDataOutputStream writer) {
        throw new UnsupportedOperationException("The method or operation is not implemented.");
    }

    
    @Override
    public final int getSize() {
        return (int) (PrimitiveDTSize + getDataSize());
    }
    
    public final long getDataSize() {
        if (_size > -1) {
            return _size;
        }
        int size = -1;
        if (getValue() != null) {
            if (getValue() instanceof UserBinaryObject) {
                size = ((UserBinaryObject) getValue()).getSize();
            } else if (getValue() instanceof CallbackEntry) {
                if (((CallbackEntry) getValue()).getValue() != null) {
                    size = ((UserBinaryObject) ((CallbackEntry) getValue()).getValue()).getSize();
                }
            }           
            _size = size;
        }
        return size;
    }

    public final void setDataSize(long value) {
        _size = value;
    }

    public final long getKeySize()
    {
        return _keySize;
    }
    
    public final void setKeySize(long value)
    {
        _keySize= value;
    }
    public final VirtualArray Read(int offset, int length) {
        VirtualArray vBuffer = null;
        UserBinaryObject ubObject = (UserBinaryObject) (getValue() instanceof CallbackEntry ? ((CallbackEntry) getValue()).getValue() : getValue());

        if (ubObject != null) {
            vBuffer = ubObject.Read(offset, length);
        }
        return vBuffer;
    }

    public final void Write(VirtualArray vBuffer, int srcOffset, int dstOffset, int length) {
        UserBinaryObject ubObject = (UserBinaryObject) (getValue() instanceof CallbackEntry ? ((CallbackEntry) getValue()).getValue() : getValue());

        if (ubObject != null) {
            ubObject.Write(vBuffer, srcOffset, dstOffset, length);
        }
    }

    public final int getLength() {
        int size = 0;
        if (getValue() != null) {
            if (getValue() instanceof UserBinaryObject) {
                size = ((UserBinaryObject) getValue()).getSize();
            } else if (getValue() instanceof CallbackEntry) {
                if (((CallbackEntry) getValue()).getValue() != null) {
                    size = ((UserBinaryObject) ((CallbackEntry) getValue()).getValue()).getSize();
                }
            }
        }

        return size;
    }
    
    public final boolean valueEquals(Object obj)
    {
        if(getValue() != null)
        {
            if (getValue() instanceof UserBinaryObject) {
                return ((UserBinaryObject) getValue()).equals(obj);
            } else if (getValue() instanceof CallbackEntry) {
                if (((CallbackEntry) getValue()).getValue() != null) {
                    return ((UserBinaryObject) ((CallbackEntry) getValue()).getValue()).equals(obj);
                }
            }
            else
                return getValue().equals(obj);
        }
        else if(obj == null)
            return true;
                    
       return false;
    }

    @Override
    public void setLength(int value) {
        throw new UnsupportedOperationException("Set length is not supported.");
    }


}
