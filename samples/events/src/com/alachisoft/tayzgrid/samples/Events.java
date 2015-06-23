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

package com.alachisoft.tayzgrid.samples;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class Events
{
    public static void main(String[] args)
    {
        try
        {
            //Create an instance of JCache's caching provider to get JCacheManager's instance by TayzGrid.
            CachingProvider provider = Caching.getCachingProvider();
            CacheManager manager = provider.getCacheManager();        
            
            //Get a cache from manager via its string name.
            javax.cache.Cache cache = manager.getCache("mypartitionedcache");
            cache.clear();

            //Registering for item added type of event.
            EventListener myListener = new EventListener(cache, EventType.ItemAdded);;
            
            //Adding an item to the cache.
            cache.put("Item:1", "Value:1");
           
            //Observer the behaviour of implemented event type.
            
            //Registering for item updated type of event.
            myListener = new EventListener(cache, EventType.ItemUpdated);
       
            cache.replace("Item:1", "Value:1");
            
            //Observer the behaviour of implemented event type.

            myListener = new EventListener(cache, EventType.ItemExpired);
            
            //Observer the behaviour of implemented event type.

            //Registering for item removed type of event.
            myListener = new EventListener(cache, EventType.ItemRemoved);
            
            cache.remove("Item:1");
            
            //Observer the behaviour of implemented event type.

            myListener = new EventListener(cache, EventType.ItemAddedUpdated);
           
            //Must dispose cache
            cache.close();

            System.exit(0);

        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            System.exit(0);
        }

    }
}
