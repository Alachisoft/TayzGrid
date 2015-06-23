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

import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.Arrays;

public class JarFileLoader extends URLClassLoader {

    public JarFileLoader(ClassLoader parent)
    {
        //ignore urls
        //super(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs());
        super(new URL[]{},parent);
//        URL[] sysLoaderArr = ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
//        URL[] result = Arrays.copyOf(urls, urls.length+sysLoaderArr.length);
//        System.arraycopy(sysLoaderArr, 0, result, urls.length, sysLoaderArr.length);

    }

//      public JarFileLoader(URLClassLoader urlLoader) {
//        super(urlLoader.getURLs());
//    }

    
    public void addFile(String path) throws MalformedURLException {
        String urlPath = "";
        if (RuntimeUtil.getCurrentOS() == RuntimeUtil.OS.Windows) {
            urlPath = "jar:file:/" + path + "!/";
        } else {
            urlPath = "jar:file://" + path + "!/";
        }
        addURL(new URL(urlPath));
    }
}
