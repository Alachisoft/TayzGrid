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

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import java.util.ArrayList;

public class WildcardEnabledRegex
{

    private RegularExpression regex; 

    public WildcardEnabledRegex(String pattern)
    {
        regex = new RegularExpression(ConvertWildCard(pattern));
    }

    public final boolean IsMatch(String text)
    {
        return regex.matches(text);
    }

     public static String WildcardToRegex(String pattern)
    {
        return "^" + pattern.replace("\\*", ".*").replace("\\?", ".") + "$";
    }
    public static String ConvertWildCard(String pattern){
     
        StringBuilder myPattern = new StringBuilder(pattern);
        String newPattern = pattern;
        boolean esSeq = false;
        boolean change = false;
        ArrayList esChar = new ArrayList();
        int pos, newPos = -1;
        if (pattern.contains("\\"))
        {
            for (int i = 0; i < pattern.length(); i++)
            {
                pos = myPattern.indexOf("\\", newPos + 1);
                if (pos > newPos)
                {
                    esChar.add(myPattern.charAt(pos + 1));
                    myPattern.deleteCharAt(pos + 1);
                    newPos = pos;
                }
                esSeq = true;
            }
            pattern = myPattern.toString();
        }
        if (pattern.contains("%"))
        {
            newPattern = pattern.replaceAll("%", ".*");
            change = true;
        }
        if (pattern.contains("_"))
        {
            if (!change)
            {
                newPattern = pattern.replaceAll("_", ".?");
            }
            else
            {
                newPattern = newPattern.replaceAll("_", ".?");

            }
        }
        if (esSeq)
        {
            newPos = -1;
            myPattern = new StringBuilder(newPattern);
            for (int i = 0; i < esChar.size(); i++)
            {
                int pos1 = myPattern.indexOf("\\", newPos + 1);
                if (pos1 > newPos)
                {
                    myPattern.insert(pos1 + 1, esChar.get(i).toString());
                    newPos = pos1;
                }
            }
            newPattern = myPattern.toString();
        }
        return newPattern;
    }
}
