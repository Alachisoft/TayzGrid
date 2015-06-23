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

public class AssemblyUsage
{
 private static String VERSION = "4.6.0.0";
    
    public static void PrintLogo(boolean printlogo)
    {
        String logo = "Alachisoft (R) TayzGrid Utility - ListCaches. Version " + VERSION +
            "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";
        if (printlogo)
        {
            System.out.println(logo);
            //System.out.println();
        }
    }

    /**
     * Displays assembly usage information.
     *
     * @param printlogo speicfies whether to print logo or not.
     */
    public static void PrintUsage()
    {
        String usage = "Usage: listcaches [option[...]]." + "\r\n" + "\r\n" + "Option:" + "\r\n" + "  -a --detail" + "\r\n"
                + "    Displays detailed information about the cache(s) registered on " + "\r\n" + "    the	server " + "\r\n" + "\r\n" + "  -s --server" + "\r\n"
                + "    Specifies a server name where the TayzGrid service is running. The default " + "\r\n" + "    is the local machine" + "\r\n" + "\r\n" + "  -p --port" + "\r\n"
                + "    Specifies the port if the server channel is not using the default port. " + "\r\n" + "    The default port for the channel is "
                + CacheConfigManager.getTcpPort() + "." + "\r\n" + "\r\n" + "  -G" + "\r\n" + "    Suppresses the startup banner and copyright message." + "\r\n" + "\r\n"
                + "  -h --help" + "\r\n" + "    Displays a detailed help screen " + "\r\n" + "";
//                "  -x --dp-xml" + "\r\n"
//                + "    Displays configuration as part of detailed information in xml format. " + "\r\n" + "    This parameter affects only when -d is also specified." + "\r\n"
//                + "\r\n" + "  -z --dp-string" + "\r\n" + "    Displays configuration as part of detailed information in property-" + "\r\n"
//                + "    string format. This parameter affects only when -d is also specified." + "\r\n" + "\r\n" 
        System.out.println(usage);
    }
}
