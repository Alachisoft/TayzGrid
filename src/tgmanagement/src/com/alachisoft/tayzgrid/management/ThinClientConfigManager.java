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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.XmlConfigReader;
import java.util.Iterator;

public class ThinClientConfigManager extends CacheConfigManager
{

    /**
     * intentionally hiding the base class member. the purpose is to avoid the call to the base class because every call to the base class scans for the ncache installation
     * folder and throws exception if installation folder is not found. This class 'ThinClientConfigManager' is used for thin clients where installation folder is not mandatory.
     * Default tcp channel port.
     */
    public static final int DEF_TCP_PORT = 8250;

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId A string identifier of configuration.
     */
    public static CacheServerConfig GetConfigDom(String cacheId, String filePath, boolean inProc) throws ManagementException
    {
        try
        {
            XmlConfigReader configReader = new XmlConfigReader(filePath, cacheId);
            CacheServerConfig config = configReader.GetConfigDom();

            if (config == null)
            {
                return config;
            }

            if (!inProc)
            {
                inProc = config.getInProc();
            }

            if (inProc)
            {
                boolean isAuthorize = false;

                return config;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new ManagementException(e.getMessage(), e);
        }
    }

    /**
     * Initialize a registered cache given by the ID.
     *
     * @param cacheId A string identifier of configuration.
     */
    public static java.util.ArrayList GetCacheConfig(String cacheId, String filePath, boolean inProc) throws ManagementException, Exception
    {
        try
        {
            XmlConfigReader configReader = new XmlConfigReader(filePath, cacheId);
            java.util.ArrayList propsList = configReader.getPropertiesList();
            java.util.ArrayList configsList = CacheConfig.GetConfigs(propsList, DEF_TCP_PORT);
            for (Iterator it = configsList.iterator(); it.hasNext();)
            {
                CacheConfig config = (CacheConfig) it.next();
                if (!inProc)
                {
                    inProc = config.getUseInProc();
                }
                break;
            }

            if (inProc)
            {
                return configsList;
            }
            return null;
        }
        catch (ManagementException e2)
        {
            throw e2;
        }
        catch (Exception e)
        {
            throw new ManagementException(e.getMessage(), e);
        }
    }

}
