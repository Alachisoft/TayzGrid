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

package com.alachisoft.tayzgrid.caching.enumeration;

import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Calendar;
import java.util.UUID;

/**
 * A singleton Class that is responsible for all the management of snaphot pool for Enumeration. This class can be used to get snaphot from snaphot pool for enumeration based on
 * size of data that snaphot will return and is configurable in service config.
 *
 */
public class CacheSnapshotPool
{

    private static final CacheSnapshotPool instance = new CacheSnapshotPool();
    /**
     * Minimum number of keys to be present in cache to go for snaphot pooling.
     */
    private int _minSnaphotSizeForPooling = 100000;
    /**
     * The maximum number of Snaphots available for pooling.
     */
    private int _maxSnapshotsInPool = 10;
    /**
     * The time after which we will genarate a new snaphot if a client requests enumerator on cache and if this time is not elapsed we will return the same pool.
     */
    private int _newSnapshotCreationThreshold = 120; // In secs
    /**
     * Contains the mapping between cache id and pool specific to that cache.
     */
    private java.util.HashMap _cachePoolMap;

    private CacheSnapshotPool()
    {
        if (ServicePropValues.CacheServer_EnableSnapshotPoolingCacheSize != null)
        {
            _minSnaphotSizeForPooling = Integer.decode(ServicePropValues.CacheServer_MinimumSnaphotSizeForPooling);
        }
        if (ServicePropValues.CacheServer_MaxNumOfSnapshotsInPool != null)
        {
            _maxSnapshotsInPool = Integer.decode(ServicePropValues.CacheServer_SnapshotPoolSize);
        }
        if (ServicePropValues.CacheServer_NewSnapshotCreationTimeInSec != null)
        {
            _newSnapshotCreationThreshold = Integer.decode(ServicePropValues.CacheServer_SnapshotCreationThreshold);
        }

        _cachePoolMap = new java.util.HashMap();
    }

    public static CacheSnapshotPool getInstance()
    {
        return instance;
    }

    /**
     * Get a snaphot from the pool
     *
     * @param pointerID
     * @param cache
     * @return
     */
    public final Object[] GetSnaphot(String pointerID, CacheBase cache) throws GeneralFailureException, OperationFailedException, CacheException
    {
        CachePool pool = null;

        if (_cachePoolMap.containsKey(cache.getContext().getCacheRoot().getName()))
        {
            pool = (CachePool) ((_cachePoolMap.get(cache.getContext().getCacheRoot().getName()) instanceof CachePool) ? _cachePoolMap.get(cache.getContext().getCacheRoot().getName()) : null);
            return pool.GetSnaphotInPool(pointerID, cache);
        }
        else
        {
            pool = new CachePool(_minSnaphotSizeForPooling, _maxSnapshotsInPool, _newSnapshotCreationThreshold);
            _cachePoolMap.put(cache.getContext().getCacheRoot().getName(), pool);

        }
        return pool.GetSnaphotInPool(pointerID, cache);
    }

    /**
     * Get a snaphot from the pool for a particular cache.
     */
    public final void DiposeSnapshot(String pointerID, CacheBase cache)
    {
        CachePool pool = null;

        if (_cachePoolMap.containsKey(cache.getContext().getCacheRoot().getName()))
        {
            pool = (CachePool) ((_cachePoolMap.get(cache.getContext().getCacheRoot().getName()) instanceof CachePool) ? _cachePoolMap.get(cache.getContext().getCacheRoot().getName()) : null);
            pool.DiposeSnapshotInPool(pointerID);
        }

    }

    /**
     * Dispose the pool created for a cache when the cache is disposed.
     *
     * @param cacheId
     */
    public final void DisposePool(String cacheId)
    {
        _cachePoolMap.remove(cacheId);
    }

    private static class CachePool
    {

