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

package com.alachisoft.tayzgrid.caching.queries.filters;

public class RuntimeValue implements IGenerator, java.lang.Comparable
{
    @Override
    public final int compareTo(Object obj)
    {
        if (obj instanceof RuntimeValue)
        {
            return 0;
        }
        return -1;
    }

    @Override
    public final Object Evaluate()
    {
        return null;
    }

    @Override
    public final Object Evaluate(String paramName, java.util.Map vales)
    {
        Object retVal = null;
        java.util.ArrayList list = (java.util.ArrayList) ((vales.get(paramName) instanceof java.util.ArrayList) ? vales.get(paramName) : null);
        if (list != null)
        {
            retVal = list.get(0);
            list.remove(0);
        }
        else
        {
            if (vales.containsKey(paramName))
            {
                retVal = vales.get(paramName);
            }
            else
            {
                throw new IllegalArgumentException("Provided data contains no field or property named " + paramName);
            }
        }

        return retVal;
    }
}
