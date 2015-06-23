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

import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.web.caching.Cache;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;


public class Tags {
    
    public static void runTagsDemo()
    {
        try
        {
            System.out.println();
            
            //Create an instance of JCache's caching provider to get JCacheManager's instance by TayzGrid.
            CachingProvider provider = Caching.getCachingProvider();
            CacheManager manager = provider.getCacheManager();        
            
            //Get a cache from manager via its string name.
            javax.cache.Cache jCache = manager.getCache("mypartitionedcache");
                  
            jCache.clear();
                        
            //Get TayzGrid API's cache instance. 
            Cache cache = (Cache) jCache.unwrap(Cache.class); 
            
            //Adding items with named Tags       
            //These tags are used to identify products who are ISOCertified, QCPassed and fall in ClassA
            Tag[] tagList = new Tag[3];
            tagList[0] = new Tag("ISOCertified");
            tagList[1] = new Tag("QCPassed");
            tagList[2] = new Tag("ClassA");
            
            //4 Items are added to the cache
            cache.add("Product:MobilePhone1", "ProductID: XYZ", tagList);
            cache.add("Product:MobilePhone2", "ProductID: ABC", tagList);
            cache.add("Product:MobilePhone3", "ProductID: 123");
            cache.add("Product:MobilePhone4", "ProductID: 456");
            
            //Retrieve items who are QCPassed
            Tag itemTag = new Tag("QCPassed");
            
            HashMap items = cache.getByTag(itemTag);
            if ( !items.isEmpty() )
            {
                Iterator iter = items.values().iterator();
                while(iter.hasNext())
                {
                    System.out.println(iter.next().toString());
                }
            }
            
            //Get keys by tags
            //Here keys can be retrived from the cache via following three methods
            
            //Retrives keys by specified tag
            Collection keysByTag = cache.getKeysByTag(tagList[0]); 
            
            //Retrieves those keys only where complete tagList matches
            Collection keysByEntireTagList = cache.getKeysByAllTags(tagList); 
            
            //Retrieves keys where any item in tagList matches
            Collection keysByAnyTagInList = cache.getKeysByAnyTag(tagList);
                        
            //Get Data by tags
            //Here values can be retrived from the cache via following three methods
                        
            //Retrives values by specified tag
            HashMap valuesByTag = cache.getByTag(tagList[0]);
            
            //Retrieves those values only where complete tagList matches
            HashMap valuesByEntireTagList = cache.getByAllTags(tagList);
            
            //Retrivies values where any item in tagList matches
            HashMap valuesByAnyTagInList = cache.getByAnyTag(tagList);
            
            //Remove items from Cache by tags
            
            //Removes values by specified tag
            cache.removeByTag(tagList[0]);
            
            //Removes those values only where complete tagList matches
            cache.removeByAllTags(tagList);
            
            //Removes values where any item in tagList matches
            cache.removeByAnyTag(tagList);
                                    
            System.out.println();
            
            //Must dispose cache
            cache.dispose();

        }
        catch(Exception ex)
        {
            
        }
    }
    
}
