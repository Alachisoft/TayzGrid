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
        String logo = "Alachisoft (R) TayzGrid Utility - Verify TayzGrid License. Version " + VERSION +
            "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

        if (printlogo)
        {
            System.out.println(logo);
        }
    }

    public static void PrintUsage()
    {
        String usage = "Description: This tool verify the TayzGrid License. For registered version it will display the "+
                        "registration details. In evaluation mode it will display the remaining day if evaluation is still valid else " +
                        "give the expiration message." +
                        "\n\nUsage: verifyncachelicense [option[...]]." +
                        "\nOption:" +
                        "\n\n     -G" +
                        "\n         Suppresses the startup banner and copyright message." +
                        "\n\n     -h --help" +
                        "\n         Displays a detailed help screen\n";

                    System.out.println(usage);
    }
}

