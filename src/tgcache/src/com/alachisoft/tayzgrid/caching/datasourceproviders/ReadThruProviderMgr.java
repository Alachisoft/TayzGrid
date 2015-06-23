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

import com.alachisoft.tayzgrid.caching.cacheloader.JarFileLoader;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.ReadThruProvider;
import com.alachisoft.tayzgrid.common.util.LanguageContext;
import com.alachisoft.tayzgrid.common.util.AuthenticateFeature;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.config.newdom.Assembly;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;
import com.alachisoft.tayzgrid.caching.datasourceproviders.JCache.*;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import java.io.*;
import java.util.HashMap;
import javax.cache.integration.*;

/**
 * Manager class for read-through and write-through operations
 */
public class ReadThruProviderMgr implements IDisposable {

    private Assembly _asm;
    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context;
    /**
     * The external datasource reader
     */
    private ReadThruProvider _dsReader;

    private boolean _isDsReader = true;
    /**
     * The NewTrace management.
     */
    //private NewTrace nTrace;
    private String _cacheName;
    private LanguageContext _languageContext = LanguageContext.values()[0];

    public final LanguageContext getProviderType() {
        return _languageContext;
    }

    private ILogger getCacheLog() {
        return _context.getCacheLog();
    }

    /**
     * Constructor
     */
    public ReadThruProviderMgr() {
    }

    public final String getCacheId() {
        return _cacheName;
    }

    /**
     * Overloaded constructor Initializes the object based on the properties
     * specified in configuration
     *
     * @param properties properties collection for this cache.
     */
    public ReadThruProviderMgr(String cacheName, java.util.Map properties, CacheRuntimeContext context) throws ConfigurationException {
        _cacheName = cacheName;
        _context = context;
        Initialize(properties);
    }
    
