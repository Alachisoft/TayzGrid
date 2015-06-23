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

package com.alachisoft.tayzgrid.caching.statistics;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * Info class that holds statistics related to cache.
 */
public class CacheStatistics implements Cloneable, ICompactSerializable, java.io.Serializable {

    /**
     * The name of the cache scheme.
     */
    private String _className = "";

    /**
     * The up time of cache.
     */
    private java.util.Date _upTime = new java.util.Date(0);

    /**
     * The current number of objects in the cache.
     */
    private long _count;

    /**
     * The current number of session objects in the cache. We need this count so
     * that user can not have more than 300 concurrent sessions in the cache.
     */
    private long _sessionCount;

    /**
     * The highest number of objects contained by the cache at any time.
     */
    private long _hiCount;

    /**
     * The maximum number of objects to be contained by the cache.
     */
    private long _maxCount;

    /**
     * The maximum capacity of the cache.
     */
    private long _maxSize;

    /**
     * The number of objects fetched successfuly.
     */
    private long _hitCount;

    /**
     * The number of objects fetch failures.
     */
    private long _missCount;

    /**
     * The number of objects fetch failures.
     */
    private long _dataSize;

    /**
     * The name of the cache scheme.
     */
    private String _perfInst = "";

    /**
     * A map of local buckets maintained at each node.
     */
    private java.util.HashMap _localBuckets;

    //maximum of 4 unique clients can connect to the cache in Free Edition.
    //Hasthable is synched by default
    private java.util.HashMap _clientsList = new java.util.HashMap(4);

     /// <summary> Connected Clients for local Cache. </summary>
    private ArrayList _connectedClients = new ArrayList();
    
    //In Free edition only client within the cluster can connect with the cache.
    //Currently we have limitation of 2 nodes cluster, therefore there can
    //be maximum 2 client (nodes).
    public static final int MAX_CLIENTS_IN_EXPRESS = 2;

    /**
     * Constructor.
     */
    public CacheStatistics() {
        this("", "");
    }

    /**
     * Constructor.
     */
    public CacheStatistics(String instanceName, String className) {
        _className = className;
        _perfInst = instanceName;
        _upTime = new java.util.Date();
    }

    /**
     * Copy constructor.
     *
     * @param stat
     */
    protected CacheStatistics(CacheStatistics stat) {
        synchronized (stat) {
            this._className = stat._className;
            this._perfInst = stat._perfInst;
            this._upTime = stat._upTime;
            this._count = stat._count;
            this._hiCount = stat._hiCount;
            this._maxCount = stat._maxCount;
            this._maxSize = stat._maxSize;
            this._hitCount = stat._hitCount;
            this._missCount = stat._missCount;
            this._localBuckets = stat._localBuckets != null ? (java.util.HashMap) ((stat._localBuckets instanceof java.util.HashMap) ? stat._localBuckets.clone() : null) : null;
            
        }
    }

    public final boolean AcceptClient(InetAddress clientAddress) {
        synchronized (_clientsList) {
            if (_clientsList.containsKey(clientAddress)) {
                int refCount = (Integer) _clientsList.get(clientAddress);
                refCount++;
                _clientsList.put(clientAddress, refCount);
                return true;
            } else if (_clientsList.size() < MAX_CLIENTS_IN_EXPRESS) {
                int refCount = 1;
                _clientsList.put(clientAddress, refCount);
                return true;
            }
            return false;
        }
    }

    public final void DisconnectClient(InetAddress clientAddress) {
        synchronized (_clientsList) {
            if (_clientsList.containsKey(clientAddress)) {
                int refCount = (Integer) _clientsList.get(clientAddress);
                refCount--;
                if (refCount == 0) {
                    _clientsList.remove(clientAddress);
                } else {
                    _clientsList.put(clientAddress, refCount);
                }
            }
        }
    }

    /**
     * The type of caching scheme.
     */
    public final String getClassName() {
        return _className;
    }

    public final void setClassName(String value) {
        _className = value;
    }

    /**
     * The type of caching scheme.
     */
    public final String getInstanceName() {
        return _perfInst;
    }

    public final void setInstanceName(String value) {
        _perfInst = value;
    }

    /**
     * The name of the cache scheme.
     */
    public final java.util.Date getUpTime() {
        return _upTime;
    }

    public final void setUpTime(java.util.Date value) {
        _upTime = value;
    }

    /**
     * The current number of objects in the cache.
     */
    public final long getCount() {
        return _count;
    }

    public final void setCount(long value) {
        _count = value;
    }

    public final long getSessionCount() {
        return _sessionCount;
    }

    public final void setSessionCount(long value) {
        _sessionCount = value;
    }

    public final java.util.HashMap getClientsList() {
        return _clientsList;
    }

    public final void setClientsList(java.util.HashMap value) {
        _clientsList = value;
    }

    /**
     * The highest number of objects contained by the cache at any time.
     */
    public final long getHiCount() {
        return _hiCount;
    }

    public final void setHiCount(long value) {
        _hiCount = value;
    }

    /**
     * The highest number of objects contained by the cache at any time.
     */
    public final long getMaxCount() {
        return _maxCount;
    }

    public final void setMaxCount(long value) {
        _maxCount = value;
    }

    /**
     * The maximum capacity of the cache at any time.
     */
    public long getMaxSize() {
        return _maxSize;
    }

