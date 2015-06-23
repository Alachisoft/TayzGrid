
package com.alachisoft.tayzgrid.caching.datasourceproviders.JCache;

import java.util.HashMap;
import java.util.*;
import java.io.*;
import javax.cache.integration.CacheWriter;
import javax.cache.*;
import javax.cache.integration.CacheWriterException;
import java.lang.Cloneable;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.*;

/**
 *
 */
public class JCacheWriteThruProvider<K,V> implements WriteThruProvider{
    private CacheWriter _jcacheWriter;
    
    public JCacheWriteThruProvider(CacheWriter<K,V> jcacheWriter){
        _jcacheWriter = jcacheWriter;
    }

    @Override
    public void init(HashMap parameters, String cacheId) throws Exception {
        
    }
    
    private OperationResult addOrUpdate(WriteOperation operation)throws Exception {
        K key = (K)operation.getKey();
        V value = (V)operation.getProviderCacheItem().getValue();
        OperationResult result = new OperationResult(operation, OperationResult.Status.Failure);
        Entry<K,V> entry = new Entry<K, V>(key, value);
        try {
            _jcacheWriter.write(entry);
            result.setDSOperationStatus(OperationResult.Status.Success);
        }
        catch (Exception e) {
               result.setError("Writing to the backing source failed.");
               result.setException(e);
               result.setDSOperationStatus(OperationResult.Status.Failure);
        }
        return result;
    }
    
    private OperationResult delete(WriteOperation operation) throws Exception {
        K key = (K)operation.getKey();
         OperationResult result = new OperationResult(operation, OperationResult.Status.Failure);
        try {
            _jcacheWriter.delete(key);
            result.setDSOperationStatus(OperationResult.Status.Success); 
            }
        catch(Exception e){
            result.setError("Deleting from the backing source failed.");
            result.setException(e);
            result.setDSOperationStatus(OperationResult.Status.Failure);
        }
        return result;
    }

    @Override
    public OperationResult writeToDataSource(WriteOperation operation) throws Exception {
        OperationResult result = new OperationResult(operation, OperationResult.Status.Failure);
        if(operation.getOperationType() == WriteOperationType.Add || operation.getOperationType() == WriteOperationType.Update){
            result = addOrUpdate(operation);
        }
        else if(operation.getOperationType() == WriteOperationType.Delete){
            result = delete(operation);
        }
        return result;
    }

    private OperationResult[] addOrUpdate(WriteOperation[] operations) throws Exception {
        
        OperationResult[] results = new OperationResult[operations.length];
        Entry<K,V> entry = null;
        Entry<K,V> entry2 = null;
        Exception ex = null;
        Collection<Cache.Entry<K,V>> addColl = new ArrayList<Cache.Entry<K,V>>();
        K key = null;
        V value = null;
        for(int i = 0; i < operations.length; i++) {
                key = (K)operations[i].getKey();
                value = (V)operations[i].getProviderCacheItem().getValue();
                entry = new Entry(key, value);
                addColl.add(entry);
                results[i] = new OperationResult(operations[i], OperationResult.Status.Success);
        }
        try {
           _jcacheWriter.writeAll(addColl);  
        }
        catch(Exception e) {
            ex = e;
        }
        if(ex != null || !addColl.isEmpty()) {
            for (int i=0;i<operations.length;i++) {
                key = (K)operations[i].getKey();
                for(Object entry1: addColl) {
                   entry2 = (Entry<K,V>)(entry1);
                   if(entry2.getKey() == key) {
                       results[i].setDSOperationStatus(OperationResult.Status.Failure);
                       results[i].setException(ex);
                       results[i].setError("Writing to the backing source failed.");
                   }
                }
            }
        }
        return results;
    }
    
    private OperationResult[] delete(WriteOperation[] operations) throws Exception {
        Exception ex = null;
        OperationResult[] results = new OperationResult[operations.length];
        K key = null;
        Collection<K> delKeys = new ArrayList<K>();
        for(int i = 0; i<operations.length;i++) {
                key = (K)operations[i].getKey();
                delKeys.add(key);
                results[i] = new OperationResult(operations[i], OperationResult.Status.Success);
            }
        try {
            _jcacheWriter.deleteAll(delKeys);
        }
        //If the exception is to be handled by the app developer, this try-catch
        //block is unnecessaary-review
        catch(Exception e){
            ex = e;   
        }
        if(ex != null || !delKeys.isEmpty()) {
            for (int i=0;i<operations.length; i++) {
                //recheck. looks Suspicious
                for (Object key1: delKeys) {
                   if(key1 == operations[i].getKey()) { 
                        results[i].setDSOperationStatus(OperationResult.Status.Failure);
                        results[i].setException(ex);
                        results[i].setError("Deleting from the backing source failed");
                   }
                }
            }
        }
            return results;
    }
 
    
    @Override
    public OperationResult[] writeToDataSource(WriteOperation[] operations) throws Exception {
        OperationResult[] results = new OperationResult[operations.length];
        
        if(operations[0].getOperationType() == WriteOperationType.Add || operations[0].getOperationType() == WriteOperationType.Update) {
            results = addOrUpdate(operations);
        }
        else if(operations[0].getOperationType() == WriteOperationType.Delete) {
            results = delete(operations);
        }
        return results;      
    }

    @Override
    public void dispose() throws Exception {
        if(_jcacheWriter instanceof java.io.Closeable)
            ((java.io.Closeable)_jcacheWriter).close();
    }    
}




