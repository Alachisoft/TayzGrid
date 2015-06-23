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
		String logo = "Alachisoft (R) TayzGrid Utility - CreateCache. Version " + VERSION + "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

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

		String usage = "Usage: createcache [option[...]]." + "\r\n" + "\r\n" + "" + "\r\n" + " "
                        + "  Specifies the id/name of the cache for which cache will be registered. " + "\r\n" + "\r\n"
                        + "  -s --server" + "\r\n" + "    Specifies the TayzGrid server names/ips where Cache should be "
                        + "configured, seperated by commas e.g. 120.168.98.10,120.168.98.9" + "\r\n" + "\r\n" + "  -S --cache-size" + "\r\n" 
                        + "     Specifies the size(MB) of the cache to be " + "created. "+ "\r\n" + "\r\n"
                        + "For"+ " Simple Case: Cache will be created by input and default configuration settings."  
                         + "\r\n" + "\r\n" + "  -t --topology " + "\r\n" + "     Specifies the topology "
                        + "in case of clustered cache. Possible values are" + "\r\n" + "     i.   local-cache " + "\r\n" 
                        + "     ii.  replicated" + "\r\n" + "     iii.  partitioned"
                        + "\r\n"  + "\r\n" +
                        "\r\n" + "For Advance Case: In this case all configuration related settings will be taken from"
                        + " specified configuration file." + "\r\n" + "\r\n" + " -T --path" + "\r\n" + "    Specifies the"
                        + " path of the cache source config which will be configured. " + "\r\n" + "\r\n" + "\r\n" + 
                        "Optional:" + "\r\n" + "\r\n" + " For Simple case:" + "\r\n" + "\r\n" + "  -y --evict-policy " +
                        "\r\n" + "     Specifies the eviction policy for cache items. Cached items will be " +"\r\n" + "\r\n" +
                        "     cleaned from the cache according to the specified policy if the cache" + "\r\n" + "    "
                        + " reaches its limit. Possible values are" + "\r\n" + "     i.   Priority  " + "\r\n" + "  "
                        + "   ii.  lfu" + "\r\n" + "     iii. lru (default)" + "\r\n" + "\r\n" +  "  -c --clientport " +
                        "\r\n" + "     Specifies the client port of cache " + "\r\n" + "\r\n"  + "  -r --range " +
                        "\r\n" + "     Specifies the port range for the cluster "+ "    " + "\r\n" + "\r\n" + "  -I --inproc " +
                        "\r\n" + "     Specifies the isolation level  "+ "    " + "\r\n" + "\r\n" + "  -o --ratio " 
                        + "\r\n" + "     Specifies the eviction ratio(Percentage) for cache items. Cached items will be "
                        + "" 
                        + "\r\n" + "     cleaned from the cache according to the specified ratio if the cache" + "\r\n" 
                        + "     reaches its limit. Default value is 5 (percent)" + "\r\n" + "  " + "\r\n" + " "
                        + " -i -interval " + "\r\n" + "     Specifies the time interval(seconds) after which cache"
                        + " cleanup is called." + "\r\n" + "     Default clean-interval is 15 (seconds)" + "\r\n" + 
                        "\r\n" + "\r\n" + "  -d --def-priority " + "\r\n" + "    "
                        + " Specifies the default priority in case of priority based eviction policy is selected." 
                        + "\r\n" + "     Possible values are" + "\r\n" + "     i.   high" + "\r\n" + "   "
                        + "  ii.  above-normal" + "\r\n" + "     iii. normal (default)" + "\r\n" + "   "
                        + "  iv.  below-normal" + "\r\n" + "     v.   low" + "\r\n" + "\r\n" + " For Both cases:" 
                        + "\r\n" + "\r\n" + " -p --port" + "\r\n" + "  "
                        + "  Specifies the port on which TayzGrid server is listening." + "\r\n" + "\r\n" + " "
                        + " -G --nologo" + "\r\n" + "    Suppresses display of the logo banner " + "\r\n" + "\r\n" 
                        + "  -h --help" + "\r\n" + "   "
                        + " Displays a detailed help screen " + "\r\n" + "";

                
		System.out.println(usage);
	}
}
