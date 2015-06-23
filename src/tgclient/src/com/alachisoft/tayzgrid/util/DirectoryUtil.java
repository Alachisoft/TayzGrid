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

package com.alachisoft.tayzgrid.util;

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.management.ThinClientConfigManager;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.io.File;

public class DirectoryUtil
{

    private static String _installDir = null;

    /**
     * search for the specified file in the executing assembly's working folder if the file is found, then a path string is returned back. otherwise it returns null.
     *
     * @param fileName
     * @return
     */
    public static String GetFileLocalPath(String fileName)
    {
        try
        {
            String path = getConfigPath(fileName);
            if ((new java.io.File(path)).isFile())
            {
                return path;
            }
        }
        catch (Exception e)
        {
        }
        return null;
    }

    /**
     * search for the specified file in TayzGrid install directory. if the file is found then returns the path string from where the file can be loaded. otherwise it returns null.
     *
     * @param fileName
     * @return
     */
    public static String GetFileGlobalPath(String fileName, String directoryName)
    {
        String directoryPath = "";
        String filePath = "";
        tangible.RefObject<String> tempRef_directoryPath = new tangible.RefObject<String>(directoryPath);
        try
        {
            boolean tempVar = !SearchGlobalDirectory(directoryName, false, directoryPath);

            directoryPath = tempRef_directoryPath.argvalue;
            if (tempVar)
            {
                return null;
            }
        }
        catch (Exception e)
        {
        }
        filePath = new File(directoryPath, fileName).getPath();//Path.Combine(directoryPath, fileName);
        if (!(new java.io.File(filePath)).isFile())
        {
            return null;
        }
        return filePath;

    }

    public static java.util.ArrayList GetCacheConfig(String cacheId, boolean inproc)
    {
        String filePath = GetFileLocalPath("cache.conf");
        java.util.ArrayList configurationList = null;
        if (filePath != null)
        {
            try
            {
                configurationList = ThinClientConfigManager.GetCacheConfig(cacheId, filePath, inproc);
            }
            catch (Exception exception)
            {
            }
        }

        return configurationList;
    }

    public static CacheServerConfig GetCacheDom(String cacheId, boolean inproc) throws ManagementException
    {
        String filePath = GetFileLocalPath("cache.conf");
        CacheServerConfig dom = null;
        if (filePath != null)
        {
            dom = ThinClientConfigManager.GetConfigDom(cacheId, filePath, inproc);
        }
        return dom;
    }

    public static boolean SearchLocalDirectory(String directoryName, boolean createNew, tangible.RefObject<String> path) throws Exception
    {
        path.argvalue = (new java.io.File(System.getProperty("user.dir")).getParent());
        if (!(new java.io.File(path.argvalue)).isDirectory())
        {
            if (createNew)
            {
                try
                {
                    (new java.io.File(path.argvalue)).mkdir();
                    return true;
                }
                catch (Exception e)
                {
                    throw e;
                }
            }
            return false;
        }
        return true;
    }

    public static boolean SearchGlobalDirectory(String directoryName, boolean createNew, String path) throws Exception
    {
        String ncacheInstallDirectory = AppUtil.getInstallDir();
        path = "";

        if (ncacheInstallDirectory == null)
        {
            return false;
        }

        path = new File(ncacheInstallDirectory, directoryName).getPath();//Path.Combine(ncacheInstallDirectory, directoryName);
        if (!(new java.io.File(path)).isDirectory())
        {
            if (createNew)
            {
                try
                {
                    (new java.io.File(path)).mkdir();
                    return true;
                }
                catch (Exception e2)
                {
                    throw e2;
                }
            }
            return false;
        }
        return true;
    }

    public static String getConfigPath(String filename) throws Exception
    {
        String separator = System.getProperty("file.separator");
        String path = "";

        //Making SetConfigPath property available on Windows.
        path = TayzGrid.getConfigPath();
        if (path != null && path.equalsIgnoreCase("") == false)
        {
            return path.concat(separator + filename);
        }

        if (System.getProperty("os.name").toLowerCase().startsWith("win"))
        {
            //<editor-fold defaultstate="collapsed" desc=" Library Path for Windows ">
            //get current execution path
            path = System.getProperty("user.dir");
            if (path != null)
            {
                if (!path.endsWith(separator))
                {
                    path = path.concat(separator);
                }
                path = path.concat(filename);

                if (fileExists(path))
                {
                    return path;
                }
            }

            //get ncache installation path
            path = getInstallDir();
            if (path != null)
            {
                if (path != null)
                {
                    if (!path.endsWith(separator))
                    {
                        path = path.concat(separator);
                    }

                    path = path.concat("config" + separator + filename);

                    if (fileExists(path))
                    {
                        return path;
                    }
                }
            }

            //</editor-fold>
        }
        else
        {
            //<editor-fold defaultstate="collapsed" desc=" Library Path for linux ">
            path = TayzGrid.getConfigPath();
            if (path != null && path.equalsIgnoreCase("") == false)
            {
                return path.concat(separator + filename);
            }
            else
            {
                path = ServicePropValues.getTGHome();
                if (path != null && path.equalsIgnoreCase("") == false)
                {
                    path = path.concat(separator + "config");
                }
                else
                {
                    path = System.getenv("NCACHE_MEMCACHED_ROOT");
                    if (path != null && path.equalsIgnoreCase("") == false)
                    {
                        path = path.concat(separator + "config");
                    }
                    //</editor-fold>
                }
                if (path == null || path.equalsIgnoreCase("") == true)
                {
                    path = "/opt/tayzgrid/config/" + filename;
                    return path;
                }
            }
        }
        if (path == null)
        {
            throw new Exception("Unable to find " + filename + "; please reset Enviorment variables");
        }
        return path.concat(separator + filename);
    }

    /**
     * Determine whether file exists at specified path
     *
     * @param path File path to be checked
     * @return True if file exists, false otherwise
     */
    public static boolean fileExists(String path)
    {
        File check = new File(path);
        try
        {
            return check.exists();
        }
        catch (SecurityException se)
        {
        }
        return false;
    }

    /**
     * Read windows registry and return ncache installation directory
     *
     * @return Ncache installation directory path
     */
    public static String getInstallDir()
    {
        if (_installDir == null)
        {
            _installDir = ServicePropValues.getTGHome();
        }
        return _installDir;
    }

    public static String GetBaseFilePath(String fileName)
        {
            //if file present in local directory
            String path = System.getProperty("user.dir") + fileName;
            File f = new File(path);
             if(f.exists()) { return path; }

            //check global directory of ncache to find file.
            String directoryPath = "";
            String filePath = "";
            try {
            if (!SearchGlobalDirectory("config", false, directoryPath))
            {
                return null;
            }
            }
            catch(Exception e){return null;}
            filePath = new File(directoryPath, fileName).getPath();
            File f1 = new File(path);
            if(f1.exists()) { return filePath; }

            return null;


        }
}
