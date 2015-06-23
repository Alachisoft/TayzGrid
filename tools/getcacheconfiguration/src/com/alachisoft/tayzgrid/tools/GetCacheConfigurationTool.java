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

import com.alachisoft.tayzgrid.management.*;
import com.alachisoft.tayzgrid.common.configuration.*;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.UnknownHostException;

public final class GetCacheConfigurationTool {

    private static GetCacheConfigurationParam ccParam = new GetCacheConfigurationParam();
    private static CacheRPCService NCache;

    /**
     * Validate all parameters in property string.
     */
    private static boolean ValidateParameters() {
        AssemblyUsage.PrintLogo(ccParam.getIsLogo());
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(ccParam.getCacheId())) {
            System.err.println("Error: Config Path not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(ccParam.getServer())) {
            System.err.println("Error: Server not specified");
            return false;
        }
        return true;
    }

    private static void LogEvent(String msg) {
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {
        String failedNodes = "";
        ICacheServer cacheServer = null;

        try {
            NCache = ToolsRPCService.GetRPCService();

            GetCacheConfigurationParam param = new GetCacheConfigurationParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = (GetCacheConfigurationParam) tempRef_param.argvalue;
            ccParam = (GetCacheConfigurationParam) param;

            if (ccParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }
            if (ccParam.getPort() != -1) {
                NCache.setPort(ccParam.getPort());
            }
            if (ccParam.getServer() != null && !ccParam.getServer().equals("")) {
                NCache.setServerName(ccParam.getServer());
            }

            String _filename = null;
            String _path = null;
            if (ccParam.getPath() != null && !ccParam.getPath().equals("")) {

                java.io.File temp = new File(ccParam.getPath());

                if (temp.isFile()) {
                } else {
                    _filename = ccParam.getCacheId() + ".conf";
                    ccParam.setPath(ccParam.getPath() + "\\" + _filename);
                }
            } else {

                File temp = new File(URLDecoder.decode(Application.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));

                ccParam.setPath(temp.getParent());

                _filename = ccParam.getCacheId() + ".conf";
                ccParam.setPath(ccParam.getPath() + "/" + _filename);
            }

            if (ccParam.getServer() != null && !ccParam.getServer().equals("")) {
                NCache.setServerName(ccParam.getServer());
            }

            if (ccParam.getPort() != -1) {
                NCache.setPort(ccParam.getPort());
            }

            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

            if (cacheServer != null) {
                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(ccParam.getCacheId());
                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }
                serverConfig.setCacheDeployment(null);
                StringBuilder xml = new StringBuilder();
                java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.CacheServerConfig> configurations = new java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.CacheServerConfig>();
                configurations.add(serverConfig);
                ConfigurationBuilder builder = new ConfigurationBuilder(configurations.toArray(new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[0]));
                builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
                xml.append(builder.GetXmlString());

                WriteXmlToFile(xml.toString());
                System.out.println("Config File Saved On : " + ccParam.getPath());
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        } finally {
            
            if(NCache!=null) {
                NCache.dispose();
            }
            if(cacheServer!=null) {
                cacheServer.dispose();
            }
        }
    }

    private static void WriteXmlToFile(String xml) throws ManagementException, FileNotFoundException, IOException {
        if (ccParam.getPath().length() == 0) {
            throw new ManagementException("Can not locate Path for writing config.");
        }
        File fs = null;
        FileOutputStream sw = null;

        try {
            fs = new File(ccParam.getPath());
            sw = new FileOutputStream(fs);

            sw.write(xml.getBytes());
            sw.flush();
        } catch (RuntimeException e) {
            throw new ManagementException(e.getMessage(), e);
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (RuntimeException e) {
                }
                sw = null;
            }
            if (fs != null) {
                try {
                } catch (RuntimeException e2) {
                }
                fs = null;
            }
        }
    }
}
