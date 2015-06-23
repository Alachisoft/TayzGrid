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

package com.alachisoft.tayzgrid.common;

import java.io.File;

public class DirectoryUtil
{

    /**
     * search for the specified file in the executing assembly's working folder if the file is found, then a path string is returned back. otherwise it returns null.
     *
     * @param fileName
     * @return
     */
    public static String GetFileLocalPath(String fileName)
    {
        String path = (new java.io.File(System.getProperty("user.dir")).getParent() + "\\" + fileName);
        if ((new java.io.File(path)).isFile())
        {
            return path;
        }
        return null;
    }

    /**
     * search for the specified file in NCache install directory. if the file is found then returns the path string from where the file can be loaded. otherwise it returns null.
     *
     * @param fileName
     * @return
     */
    public static String GetFileGlobalPath(String fileName, String directoryName)
    {
        String directoryPath = "";
        String filePath = "";

        tangible.RefObject<String> tempRef_directoryPath = new tangible.RefObject<String>(directoryPath);
        boolean tempVar = !SearchGlobalDirectory(directoryName, false, tempRef_directoryPath);
        directoryPath = tempRef_directoryPath.argvalue;
        if (tempVar)
        {
            return null;
        }
        File directoryPathFile = new File(directoryPath);
        filePath = new File(directoryPathFile, fileName).getPath();
        if (!(new java.io.File(filePath)).isFile())
        {
            return null;
        }
        return filePath;
    }

    public static boolean SearchLocalDirectory(String directoryName, boolean createNew, tangible.RefObject<String> path)
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
                catch (RuntimeException e)
                {
                    throw e;
                }
            }
            return false;
        }
        return true;
    }

    public static boolean SearchGlobalDirectory(String directoryName, boolean createNew, tangible.RefObject<String> path)
    {
        String ncacheInstallDirectory = AppUtil.getInstallDir();
        path.argvalue = "";
        if (ncacheInstallDirectory == null)
        {
            return false;
        }
        File ncacheInstallDirectoryFile = new File(ncacheInstallDirectory);
        path.argvalue = new File(ncacheInstallDirectoryFile, directoryName).getPath();
        if (!(new java.io.File(path.argvalue)).isDirectory())
        {
            if (createNew)
            {
                try
                {
                    (new java.io.File(path.argvalue)).mkdir();
                    return true;
                }
                catch (RuntimeException e)
                {
                    throw e;
                }
            }
            return false;
        }
        return true;
    }

    public static String getDeployedAssemblyFolder()
    {
        return Common.combinePath(AppUtil.getInstallDir(), AppUtil.DeployedAssemblyDir);
    }

    public static File createDeployAssemblyFolder(String cacheID)
    {
        return new File(Common.combinePath(getDeployedAssemblyFolder(),cacheID.toLowerCase()));
    }
}