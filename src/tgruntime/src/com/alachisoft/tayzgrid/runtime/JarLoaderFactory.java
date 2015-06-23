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

package com.alachisoft.tayzgrid.runtime;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 *
 * @author 
 */

public final class JarLoaderFactory
{
    //make sure no one else instantiates.
    private JarLoaderFactory()
    {
    }
    
    //private static JarLoaderFactory instance = null;
    private static HashMap<String,JarFileLoader> _loaderStore=new HashMap<String, JarFileLoader>();
    private static File _basePath =new File("C:\\Program Files\\TayzGrid\\deploy\\");
    
    private static Object _PathLock=new Object();
    private static Object _storeLock=new Object();
    
    public static void setPath(File value)
    {
        synchronized(_PathLock)
        {
            _basePath = value;
        }
    }
    
    public final static JarFileLoader getLoader(String cacheContext) throws MalformedURLException
    {
        if(cacheContext ==null || cacheContext.isEmpty())
        {return null;}
        
        synchronized (_storeLock)
        {
//        if(instance==null)
//        {instance = new JarLoaderFactory();}
            if (_loaderStore.containsKey(cacheContext))
            {
                return _loaderStore.get(cacheContext);
            } else
            {
                synchronized (_PathLock)
                {
                    JarFileLoader jfl = createLoader(new File(_basePath, cacheContext));
                    if(jfl==null)
                    {return null;}
                    _loaderStore.put(cacheContext, jfl);
                    return jfl;
                }
            }
        }
    }
    
    private final static JarFileLoader createLoader(java.io.File deployedFolder) throws MalformedURLException
    {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if(contextLoader ==null)
        {return null;}
        
        JarFileLoader cl = new JarFileLoader(contextLoader);
        
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
            cl.addFile(path);
        }
        return cl;
    }
}
