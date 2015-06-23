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

public class AssemblyUsage {
 private static String VERSION = "4.6.0.0";
    public static void PrintLogo(boolean printlogo)
    {
        String logo = "Alachisoft (R) TayzGrid Utility - GetCacheCount. Version " + VERSION +
            "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

        if (printlogo)
        {
            System.out.println(logo);
        }
    }

    public static void PrintUsage()
    {
        String usage = "Description: This tool gives you the item count in the cache."+
                        "\n\nUsage: getcachecount cache-id [option[...]]." +
                        "\nArgument:" +
                        "\n\n     cache-id" +
                        "\n         Specifies the id of the cache." +
                        "\nOption:" +
                        "\n\n     -s --server" +
                        "\n         Specifies a server name where the TayzGrid service is running and a cache with the specified cache-id is registered. The default is the local machine." +
                        "\n\n     -p --port" +
                        "\n          Specifies the port if the server channel is not using the default port. The default is 8250 for http and 8251 for tcp channels" +
                        "\n\n     -G" +
                        "\n         Suppresses the startup banner and copyright message." +
                        "\n\n     -h --help" +
                        "\n         Displays a detailed help screen\n";

                    System.out.println(usage);
    }
}
