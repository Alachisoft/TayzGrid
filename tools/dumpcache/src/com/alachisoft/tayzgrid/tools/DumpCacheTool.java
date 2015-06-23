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

import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheInitParams;
import com.alachisoft.tayzgrid.web.caching.CacheMode;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.util.Enumeration;
import tangible.RefObject;

final class DumpCacheTool {

    private static DumpCacheParam cParam = new DumpCacheParam();

    public static void Run(String[] args) {

        try {
            Object param = new DumpCacheParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (DumpCacheParam) param;
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
//            if(!cacheExists)
//                throw new Exception("'client.conf' not found or does not contain server information");
             CacheInitParams ciParams = new CacheInitParams();
            ciParams.setMode(CacheMode.OutProc);
            com.alachisoft.tayzgrid.web.caching.Cache cache  = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(cParam.getCacheId(), ciParams);
            cache.setExceptionsEnabled(true);

            System.out.println("Cache count: " + Long.toString(cache.getCount()));
            Enumeration keys = cache.getEnumerator();

            if (keys != null) {
                long index = 0;
                cParam.setKeyFilter(cParam.getKeyFilter().trim());
                boolean checkFilter = cParam.getKeyFilter().length() == 0 ? false : true;

                while (keys.hasMoreElements()) {
                    if ((cParam.getKeyCount() > 0) && (index >= cParam.getKeyCount())) {
                        break;
                    }

                    Cache.Entry entry = (Cache.Entry) keys.nextElement();

                    if (checkFilter == true) {
                        String tmpKey = entry.getKey().toString();

                        if (tmpKey.contains(cParam.getKeyFilter()) == true) {
                            System.out.println(tmpKey);
                        }
                    } else {
                        System.out.println(entry.getKey().toString());
                    }
                    index++;
                }//end while
            }//end if
            cache.dispose();
        }//end try block
        catch (Exception e) {
            System.err.println("");
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Validate all parameters in property string.
     */
    private static boolean ValidateParameters() {
         AssemblyUsage.PrintLogo(cParam.getIsLogo());
        // Validating CacheId
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }
       

        return true;
    }
}
