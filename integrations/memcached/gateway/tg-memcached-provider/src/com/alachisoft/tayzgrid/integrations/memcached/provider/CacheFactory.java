
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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CacheFactory {
      public static ILogger _logger=null;
     
      public static void setLogger(ILogger logger)
      {
          _logger=logger;
      }
      public static IMemcachedProvider createCacheProvider(String cacheProviderName)
        {
            synchronized(CacheFactory.class)
            {
                try
                {
                    
                IMemcachedProvider instance=MemcachedProvider.getInstance();
                
                
                if (instance == null)
                {
                    MemcachedProvider.setInstance(new MemcachedProvider());
                    MemcachedProvider.getInstance().initCache(cacheProviderName);
                }
                }
                catch(Exception e)
                {
                    if(_logger!=null)
                        _logger.Error("CacheFactory","\tUnable to initialize cache instance. Exception: "+e);
                    MemcachedProvider.setInstance(null);
                }
            }
            

            return MemcachedProvider.getInstance();
        }

        public static void disposeCacheProvider()
        {
            try
            {
            if (MemcachedProvider.getInstance() != null)
                MemcachedProvider.getInstance().dispose();
            }
            catch(Exception e)
            { 
            }
        }
}
