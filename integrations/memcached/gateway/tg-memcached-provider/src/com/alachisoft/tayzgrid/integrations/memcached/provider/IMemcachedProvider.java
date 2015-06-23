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
import java.util.*;

public interface IMemcachedProvider 
{
        OperationResult initCache(String cacheID)throws CacheRuntimeException,InvalidArgumentsException;

        OperationResult set(String key, int flags, long expirationTimeInSeconds, Object dataBlock) throws CacheRuntimeException,InvalidArgumentsException;
        OperationResult add(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException;
        OperationResult replace(String key, int flags, long expirationTimeInSeconds, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException;
        OperationResult checkAndSet(String key, int flags, long expirationTimeInSeconds,  long casUnique, Object dataBlock)throws CacheRuntimeException,InvalidArgumentsException;
       
        List<GetOpResult> get(String[] keys)throws CacheRuntimeException,InvalidArgumentsException;

        OperationResult append(String key, Object dataToAppend,long casUnique)throws CacheRuntimeException,InvalidArgumentsException;
        OperationResult prepend(String key, Object dataToPrepend,long casUnique)throws CacheRuntimeException,InvalidArgumentsException;

        OperationResult delete(String key,long casUnique)throws CacheRuntimeException,InvalidArgumentsException;

        MutateOpResult increment(String key, long value, Object initialValue, long expirationTimeInSeconds,long casUnique)throws CacheRuntimeException,InvalidArgumentsException;
        MutateOpResult decrement(String key, long value, Object initialValue, long expirationTimeInSeconds,long casUnique)throws CacheRuntimeException,InvalidArgumentsException;

        OperationResult flush_All(long expirationTimeInSeconds)throws CacheRuntimeException,InvalidArgumentsException;
        OperationResult touch(String key, long expirationTimeInSeconds)throws CacheRuntimeException,InvalidArgumentsException;
        
        OperationResult getVersion();
        OperationResult getStatistics(String argument);

        OperationResult reassignSlabs(int sourceClassID, int destinationClassID);
        OperationResult automoveSlabs(int option);
        OperationResult setVerbosityLevel(int verbosityLevel);
        void dispose()throws CacheRuntimeException;
}
