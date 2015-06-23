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
import com.alachisoft.tayzgrid.samples.data.Product;
import java.util.Random;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class DataProviders
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

            Product product = new Product();
            product.setName("Item:X");
            product.setId(5);
            product.setCategory("Electronics");
            product.setClassName("ClassA");
            
            Customer customer = new Customer();
            customer.setName("Daniel Belanger");            
            customer.setAge(24);
            customer.setGender("Male");
            customer.setContactNo("56435-3215");
            customer.setAddress("Chicago, Illinois");
            
            //The DataSource providers will be used by JCache as per specification as Oracle documents it.
            //If an operation on cache triggers backing source's (Write-Thru/Read-Thru) call it will be handled
            //by the TayzGrid provider. In order to run this sample without any problems, the Write-Thru/Read-Thru 
            //provider(s) must be deployed to server(s) via TayzGrid Manager prior to the running of this sample.
            
            cache.put(getkey("Item:X"), product);
            cache.put("Customer:DanielBelanger", customer);

            //clearing cache
            cache.clear();


            //Getting item from backing source
            Customer customerFound = (Customer) cache.get("Customer:DanielBelanger");
            
            if (customerFound != null)
            {
                printCustomerDetails(customerFound);
            }
            else
            {
                System.out.println("Customer not found!");
            }

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
    
    public static void printCustomerDetails(Customer customer) {
        System.out.println();        
        System.out.println("Customer Details are as follows: ");
        System.out.println("Name:       " + customer.getName());
        System.out.println("Age:        " + customer.getAge());
        System.out.println("Gender:     " + customer.getGender());
        System.out.println("Contact No: "  + customer.getContactNo());
        System.out.println("Address:    " + customer.getAddress());
    }
      public static String getkey(String prefix)
    {
        Random r= new Random();
        return prefix + r.nextInt();
    }
}