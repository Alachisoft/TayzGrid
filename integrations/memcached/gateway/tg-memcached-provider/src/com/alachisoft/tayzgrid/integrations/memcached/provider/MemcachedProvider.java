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

package com.alachisoft.tayzgrid.integrations.memcached.provider;

import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.CacheRuntimeException;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.InvalidArgumentsException;
import com.alachisoft.tayzgrid.web.caching.*;
import com.alachisoft.tayzgrid.runtime.caching.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class MemcachedProvider implements IMemcachedProvider
{
        private Cache _cache;
        private Timer _timer;
        private static MemcachedProvider provider=null;
        
        public static MemcachedProvider getInstance()
        {
            return provider;
        }
        
        public static void setInstance(MemcachedProvider provider)
        { 
            MemcachedProvider.provider=provider; 
        }
        
        // <editor-fold defaultstate="collapsed" desc="Public Interface Implemented Methods">
        @Override
        public OperationResult initCache(String cacheID)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (cacheID.equals(""))
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();

            try 
            {
                _cache=TayzGrid.initializeCache(cacheID);
                returnObject = createReturnObject(Result.SUCCESS, null);
            }
            catch(Exception e)
            {
                throwCacheRuntimeException(e);
            }

            return returnObject;
        }
        
        @Override
        public OperationResult set(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals("") || dataBlock==null || flags < 0)
                throwInvalidArgumentsException();

            OperationResult returnObject = insertItemSuccessfully(key,flags,expirationTimeInSeconds,dataBlock);
            return returnObject;
        }

        @Override
        public OperationResult add(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals("") || dataBlock == null || flags < 0 )
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();
            try
            {
                returnObject = addItemSuccessfully(key, flags, expirationTimeInSeconds, dataBlock);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }
        
        @Override
        public OperationResult replace(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals("") || dataBlock == null || flags < 0 )
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();
            try
            {
                if (_cache.contains(key))
                    returnObject = insertItemSuccessfully(key, flags, expirationTimeInSeconds, dataBlock);
                else
                    returnObject = createReturnObject(Result.ITEM_NOT_FOUND, null);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }

            return returnObject;

        }

        @Override
        public OperationResult checkAndSet(String key, int flags, long expirationTimeInSeconds, long casUnique, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals("") || dataBlock == null || flags < 0 || casUnique < 0)
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();
            try
            {
                CacheItem getCacheItem = _cache.getCacheItem(key);

                if (getCacheItem == null)
                    returnObject = createReturnObject(Result.ITEM_NOT_FOUND, null);
                else
                    if (getCacheItem.getVersion().getVersion() == casUnique)
                        returnObject = insertItemSuccessfully(key,flags,expirationTimeInSeconds,dataBlock);
                    else
                        returnObject = createReturnObject(Result.ITEM_MODIFIED, null);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }

        @Override
        public List<GetOpResult> get(String[] keys)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (keys.length == 0)
                throwInvalidArgumentsException();

            List<GetOpResult> getObjects = new ArrayList<GetOpResult>();
           
            try
            {
                for(int i=0;i<keys.length;i++)
                {
                    CacheItem getObject = _cache.getCacheItem(keys[i]);
                    if (getObject != null)
                        getObjects.add(createGetObject(keys[i], getObject));
                }
            }
            catch(Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return getObjects;
            
        }

        @Override
        public OperationResult append(String key, Object dataToAppend,long casUnique)throws CacheRuntimeException,InvalidArgumentsException
        {
          return concat(key, dataToAppend, casUnique, UpdateType.append);
        }

        @Override
        public OperationResult prepend(String key, Object dataToPrepend,long casUnique)throws CacheRuntimeException,InvalidArgumentsException
        {
           return concat(key, dataToPrepend, casUnique, UpdateType.prepend);
        }
        
        @Override
        public OperationResult delete(String key,long casUnique)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals(""))
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();
            try
            {
                if(casUnique==0)
                {
                    Object obj=_cache.remove(key);
                    if(obj==null)
                        returnObject=createReturnObject(Result.ITEM_NOT_FOUND,null);
                    else
                        returnObject=createReturnObject(Result.SUCCESS,null);
                }
                else
                {
                    CacheItem cacheItem=_cache.getCacheItem(key);
                    if (cacheItem==null)
                        returnObject=createReturnObject(Result.ITEM_NOT_FOUND,null);
                    else
                    {
                        if(cacheItem.getVersion().getVersion()!=casUnique)
                        {
                            returnObject=createReturnObject(Result.ITEM_MODIFIED,null);
                        }
                        else
                        {
                        _cache.delete(key);
                        returnObject = createReturnObject(Result.SUCCESS, null);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }

            return returnObject;
        }

        @Override
        public MutateOpResult increment(String key, long value, Object initialValue, long expirationTimeInSeconds,long casUnique)throws CacheRuntimeException,InvalidArgumentsException
        {
           return mutate(key, value, initialValue, expirationTimeInSeconds, casUnique, UpdateType.increment);
        }
        
        @Override
        public MutateOpResult decrement(String key, long value, Object initialValue, long expirationTimeInSeconds,long casUnique)throws CacheRuntimeException,InvalidArgumentsException
        {
           return mutate(key, value, initialValue, expirationTimeInSeconds, casUnique, UpdateType.decrement);
        }

        @Override
        public OperationResult flush_All(long expirationTimeInSeconds)throws CacheRuntimeException,InvalidArgumentsException
        {
           
            OperationResult returnObject = new OperationResult();
            try
            {
                if (expirationTimeInSeconds == 0)
                    _cache.clear();
                else
                {
                    long dueTimeInMilliseconds = expirationTimeInSeconds * 1000;
                    _timer=new Timer();
                    _timer.schedule(new TimerTask(){public void run(){flushExpirationCallBack();}},  dueTimeInMilliseconds);
                }
                returnObject = createReturnObject(Result.SUCCESS, null);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }

        @Override
        public OperationResult touch(String key, long expirationTimeInSeconds)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals(""))
                throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();

            try
            {
                
                    CacheItemAttributes attributes = new CacheItemAttributes();
                    Date date=createExpirationDate(expirationTimeInSeconds);
                    attributes.setAbsoluteExpiration(date);
                    boolean result=_cache.setAttributes(key, attributes);
                    if(result)
                        returnObject = createReturnObject(Result.SUCCESS, null);
                    else
                        returnObject = createReturnObject(Result.ITEM_NOT_FOUND, null);
               
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }

        @Override
        public OperationResult getVersion()
        {

            return createReturnObject(Result.SUCCESS,"1.4.5_4_gaa7839e");
            
        }

        @Override
        public OperationResult getStatistics(String argument)
        {
            if(argument==null)
                argument="";
            if(argument.startsWith("cachedump"))
                argument="cachedump";
            Hashtable allStatistics = new Hashtable();
            
            if (argument.equals("")) {
            allStatistics = generalStats();
        } else if (argument.equals("settings")) {
            allStatistics = settingsStats();
        } else if (argument.equals("items")) {
            allStatistics = itemsStats();
        } else if (argument.equals("sizes")) {
            allStatistics = itemSizesStats();
        } else if (argument.equals("slabs")) {
            allStatistics = slabsStats();
        } else if (argument.equals("cachedump")) {
            allStatistics = cacheDumpStats();
        }
            return createReturnObject(Result.SUCCESS, allStatistics);
        }

        @Override
        public OperationResult reassignSlabs(int sourceClassID, int destinationClassID)
        {
            return createReturnObject(Result.SUCCESS, null);
        }

        @Override
        public OperationResult automoveSlabs(int option)
        {
            return createReturnObject(Result.SUCCESS, null);
        }

        @Override
        public OperationResult setVerbosityLevel(int verbosityLevel)
        {
            return createReturnObject(Result.SUCCESS, null);
        }
        
        @Override
        public void dispose()throws CacheRuntimeException
        {
            try
            {
            _cache.dispose();
            }
            catch(Exception e)
            {
                throwCacheRuntimeException(e);
            }
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Private Utility Methods">
        private OperationResult insertItemSuccessfully(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException
        { 
            OperationResult returnObject = new OperationResult();
            try
            {
                if (expirationTimeInSeconds < 0) {
                    _cache.delete(key);
                    return createReturnObject(Result.SUCCESS, (long)0);
                }
                CacheItem cacheItem = createCacheItem(flags, dataBlock, expirationTimeInSeconds);
                CacheItemVersion version = _cache.insert(key, cacheItem);
                returnObject.setReturnValue(version.getVersion());
                returnObject.setReturnResult(Result.SUCCESS);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }

        private OperationResult addItemSuccessfully(String key,int flags,long expirationTimeInSeconds,Object dataBlock)throws CacheRuntimeException
        {
            
            OperationResult returnObject = new OperationResult();
            try
            {
                if (expirationTimeInSeconds < 0) {
                    if (_cache.contains(key)) {
                        return createReturnObject(Result.ITEM_EXISTS, null);
                    } else {
                        return createReturnObject(Result.SUCCESS, (long)0);
                    }
                }
                CacheItem cacheItem = createCacheItem(flags, dataBlock, expirationTimeInSeconds);
                CacheItemVersion version = _cache.add(key, cacheItem);
                returnObject.setReturnValue(version.getVersion());
                returnObject.setReturnResult(Result.SUCCESS);
            }
            catch( com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException e)
            {
                return createReturnObject(Result.ITEM_EXISTS,null);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }
        
        private OperationResult concat(String key, Object dataToAppend, long casUnique,UpdateType updateType)throws CacheRuntimeException,InvalidArgumentsException
        {
            if (key.equals("") || dataToAppend==null||casUnique<0)
                        throwInvalidArgumentsException();

            OperationResult returnObject = new OperationResult();
            try
            {
                CacheItem getObject = _cache.getCacheItem(key);

                if (getObject == null)
                    returnObject = createReturnObject(Result.ITEM_NOT_FOUND, null);
                else
                    if((casUnique>0 && getObject.getVersion().getVersion()==casUnique)||casUnique==0)
                        returnObject = joinObjects(key, getObject, dataToAppend, updateType);
                else
                        returnObject=createReturnObject(Result.ITEM_MODIFIED,null);
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
        }
        
        private MutateOpResult mutate(String key,long value,Object initialValue,long expirationTimeInSeconds,long casUnique,UpdateType updateType)throws CacheRuntimeException,InvalidArgumentsException
        {
    
            if (key.equals("") || value < 0 ||(initialValue!=null && !isUnsignedNumeric(initialValue)))
                    throwInvalidArgumentsException();

            MutateOpResult returnObject = new MutateOpResult();
            OperationResult opResult = new OperationResult();
            try
            {
                CacheItem getObject = _cache.getCacheItem(key);

                if (getObject == null)
                {
                    if (initialValue == null || expirationTimeInSeconds == 4294967295L)
                    {
                        returnObject.setReturnValue(null);
                        returnObject.setReturnResult(Result.ITEM_NOT_FOUND);
                    }
                    else
                    {
                        opResult = insertItemSuccessfully(key, 10, expirationTimeInSeconds, initialValue.toString().getBytes());
                        returnObject.setReturnValue(opResult.getReturnValue());
                        returnObject.setReturnResult(opResult.getReturnResult());
                        returnObject.setMutateResult(Long.parseLong(initialValue.toString()));
                    }
                }
                else
                    if ((casUnique > 0 && getObject.getVersion().getVersion() == casUnique) || casUnique == 0)
                        returnObject = updateIfNumeric(key, getObject, value, updateType);
                    else
                    {
                        returnObject.setReturnValue(null);
                        returnObject.setReturnResult(Result.ITEM_MODIFIED);
                    }

            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }
            return returnObject;
}

        private CacheItem createCacheItem(int flags, Object dataBlock, long expirationTimeInSeconds)
        {
            CacheItem cacheItem = new CacheItem(createObjectArray(flags, dataBlock));
            if (expirationTimeInSeconds != 0)
                cacheItem.setAbsoluteExpiration(createExpirationDate(expirationTimeInSeconds));
            return cacheItem;
        }

        private byte[] createObjectArray(int flags, Object dataBlock)
        {
            byte[] flagBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(flags).array();
            byte[] dataBytes = (byte[])dataBlock;
            byte[] objectArray = new byte[flagBytes.length + dataBytes.length];
            System.arraycopy(flagBytes, 0, objectArray, 0, flagBytes.length);
            System.arraycopy(dataBytes, 0, objectArray, flagBytes.length, dataBytes.length);
            return objectArray;
        }
        
         private ObjectArrayData getObjectArrayData(Object retrievedObject)
        {
            byte[] objectArray = (byte[])retrievedObject;
            byte[] dataArray = new byte[objectArray.length - 4];
            System.arraycopy(objectArray, 4, dataArray, 0, dataArray.length);

            ObjectArrayData objectArrayData = new ObjectArrayData();
            objectArrayData.flags = ByteBuffer.wrap(objectArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
            objectArrayData.dataBytes = dataArray;
            return objectArrayData;

        }
        private Date createExpirationDate(long expirationTimeInSeconds)
        {
             
            Date dateTime=new Date();
            if (expirationTimeInSeconds <= 2592000)
                dateTime.setSeconds(dateTime.getSeconds()+(int)expirationTimeInSeconds);
            else
            {
                Date unixTime = new Date(expirationTimeInSeconds*1000L);
                dateTime = unixTime;
            }
            return dateTime;
        }

        private OperationResult createReturnObject(Result result, Object value)
        {
            OperationResult returnObject = new OperationResult();
            returnObject.setReturnResult(result);
            returnObject.setReturnValue(value);
            return returnObject;
        }

        private GetOpResult createGetObject(String key, CacheItem cacheItem)
        {
            GetOpResult getObject = new GetOpResult();
            ObjectArrayData objectArrayData = getObjectArrayData(cacheItem.getValue());
            getObject.setKey(key);
            getObject.setFlag(objectArrayData.flags);
            getObject.setReturnValue(objectArrayData.dataBytes);
            getObject.setVersion(cacheItem.getVersion().getVersion());
            getObject.setReturnResult(Result.SUCCESS);
            return getObject;
        }

        private OperationResult joinObjects(String key, CacheItem cacheItem, Object objectToJoin,UpdateType updateType) throws CacheRuntimeException
        {
            OperationResult returnObject = new OperationResult();
            ObjectArrayData objectDataArray = getObjectArrayData(cacheItem.getValue());
            byte[] originalByteObject = objectDataArray.dataBytes;
            byte[] byteObjectToJoin = (byte[])objectToJoin;

            byte[] joinedObject = new byte[originalByteObject.length + byteObjectToJoin.length];

            if (updateType == UpdateType.append)
            {
                System.arraycopy(originalByteObject, 0, joinedObject, 0, originalByteObject.length);
                System.arraycopy(byteObjectToJoin, 0, joinedObject, originalByteObject.length, byteObjectToJoin.length);
            }
            else
            {
                System.arraycopy(byteObjectToJoin, 0, joinedObject, 0, byteObjectToJoin.length);
                System.arraycopy(originalByteObject, 0, joinedObject, byteObjectToJoin.length, originalByteObject.length);
            }
            cacheItem.setValue(createObjectArray(objectDataArray.flags, joinedObject));
            try
            {
                CacheItemVersion version = _cache.insert(key, cacheItem);
                returnObject = createReturnObject(Result.SUCCESS, version.getVersion());
            }
            catch (Exception e)
            {
                throwCacheRuntimeException(e);
            }

            return returnObject;
        }
        
        private MutateOpResult updateIfNumeric(String key, CacheItem cacheItem, long value, UpdateType updateType)throws CacheRuntimeException
        {
            MutateOpResult returnObject = new MutateOpResult();
            ObjectArrayData objectDataArray = getObjectArrayData(cacheItem.getValue());
            String tempObjectString = "";
            try
            {
                tempObjectString = new String(objectDataArray.dataBytes);
            }
            catch (Exception e)
            {
               throwCacheRuntimeException(e);
            }

            if (isUnsignedNumeric(tempObjectString))
            {
                long originalValue = Long.valueOf(tempObjectString);
                long finalValue;

                if (updateType == UpdateType.increment)
                {
                    finalValue = originalValue + value;
                }
                else
                {
                    if (value > originalValue)
                        finalValue = 0;
                    else
                        finalValue = originalValue - value;
                }
                cacheItem.setValue(createObjectArray(objectDataArray.flags,(finalValue + "").getBytes()));
                try
                {
                    CacheItemVersion version = _cache.insert(key, cacheItem);
                    returnObject.setReturnResult(Result.SUCCESS);
                    returnObject.setReturnValue(version.getVersion());
                    returnObject.setMutateResult(finalValue);
                }
                catch (Exception e)
                {
                    throwCacheRuntimeException(e);
                }
            }
            else
            {
                returnObject.setReturnResult(Result.ITEM_TYPE_MISMATCHED);
                returnObject.setReturnValue(null);
                returnObject.setMutateResult(0);
            }
            return returnObject;
            
        }

        private Boolean isUnsignedNumeric(Object item)
        {
            try
            {
                if(Long.parseLong(item.toString())<0)
                    return false;
                return true;
            }
            catch(Exception e)
            {
                return false;
            }
        }

        private void flushExpirationCallBack()
        {
            try
            {
                _cache.clear();
                _timer.cancel();
            }
            catch (Exception e)
            {
                //Do nothing
            }
        }

        private void throwInvalidArgumentsException() throws InvalidArgumentsException
        {
            InvalidArgumentsException exception = new InvalidArgumentsException("Invalid Arguments Specified.");
            throw exception;
        }

        private void throwCacheRuntimeException(Exception ex) throws CacheRuntimeException
        {
            CacheRuntimeException exception = new CacheRuntimeException("Exception Occured at server. "+ ex.getMessage(),ex);
            throw exception;
        }

        private Hashtable generalStats()
        {
            Hashtable generalStatistics = new Hashtable();
            String statValue;
            
            statValue = "0";
            generalStatistics.put("pid", statValue);
            statValue = "0";
            generalStatistics.put("uptime", statValue);
            statValue = "0";
            generalStatistics.put("time", statValue);
            statValue = "0";
            generalStatistics.put("version", statValue);
            statValue = "0";
            generalStatistics.put("pointer_size", statValue);
            statValue = "0";
            generalStatistics.put("rusage_user", statValue);
            statValue = "0";
            generalStatistics.put("rusage_system", statValue);
            statValue = "0";
            generalStatistics.put("curr_items", statValue);
            statValue = "0";
            generalStatistics.put("total_items", statValue);
            statValue = "0";
            generalStatistics.put("bytes", statValue);
            statValue = "0";
            generalStatistics.put("curr_connections", statValue);
            statValue = "0";
            generalStatistics.put("total_connections", statValue);
            statValue = "0";
            generalStatistics.put("connection_structures", statValue);
            statValue = "0";
            generalStatistics.put("reserved_fds ", statValue);
            statValue = "0";
            generalStatistics.put("cmd_get", statValue);
            statValue = "0";
            generalStatistics.put("cmd_set", statValue);
            statValue = "0";
            generalStatistics.put("cmd_flush", statValue);
            statValue = "0";
            generalStatistics.put("cmd_touch", statValue);
            statValue = "0";
            generalStatistics.put("get_hits", statValue);
            statValue = "0";
            generalStatistics.put("get_misses", statValue);
            statValue = "0";
            generalStatistics.put("delete_misses", statValue);
            statValue = "0";
            generalStatistics.put("delete_hits", statValue);
            statValue = "0";
            generalStatistics.put("incr_misses", statValue);
            statValue = "0";
            generalStatistics.put("incr_hits", statValue);
            statValue = "0";
            generalStatistics.put("decr_misses", statValue);
            statValue = "0";
            generalStatistics.put("decr_hits", statValue);
            statValue = "0";
            generalStatistics.put("cas_misses", statValue);
            statValue = "0";
            generalStatistics.put("cas_hits", statValue);
            statValue = "0";
            generalStatistics.put("cas_badval", statValue);
            statValue = "0";
            generalStatistics.put("touch_hits", statValue);
            statValue = "0";
            generalStatistics.put("touch_misses", statValue);
            statValue = "0";
            generalStatistics.put("auth_cmds", statValue);
            statValue = "0";
            generalStatistics.put("auth_errors", statValue);
            statValue = "0";
            generalStatistics.put("evictions", statValue);
            statValue = "0";
            generalStatistics.put("reclaimed", statValue);
            statValue = "0";
            generalStatistics.put("bytes_read", statValue);
            statValue = "0";
            generalStatistics.put("bytes_written", statValue);
            statValue = "0";
            generalStatistics.put("limit_maxbytes", statValue);
            statValue = "0";
            generalStatistics.put("threads", statValue);
            statValue = "0";
            generalStatistics.put("conn_yields", statValue);
            statValue = "0";
            generalStatistics.put("hash_power_level", statValue);
            statValue = "0";
            generalStatistics.put("hash_bytes", statValue);
            statValue = "0";
            generalStatistics.put("hash_is_expanding", statValue);
            statValue = "0";
            generalStatistics.put("expired_unfetched", statValue);
            statValue = "0";
            generalStatistics.put("evicted_unfetched", statValue);
            statValue = "0";
            generalStatistics.put("slab_reassign_running", statValue);
            statValue = "0";
            generalStatistics.put("slabs_moved ", statValue);

            return generalStatistics;
        }

        private Hashtable settingsStats()
        {
            Hashtable settingsStatistics = new Hashtable();
            String statValue;

             statValue = "0";
            settingsStatistics.put("maxbytes", statValue);
            statValue = "0";
            settingsStatistics.put("maxconns", statValue);
            statValue = "0";
            settingsStatistics.put("tcpport", statValue);
            statValue = "0";
            settingsStatistics.put("udpport", statValue);
            statValue = "0";
            settingsStatistics.put("inter", statValue);
            statValue = "0";
            settingsStatistics.put("verbosity", statValue);
            statValue = "0";
            settingsStatistics.put("oldest", statValue);
            statValue = "0";
            settingsStatistics.put("evictions", statValue);
            statValue = "0";
            settingsStatistics.put("domain_socket", statValue);
            statValue = "0";
            settingsStatistics.put("umask", statValue);
            statValue = "0";
            settingsStatistics.put("growth_factor", statValue);
            statValue = "0";
            settingsStatistics.put("chunk_size", statValue);
            statValue = "0";
            settingsStatistics.put("num_threads", statValue);
            statValue = "0";
            settingsStatistics.put("stat_key_prefix", statValue);
            statValue = "0";
            settingsStatistics.put("detail_enabled", statValue);
            statValue = "0";
            settingsStatistics.put("reqs_per_event", statValue);
            statValue = "0";
            settingsStatistics.put("cas_enabled", statValue);
            statValue = "0";
            settingsStatistics.put("tcp_backlog", statValue);
            statValue = "0";
            settingsStatistics.put("auth_enabled_sasl", statValue);
            statValue = "0";
            settingsStatistics.put("item_size_max", statValue);
            statValue = "0";
            settingsStatistics.put("maxconns_fast", statValue);
            statValue = "0";
            settingsStatistics.put("hashpower_init", statValue);
            statValue = "0";
            settingsStatistics.put("slab_reassign", statValue);
            statValue = "0";
            settingsStatistics.put("slab_automove", statValue);

            return settingsStatistics;
        }

        private Hashtable itemsStats()
        {
            Hashtable itemsStatistics = new Hashtable();
            String statValue;

            statValue = "0";
            itemsStatistics.put("number", statValue);
            statValue = "0";
            itemsStatistics.put("age", statValue);
            statValue = "0";
            itemsStatistics.put("evicted", statValue);
            statValue = "0";
            itemsStatistics.put("evicted_nonzero", statValue);
            statValue = "0";
            itemsStatistics.put("evicted_time", statValue);
            statValue = "0";
            itemsStatistics.put("outofmemory", statValue);
            statValue = "0";
            itemsStatistics.put("tailrepairs", statValue);
            statValue = "0";
            itemsStatistics.put("reclaimed", statValue);
            statValue = "0";
            itemsStatistics.put("expired_unfetched", statValue);
            statValue = "0";
            itemsStatistics.put("evicted_unfetched", statValue);

            return itemsStatistics;
        }

        private Hashtable itemSizesStats()
        {

            Hashtable itemSizesStatistics = new Hashtable();
            itemSizesStatistics.put("0", "0");
            return itemSizesStatistics;
        }

        private Hashtable slabsStats()
        {
            Hashtable generalStatistics = new Hashtable();
            String statValue;

            statValue = "0";
            generalStatistics.put("pid", statValue);
            statValue = "0";
            generalStatistics.put("uptime", statValue);
            statValue = "0";
            generalStatistics.put("time", statValue);
            statValue = "0";
            generalStatistics.put("version", statValue);
            statValue = "0";
            generalStatistics.put("pointer_size", statValue);
            statValue = "0";
            generalStatistics.put("rusage_user", statValue);
            statValue = "0";
            generalStatistics.put("rusage_system", statValue);
            statValue = "0";
            generalStatistics.put("curr_items", statValue);
            statValue = "0";
            generalStatistics.put("total_items", statValue);
            statValue = "0";
            generalStatistics.put("bytes", statValue);
            statValue = "0";
            generalStatistics.put("curr_connections", statValue);
            statValue = "0";
            generalStatistics.put("total_connections", statValue);
            statValue = "0";
            generalStatistics.put("connection_structures", statValue);
            statValue = "0";
            generalStatistics.put("reserved_fds ", statValue);
            statValue = "0";
            generalStatistics.put("cmd_get", statValue);
            statValue = "0";
            generalStatistics.put("cmd_set", statValue);
            statValue = "0";
            generalStatistics.put("cmd_flush", statValue);
            statValue = "0";
            generalStatistics.put("cmd_touch", statValue);
            statValue = "0";
            generalStatistics.put("get_hits", statValue);
            statValue = "0";
            generalStatistics.put("get_misses", statValue);
            statValue = "0";
            generalStatistics.put("delete_misses", statValue);
            statValue = "0";
            generalStatistics.put("delete_hits", statValue);
            statValue = "0";
            generalStatistics.put("incr_misses", statValue);
            statValue = "0";
            generalStatistics.put("incr_hits", statValue);
            statValue = "0";
            generalStatistics.put("decr_misses", statValue);
            statValue = "0";
            generalStatistics.put("decr_hits", statValue);
            statValue = "0";
            generalStatistics.put("cas_misses", statValue);
            statValue = "0";
            generalStatistics.put("cas_hits", statValue);
            statValue = "0";
            generalStatistics.put("cas_badval", statValue);
            statValue = "0";
            generalStatistics.put("touch_hits", statValue);
            statValue = "0";
            generalStatistics.put("touch_misses", statValue);
            statValue = "0";
            generalStatistics.put("auth_cmds", statValue);
            statValue = "0";
            generalStatistics.put("auth_errors", statValue);
            statValue = "0";
            generalStatistics.put("evictions", statValue);
            statValue = "0";
            generalStatistics.put("reclaimed", statValue);
            statValue = "0";
            generalStatistics.put("bytes_read", statValue);
            statValue = "0";
            generalStatistics.put("bytes_written", statValue);
            statValue = "0";
            generalStatistics.put("limit_maxbytes", statValue);
            statValue = "0";
            generalStatistics.put("threads", statValue);
            statValue = "0";
            generalStatistics.put("conn_yields", statValue);
            statValue = "0";
            generalStatistics.put("hash_power_level", statValue);
            statValue = "0";
            generalStatistics.put("hash_bytes", statValue);
            statValue = "0";
            generalStatistics.put("hash_is_expanding", statValue);
            statValue = "0";
            generalStatistics.put("expired_unfetched", statValue);
            statValue = "0";
            generalStatistics.put("evicted_unfetched", statValue);
            statValue = "0";
            generalStatistics.put("slab_reassign_running", statValue);
            statValue = "0";
            generalStatistics.put("slabs_moved ", statValue);

            return generalStatistics;
        }
        
        private Hashtable cacheDumpStats()
        {
            Hashtable cacheDumpStatistics = new Hashtable();
            cacheDumpStatistics.put("0", "0");
            return cacheDumpStatistics;
        }
        // </editor-fold>
        
        private enum UpdateType
        {
            append,
            prepend,
            increment,
            decrement
        }
        
        class ObjectArrayData
        {
            public int flags;
            public byte[] dataBytes;
        }
}
