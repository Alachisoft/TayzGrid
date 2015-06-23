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

package com.alachisoft.tayzgrid.common.datastructures;

public class VirtualIndex implements java.lang.Comparable
{

    private static final int maxSize = 79 * 1024;
    private int x, y;

    public VirtualIndex()
    {
    }

    public VirtualIndex(int index)
    {
        IncrementBy(index);
    }

    public final void Increment()
    {
        x++;
        if (x == maxSize)
        {
            x = 0;
            y++;
        }
    }

    public final void IncrementBy(int count)
    {
        int number = (this.y * maxSize) + this.x + count;
        this.x = number % maxSize;
        this.y = number / maxSize;
    }

    public final int getXIndex()
    {
        return x;
    }

    public final int getYIndex()
    {
        return y;
    }

    public final int getIndexValue()
    {
        return (y * maxSize) + x;
    }

    public final VirtualIndex clone()
    {
        VirtualIndex clone = new VirtualIndex();
        clone.x = x;
        clone.y = y;
        return clone;
    }

    //<editor-fold defaultstate="collapsed" desc="IComparable">
    public final int compareTo(Object obj)
    {
        VirtualIndex other = null;
        if (obj instanceof VirtualIndex)
        {
            other = (VirtualIndex) ((obj instanceof VirtualIndex) ? obj : null);
        }
        else if (obj instanceof Integer)
        {
            other = new VirtualIndex((Integer) obj);
        }
        else
        {
            return -1;
        }

        if (other.getIndexValue() == getIndexValue())
        {
            return 0;
        }
        else if (other.getIndexValue() > getIndexValue())
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
    //</editor-fold>
}
