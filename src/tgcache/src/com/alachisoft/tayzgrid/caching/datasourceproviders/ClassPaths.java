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

package com.alachisoft.tayzgrid.caching.datasourceproviders;

import com.alachisoft.tayzgrid.caching.cacheloader.JarFileLoader;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * Loads all deployed jars in the given directory
 *
 * @author  
 */
public class ClassPaths
{

    public static JarFileLoader addPath(java.io.File deployedFolder) throws MalformedURLException
    {
        return addPath(deployedFolder, null);
    }

    public static JarFileLoader addPath(java.io.File deployedFolder, ILogger logger) throws MalformedURLException
    {
        JarFileLoader cl = new JarFileLoader(new URL[]
                {
                });
        File[] deployedJars = deployedFolder.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".jar");
            }
        });
        if(deployedJars== null) throw new MalformedURLException("Unable to find deployed jar files for cache loader");
        for (File jar : deployedJars)
        {
            String path = jar.getPath();
//            if (logger != null)
//            {
//                logger.CriticalInfo(path);
//            }
            cl.addFile(path);
        }
        return cl;
    }
}
