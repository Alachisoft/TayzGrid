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

import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheRPCService;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;
import java.util.Iterator;
import tangible.RefObject;

public class StartCacheTool {

    private static CacheRPCService NCache;
    private static StartCacheParam cParam = new StartCacheParam();
    private static String _partId = ""; 
    /**
     * The main entry point for the tool.
     *
     * @param args
     * @throws UnknownHostException
     * @throws Exception
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {
        boolean isSuccessfull=false;
        try {
            NCache = ToolsRPCService.GetRPCService();
            
            Object param = new StartCacheParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (StartCacheParam) param;
            
            if(cParam.getIsUsage())
            {
            AssemblyUsage.PrintUsage();
            return;
            }
            if(!ValidateParameters())
            {
            return ;
            }
            
            if(cParam.getPort()!=-1)
            {
            NCache.setPort(cParam.getPort());
            }
            if(cParam.getServer()!=null&&!cParam.getServer().equals(""))                
            {
                NCache.setServerName(cParam.getServer());
            }
            CacheServerConfig config = null;
            ICacheServer m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            if (m != null) {
                
                for (Iterator it = cParam.getCacheIds().iterator(); it.hasNext();) {
                    String cache = (String) it.next();
                    try {
                        config = m.GetCacheConfiguration(cache);
                        if(config != null && config.getInProc()){
                            throw new Exception("InProc caches cannot be started explicitly.");
                        }
                        
                        System.out.println("Starting cache '" + cache + "' on " + NCache.getServerName() + ":" + NCache.getPort() + ".");
                        m.StartCache(cache, _partId, com.alachisoft.tayzgrid.common.EncryptionUtil.Encrypt(""), com.alachisoft.tayzgrid.common.EncryptionUtil.Encrypt(""));
                        isSuccessfull=true;
                        if(isSuccessfull)
                        System.out.println(cache + " started on " + NCache.getServerName() + ":" + NCache.getPort() + ".");
                        
                    } catch (SecurityException e) {
                        System.err.println("Failed to start '" + cache + "'. Error: " + e.getMessage() + "");
                        isSuccessfull=false;
                    } catch (RuntimeException e) {
                        System.err.println("Failed to start '" + cache + "'. Error: " + e.getMessage() + "");
                    } catch (Exception e) {
                        System.err.println("Failed to start '" + cache + "'. Error: " + e.getMessage() + "");
                    }
                }
                
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        } finally {
            
            NCache.dispose();
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
