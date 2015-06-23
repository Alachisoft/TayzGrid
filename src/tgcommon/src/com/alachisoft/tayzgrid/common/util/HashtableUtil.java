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
package com.alachisoft.tayzgrid.common.util;

import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// <summary>
    /// Provide methods to convert hashtable into a String form, and repopulating
    /// hastable from String. The conversion do not save type information and assumes
    /// that keys are of int type, while values are of String type
    /// </summary>
    public class HashtableUtil
    {
        /// <summary>
        /// Convert hastable to a String form
        /// </summary>
        /// <param name="table">Hashtable containg int key and String value</param>
        /// <returns>String representation of hashtable</returns>
        public static String ToString(HashMap table)
        {

            if (table != null)
            {
                StringBuilder toStr = new StringBuilder();
                Iterator hasTable=table.entrySet().iterator();
                Map.Entry pair;
                while(hasTable.hasNext())
                {
                    pair=(Map.Entry)hasTable.next();
                    toStr.append( pair.getKey().toString()+"$"+pair.getValue().toString()+"\r\n");
                }
                return toStr.toString();
            }
            return "";
        }

        public static String ToString(ArrayList list)
        {

            if (list != null)
            {
                StringBuilder toStr = new StringBuilder();
                for (Object entry : list)
                {
                    toStr.append(entry.toString()+"\r\n");
                }
                return toStr.toString();
            }
            return "";
        }

        /// <summary>
        /// Populate a hastable from its String representation
        /// </summary>
        /// <param name="rep">String representation of hashtable</param>
        /// <returns>Hashtable formed from String representation</returns>
        public static HashMap FromString(String rep)
        {
            if (rep != null && rep.length()>0)
            {
                HashMap table = new HashMap();
                String[] entries = rep.split("\r\n" );

                for (String entry : entries)
                {
                    String[] keyVal = entry.split("$");
                    table.put(Integer.decode(keyVal[0]), keyVal[1]);
                }
                return table;
            }
            return null;
        }
    }
