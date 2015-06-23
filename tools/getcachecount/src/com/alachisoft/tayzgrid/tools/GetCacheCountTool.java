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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import tangible.RefObject;

final class GetCacheCountTool {

    private static GetCacheCountParam cParam = new GetCacheCountParam();
public static boolean IsValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
    static public void Run(String[] args) throws Exception {
        Cache cache = null;
        
        try {
            
            Object param = new GetCacheCountParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (GetCacheCountParam) param;

            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }
            
            if(!ValidateParameters())
            {
            return;
            }
            
            if(cParam.getPort()==-1)
            {
            cParam.setPort(0);
            }

            System.out.println("Initializing cache : " + cParam.getCacheId());
            System.out.println(" ");
            CacheInitParams cInit = new CacheInitParams();
            cInit.setServer(cParam.getServer());
            cInit.setPort(cParam.getPort());
            CacheInitParams ciParams = new CacheInitParams();
            ciParams.setMode(CacheMode.OutProc);
            cache = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(cParam.getCacheId(), ciParams);
            
            System.out.println("Cache item count: " + cache.getCount());
            cache.dispose();

        } catch (Exception e) {
            System.err.println("Unable to Initialize cache '" + cParam.getCacheId() + "'");
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
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
}