        /**
         * Minimum number of keys to be present in cache to go for snaphot pooling.
         */
        private int _minimumSnaphotSizeForPooling = 100000;
        /**
         * The maximum number of Snaphots available for pooling.
         */
        private int _maxNumOfSnapshotsInPool = 10;
        /**
         * The time after which we will genarate a new snaphot if a client requests enumerator on cache and if this time is not elapsed we will return the same pool.
         */
        private int _newSnapshotCreationTimeInSec = 120;
        /**
         * The time on which a new snapshot was created and added to Snaphot Pool
         */
        private java.util.Date _lastSnaphotCreationTime = new java.util.Date(0);
        /**
         * The pool containing all the available snaphots.
         */
        private java.util.HashMap<String, Object[]> _pool = new java.util.HashMap<String, Object[]>();
        /**
         * Contains the mapping between pointer and its snaphot. tells which pointer is using which snapshot in pool.
         */
        private java.util.HashMap<String, String> _enumeratorSnaphotMap = new java.util.HashMap<String, String>();
        /**
         * Contains the map for each snapshot and number of enumerators on it. Tells how many emumerators a references a particluar snaphot.
         */
        private java.util.HashMap<String, Integer> _snapshotRefCountMap = new java.util.HashMap<String, Integer>();
        /**
         * holds the id of current usable snaphot of the pool
         */
        private String _currentUsableSnapshot;

        /**
         * Returns a unique GUID that is assigned to the new snaphot added to the pool
         *
         * @return
         */
        private String GetNewUniqueID()
        {
            return UUID.randomUUID().toString();
        }

        public CachePool(int minSnaphotSizeForPooling, int maxSnapshotsInPool, int newSnapshotCreationThreshold)
        {
            _minimumSnaphotSizeForPooling = minSnaphotSizeForPooling;
            _maxNumOfSnapshotsInPool = maxSnapshotsInPool;
            _newSnapshotCreationTimeInSec = newSnapshotCreationThreshold;
        }

        /**
         * Return a snaphot from the snaphot pool to be used by current enumerator
         *
         * @param pointerID unque id of the enumeration pointer being used by current enumerator.
         * @param cache underlying cache from which snapshot has to be taken.
         * @return snaphot as an array.
         */
        public final Object[] GetSnaphotInPool(String pointerID, CacheBase cache) throws GeneralFailureException, OperationFailedException, CacheException
        {
            String uniqueID = "";
            double totalSec = 0;
            if (cache.getCount() < _minimumSnaphotSizeForPooling)
            {
                return cache.getKeys();
            }
            else
            {
                if (_pool.isEmpty())
                {
                    uniqueID = GetNewUniqueID();
                    _pool.put(uniqueID, cache.getKeys());
                    _lastSnaphotCreationTime = new java.util.Date();
                    _currentUsableSnapshot = uniqueID;
                }
                else if (_pool.size() < _maxNumOfSnapshotsInPool)
                {
                    TimeSpan elapsedTime = null;
                    try
                    {
                        elapsedTime = TimeSpan.Subtract(Calendar.getInstance().getTime(), _lastSnaphotCreationTime);
                    }
                    catch (ArgumentException argumentException)
                    {
                    }
                    totalSec = elapsedTime.getTotalMiliSeconds() / 1000;
                    if (totalSec >= _newSnapshotCreationTimeInSec)
                    {
                        uniqueID = GetNewUniqueID();
                        _pool.put(uniqueID, cache.getKeys());
                        _lastSnaphotCreationTime = new java.util.Date();
                        _currentUsableSnapshot = uniqueID;
                    }
                }

                if (!_enumeratorSnaphotMap.containsKey(pointerID))
                {
                    _enumeratorSnaphotMap.put(pointerID, uniqueID);

                    if (!_snapshotRefCountMap.containsKey(uniqueID))
                    {
                        _snapshotRefCountMap.put(uniqueID, 1);
                    }
                    else
                    {
                        int refCount = _snapshotRefCountMap.get(uniqueID);
                        refCount++;
                        _snapshotRefCountMap.put(uniqueID, refCount);
                    }
                }

                return _pool.get(_currentUsableSnapshot);
            }
        }

        /**
         * Dispose a snaphot from the pool that is not being used by any emumerator.
         *
         * @param pointerID unque id of the enumeration pointer being used by current enumerator.
         */
        public final void DiposeSnapshotInPool(String pointerID)
        {
            if (_enumeratorSnaphotMap.containsKey(pointerID))
            {
                String snapshotID = _enumeratorSnaphotMap.get(pointerID);
                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(snapshotID))
                {
                    if (_snapshotRefCountMap.containsKey(snapshotID))
                    {
                        int refCount = _snapshotRefCountMap.get(snapshotID);
                        refCount--;
                        if (refCount == 0)
                        {
                            _pool.remove(snapshotID);
                        }
                        else
                        {
                            _snapshotRefCountMap.put(snapshotID, refCount);
                        }
                    }
                }
            }
        }
    }
}