    public void setMaxSize(long value) {
        _maxSize = value;
    }

    /**
     * The number of objects fetched successfuly.
     */
    public final long getHitCount() {
        return _hitCount;
    }

    public final void setHitCount(long value) {
        _hitCount = value;
    }

    /**
     * The number of objects fetch failures.
     */
    public final long getMissCount() {
        return _missCount;
    }

    public final void setMissCount(long value) {
        _missCount = value;
    }

    public final java.util.HashMap getLocalBuckets() {
        return _localBuckets;
    }

    public final void setLocalBuckets(java.util.HashMap value) {
        _localBuckets = value;
    }

    /**
     * Creates a new object that is a copy of the current instance.
     *
     * @return A new object that is a copy of this instance.
     */
    public Object clone() {
        return new CacheStatistics(this);
    }

    /**
     * returns the string representation of the statistics.
     *
     * @return
     */
    @Override
    public String toString() {
        synchronized (this) {
            StringBuilder ret = new StringBuilder();
            ret.append("Stats[Sch:" + getClassName() + ", Cnt:" + (new Long(getCount())).toString() + ", ");
            ret.append("Hi:" + (new Long(getHiCount())).toString() + ", ");
            ret.append("MxS:" + (new Long(getMaxSize())).toString() + ", ");
            ret.append("MxC:" + (new Long(getMaxCount())).toString() + ", ");
            ret.append("Hit:" + (new Long(getHitCount())).toString() + ", ");
            ret.append("Miss:" + (new Long(getMissCount())).toString() + "]");
            return ret.toString();
        }
    }

    /**
     * Updates the count and HiCount of statistics
     *
     * @param count
     */
    public final void UpdateCount(long count) {
        synchronized (this) {
            _count = count;
            if (_count > _hiCount) {
                _hiCount = _count;
            }
        }
    }

    /**
     * Updates the session items count of statistics
     *
     * @param sessionCountUpdateFlag This flag indicates how to update the
     * sessionCount possible values are as follows: - 1. -1 (decrement the
     * sessionCount by 1) 2. 0 (reset the sessionCount to 0) 3. +1 (increment
     * the sessionCount by 1)
     *
     */
    protected final void UpdateSessionCount(int sessionCountUpdateFlag) {
        synchronized (this) {
            switch (sessionCountUpdateFlag) {
                case -1:
                    _sessionCount--;
                    break;
                case 0:
                    _sessionCount = 0;
                    break;
                case 1:
                    _sessionCount++;
                    break;
            }
        }
    }

    /**
     * Increases the miss count by one.
     */
    public final void BumpMissCount() {
        synchronized (this) {
            ++_missCount;
        }
    }

    /**
     * Increases the hit count by one.
     */
    public final void BumpHitCount() {
        synchronized (this) {
            ++_hitCount;
        }
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _className = (String) reader.readObject();
        _perfInst = (String) reader.readObject();
        _upTime = (new NCDateTime(reader.readLong())).getDate();
        _count = reader.readLong();
        _hiCount = reader.readLong();
        _maxSize = reader.readLong();
        _maxCount = reader.readLong();
        _hitCount = reader.readLong();
        _missCount = reader.readLong();
        Object tempVar = reader.readObject();
        _clientsList = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
        
        _localBuckets = new java.util.HashMap();
        int count = reader.readInt();
        for (int i = 0; i < count; i++) {
            BucketStatistics tmp = new BucketStatistics();
            int bucketId = reader.readInt();
            tmp.DeserializeLocal(reader);
            _localBuckets.put(bucketId, tmp);
        }
    }

    public void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_className);
        writer.writeObject(_perfInst);

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.MILLISECOND, 0);
        c.setTime((Date) _upTime);
        NCDateTime ncdt = null;

        try {
            ncdt = new NCDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
        } catch (ArgumentException argumentException) {
        }

        writer.writeLong(ncdt.getTicks());
        writer.writeLong(_count);
        writer.writeLong(_hiCount);
        writer.writeLong(_maxSize);
        writer.writeLong(_maxCount);
        writer.writeLong(_hitCount);
        writer.writeLong(_missCount);
        writer.writeObject(_clientsList);
        

        int count = _localBuckets != null ? _localBuckets.size() : 0;
        writer.writeInt(count);
        if (_localBuckets != null) {
            Iterator ide = _localBuckets.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                writer.writeInt((Integer) pair.getKey());
                ((BucketStatistics) pair.getValue()).SerializeLocal(writer);
            }
        }
    }

    public static CacheStatistics ReadCacheStatistics(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        byte isNull = reader.readByte();
        if (isNull == 1) {
            return null;
        }
        CacheStatistics newStats = new CacheStatistics();
        newStats.deserialize(reader);
        return newStats;
    }

    public static void WriteCacheStatistics(CacheObjectOutput writer, CacheStatistics stats) throws IOException {
        byte isNull = 1;
        if (stats == null) {
            writer.writeByte(isNull);
        } else {
            isNull = 0;
            writer.writeByte(isNull);
            stats.serialize(writer);
        }
        return;
    }

    /**
     * @return the _connectedClients
     */
    public ArrayList getConnectedClients() {
        return _connectedClients;
    }

    /**
     * @param _connectedClients the _connectedClients to set
     */
    public void setConnectedClients(ArrayList _connectedClients) {
        this._connectedClients = _connectedClients;
    }
}
