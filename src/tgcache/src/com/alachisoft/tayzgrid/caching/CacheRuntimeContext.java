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

import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationManager;

import com.alachisoft.tayzgrid.caching.cacheloader.CacheStartupLoader;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DatasourceMgr;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.propagator.AlertNotificationTypes;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.persistence.PersistenceManager;
import com.alachisoft.tayzgrid.processor.EntryProcessorManager;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;

public class CacheRuntimeContext implements IDisposable {

    
   private EntryProcessorManager entryProcessorManager;

//private JarFileLoader _loader ;
    /**
     * The type of user data stored in cache.
     */
    private DataFormat _InMemorydataFormat = DataFormat.Binary;
    /**
     * The one and only manager of the whole cache sytem.
     */
    private Cache _cacheRoot;
    /**
     * Logger used for NCache Logging.
     */
    private ILogger _logger;
    /**
     * The one and only manager of the whole cache sytem.
     */
    private CacheBase _cacheImpl;
    /**
     * Manager for implementing expiration
     */
    public ExpirationManager ExpiryMgr;
    public ExpirationContract ExpirationContract;
    /**
     * scheduler for auto-expiration tasks.
     */
    public TimeScheduler TimeSched;
    /**
     * Asynchronous event processor.
     */
    public AsyncProcessor AsyncProc;
    /**
     * The performance statistics collector object.
     */
    public com.alachisoft.tayzgrid.caching.statistics.StatisticCounter PerfStatsColl;
   
  
    /**
     * Serialization context(actually name of the cache) used by the Compact
     * framework.
     */
    public String _serializationContext;
    /**
     * Renders the cache to its client.
     */
    private CacheRenderer _renderer;
 
    /**
     */
    public PersistenceManager PersistenceMgr;
    private String _cacheName;
    /**
     * Contains the user defined types regisered with the Compact Framework.
     */
    // new code for data sharing
    public java.util.HashMap _dataSharingKnownTypesforNet = new java.util.HashMap();
    private IAlertPropagator _emailNotifier;
    private AlertNotificationTypes _alertTypes;


    private boolean _isStartedAsMirror = false;
    

   
    /**
     * Manager for read-trhough and write-through operations.
     */
    private DatasourceMgr _dsMgr;
    private CacheStartupLoader _cacheStartupLoader;


    public CacheRuntimeContext()
    {

    }
    


    public ClientDeathDetectionMgr ClientDeathDetection;

    /**
     * Types that need to be returned to .Net clients
     *
     * @return
     */
    public final java.util.HashMap getDataSharingKnowTypesNET() {
        return _dataSharingKnownTypesforNet;
    }

    

    public DataFormat getInMemoryDataFormat() {
        return _InMemorydataFormat;
    }

    public void setInMemoryDataFormat(DataFormat value) {
        _InMemorydataFormat = value;
    }

    /**
     * The one and only manager of the whole cache sytem.
     *
     * @return
     */
    public final Cache getCacheRoot() {
        return _cacheRoot;
    }

    /**
     *
     * @param value
     */
    public final void setCacheRoot(Cache value) {
        _cacheRoot = value;
    }

    /**
     * The one and only manager of the whole cache sytem.
     *
     * @return
     */
    public final CacheBase getCacheImpl() {
        return _cacheImpl;
    }

    public final void setCacheImpl(CacheBase value) {
        _cacheImpl = value;
    }



    public final boolean getIsStartedAsMirror() {
        return _isStartedAsMirror;
    }

    public final void setIsStartedAsMirror(boolean value) {
        _isStartedAsMirror = value;
    }

    public final IAlertPropagator getEmailAlertNotifier() {
        return _emailNotifier;
    }

    public final void setEmailAlertNotifier(IAlertPropagator value) {
        _emailNotifier = value;
    }

    public final AlertNotificationTypes getCacheAlertTypes() {
        return _alertTypes;
    }

    public final void setCacheAlertTypes(AlertNotificationTypes value) {
        _alertTypes = value;
    }

    /**
     * The one and only manager of the whole cache system.
     *
     * @return
     */
    public final CacheBase getCacheInternal() {
        return getCacheImpl().getInternalCache();
    }

 

    /**
     * Gets Cache serialization context used by CompactSerialization Framework.
     *
     * @return
     */
    public final String getSerializationContext() {
        return _serializationContext;
    }

    public final void setSerializationContext(String value) {
        _serializationContext = value;
    }

    public final CacheRenderer getRender() {
        return _renderer;
    }

    public final void setRender(CacheRenderer value) {
        _renderer = value;
    }


  

    /**
     * The one and only manager of the whole cache sytem.
     *
     * @return
     */
    public final DatasourceMgr getDsMgr() {
        return _dsMgr;
    }

    public final void setDsMgr(DatasourceMgr value) {
        _dsMgr = value;
    }

    public final CacheStartupLoader getCSLMgr() {
        return _cacheStartupLoader;
    }

    public final void setCSLMgr(CacheStartupLoader value) {
        _cacheStartupLoader = value;
    }

    public final ILogger getCacheLog() {
        return _logger;
    }

    public final void setCacheLog(ILogger value) {
        _logger = value;
    }

    /**
     * The one and only manager of the whole cache sytem.
     */
    public final boolean getIsClusteredImpl() {
        return CacheHelper.IsClusteredCache(getCacheImpl());
    }

    public final boolean getIsDbSyncCoordinator() {
        // incase of partitioned and local cache any node can initialize the hint
        // but only coordinator can do synchronization
        if (getCacheRoot().getCacheType().equals("partitioned-server") || getCacheRoot().getCacheType().equals("local-cache")
                || getCacheRoot().getCacheType().equals("overflow-cache")) {
            return true;
        } // incase of replicated and partiotion of replica only coordinator/subcoordinator can initialize and synchronize the hint.
        else if (((getCacheRoot().getCacheType().equals("replicated-server")) && ExpiryMgr.getIsCoordinatorNode())) {
            return true;
        } else {
            return false;
        }
    }



    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     * @param disposing
     */
    private void dispose(boolean disposing) throws InterruptedException {
        synchronized (this) {
            if (getSerializationContext() != null) {

                FormatterServices impl = FormatterServices.getDefault();
                impl.unregisterKnownType(null, getSerializationContext(), true);
            }

            if (PerfStatsColl != null) {
                PerfStatsColl.dispose();
                PerfStatsColl = null;
            }
            if (ExpiryMgr != null) {
                ExpiryMgr.dispose();
                ExpiryMgr = null;
            }
            if (getCacheImpl() != null) {
                getCacheImpl().dispose();
                setCacheImpl(null);
            }
            if (TimeSched != null) {
                TimeSched.dispose();
                TimeSched = null;
            }
            if (AsyncProc != null) {
                AsyncProc.Stop();
                AsyncProc = null;
            }
            setEmailAlertNotifier(null);
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        try {
            dispose(true);
        } catch (InterruptedException interruptedException) {
            _logger.Error(interruptedException.toString());
        }
    }

    /**
     * @return the entryProcessorManager
     */
    public EntryProcessorManager getEntryProcessorManager() {
        return entryProcessorManager;
    }

    /**
     * @param entryProcessorManager the entryProcessorManager to set
     */
    public void setEntryProcessorManager(EntryProcessorManager entryProcessorManager) {
        this.entryProcessorManager = entryProcessorManager;
    }
}
