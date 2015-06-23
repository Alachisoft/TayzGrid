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

package com.alachisoft.tayzgrid.runtime.util;


public class RuntimeUtil {

    public enum OS{
        Windows,
        Linux,
        Mac,
        Solaris,
        Unknown
    }

    public static RuntimeUtil.OS getCurrentOS(){
        String osProperty = System.getProperty("os.name");

        if(osProperty != null){
            osProperty = osProperty.toLowerCase();
            if(osProperty.indexOf("win")>=0){
                return OS.Windows;
            }else if(osProperty.indexOf("nix")>=0 || osProperty.indexOf("nux")>=0 || osProperty.indexOf("aix")>=0){
                return OS.Linux;
            }else if(osProperty.indexOf("sunos")>=0){
                return OS.Solaris;
            }else if(osProperty.indexOf("mac")>=0){
                return OS.Mac;
            }
        }else{
            return OS.Unknown;
        }
        return OS.Unknown;
    }
}

