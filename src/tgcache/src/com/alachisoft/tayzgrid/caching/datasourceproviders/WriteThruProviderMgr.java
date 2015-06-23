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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.cacheloader.JarFileLoader;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.DirectoryUtil;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.util.AuthenticateFeature;
import com.alachisoft.tayzgrid.common.util.LanguageContext;
import com.alachisoft.tayzgrid.config.newdom.Assembly;
import com.alachisoft.tayzgrid.caching.datasourceproviders.JCache.*;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteThruProvider;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.io.File;
import javax.cache.integration.CacheWriter;
import com.alachisoft.tayzgrid.caching.*;
import static com.alachisoft.tayzgrid.caching.OpCode.Add;
import static com.alachisoft.tayzgrid.caching.OpCode.Update;

/**
 * Manager class for read-through and write-through operations
 */
public class WriteThruProviderMgr implements IDisposable {

    private JarFileLoader _loader;
    /**
     * The write-behind task. Responsible for
     * updating/inserting/removing/clearing an object to/from the datasource.
     */
    private String _myProvider;
    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context;
    /**
     * The external datasource writer
     */
    private WriteThruProvider _dsWriter;
    /**
     * This will help to identify language type of read object
     */
    private LanguageContext _languageContext = LanguageContext.values()[0];
    private boolean _asyncWrites = true;
    private String _cacheName;
    private long _operationDelay;

    public final LanguageContext getLanguageContext() {
        return _languageContext;
    }

    public final void setLanguageContext(LanguageContext value) {
        _languageContext = value;
    }

    private ILogger getCacheLog() {
        return _context.getCacheLog();
    }

    /**
     * Constructor
     */
    public WriteThruProviderMgr() {
    }

    /**
     * Overloaded constructor Initializes the object based on the properties
     * specified in configuration
     *
     * @param properties properties collection for this cache.
     */
    public WriteThruProviderMgr(String cacheName, java.util.Map properties, CacheRuntimeContext context, int timeout, long operationDelay, String providerName) throws ConfigurationException {
        _context = context;
        _cacheName = cacheName;
        _operationDelay = operationDelay;
        _myProvider = providerName;
        Initialize(properties);
    }
    
