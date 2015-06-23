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

final class AssemblyUsage {

    private static String VERSION = "4.6.0.0";
    
    public static void PrintLogo(boolean printlogo)
    {
        String logo = "Alachisoft (R) TayzGrid Utility - DumpCache. Version " + VERSION +
            "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

        if (printlogo)
        {
            System.out.println(logo);
        }
    }

    public static void PrintUsage()
    {
        String usage = "Description: dumpcache enumerates the cache and dump the keys on console."+
                        "\n\nUsage: dumpcache cache-id [keyCount] [keyFilter]." +
                        "\nArgument:" +
                        "\n\n     cache-id" +
                        "\n         Specifies id of cache to be dumped." +
                        "\n\nOption:" +
                        "\n\n     -k --key-count Key-Count" +
                        "\n         Specifies the number of keys. The default value is 1000. " +
                        "\n\n     -F --key-filter Key-Filter" +
                        "\n         Specifies the keys that contain this substring. Bydefault it is empty. " +
                        "\n\n     -h --help" +
                        "\n         Displays a detailed help screen\n";

                    System.out.println(usage);
    }
}
