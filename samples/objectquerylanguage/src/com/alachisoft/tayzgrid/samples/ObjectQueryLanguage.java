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
import com.alachisoft.tayzgrid.samples.data.Product;
import com.alachisoft.tayzgrid.web.caching.*;
import java.util.HashMap;
import java.util.Iterator;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class ObjectQueryLanguage
{

    public static void main(String[] args)
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

            //Querying an item in TayzGrid requires either NamedTags or
            //Indexes defined for objects or both.

            //Querying a simple cache item via namedTags

            NamedTagsDictionary nameTagDict = new NamedTagsDictionary();

            nameTagDict.add("Employee", "DavidBrown");
            nameTagDict.add("Department", "Mathematics");

            cache.add("Employee:DavidBrown", "Department:Mathematics", nameTagDict);

            //Defining Searching criteria
            HashMap criteria = new HashMap();
            criteria.put("Employee", "DavidBrown");

            String query = "SELECT java.lang.String WHERE this.Employee = ?";

            HashMap result = cache.searchEntries(query, criteria);

            if (!result.isEmpty())
            {
                Iterator itor = result.values().iterator();
                while (itor.hasNext())
                {
                    System.out.println(itor.next());
                }
            }

            System.out.println();

            //New product object whose index is definex in TayzGrid Manager
            Product product = new Product(1,
                                      "UninterruptedPowerSupply",
                                      "ClassA",
                                      "QCPassed");

            cache.add("Product:UPS", product);

            //  Query can only be applied to Primitive data types:
            //  java.lang.Integer
            //  java.lang.String
            //  java.lang.Double
            //  java.lang.Float
            //  and
            //  for those non primitive data types whose indexes are defined 
            //  in TayzGrid manager

            query = "Select com.alachisoft.tayzgrid.samples.data.Product Where this.productId = ?";
            criteria = new HashMap();
            criteria.put("productId", 1);

            result = cache.searchEntries(query, criteria);

            if (!result.isEmpty())
            {
                Iterator itor = result.values().iterator();
                while (itor.hasNext())
                {
                    Product productFound = (Product) itor.next();
                    printProductDetails(productFound);
                }
            }

            //Querying indexed object via NamedTags

            nameTagDict = new NamedTagsDictionary();
            nameTagDict.add("Name", "UninterruptedPowerSupply");
            nameTagDict.add("Class", "ClassA");

            cache.insert("Product:UPS", product, nameTagDict);

            query = "Select com.alachisoft.tayzgrid.samples.data.Product Where this.name = ?";

            criteria = new HashMap();
            criteria.put("name", "UninterruptedPowerSupply");

            result = cache.searchEntries(query, criteria);

            if (!result.isEmpty())
            {
                Iterator itor = result.values().iterator();
                while (itor.hasNext())
                {
                    Product productFound = (Product) itor.next();
                    printProductDetails(productFound);
                }
            }

            //Must dispose cache
            cache.dispose();

            System.exit(0);

        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            System.exit(0);
        }
    }
    
    static void printProductDetails(Product product)
    {        
        System.out.println("Id:       " + product.getId());            
        System.out.println("Name:     " + product.getName());            
        System.out.println("Class:    " + product.getClassName());            
        System.out.println("Category: " + product.getCategory());       
        System.out.println();
    }    
}
