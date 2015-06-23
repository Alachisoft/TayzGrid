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

package com.alachisoft.tayzgrid.utility.jarscanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class JarFileLoader extends URLClassLoader
{
    //-------------------------------------------------------------------
    public JarFileLoader(String... jarPaths) throws MalformedURLException
    {
        //--- Call the parent ClassLoader (for the sake of compatibility :( )
        super(new URL[] {});
        //--- Add all jars to the parent ClassLoader path ...
        this.addFile(jarPaths);
    }
    //-------------------------------------------------------------------
    /**
    * @deprecated use JarFileLoader(String... jarPaths) constructor instead.
    */
    @Deprecated
    public JarFileLoader (URL[] urls)
    {
        super (urls);
    }
    //-------------------------------------------------------------------
    public void addFile (String... jarPath) throws MalformedURLException
    {
        if (jarPath != null && jarPath.length > 0)
        {
            for (String path : jarPath)
            {
                //--- Add this url to parent ClassLoader ...
                addURL(new File(path).toURI().toURL());
            }
        }
    }
    //-------------------------------------------------------------------
}
