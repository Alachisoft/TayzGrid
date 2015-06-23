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
 * Internal class that helps display assembly usage information.
 */
public final class AssemblyUsage {

    private static String VERSION = "4.6.0.0";

    /**
     * Displays logo banner
     *
     * @param printlogo Specifies whether to print logo or not
     */
    public static void PrintLogo(boolean printlogo) {
        String logo = "Alachisoft (R) TayzGrid Utility - RemoveBackingSource. Version " + VERSION + "\nCopyright (C) Alachisoft 2015. All rights reserved.\n";

        if (printlogo) {
            System.out.println(logo);
            System.out.println();
        }
    }

    /**
     * Displays assembly usage information.
     */
    public static void PrintUsage() {

        String usage = "Usage: removebackingsource  [option[...]]." + "\r\n" + "\r\n"
                + " " + "\r\n" + "   "
                + " Specifies the id/name of the cache for which backing source will be removed. "
                + "\r\n" + "\r\n" + " -n --provider-name" + "\r\n" + "    Specifies the provider name."
                + "\r\n" + "\r\n" + " -R --readthru" + "\r\n" + "  "
                + "  Specifies if provided backing source is configured for ReadThru." + "\r\n"
                + "\r\n" + " -W --writethru" + "\r\n" + "    "
                + "Specifies if provided backing source is configured for WriteThru." + "\r\n" + "\r\n"
                + "Optional:" + "\r\n" + "\r\n" + " -s --server" + "\r\n" + " "
                + "   Specifies the TayzGrid server name/ip." + "\r\n" + "\r\n" + " -p --port" + "\r\n" + "   "
                + " Specifies the port on which TayzGrid server is listening." + "\r\n" + "\r\n"
                + " -G" + "\r\n" + " "
                + "   Suppresses display of the logo banner " + "\r\n" + "\r\n" + "  -h --help" + "\r\n" + " "
                + "   Displays a detailed help screen " + "\r\n" + "";

        System.out.println(usage);
    }
}
