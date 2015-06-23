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

import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.samples.data.Customer;


import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.LockHandle;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class ItemLocking
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

            //Locking prevents multiple clients from updating the same data simultaneously
            //and also provides the data consistency.

            //Adding an item the cache
            Customer customer = new Customer();
            customer.setName("Kirsten Goli");
            customer.setAge(40);
            customer.setAddress("45-A West Boulevard, Cartago, Costa Rica");
            customer.setGender("Female");
            customer.setContactNo("52566-1779");
            
            cache.add("Customer:KirstenGoli", customer);

            //Get     
            TimeSpan timeSpan = new TimeSpan();
            timeSpan.setSeconds(20);
            LockHandle lockHandle = new LockHandle();
            Customer getCustomer = (Customer) cache.get(
                    "Customer:KirstenGoli",
                    timeSpan,
                    lockHandle,
                    true);

            printCustomerDetails(getCustomer);

            System.out.println("Lock acquired on " + lockHandle.getLockId());

            //Lock item in cache
            boolean isLocked = cache.lock("Customer:KirstenGoli", timeSpan, lockHandle);

            if (!isLocked)
            {
                System.out.println("Lock acquired on " + lockHandle.getLockId());
            }

            //Unlock item in cache
            cache.unlock("Customer:KirstenGoli");

            //Unlock via lockhandle
            cache.unlock("Customer:KirstenGoli", lockHandle);

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
