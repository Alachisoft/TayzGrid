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

import com.alachisoft.tayzgrid.management.CacheConfigManager;

public class AssemblyUsage {

    private static String VERSION = "4.6.0.0";

    /**
     * Displays logo banner
     *
     * @param printlogo Specifies whether to print logo or not
     */
    public static void PrintLogo(boolean printlogo) {
        String logo = "Alachisoft (R) TayzGrid Utility - StartCache. Version " + VERSION + "" + "\r\n" + "Copyright (C) Alachisoft 2015. All rights reserved.";

        if (printlogo) {
            System.out.println(logo);
            System.out.println();
        }
    }

    /**
     * Displays assembly usage information.
     */
    public static void PrintUsage() {

        String usage = "Usage: startcache cache-id(s) [option[...]]." + "\r\n" + "Argument:" + "\r\n" + "  cache-id(s)" + "\r\n"
                + "    Specifies one or more id(s) of caches registered on the server. " + "\r\n" + "    The cache(s) with this/these id(s) is/are started on the server." + "\r\n"
                + "\r\n" + "Option:" + "\r\n" + "  -s --server" + "\r\n" + "    Specifies a server name where the TayzGrid service is running. The default " + "\r\n"
                + "    is the local machine" + "\r\n" + "\r\n" + "  -p --port" + "\r\n" + "    Specifies the port if the server channel is not using the default port. " + "\r\n"
                + "    The default is " + CacheConfigManager.getHttpPort() + " for http and " + CacheConfigManager.getTcpPort() + " for tcp channels" + "\r\n" + "\r\n"
                + "  -G --nologo" + "\r\n" + "    Suppresses display of the logo banner " + "\r\n" + "\r\n" + "  -h --help"
                + "\r\n" + "    Displays a detailed help screen " + "\r\n" + "";
        System.out.println(usage);
    }
}