    public ReadThruProviderMgr(String cacheName, CacheLoader loader, CacheRuntimeContext context) throws ConfigurationException
    {
        _cacheName = cacheName;
        _context = context;
        _dsReader = new JCacheReadThruProvider((CacheLoader)loader);
        _languageContext = LanguageContext.JAVA; // language context will always be Java for JCache loader
        try
        {
            _dsReader.init(new HashMap(), _cacheName);
        }
        catch (Exception e) {
            _context.getCacheLog().CriticalInfo(e.getMessage().toString());
            _context.getCacheLog().CriticalInfo(e.getCause().toString());
            
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {

        if (_dsReader != null) {
            synchronized (_dsReader) {
                try {
                    _dsReader.dispose();
                } catch (Exception e) {
                    getCacheLog().Error("ReadThruProviderMgr", "User code threw " + e.toString());
                }
            }
            _dsReader = null;
        }

    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param properties properties collection for this cache.
     */
    private void Initialize(java.util.Map properties) throws ConfigurationException {
        Assembly asm = null;

        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            if (!properties.containsKey("assembly-name")) {
                throw new ConfigurationException("Missing assembly name for read-thru option");
            }
            if (!properties.containsKey("class-name")) {
                throw new ConfigurationException("Missing class name for read-thru option");
            }

            String assembly = String.valueOf(properties.get("assembly-name"));
            //_context.getCacheLog().CriticalInfo(assembly);
            String classname = String.valueOf(properties.get("class-name"));
            //_context.getCacheLog().CriticalInfo(classname);

            //This is added to load the .exe and .dll providers
            //to keep previous provider running this bad chunk of code is written
            //later on you can directly provide the provider name read from config.
            String extension = ".dll";
            if (properties.containsKey("full-name")) {
                extension = String.valueOf(properties.get("full-name")).split("\\.")[String.valueOf(properties.get("full-name")).split("\\.").length - 1];
            }

            java.util.HashMap startupparams = (java.util.HashMap) ((properties.get("parameters") instanceof java.util.Map) ? properties.get("parameters") : null);

            if (startupparams == null) {
                startupparams = new java.util.HashMap();
            }

//            java.util.Iterator paramsIterator = startupparams.values().iterator();
//            while (paramsIterator.hasNext()) {
//                _context.getCacheLog().CriticalInfo(paramsIterator.next().toString());
//            }

            if (extension.endsWith(".dll") || extension.endsWith(".exe")) {
                AuthenticateFeature.Authenticate(LanguageContext.DOTNET);
                _languageContext = LanguageContext.DOTNET;
                try {
                    String path = AppUtil.DeployedAssemblyDir + _cacheName.toLowerCase() + GetReadThruAssemblyPath(assembly) + extension;
                    try {

                    } catch (Exception e) {
                        String message = String.format("Could not load assembly \"" + path + "\". %1$s", e.getMessage());
                        throw new Exception(message);
                    }
                    //ask
                    if (_dsReader == null) {
                        throw new Exception("Unable to instantiate " + classname);
                    }
                    _dsReader.init(startupparams, _cacheName);

                } catch (java.lang.ClassCastException e) {
                    _context.getCacheLog().CriticalInfo(e.getMessage().toString());
                    _context.getCacheLog().CriticalInfo(e.getCause().toString());
                    throw new ConfigurationException("The class specified in read-thru does not implement IDatasourceReader");
                } catch (Exception e) {
                    _context.getCacheLog().CriticalInfo(e.getMessage().toString());
                    _context.getCacheLog().CriticalInfo(e.getCause().toString());
                    throw new ConfigurationException(e.getMessage(), e);
                }
            } 
            else if (extension.endsWith("jar") || extension.endsWith("class")) {

                AuthenticateFeature.Authenticate(LanguageContext.JAVA);

                _languageContext = LanguageContext.JAVA;
                //_context.getCacheLog().CriticalInfo(_languageContext.name());
                //_context.getCacheLog().CriticalInfo(classname);

                File deployedFolder = DirectoryUtil.createDeployAssemblyFolder(_cacheName);

                if (!deployedFolder.exists()) {
                    throw new ConfigurationException("Could not load " + assembly + ". Deploy the required provider's Jar file(s)");
                }
                try {
                    JarFileLoader cl = ClassPaths.addPath(deployedFolder, _context.getCacheLog());
                    Object reader = cl.loadClass(classname).newInstance();
                    if(reader instanceof CacheLoader)
                    {
                        JCacheReadThruProvider jreadThru = new JCacheReadThruProvider((CacheLoader<Object, Object>) reader);
                        _dsReader = jreadThru;
                    }
                    else if(reader instanceof ReadThruProvider)
                    {
                        _dsReader = (ReadThruProvider) reader;
                    }
                    _dsReader.init(startupparams, _cacheName);

                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("ClassNotFoundException " + classname);
                } catch (Exception exception) {
                    throw new ConfigurationException(exception.getMessage());
                }
                SerializationUtil.registerTypeInfoMap(_cacheName, _context.getCacheRoot().GetTypeInfoMap());
            }
        } catch (ConfigurationException e) {
            _context.getCacheLog().CriticalInfo(e.getMessage().toString());
            throw e;

        } catch (Exception e) {
            _context.getCacheLog().CriticalInfo(e.getMessage().toString());
            _context.getCacheLog().CriticalInfo(e.getCause().toString());

            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }
    private String GetReadThruAssemblyPath(String asm) {
        String path = "/";
        String[] folderNames = asm.split("[,=]", -1);
        path = path + folderNames[0];
        return path;
    }

    private String GetReadThruAssembly(String asm) {
        String path = "";
        String[] folderNames = asm.split("[,=]", -1);
        path = folderNames[0];
        return path;
    }

    /**
     * Responsible for loading the object from the external data source. Key is
     * passed as parameter.
     *
     * @param key
     * @return
     *
     */
    public final void ReadThru(Object key, tangible.RefObject<ProviderCacheItem> item) throws OperationFailedException {
        item.argvalue = new ProviderCacheItem();
        try {

            _dsReader.loadFromSource(key, item.argvalue);
            this._context.PerfStatsColl.incrementReadThruPerSec();
        } catch (Exception e) {
            //Client doesnt throw the inner exception
            //Client casts the thrown exception message into Operation failed Exception therefore the current inner exception will be casted
            //in Operation failed exception > Inner Exception > Inner Exception
            throw new OperationFailedException("IReadThruProvider.LoadFromSource failed. Error: " + e.toString(), e);
        }
    }

    /**
     *
     * @param keys
     * @return
     */
    public final java.util.HashMap<Object, ProviderCacheItem> ReadThru(Object[] keys) throws OperationFailedException {
        java.util.HashMap<Object, ProviderCacheItem> cacheItems = null;
        try {
            cacheItems = _dsReader.loadFromSource(keys);
            this._context.PerfStatsColl.incrementReadThruPerSecBy(keys.length);
        } catch (Exception e) {
            throw new OperationFailedException("IReadThruProvider.LoadFromSource failed. Error: " + e.getMessage(), e);
        }
        return cacheItems;
    }
}
