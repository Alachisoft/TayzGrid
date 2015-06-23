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
import com.alachisoft.tayzgrid.web.caching.*;
import java.util.Scanner;
import tangible.RefObject;

public class ClearCacheTool {

    private static ClearCacheParam cParam = new ClearCacheParam();
    public static void Run(String[] args) throws Exception {
        try {
            Object param = new ClearCacheParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ClearCacheParam) param;

            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }
            ClearCache(cParam.getCacheId());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
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

    private static void ClearCache(String cacheId) throws Exception {
        Cache cache;
        boolean isSuccessFull=false;
        try {
            CacheInitParams ciParams = new CacheInitParams();
            ciParams.setMode(CacheMode.OutProc);
            cache = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(cacheId, ciParams);

            try {
                if (!cParam.getIsForce()) {
                    long count = cache.getCount();
                    System.out.println(cParam.getCacheId() + " cache currently has " + count + " items. ");
                    System.out.println("Do you really want to clear it (Y or N)?");
                    Scanner scaner = new Scanner(System.in);
                    String response = scaner.nextLine();
                    if (!"Y".equals(response) && !"y".equals(response)) {
                        System.out.println("Cache not cleared.");
                        return;
                    }
                }
                cache.clear();
                isSuccessFull=true;
                if(isSuccessFull)
                    System.out.println("Cache Successfully Cleared");

            } catch (RuntimeException ex) {
                System.err.println(ex.getMessage());
                Thread.sleep(1000);
                ex.printStackTrace();
            }
        } catch (Exception e) {
            if(!isSuccessFull)
            System.out.println("Cache " + cParam.getCacheId() + " not Cleared");
            if(e.getMessage() != null)
            {
                System.err.println("ERROR: \"" + cacheId + "\" cache: " + e.getMessage());
            }
            else
                System.err.println("ERROR: \"" + cacheId + "\" cannot be Initialized " );
            e.printStackTrace();
        }
        
    }
}
