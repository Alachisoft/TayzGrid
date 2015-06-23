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

import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.web.caching.Cache;
import java.util.HashMap;
import java.util.Iterator;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class NamedTags {
    public static void runNamedTagsDemo()
    {
        try
        {
            //Create an instance of JCache's caching provider to get JCacheManager's instance by TayzGrid.
            CachingProvider provider = Caching.getCachingProvider();
            CacheManager manager = provider.getCacheManager();        
            
            //Get a cache from manager via its string name.
            javax.cache.Cache jCache = manager.getCache("mypartitionedcache");
                  
            jCache.clear();
                        
            //Get TayzGrid API's cache instance. 
            Cache cache = (Cache) jCache.unwrap(Cache.class); 
            
            NamedTagsDictionary namedTagDict = new NamedTagsDictionary();
            
            namedTagDict.add("Department", "Marketing");
            namedTagDict.add("EmployeeCount", 40);
            
            cache.add("EmployeeID:1", "John Samuel", namedTagDict);
            cache.add("EmployeeID:2", "David Parker", namedTagDict);
            
            
            String query = "SELECT $Text$ WHERE this.Department = ? AND this.EmployeeCount > ?";
                                 
            HashMap values = new HashMap();
            values.put("Department", "Marketing");
            values.put("EmployeeCount", 35);
            
            HashMap resultItems = cache.searchEntries(query, values);
            
            if ( !resultItems.isEmpty() )
            {
                Iterator iter = resultItems.values().iterator();
                while( iter.hasNext() )
                {
                    System.out.println(iter.next().toString());                                                                     
                }                
            }         
            
            //Must dispose cache
            cache.dispose();

        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }        
}
