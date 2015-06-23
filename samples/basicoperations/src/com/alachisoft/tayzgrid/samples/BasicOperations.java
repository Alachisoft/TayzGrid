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
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class BasicOperations {

    public static void main(String[] args) {

        try {
            
            //Create an instance of JCache's caching provider to get JCacheManager's instance by TayzGrid.
            CachingProvider provider = Caching.getCachingProvider();
            CacheManager manager = provider.getCacheManager();        
            
            //Get a cache from manager via its string name.
            javax.cache.Cache cache = manager.getCache("mypartitionedcache");
                  
            cache.clear();

            //Another method to add item(s) to cache is via CacheItem  object
            Customer customer = new Customer();
            customer.setName("David Johnes");
            customer.setAge(23);
            customer.setGender("Male");
            customer.setContactNo("12345-6789");
            customer.setAddress("Silicon Valley, Santa Clara, California");

            //Adding an item to the cache.
            cache.put("Customer:DavidJohnes", customer);
            
            //Getting the added item by the key.
            Customer cachedCustomer = (Customer) cache.get("Customer:DavidJohnes");
            printCustomerDetails(cachedCustomer);
            
            //Updating the existing item in cache.
            customer.setAge(50);
            cache.replace("Customer:DavidJohnes", customer);

            //Get the item from the cache.
            cachedCustomer = (Customer) cache.get("Customer:DavidJohnes");
            printCustomerDetails(cachedCustomer);

            //Gets and removes item from the cache.
            cachedCustomer = (Customer) cache.getAndRemove("Customer:DavidJohnes");
            
            cache.put("Customer:DavidJohnes", customer);
            
            //Remove the existing customer
            cache.remove("Customer:DavidJohnes");
            
            cache.put("Customer:DavidJohnes", customer);

            //Remove an item with having specific value.
            cache.remove("Customer:DavidJohnes", customer);
            
            //Checking if the items exists in the cache.
            boolean keyPresent = cache.containsKey("Customer:DavidJohnes");
        
            //Dispose the cache once done
            cache.close();

            System.exit(0);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    public static void printCustomerDetails(Customer customer) {
        System.out.println();
        System.out.println("Customer Details are as follows: ");
        System.out.println("Name: " + customer.getName());
        System.out.println("Age: " + customer.getAge());
        System.out.println("Gender: " + customer.getGender());
        System.out.println("Contact No: " + customer.getContactNo());
        System.out.println("Address: " + customer.getAddress());
        System.out.println();
    }
}