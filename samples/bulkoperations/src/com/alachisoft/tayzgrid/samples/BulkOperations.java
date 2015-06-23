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

import com.alachisoft.tayzgrid.samples.data.Product;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;


public class BulkOperations
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

            String[] keysToAdd = new String[]
            {
                "Product:Cheese", "Product:Butter",
                "Product:Cream", "Product:Youghart"
            };

            Product[] products = new Product[4];

            products[0] = new Product(1, "Dairy Milk Cheese", "ClassA", "Edibles");
            products[1] =  new Product(2, "American Butter", "ClassA", "Edibles");
            products[2] = new Product(3, "Walmart Delicious Cream", "ClassA", "Edibles");
            products[3] = new Product(4, "Nestle Youghart", "ClassA", "Edibles");

            
            HashMap map = new HashMap();
   
            for(int i=0; i<keysToAdd.length; i++){
                map.put(keysToAdd[i], products[i]);
            }
 
            
            //Adding items to the cahce in bulk.            
            cache.putAll(map);
                     

            //Getting bulk items from the cache.
            Map items = cache.getAll(map.keySet());
            if (!items.isEmpty())
            {
                for (Iterator iter = items.values().iterator(); iter.hasNext();)
                {
                    Product product = (Product) iter.next();
                    printProductDetails(product);
                }
            }

            //Remove in bulk operation tries to remove all the given items provided in the keyset with matching key in the cache.
            cache.removeAll(map.keySet());          

           
            //Removes all items from the cache.
            cache.removeAll();  

            //Must dispose cache 
            cache.close();

            System.exit(0);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
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
