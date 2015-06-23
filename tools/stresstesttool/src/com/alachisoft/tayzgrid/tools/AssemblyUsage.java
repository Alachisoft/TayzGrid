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
            String logo = "Alachisoft (R) TayzGrid Utility - StressTestTool. Version " + VERSION +
                "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

            if (printlogo)
            {
                System.out.println(logo);
            }
        }

        static public void PrintUsage()
        {

            String usage =  "DESCRIPTION: StressTestTool allows you to quickly simulate heavy transactional load on a given cache. And, this helps you see how TayzGrid actually performs under stress in your own environment.Please watch TayzGrid performance counters in TayzGrid Manager \"statistics\" or regular JConsole."+
                            "\n\nNOTE: A test-case represents a user session or multiple get and update operations on the same cache key. Use test-case to simulate JSP sessions. When all test-case-iterations are used up, a user session becomes idle and left to expire. Each test-case-iteration consists of one or more gets and updates (JSP session simulation would use 1 get and 1 update). And test-case-iteration-delay represents a delay between each iteration and can be used to simulate JSP sessions behavior where a user clicks on a URL after 15-30 seconds delay."+
                            "\n\nUSAGE: stresstesttool cache-id [option[...]]." +
                            "\n\nARGUMENT:" +
                            "\n     cache-id" +
                            "\n         Name of the cache." +
                            "\n\nOPTION:" +
                            "\n\n     -n --item-count" +
                            "\n         How many total items you want to add. (default: infinite)" +
                            "\n\n     -i --test-case-iterations" +
                            "\n         How many iterations within a test case (default: 20)" +
                            "\n\n     -d --test-case-iteration-delay" +
                            "\n         How much delay (in seconds) between each test case iteration (default: 0)" +
                            "\n\n     -g --gets-per-iteration" +
                            "\n         How many gets within one iteration of a test case (default: 1)" +
                            "\n\n     -u --updates-per-iteration" +
                            "\n         How many updates within one iteration of a test case (default: 1)" +
                            "\n\n     -m --item-size" +
                            "\n         Specify in bytes the size of each cache item (default: 1024)" +
                            "\n\n     -e --sliding-expiration" +
                            "\n         Specify in seconds sliding expiration (default: 300; minimum: 15)" +
                            "\n\n     -t --thread-count" +
                            "\n         How many client threads (default: 1; max: 3)" +
                            "\n\n     -R --reporting-interval" +
                            "\n         Report after this many total iterations (default: 5000)" +
                            "\n\n     -G" +
                            "\n         Suppresses the startup banner and copyright message." +
                            "\n\n     -h --help" +
                            "\n         Displays a detailed help screen.";

                        System.out.println(usage);
        }

}
