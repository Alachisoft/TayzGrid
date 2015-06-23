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

public class AssemblyUsage
{

    private static String VERSION = "4.6.0.0";
   
    public static void PrintLogo(boolean printlogo)
    {
        String logo = "Alachisoft (R) TayzGrid Utility - AddTestData. Version " + VERSION + "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";
        if (printlogo)
        {
            System.out.println(logo);
            System.out.println();
        }
    }

    /**
     * Displays assembly usage information.
     *
     * @param printlogo specifies whether to print logo or not.
     */
    public static void PrintUsage()
    {
        String usage = "\r\n" +"Description: addtestdata adds some test data to the cache" + "\r\n" + "to confirm that the cache is functioning properly. The items are added to " + "\r\n"
                + "the cache with Absolute expiration of 5 minute. " + "\r\n" + "\r\n" + "Usage: addtestdata cache-id [option[...]]." + "\r\n" + "\r\n" + "Argument:" + "\r\n" + "  cache-id"
                + "\r\n" + "    Specifies the id of the cache. " + "\r\n" + "\r\n" + "Option:" + "\r\n" + "  -C --count " + "\r\n"
                + "    Number of items to be added to the cache. By default 10 items are added " + "\r\n" + "    to the cache.    " + "\r\n" + "\r\n" + "  -S --size" + "\r\n"
                + "    Size in bytes of each item to be added to the cache. By default items of 1k " + "\r\n" + "    (1024 bytes) are added to the cache." + "\r\n" + "  " + "\r\n"
                + "  -G" + "\r\n" + "    Suppresses the startup banner and copyright message." + "\r\n" + "\r\n" + "  -h --help" + "\r\n" + "    Displays a detailed help screen "
                + "\r\n" + "";
        System.out.println(usage);
    }
}
