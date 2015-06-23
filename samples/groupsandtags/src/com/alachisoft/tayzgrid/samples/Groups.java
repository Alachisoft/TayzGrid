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


import com.alachisoft.tayzgrid.web.caching.Cache;
import java.util.HashMap;
import java.util.Iterator;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class Groups {
    
    public static void runGroupsDemo()
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
            
            //Adding item in same group
            //Group can be done at two levels 
            //Groups and Subgroups.          
            
            cache.add("Product:CellularPhoneHTC", "HTCPhone", "Electronics", "Mobiles");
            cache.add("Product:CellularPhoneNokia", "NokiaPhone", "Electronics", "Mobiles");
            cache.add("Product:CellularPhoneSamsung", "SamsungPhone", "Electronics", "Mobiles");
            cache.add("Product:ProductLaptopAcer", "AcerLaptop", "Electronics", "Laptops");
            cache.add("Product:ProductLaptopHP", "HPLaptop", "Electronics", "Laptops");
            cache.add("Product:ProductLaptopDell", "DellLaptop", "Electronics", "Laptops");
            cache.add("Product:ElectronicsHairDryer", "HairDryer", "Electronics", "SmallElectronics");
            cache.add("Product:ElectronicsVaccumCleaner", "VaccumCleaner", "Electronics", "SmallElectronics");
            cache.add("Product:ElectronicsIron", "Iron", "Electronics", "SmallElectronics");
                        
            // Getting group data
            HashMap items = cache.getGroupData("Electronics", null); // Will return nine items since no subgroup is defined;
            if ( !items.isEmpty() ) 
            {
                System.out.println("Item count: " + items.size());
                System.out.println("Following Products are found in group 'Electronics'");
                Iterator itor = items.values().iterator();
                while( itor.hasNext() )
                {                    
                    System.out.println(itor.next().toString());
                }
                System.out.println();
            }
                        
            items = cache.getGroupData("Electronics", "Mobiles"); // Will return thre items under the subgroup Mobiles
            if ( !items.isEmpty() ) 
            {
                System.out.println("Item count: " + items.size());
                System.out.println("Following Products are found in group 'Electronics' and Subgroup 'Mobiles'");
                Iterator itor = items.values().iterator();
                while( itor.hasNext() )
                {
                    
                    System.out.println(itor.next().toString());
                }
                System.out.println();
            }                
            
            //getGroupKeys is yet another function to retrive group data.
            //It however requires multiple iterations to retrive actual data
            //1) To get List of Keys and 2) TO get items for the return List of Keys
            
            //Updating items in groups
            cache.insert("Product:ElectronicsIron", "PanaSonicIron", "Electronics", "SmallElectronics"); //Item is updated at the specified group
            
            
            //Removing group data
            System.out.println("Item count: " + cache.getCount()); // Itemcount = 9
            cache.removeGroupData("Electronics", "Mobiles");  // Will remove 3 items from cache based on subgroup Mobiles     
            
            System.out.println("Item count: " + cache.getCount()); // Itemcount = 6
            cache.removeGroupData("Electronics", null); // Will remove all items from cache based on group Electronics
            
            System.out.println("Item count: " + cache.getCount()); // Itemcount = 0
            
            System.out.println();
            
            //Must dispose cache
            cache.dispose();

        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }        
}
