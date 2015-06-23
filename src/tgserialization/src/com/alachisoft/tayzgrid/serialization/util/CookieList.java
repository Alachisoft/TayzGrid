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


package com.alachisoft.tayzgrid.serialization.util;

import java.util.ArrayList;

public class CookieList
        extends ArrayList
{

    @Override
    public int indexOf(Object o)
    {

        if (o == null)
        {
            for (int i = 0; i < size(); i++)
            {
                if (get(i) == null)
                {
                    return i;
                }
            }
        }
        else
        {
            for (int i = 0; i < size(); i++)
            {
                if (o == (get(i)))
                {
                    return i;
                }
            }
        }
        return -1;
    }
}
