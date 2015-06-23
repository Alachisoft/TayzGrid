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
package com.alachisoft.tayzgrid.common.searchalgorithms;

import java.util.Comparator;
import java.util.List;

public class BinarySearch
{
    public static void insertItem(List _link, Object o, Comparator comp)
    {

        if (comp != null)
        {
        }
        else
        {
            throw new IllegalArgumentException("Comparator is NUll");
        }

        if (_link.size() == 0)
        {
            _link.add(o);
            return;
        }

        boolean insert = false;
        int itemNumber = _link.size() / 2;
        int less = 0;
        int greater = _link.size() - 1;

        while (!insert)
        {

            Object key = _link.get(itemNumber);
            if (comp.compare(o, key) < 0) //Less than
            {
                if ((itemNumber - less) == 1)
                {
                    greater = itemNumber = less;
                    continue;
                }
                else if (itemNumber == less)
                {
                    _link.add(itemNumber, o);
                    insert = true;
                    break;
                }

                greater = itemNumber;
                itemNumber = (itemNumber - less) / 2 + less;
            }
            else if (comp.compare(o, key) > 0) //Greater than
            {
                if ((greater - itemNumber) == 1)
                {
                    less = itemNumber = greater;
                    continue;
                }
                else if (itemNumber == greater)
                {
                    _link.add(itemNumber + 1, o);
                    insert = true;
                    break;
                }

                less = itemNumber;
                itemNumber = (greater - itemNumber) / 2 + itemNumber;
            }
            else
            {
                throw new IllegalArgumentException("Duplicate Key found");
            }
        }

    }


    /**
     * Searches item using Binary Search algorithm, returns Index of item
     * @param _link List of sorted array
     * @param findItem Item to find
     * @param comp Comparator
     * @return -1 if not found, else returns the index number
     */
    public static int searchItem(List _link, Object findItem, Comparator comp)
    {
        if (comp != null)
        {
        }
        else
        {
            throw new IllegalArgumentException("Comparator is NUll");
        }

        if (_link.size() == 0)
        {
            return -1;
        }

        boolean insert = false;
        int itemNumber = _link.size() / 2;
        int less = 0;
        int greater = _link.size() - 1;

        while (!insert)
        {

            Object key = _link.get(itemNumber);

            if (comp.compare(findItem, key) < 0) //Less than
            {
                if ((itemNumber - less) == 1)
                {
                    greater = itemNumber = less;
                    continue;
                }
                else if (itemNumber == less)
                {
                    insert = true;
                    break;
                }

                greater = itemNumber;
                itemNumber = (itemNumber - less) / 2 + less;
            }
            else if (comp.compare(findItem, key) > 0) //Greater than
            {
                if ((greater - itemNumber) == 1)
                {
                    less = itemNumber = greater;
                    continue;
                }
                else if (itemNumber == greater)
                {
                    insert = true;
                    break;
                }

                less = itemNumber;
                itemNumber = (greater - itemNumber) / 2 + itemNumber;
            }
            else
            {
                return itemNumber;
            }
        }

        //Not found
        return -1;
    }
}