    public WriteThruProviderMgr(String cacheName, CacheWriter writer, CacheRuntimeContext context, int timeout, long operationDelay, String providerName) throws ConfigurationException {
        _context = context;
        _cacheName = cacheName;
        _operationDelay = operationDelay;
        _myProvider = providerName;
        _dsWriter = new JCacheWriteThruProvider((CacheWriter<Object,Object>)writer);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        if (_dsWriter != null) {
            synchronized (_dsWriter) {
                try {
                    _dsWriter.dispose();
                } catch (Exception e) {
                    getCacheLog().Error("WriteThruProviderMgr", "User code threw " + e.toString());
                }
                _dsWriter = null;
            }
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
                throw new ConfigurationException("Missing assembly name for write-thru option");
            }
            if (!properties.containsKey("class-name")) {
                throw new ConfigurationException("Missing class name for write-thru option");
            }

            String assembly = String.valueOf(properties.get("assembly-name"));
            String classname = String.valueOf(properties.get("class-name"));
            java.util.HashMap startupparams = (java.util.HashMap) ((properties.get("parameters") instanceof java.util.Map) ? properties.get("parameters") : null);

            String extension = ".dll";
            if (properties.containsKey("full-name")) {
                extension = String.valueOf(properties.get("full-name")).split("\\.")[String.valueOf(properties.get("full-name")).split("\\.").length - 1];
            }

            if (startupparams == null) {
                startupparams = new java.util.HashMap();
            }
            try {

                if (extension.equals(".dll") || extension.equals(".exe")) {
                    AuthenticateFeature.Authenticate(LanguageContext.DOTNET);
                    _languageContext = LanguageContext.DOTNET;
                    String path = AppUtil.DeployedAssemblyDir + _cacheName.toLowerCase() + GetWriteThruAssemblyPath(assembly) + extension;
                    try {
                    } catch (Exception e) {
                        String message = String.format("Could not load assembly " + path + ". Error %1$s", e.getMessage());
                        throw new Exception(message);
                    }
                    if (_dsWriter == null) {
                        throw new Exception("Unable to instantiate " + classname);
                    }
                    _dsWriter.init(startupparams, _cacheName);
                } 
                else if (extension.equals("jar") || extension.equals("class")) {
                    AuthenticateFeature.Authenticate(LanguageContext.JAVA);
                    _languageContext = LanguageContext.JAVA;

                    File deployedFolder = DirectoryUtil.createDeployAssemblyFolder(_cacheName);

                    if (!deployedFolder.exists()) {
                        throw new ConfigurationException("Could not load " + assembly + ". Deploy the required provider's Jar file(s)");

                    }

                    try {
                        if (_loader == null) {
                            _loader = ClassPaths.addPath(deployedFolder, _context.getCacheLog());
                        }
                        Object writer = _loader.loadClass(classname).newInstance();
                        if(writer instanceof CacheWriter)
                        {
                            JCacheWriteThruProvider jWriteThru = new JCacheWriteThruProvider((CacheWriter<Object,Object>)writer);
                            _dsWriter = jWriteThru;
                        }
                        else if(writer instanceof WriteThruProvider)
                        {
                            _dsWriter = (WriteThruProvider) writer;
                        }
                   //     _dsWriter = (WriteThruProvider) _loader.loadClass(classname).newInstance();
                        _dsWriter.init(startupparams, _cacheName);
                    } catch (Exception exception) {
                    }
                }
            } catch (java.lang.ClassCastException e) {
                throw new ConfigurationException("The class specified in write-thru does not implement IDatasourceWriter");
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("ClassNotFoundException " + classname);
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        } catch (ConfigurationException e2) {
            throw e2;
        } catch (Exception e) {
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    private void modifyDefualtClassLoader() {
        Thread.currentThread().setContextClassLoader(_loader);
    }

    public final void HotApplyConfig(long operationDelay) {
        _operationDelay = operationDelay;
    }

    /**
     * True if async write is enabled in config, false otherwise
     */
    public final boolean getAsyncWriteEnabled() {
        return _asyncWrites;
    }

    public final String getMyProviderName() {
        return _myProvider;
    }

    private String GetWriteThruAssemblyPath(String asm) {
        String path = "\\";
        String[] folderNames = asm.split("[,=]", -1);
        path = path + folderNames[0];
        return path;
    }

    private String GetWriteThruAssembly(String asm) {
        String path = "";
        String[] folderNames = asm.split("[,=]", -1);
        path = folderNames[0];
        return path;
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object key, CacheEntry entry, String source, String taskId, OpCode operationCode)throws Exception{
        if (_context.getDsMgr()._writeBehindAsyncProcess != null ) //_asyncWrites &&
        {
            _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new DSWriteBehindOperation(_context._serializationContext, key, null, entry, operationCode, _myProvider, _operationDelay, taskId, source, WriteBehindAsyncProcessor.TaskState.Waite));

        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object key, CacheEntry entry, String source, String taskId, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        if (_context.getDsMgr()._writeBehindAsyncProcess != null) //_asyncWrites &&
        {
            _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new DSWriteBehindOperation(_context._serializationContext, key, null, entry, operationCode, _myProvider, _operationDelay, taskId, source, state));
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object[] keys, Object[] values, CacheEntry[] entries, String source, String taskId, OpCode operationCode)throws Exception {
        if (_context.getDsMgr()._writeBehindAsyncProcess != null ) //_asyncWrites &&
        {
            //_context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new BulkWriteBehindTask(internalCache, this, keys, values, entries, source, taskId, _myProvider, operationCode));
            for (int i = 0; i < keys.length; i++) {
                _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new DSWriteBehindOperation(_context._serializationContext, keys[i], values[i], entries[i], operationCode, _myProvider, _operationDelay, taskId + "-" + i, source, WriteBehindAsyncProcessor.TaskState.Waite));
            }
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object[] keys, Object[] values, CacheEntry[] entries, String source, String taskId, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        if (_context.getDsMgr()._writeBehindAsyncProcess != null ) //_asyncWrites &&
        {
            // _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new BulkWriteBehindTask(internalCache, this, keys, values, entries, source, taskId, _myProvider, operationCode, state));
            for (int i = 0; i < keys.length; i++) {
                _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(new DSWriteBehindOperation(_context._serializationContext, keys[i], values[i], entries[i], operationCode, _myProvider, _operationDelay, taskId + "-" + i, source, state));
            }
        }
    }

    public final void WriteBehind(DSWriteBehindOperation operation)throws Exception {
        if (_context.getDsMgr()._writeBehindAsyncProcess != null ) //_asyncWrites &&
        {
            if (operation != null) {
                operation.incrementRetryCount();
                operation.setOperationState(WriteBehindAsyncProcessor.OperationState.Requeue);
                operation.setOperationDelay(_operationDelay); //config value
                _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(operation);
            }
        }
    }

    public final void WriteBehind(java.util.ArrayList operations)throws Exception {
        if (_context.getDsMgr()._writeBehindAsyncProcess != null) //_asyncWrites &&
        {
            DSWriteBehindOperation operation = null;
            for (int i = 0; i < operations.size(); i++) {
                operation = (DSWriteBehindOperation) ((operations.get(i) instanceof DSWriteBehindOperation) ? operations.get(i) : null);
                if (operations != null) {
                    operation.incrementRetryCount();
                    operation.setOperationState(WriteBehindAsyncProcessor.OperationState.Requeue);
                    operation.setOperationDelay(_operationDelay); //config value
                    _context.getDsMgr()._writeBehindAsyncProcess.Enqueue(operation);
                }
            }
        }
    }

    /**
     * Dequeue a task matching task id
     *
     * @param taskId taskId
     */
    public final void DequeueWriteBehindTask(String[] taskId) throws Exception{
        if (_context.getDsMgr() != null && _context.getDsMgr()._writeBehindAsyncProcess != null) {
            _context.getDsMgr()._writeBehindAsyncProcess.Dequeue(taskId);
        }
    }

    /**
     *
     *
     * @param taskId
     * @param state
     */
    public final void SetState(String taskId, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        if (_context.getDsMgr() != null && _context.getDsMgr()._writeBehindAsyncProcess != null) {
            _context.getDsMgr()._writeBehindAsyncProcess.SetState(taskId, state);
        }
    }

    public final void SetState(String taskId, WriteBehindAsyncProcessor.TaskState state, java.util.HashMap newTable)throws Exception {
        if (_context.getDsMgr() != null && _context.getDsMgr()._writeBehindAsyncProcess != null) {
            _context.getDsMgr()._writeBehindAsyncProcess.SetState(taskId, state, newTable);
        }
    }

    /**
     * Clone the current write behind queue
     *
     * @return write behind queue
     */
    public final WriteBehindAsyncProcessor.WriteBehindQueue CloneQueue() {
        if (_context.getDsMgr() != null && _context.getDsMgr()._writeBehindAsyncProcess != null) {
            return _context.getDsMgr()._writeBehindAsyncProcess.CloneQueue();
        }
        return null;
    }

    public final void CopyQueue(WriteBehindAsyncProcessor.WriteBehindQueue queue)throws Exception {
        if (_context.getDsMgr() != null && _context.getDsMgr()._writeBehindAsyncProcess != null) {
            _context.getDsMgr()._writeBehindAsyncProcess.MergeQueue(_context, queue);
        }
    }

    /**
     * Update the data source, according to type of operation specified
     *
     * @param cacheImpl cache
     * @param key key to reference item
     * @param cacheEntry cache entry
     * @param operationCode type of operation
     */
    public final OperationResult WriteThru(CacheBase cacheImpl, DSWriteOperation operation, boolean async, OperationContext operationContext) throws OperationFailedException {
        if (_context.getDsMgr() == null || (_context != null && !(_context.getDsMgr().getIsWriteThruEnabled() || _context.getDsMgr().getIsWriteBehindEnabled()))) {
            throw new OperationFailedException("Backing source not available. Verify backing source settings");
        }

        Exception exc = null;
        OperationResult dsResult = null;

        try {
            _context.PerfStatsColl.mSecPerDSWriteBeginSample();
            //WriteOperations
            if(_languageContext== LanguageContext.JAVA) DSWriteOperation.setClassLoader(_loader);
            dsResult = WriteThru(operation.GetWriteOperation(_languageContext));
            _context.PerfStatsColl.mSecPerDSWriteEndSample();

        } catch (Exception e) {
            exc = e;
        } finally {

            try {
                if (!async) {
                    this._context.PerfStatsColl.incrementWriteThruPerSec();
                } else {
                    this._context.PerfStatsColl.incrementWriteBehindPerSec();
                }

                if ((exc != null) || (dsResult != null && dsResult.getDSOperationStatus() == OperationResult.Status.Failure)) {
                    switch (operation.getOperationCode()) {
                        case Add:
                        case Update:
                            cacheImpl.Remove(operation.getKey(), ItemRemoveReason.Removed, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                            cacheImpl.getContext().getCacheLog().CriticalInfo("Removing key: " + operation.getKey() + " after data source write operation");
                            break;
                    }
                    _context.PerfStatsColl.incrementDSFailedOpsPerSec();
                    if (exc != null) {
                        throw new OperationFailedException("IWriteThruProvider failed." + exc.getMessage(), exc);
                    }
                } 
                
                if (dsResult != null && (dsResult.getDSOperationStatus() == OperationResult.Status.Failure || dsResult.getDSOperationStatus() == OperationResult.Status.FailureDontRemove)) {
                    _context.PerfStatsColl.incrementDSFailedOpsPerSec();
                    throw new OperationFailedException("IWriteThruProvider failed. " + ((dsResult.getException() != null) ? "Exception: " + dsResult.getException().getMessage() : (dsResult.getError() != null ? "ErrorMessage: " + dsResult.getError() : "")), exc);
                }
                if (dsResult != null && (dsResult.getDSOperationStatus() == OperationResult.Status.Failure || dsResult.getDSOperationStatus() == OperationResult.Status.FailureRetry)) {
                    _context.PerfStatsColl.incrementDSFailedOpsPerSec();
                }
            } catch (Exception ex) {
                throw new OperationFailedException("Error: " + ex.getMessage(), ex);
            }
        }

        return dsResult;
    }

    /**
     * Update the data source, according to type of operation specified
     *
     * @param cacheImpl cache
     * @param keys array of keys to be updated
     * @param values array of values. required in case of insert or add
     * operations. pass null otherwise
     * @param entries array of cache entries. required in case of remove
     * operations. pass null otherwise
     * @param returnSet the table returned from the bulk operation that was
     * performed. this table will be updated accordingly
     * @param operationCode type of operation
     */
   public final OperationResult[] WriteThru(CacheBase cacheImpl, DSWriteOperation[] operations, java.util.HashMap returnSet, boolean async, OperationContext operationContext) throws OperationFailedException{
        Exception exc = null;
        OperationResult[] result = null;

        try {
            int count = operations.length;
            WriteOperation[] writeOperations = new WriteOperation[count];
            //create write operations
            if(_languageContext== LanguageContext.JAVA) DSWriteOperation.setClassLoader(_loader);
            for (int i = 0; i < operations.length; i++) {
                writeOperations[i] = operations[i].GetWriteOperation(_languageContext);
            }
            
            _context.PerfStatsColl.mSecPerDSWriteBeginSample();

            result = WriteThru(writeOperations);
            _context.PerfStatsColl.mSecPerDSWriteEndSample(writeOperations.length);

        } catch (Exception e) {
            exc = e;
        } finally {
            if (!async) {
                this._context.PerfStatsColl.incrementWriteThruPerSecBy(operations.length);
            } else {
                this._context.PerfStatsColl.incrementWriteBehindPerSecBy(operations.length);
            }
                java.util.ArrayList failedOpsKeys = new java.util.ArrayList();
            if (result != null) //no exception
            {
                for (int i = 0; i < result.length; i++) {
                    //populate return set
                    if (result[i].getDSOperationStatus() == OperationResult.Status.Failure || result[i].getDSOperationStatus() == OperationResult.Status.FailureDontRemove) {
                        if (result[i].getException() != null) {
                            returnSet.put(result[i].getOperation().getKey(), result[i].getException());
                        } else if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(result[i].getError())) {
                            returnSet.put(result[i].getOperation().getKey(), new Exception(result[i].getError()));
                        } else {
                            returnSet.put(result[i].getOperation().getKey(), result[i].getDSOperationStatus()); //return status
                        }
                        _context.PerfStatsColl.incrementDSFailedOpsPerSec();
                    } else {
                        if (result[i].getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                            _context.PerfStatsColl.incrementDSFailedOpsPerSec();
                        }
                        returnSet.put(result[i].getOperation().getKey(), result[i].getDSOperationStatus());
                    }
                    if (result[i].getDSOperationStatus() == OperationResult.Status.Failure) {
                        switch (result[i].getOperation().getOperationType()) {
                            case Add:
                            case Update:
                                failedOpsKeys.add(result[i].getOperation().getKey());
                                break;
                        }
                    }
                }
            } else if (exc != null) //remove all batch operations
            {
                for (int i = 0; i < operations.length; i++) {
                    failedOpsKeys.add(operations[i].getKey().toString());
                    returnSet.put(operations[i].getKey().toString(),exc);
                }
                _context.PerfStatsColl.incrementDSFailedOpsPerSecBy(operations.length);
            }
            try {
                if (failedOpsKeys.size() > 0) {
                    String[] failedOps = new String[failedOpsKeys.size()];
                    System.arraycopy(failedOpsKeys.toArray(new Object[0]), 0, failedOps, 0, failedOpsKeys.size());
                    cacheImpl.Remove(failedOps, ItemRemoveReason.Removed, false, operationContext);
                }
            } catch (Exception e) {
                throw new OperationFailedException("Data Source write operation failed. Error: " + e.getMessage(), e);
            }
            

            
        }
        return result;
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return -1 if writer is null, 0 if user operation returned false, 1 if
     * successful
     */
    private OperationResult WriteThru(WriteOperation writeOperation) throws Exception {

        OperationResult result = null;
        if (_dsWriter == null ) {
            return result;
        }

        result = _dsWriter.writeToDataSource(writeOperation);
        return result;
    }


    /**
     *
     *
     * @param keys
     * @param values
     * @param opCode
     * @return
     */
    private OperationResult[] WriteThru(WriteOperation[] writeOperations) throws Exception{
        if (_dsWriter == null ) {
            return null;
        }
        return _dsWriter.writeToDataSource(writeOperations);
    }
}
