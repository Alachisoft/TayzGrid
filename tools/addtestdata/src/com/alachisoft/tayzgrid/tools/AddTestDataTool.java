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

package com.alachisoft.tayzgrid.tools;

//import com.alachisoft.tayzgrid.management.NCacheRPCService;
//import com.alachisoft.tayzgrid.management.ToolsRPCService;
//import com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration;
//import com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.web.caching.*;
import com.alachisoft.tayzgrid.runtime.*;
import java.util.Calendar;

public final class AddTestDataTool {

    private static AddTestDataParam cParam = new AddTestDataParam();

    public static void Run(String[] args) throws Exception {

        try {
            Object param = new AddTestDataParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (AddTestDataParam) param;

            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }

//            NCacheRPCService NCache = ToolsRPCService.GetRPCService();
//            com.alachisoft.tayzgrid.management.ICacheServer cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
//            
//            if(cacheServer == null)
//            {throw new Exception("TayzGrid service can not be contacted");}
//            
//            //Get client.conf not cache.conf
//            ClientConfiguration cientConfig =  cacheServer.GetClientConfiguration(cParam.getCacheId());
//            CacheConfiguration[] registered = cientConfig.getCacheConfigurations();
//            //ConfiguredCacheInfo[] registered = cacheServer.GetAllConfiguredCaches();
//            boolean cacheExists=false;
//            for(int i=0;i<registered.length;i++)
//            {
//                if(registered[i].getCacheId().equalsIgnoreCase(cParam.getCacheId()))
//                    cacheExists=true;
//            }
//            if(cacheExists)
                AddTestData(cParam.getCacheId(), cParam.getItemCount(), cParam.getDataSize());
//            else
//                throw new Exception("'client.conf' not found or does not contain server information");
            
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
        }

    }

    private static boolean ValidateParameters() {
        AssemblyUsage.PrintLogo(cParam.getIsLogo());
        // Validating CacheId
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }
        return true;
    }

    private static void AddTestData(String cacheId, long itemCount, int dataSize) throws Exception {
        Cache cache;

        try {
            CacheInitParams ciParams = new CacheInitParams();
            ciParams.setMode(CacheMode.OutProc);
            cache = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(cacheId, ciParams);

            long startCount = cache.getCount();

            System.out.println("");
            System.out.println("Adding " + itemCount + " items. Size " + dataSize + " bytes. Absolute Expiration is set to 5 minutes...");

            java.util.Date startDTime = Calendar.getInstance().getTime();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 5);
            byte[] data = new byte[dataSize];
            for (long index = 0; index < itemCount; index++) {
                try {
                    cache.insert(index, data, cal.getTime(), Cache.DefaultSlidingExpiration, CacheItemPriority.Default);

                } catch (RuntimeException ex) {
                    System.out.println(ex.getMessage());
                    Thread.sleep(1000);
                }
            }
            java.util.Date endDTime = new java.util.Date();
            long endCount = cache.getCount();

            cache.dispose();

            System.out.println("");
            System.out.println("");
            System.out.println("AddTestData started at:  " + startDTime.toString());
            System.out.println("AddTestData ended at:    " + endDTime.toString());
            System.out.println("");
            System.out.println("Old cache count :      " + startCount);
            System.out.println("New cache count :      " + endCount);
            System.out.println("");
        } 
        catch (Exception e) 
        {
            System.err.println("ERROR: \"" + cacheId + "\" cache: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
