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

import com.alachisoft.tayzgrid.common.EncryptionUtil;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheRPCService;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;
import java.util.Iterator;
import tangible.RefObject;

public class StopCacheTool {

    /**
     * TayzGrid service controller.
     */
    private static CacheRPCService NCache = null;
    private static StopCacheParam cParam = new StopCacheParam();

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException {
        
        try {
            NCache = ToolsRPCService.GetRPCService();
            Object param = new StopCacheParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            boolean isSuccessfull=false;
            cParam = (StopCacheParam) param;

            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }
            
            if (!ValidateParameters()) {
                return;
            }
            
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }
            
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }
            byte[] userId = null;
            byte[] paswd = null;
                    
            ICacheServer m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            if (m != null) 
            {
                
                for (Iterator it = cParam.getCacheIds().iterator(); it.hasNext();)
                {
                    String cache = (String) it.next();
                    
                    try {
                        
                        System.out.println("Stopping cache '" + cache + "' on " + NCache.getServerName() + ":" + NCache.getPort() + "");
                        m.StopCache(cache, null, userId, paswd,false);
                        isSuccessfull=true;
                        if(isSuccessfull)
                        {
                            System.out.println(cache + " Stopped  on " + NCache.getServerName() + ":" + NCache.getPort() + "");
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.println("Failed to stop '" + cache + "'. Error: " + e.getMessage() + "");
                    }
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e)
        {
            System.err.println("Error : " + e.getMessage() );
        }
        finally
        {
            if(NCache!=null)NCache.dispose();
        }
    }

    private static boolean ValidateParameters() {

        AssemblyUsage.PrintLogo(cParam.getIsLogo());

        if (cParam.getCacheIds().isEmpty()) {
            System.out.println("Error: cache-id not specified");
            return false;
        }

        return true;
    }
}
