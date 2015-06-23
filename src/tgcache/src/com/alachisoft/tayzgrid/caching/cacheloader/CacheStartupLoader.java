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

package com.alachisoft.tayzgrid.caching.cacheloader;

import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.util.LanguageContext;
import com.alachisoft.tayzgrid.runtime.cacheloader.CacheLoader;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.util.AuthenticateFeature;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.datasourceproviders.ClassPaths;
import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationType;

import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.runtime.cacheloader.LoaderState;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.util.SerializationBitSet;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class CacheStartupLoader implements IDisposable {

    private LoadCacheTask _task;
    private CacheLoader _cacheLoader;
    private Cache _cache;
    private int _noOfRetries;
    private int _retryInterval;
    private java.util.Map _properties;
    private boolean _loadCache = false;
    private ILogger cacheLog;
    private boolean _enabled;
    private LanguageContext _languageContext = LanguageContext.NONE;
    private static final String clInterface = "com.alachisoft.tayzgrid.runtime.cacheloader.CacheLoader";
    private JarFileLoader _loader;
    private boolean _isTaskCompleted = false;
    private boolean _isloaderTaskIntruppted = false;

    public final boolean getIsCacheLoaderTaskCompleted() {
        return _isTaskCompleted;
    }

    public final void setIsCacheLoaderTaskCompleted(boolean value) {
        _isTaskCompleted = value;
    }

    public final boolean getIsCacheLoaderTaskIntruppted() {
        return _isloaderTaskIntruppted;
    }

    public final void setIsCacheLoaderTaskIntruppted(boolean value) {
        _isloaderTaskIntruppted = value;
    }

    public CacheStartupLoader(java.util.Map properities, Cache cache, ILogger cacheLog) throws Exception {
        if (properities.containsKey("retries")) {
            _noOfRetries = Integer.parseInt(properities.get("retries").toString());
        } else {
            _noOfRetries = 0;
        }
        if (properities.containsKey("retry-interval")) {
            _retryInterval = Integer.parseInt(properities.get("retry-interval").toString());
        } else {
            _retryInterval = 0;
        }
        if (properities.containsKey("enabled")) {
            _enabled = Boolean.parseBoolean(properities.get("enabled").toString());
        }

        _cache = cache;
        this.cacheLog = cacheLog;

        _properties = properities;
        try {
            if (_enabled) {
                Initialize(properities);
            }
        } catch (Exception ex) {
            EventLogger.LogEvent("CacheLoader.Initialize. Error:" + ex.toString(), EventType.WARNING);
            cacheLog.Error("CacheStartupLoader.Initialize", ex.toString());
            throw ex;
        }
    }

    public final boolean getIsCacheloaderEnabled() {
        return _enabled;
    }

    public final boolean getExecuteCacheLoader() {
        return _loadCache;
    }

    public final void setExecuteCacheLoader(boolean value) {
        _loadCache = value;
    }

    public final LoadCacheTask getTask() {
        return _task;
    }

    public final void setTask(LoadCacheTask value) {
        _task = value;
    }

    public final int getNoOfRetries() {
        return _noOfRetries;
    }

    public final void setNoOfRetries(int value) {
        _noOfRetries = value;
    }

    public final int getRetryInterval() {
        return _retryInterval * 1000;
    }

    public final void setRetryInterval(int value) {
        _retryInterval = value;
    }

    public final java.util.Map getProperties() {
        return _properties;
    }

    public final void setProperties(java.util.Map value) {
        _properties = value;
    }

    public final void Initialize() throws ConfigurationException {
        Initialize(getProperties());
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param properties properties collection for this cache.
     */
    private void Initialize(java.util.Map properties) throws ConfigurationException {

        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            if (!properties.containsKey("assembly")) {
                throw new ConfigurationException("Missing assembly name");
            }
            if (!properties.containsKey("classname")) {
                throw new ConfigurationException("Missing class name");
            }

            String assembly = String.valueOf(properties.get("assembly"));
            String classname = String.valueOf(properties.get("classname"));
            String assemblyFullName = String.valueOf(properties.get("full-name"));

//            This is added to load the .exe and .dll providers
//            to keep previous provider running this bad chunk of code is written
//            later on you can directly provide the provider name read from config.
            String extension = ".dll";
            if (properties.containsKey("full-name")) {
                extension = assemblyFullName.split("\\.")[assemblyFullName.split("\\.").length - 1];
            }
            java.util.HashMap startupparams = (java.util.HashMap) ((properties.get("parameters") instanceof java.util.Map) ? properties.get("parameters") : null);
            if (startupparams == null) {
                startupparams = new java.util.HashMap();
            }
            try {
                if (extension.endsWith(".dll") || extension.endsWith(".exe")) {
                    throw new UnsupportedOperationException(".exe or .dll not allowed as of yet");
                } else if (extension.endsWith("jar") || extension.endsWith("class")) //else if (extension.endsWith("\\."+"jar") || extension.endsWith("\\."+"class"))
                {
                    AuthenticateFeature.Authenticate(LanguageContext.JAVA);
                    _languageContext = LanguageContext.JAVA;
                    File path = DirectoryUtil.createDeployAssemblyFolder(_cache.getName());
                    if (_loader == null) {
                        _loader = ClassPaths.addPath(path, this.cacheLog);
                    }
                    _cacheLoader = (CacheLoader) _loader.loadClass(classname).newInstance();
                    _cacheLoader.init(startupparams);
                    SerializationUtil.registerTypeInfoMap(_cache.getName(), _cache.GetTypeInfoMap());
                }
            } catch (ClassCastException e) {
                throw new ConfigurationException("The class specified does not implement ICacheLoader");
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        } catch (ConfigurationException e2) {
            throw e2;
        } catch (Exception e) {
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    public final void LoadCache() {
        boolean userReturn = true;
        int retryCount = 0;
        byte[] serializedObject = null;
        java.util.LinkedHashMap data = new java.util.LinkedHashMap();
        LoaderState index = new LoaderState();
        do {
            userReturn = false;
            data.clear();
            try {
                userReturn = _cacheLoader.loadNext(data, index);
            } catch (Exception e) {
                if (cacheLog != null && cacheLog.getIsErrorEnabled()) {
                    EventLogger.LogEvent("ICacheLoader.LoadNext. Error:" + e.toString(), EventType.WARNING);
                    cacheLog.Error("CacheStartupLoader.Load()", e.toString());
                    continue;
                }
            }
            try {

                if (data != null) {
                    Map.Entry keyValue;
                    Iterator de = data.entrySet().iterator();
                    while (de.hasNext()) {
                        keyValue = (Map.Entry) de.next();
                        retryCount = 0;
                        if (!(keyValue.getValue() instanceof ProviderCacheItem)) {
                            if (cacheLog != null && cacheLog.getIsErrorEnabled()) {
                                cacheLog.Error("CacheStartupLoader.Load()", "Invalid Key/Value type specified");
                                continue;
                            }
                        }

                        Object key = keyValue.getKey();
                        ProviderCacheItem item = (ProviderCacheItem) keyValue.getValue();

                        if (item == null) {
                            continue;
                        }
                        //expiration hints...
                        if(_cache.getContext().ExpirationContract!=null){
                        java.util.HashMap resolutionMap = _cache.getContext().ExpirationContract.resolveClientExpirations(item.getAbsoluteExpiration(), item.getSlidingExpiration());
                        item.setAbsoluteExpiration((Date)resolutionMap.get(ExpirationType.FixedExpiration));
                        item.setSlidingExpiration((TimeSpan)resolutionMap.get(ExpirationType.SlidingExpiration));
                        }
                        
                        int expType = CacheLoaderUtil.EvaluateExpirationParameters(item.getAbsoluteExpiration(), item.getSlidingExpiration());

                        ExpirationHint expiration = com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHelper.GetExpirationHint( item.getAbsoluteExpiration(), item.getSlidingExpiration());

                        if (expiration != null) {
                            if (item.isResyncItemOnExpiration()) {
                                expiration.SetBit(ExpirationHint.NEEDS_RESYNC);
                            }
                        }

                        String resyncProviderName = item.getResyncProviderName() == null ? null : item.getResyncProviderName().toLowerCase();
                        //query and tag info...
                        java.util.HashMap queryInfo = new java.util.HashMap();
                        TypeInfoMap typeMap = _cache.GetTypeInfoMap();
                        BitSet flag = new BitSet();
                        switch (_languageContext) {
                            case DOTNET:
                                if (typeMap != null) {
                                    queryInfo.put("query-info", CacheLoaderUtil.GetQueryInfo(item.getValue(), typeMap));
                                }
                                try {
                                    if (item.getTags() != null) {
                                        queryInfo.put("tag-info", CacheLoaderUtil.GetTagInfo(item.getValue(), item.getTags()));
                                    }

                                    if (item.getNamedTags() != null) {
                                        java.util.HashMap namedTagInfo = CacheLoaderUtil.GetNamedTagsInfo(item.getValue(), item.getNamedTags(), typeMap);
                                        if (namedTagInfo != null) {
                                            queryInfo.put("named-tag-info", namedTagInfo);
                                        }
                                    }
                                } catch (IllegalArgumentException exception) {
                                    cacheLog.Error("CacheStartupLoader.Load()", exception.getMessage());
                                    continue;
                                }
                                break;
                            case JAVA:
                                ProviderCacheItem jItem = (ProviderCacheItem) item;
                                Object javaQueryInfoMap = jItem.getValue();
                                if (javaQueryInfoMap != null) {
                                    com.alachisoft.tayzgrid.serialization.util.SerializationBitSet tempFlag = new SerializationBitSet(flag.getData());
                                    serializedObject = (byte[]) SerializationUtil.safeSerialize(javaQueryInfoMap, _cache.getName(), tempFlag);
                                    flag.setData(tempFlag.getData());
                                }
                                if (typeMap != null) {
                                    queryInfo.put("query-info", CacheLoaderUtil.GetQueryInfo(item.getValue(), typeMap));
                                }
                                try {
                                    if (item.getTags() != null) {
                                        queryInfo.put("tag-info", CacheLoaderUtil.GetTagInfo(item.getValue(), item.getTags()));
                                    }

                                    if (item.getNamedTags() != null) {
                                        java.util.HashMap namedTagInfo = CacheLoaderUtil.GetNamedTagsInfo(item.getValue(), item.getNamedTags(), typeMap);
                                        if (namedTagInfo != null) {
                                            queryInfo.put("named-tag-info", namedTagInfo);
                                        }
                                    }
                                } catch (IllegalArgumentException exception) {
                                    cacheLog.Error("CacheStartupLoader.Load()", exception.getMessage());
                                    continue;
                                }
                                break;
                        }

                        ////verify group/subgroup and tags
                        //eviction hint...
                        EvictionHint eviction = new PriorityEvictionHint(item.getItemPriority());

                        //object serialization...
                        if (_languageContext == LanguageContext.DOTNET) {
                            Object tempVar = Safeserialize((Object) item.getValue());
                            serializedObject = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                            
                        }

                        //convert to user binary object
                        UserBinaryObject ubObject = null;
                        if (serializedObject != null) {
                            ubObject = UserBinaryObject.CreateUserBinaryObject(serializedObject);
                        }

                        while (retryCount <= getNoOfRetries()) {
                            if (_cache.getIsRunning()) {
                                try {

                                    _cache.Insert(key, ubObject, expiration, eviction, item.getGroup(), item.getSubGroup(), queryInfo, flag, null, 0, LockAccessType.IGNORE_LOCK, null, resyncProviderName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

                                    break;
                                } catch (Exception e) {
                                    retryCount++;
                                    Thread.sleep(getRetryInterval());

                                    if (retryCount > getNoOfRetries()) {
                                        if (e instanceof OperationFailedException) {
                                            if (!((OperationFailedException) e).getIsTracable()) {
                                                if (cacheLog != null && cacheLog.getIsErrorEnabled()) {
                                                    cacheLog.Error("CacheStartupLoader.Load()", e.toString());

                                                    break;
                                                }
                                            }
                                        } else {
                                            if (cacheLog != null && cacheLog.getIsErrorEnabled()) {
                                                cacheLog.Error("CacheStartupLoader.Load()", e.toString());
                                                break;
                                            }

                                        }
                                    }
                                }
                            } else {
                                return;
                            }
                        }

                    }
                }
            } catch (Exception e) {
                if (cacheLog != null && cacheLog.getIsErrorEnabled()) {
                    EventLogger.LogEvent("CacheStartupLoader.Load(): " + e.toString(), EventType.WARNING);
                    cacheLog.Error("CacheStartupLoader.Load()", e.toString());
                    continue;
                }
            }

        } while (userReturn);
        _isTaskCompleted = true;
        _isloaderTaskIntruppted = false;

        getTask().dispose();
    }

    private String[] loadClasses(String path, String interfaceName) throws Exception {
        List list = new ArrayList();
        ArrayList jarClasses = new ArrayList();
        if (path.endsWith(".jar")) {

            JarInputStream jarFile = new JarInputStream(new FileInputStream(path));
            JarEntry jarEntry;

            while (true) {
                jarEntry = jarFile.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if (jarEntry.isDirectory()) {
                    continue;
                }

                if ((jarEntry.getName().endsWith(".class"))) {
                    jarClasses.add(jarEntry.getName().replaceAll("/", "\\."));
                }
            }
            if (jarClasses != null && jarClasses.size() <= 0) {
                return null;
            }
            try {
                URL urls[]
                        = {};
                JarFileLoader cl = new JarFileLoader(urls);
                cl.addFile(path);
                Class interFace = Class.forName(interfaceName);
                for (int i = 0; i < jarClasses.size(); i++) {

                    String[] tempClass = jarClasses.get(i).toString().split(".class");
                    Class cls = cl.loadClass(tempClass[0]);
                    boolean match = !cls.isInterface() && !cls.isEnum() && interFace.isAssignableFrom(cls);
                    if (match) {
                        list.add(tempClass[0]);
                    }
                }
                return (String[]) list.toArray(new String[0]);

            } catch (Exception ex) {
                throw ex;
            }
        } else if (path.endsWith(".class")) {
            try {
                File tempFile = new File(path);
                String pth = path.replaceAll(tempFile.getName(), "");
                File file = new File(pth);
                URL url = file.toURL();
                URL[] urls = new URL[]{
                    url
                };
                Class interFace = Class.forName(interfaceName);
                String[] tempClass = (tempFile.getName()).split("\\.");
                URLClassLoader clLoader = new URLClassLoader(urls);
                Class cls = clLoader.loadClass(tempClass[0]);
                boolean match = !cls.isInterface() && !cls.isEnum() && interFace.isAssignableFrom(cls);
                if (match) {
                    list.add(tempClass[0]);
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return (String[]) list.toArray(new String[0]);
    }

    private Object Safeserialize(Object serializableObject) throws java.io.IOException {
        if (serializableObject != null) {
            serializableObject = CompactBinaryFormatter.toByteBuffer(serializableObject, _cache.getName());
        }
        return serializableObject;
    }

    public final void dispose() {

        if (_cacheLoader != null) {
            try {
                _cacheLoader.dispose();
            } catch (Exception ex) {

                EventLogger.LogEvent("ICacheLoader.Dispose. Error:" + ex.toString(), EventType.ERROR);
                cacheLog.Error("CacheStartupLoader.Dispose(): ", ex.toString());
            }

            _cacheLoader = null;
        }
        if (_task != null && _task.isIsRunnung()) {

            _task.dispose();
        }
    }
}
