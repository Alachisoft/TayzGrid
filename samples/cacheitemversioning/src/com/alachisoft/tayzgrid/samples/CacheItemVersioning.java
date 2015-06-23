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

import com.alachisoft.tayzgrid.samples.data.Customer;
import com.alachisoft.tayzgrid.web.caching.*;
import com.alachisoft.tayzgrid.samples.data.Customer;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;


public class CacheItemVersioning
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
                        
            //Get TayzGrid API cache instance. 
            Cache cache = (Cache) jCache.unwrap(Cache.class); 
                        

            //Cache item versioning makes possible the concurrency checks on cacheitems.
            //Scenario:
            //User X and Y has fetched data from cache
            //User Y makes changes to some cache items and write back the changes to cache
            //Now the version of cache items with User X are obsolete and it can determine 
            //so by using cache item versioning.

            Customer customer = new Customer();
            customer.setName("Scott Prince");
            customer.setAddress("95-A Barnes Road Wallingford, CT");
            customer.setGender("Male");
            customer.setContactNo("25632-5646");
            customer.setAge(23);

            CacheItemVersion version1 = cache.add("Customer:ScottPrince", customer);

            //updaing the customer object in cache;
            customer.setAge(33);
            CacheItemVersion version2 = cache.insert("Customer:ScottPrince", customer);

            if (version1 != version2)
            {
                System.out.println("Item has changed since last time it was fetched.");
                System.out.println();
            }

            //GetItNewer
            //Retrives item from cache based on CacheItemVersion
            //Get item only is version superior to version2
            Customer customer2 = (Customer) cache.getIfNewer("Customer:ScottPrince",
                                                             version2);

            if (customer2 == null)
            {
                System.out.println("Latest version of item is already available");
            }
            else
            {
                System.out.println("Current Version of Customer:ScottPrince is: " + version2.getVersion());
                printCustomerDetails(customer2);
            }

            //Remove
            customer.setName("Mr. Scott n Price");
            CacheItemVersion version3 = cache.insert("Customer:ScottPrince", customer);

            Customer customerRemoved = (Customer) cache.remove("Customer:ScottPrince", version3);

            if (customerRemoved == null)
            {
                System.out.println("Remove failed. The newer version of item exists in cache.");
            }
            else
            {
                System.out.println("Following Customer is removed from cache:");
                printCustomerDetails(customerRemoved);
            }

            //Always manipulate the latest item as follows.

            CacheItemVersion version4 = cache.insert("Customer:ScottPrince", customer);
            customer2 = (Customer) cache.getIfNewer("Customer:ScottPrince", version4);

            if (customer2 == null)
            {
                System.out.println("Item version available is latest!");
            }
            else
            {
                System.out.println("Latest available Item version is fetched from cache");
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
    
     public static void printCustomerDetails(Customer customer) {
        System.out.println();        
        System.out.println("Customer Details are as follows: ");
        System.out.println("Name: " + customer.getName());
        System.out.println("Age: " + customer.getAge());
        System.out.println("Gender: " + customer.getGender());
        System.out.println("Contact No: "  + customer.getContactNo());
        System.out.println("Address: " + customer.getAddress());
    }
}
