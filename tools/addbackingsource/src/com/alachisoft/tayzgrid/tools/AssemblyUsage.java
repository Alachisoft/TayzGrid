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

/** 
 Internal class that helps display assembly usage information.
*/
public final class AssemblyUsage
{
    
    private static String VERSION = "4.6.0.0";
	/** 
	 Displays logo banner
	 
	 @param printlogo Specifies whether to print logo or not
	*/
	public static void PrintLogo(boolean printlogo)
	{
		String logo = "Alachisoft (R) TayzGrid Utility - AddBackingSource . Version " + VERSION + "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

        if (printlogo)
        {
            System.out.println(logo);
            System.out.println();
        }
	}

	/** 
	 Displays assembly usage information.
	*/

	public static void PrintUsage()
	{

		String usage = "Usage: addbackingsource  [option[...]]." + 
                        "\r\n" + "\r\n" + " " + "\r\n" + "  "
                        + "  Specifies the id/name of the cache for which backing source will be configured. "
                        + "\r\n" + "\r\n" + " -a --assembly-path" + "\r\n" + "   "
                        + " Specifies the path of the assembly which will be configured as a backing source. "
                        + "\r\n" + "\r\n" + " -c --class" + "\r\n" + "   "
                        + " Specifies the class from the backing source assembly which implements ReadThru/WriteThru."
                        + " " + "\r\n" + "\r\n" + " -n --provider-name" + "\r\n" + " "
                        + "   Specifies the provider name." + "\r\n" + "\r\n" + " -r --readthru" + "\r\n" + 
                        "    Specifies if provided backing source is configured for ReadThru." + "\r\n" + "\r\n" 
                        + " -w --writethru" + "\r\n" + "    Specifies if provided backing source is configured for WriteThru." 
                        + "\r\n" + "\r\n" + "Optional:" + "\r\n" +  "\r\n" + " -b --IsBatching" + "\r\n" + "    "
                        + "Specifies that whether you wan to enable batching or not." + "\r\n" +" -od --Operation-delay" + "\r\n" + "    "
                        + "Specify the time that cache suspend each operation write on datasource." +  "\r\n" + " -bi --Batch-interval" + "\r\n" + "    "
                        + "Specifies periodic interval for opearion expiration." +"\r\n"+ " -ops --Operation-per-second" + "\r\n" + "    "
                        + "Specifies the rate at awhich cache writes the updates to database." + "\r\n"  +" -oql --Operation-Queue-Limit" + "\r\n" + "    "
                        + "Specifies maximum opearion count to be requeued in case of datasource write operation failure." +"\r\n"+" -oer --Operation-Eviction-Ratio" + "\r\n" + "    "
                        + "Specifies failed opearions eviction ratio." +"\r\n"+ " -s --server" + "\r\n" + "    "
                        + "Specifies the TayzGrid server name/ip." + "\r\n" + "\r\n" + " -p --port" + "\r\n" + "  "
                        + "  Specifies the port on which TayzGrid server is listening."
                        + "\r\n" + "\r\n" + " -l --parameter-list" + "\r\n" + "  "
                        + "  Specifies the list of the parameters passed to the backing source provider "

                        + "($ seperated) e.g. key1=value1$key2=value2$...." + "\r\n" + "\r\n"
                        + " -t --default" + "\r\n" + "   "

                        + " Specifies the default provider in case of multiple providers."
                        + "\r\n" + "\r\n" + " -d --no-depoly" + "\r\n" + "    Specify if no assembly should be deployed." +
                        "\r\n" + "\r\n" + " -D --dep-asm-path" + "\r\n" + "    Specifies the dependant assembly folder/path"

                       + "\r\n" + "\r\n" + " -G" + "\r\n" + " "
                        + "   Suppresses display of the logo banner " + "\r\n" + "\r\n" + " -lo --loaderOnly" +
                        "\r\n" + "  " + "  Specifies if the provider needs to be configured as a loader only."
                        + "\r\n" + "\r\n" +
                        "  -h --help" + "\r\n" + "    "

                        + "Displays a detailed help screen " + "\r\n" + "";

		System.out.println(usage);
	}
}
