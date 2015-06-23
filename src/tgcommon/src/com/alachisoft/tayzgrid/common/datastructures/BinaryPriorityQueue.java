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

import java.util.*;

public class BinaryPriorityQueue implements IPriorityQueue, Cloneable
{

    protected java.util.ArrayList InnerList = new java.util.ArrayList();
    protected java.util.Comparator Comparer;
    boolean isSynch = false;

    //<editor-fold defaultstate="collapsed" desc="contructors">
    public BinaryPriorityQueue()
    {
        //: System.Collections.Comparrer.Default doesnt exist so using Comparer to null and include logic to call in compare of the object
        this(null);
    }

    public BinaryPriorityQueue(java.util.Comparator c)
    {
        Comparer = c;
    }

    public BinaryPriorityQueue(int C)
    {
        this(null, C);
    }

    public BinaryPriorityQueue(java.util.Comparator c, int Capacity)
    {
        Comparer = c;
        InnerList.ensureCapacity(Capacity);
    }

    protected BinaryPriorityQueue(java.util.ArrayList Core, java.util.Comparator Comp, boolean Copy)
    {
        Object tempVar = null;
        if (Copy)
        {
            tempVar = Core.clone();
            InnerList = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

        }
        else
        {
            InnerList = Core;
        }
        Comparer = Comp;
    }

//</editor-fold>
    protected final void SwitchElements(int i, int j)
    {
        Object h = InnerList.get(i);
        InnerList.set(i, InnerList.get(j));
        InnerList.set(j, h);
    }

    protected int OnCompare(int i, int j)
    {
        return Comparer.compare(InnerList.get(i), InnerList.get(j));
    }

    //<editor-fold defaultstate="collapsed" desc="public Methods">
    /**
     * Push an object onto the PQ
     *
     * @param O The new object
     * @return The index in the list where the object is _now_. This will change when objects are taken from or put onto the PQ.
     */
    public int push(Object O)
    {
        int p = InnerList.size(), p2;
        InnerList.add(O); // E[p] = O
        do
        {
            if (p == 0)
            {
                break;
            }
            p2 = (p - 1) / 2;
            if (OnCompare(p, p2) < 0)
            {
                SwitchElements(p, p2);
                p = p2;
            }
            else
            {
                break;
            }
        }
        while (true);
        return p;
    }

    /**
     * Get the smallest object and remove it.
     *
     * @return The smallest object
     */
    public Object pop()
    {
        Object result = InnerList.get(0);
        int p = 0, p1, p2, pn;
        InnerList.set(0, InnerList.get(InnerList.size() - 1));
        InnerList.remove(InnerList.size() - 1);
        do
        {
            pn = p;
            p1 = 2 * p + 1;
            p2 = 2 * p + 2;
            if (InnerList.size() > p1 && OnCompare(p, p1) > 0)
            {
                p = p1;
            }
            if (InnerList.size() > p2 && OnCompare(p, p2) > 0)
            {
                p = p2;
            }

            if (p == pn)
            {
                break;
            }
            SwitchElements(p, pn);
        }
        while (true);
        return result;
    }

    /**
     * Notify the PQ that the object at position i has changed and the PQ needs to restore order. Since you dont have access to any indexes (except by using the explicit
     * IList.this) you should not call this function without knowing exactly what you do.
     *
     * @param i The index of the changed object.
     */
    public void update(int i)
    {
        int p = i, pn;
        int p1, p2;
        do
        {
            if (p == 0)
            {
                break;
            }
            p2 = (p - 1) / 2;
            if (OnCompare(p, p2) < 0)
            {
                SwitchElements(p, p2);
                p = p2;
            }
            else
            {
                break;
            }
        }
        while (true);
        if (p < i)
        {
            return;
        }
        do
        {
            pn = p;
            p1 = 2 * p + 1;
            p2 = 2 * p + 2;
            if (InnerList.size() > p1 && OnCompare(p, p1) > 0)
            {
                p = p1;
            }
            if (InnerList.size() > p2 && OnCompare(p, p2) > 0)
            {
                p = p2;
            }

            if (p == pn)
            {
                break;
            }
            SwitchElements(p, pn);
        }
        while (true);
    }

    /**
     * Get the smallest object without removing it.
     *
     * @return The smallest object
     */
    public Object peek()
    {
        if (InnerList.size() > 0)
        {
            return InnerList.get(0);
        }
        return null;
    }

    public boolean Contains(Object value)
    {
        return InnerList.contains(value);
    }

    public void Clear()
    {
        InnerList.clear();
    }

    public int getCount()
    {
        return InnerList.size();
    }

    private java.util.Iterator GetEnumerator()
    {
        return InnerList.iterator();
    }

    public final void CopyTo(Object[] array, int index)
    {
        for (int i = 0; i < index; i++)
        {
            InnerList.add(index + i, array[i]);
        }
    }

    public final Object clone()
    {
        return new BinaryPriorityQueue(InnerList, Comparer, true);
    }

    public final boolean getIsSynchronized()
    {
        return this.isSynch;
    }

    public final Object getSyncRoot()
    {
        return this;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Explicit implementaion">
    private boolean getIsReadOnly()
    {
        return false;
    }

    private Object getItem(int index)
    {
        return InnerList.get(index);
    }

    private void setItem(int index, Object value)
    {
        InnerList.set(index, value);
        update(index);
    }

    private int Add(Object o)
    {
        return push(o);
    }

    private boolean getIsFixedSize()
    {
        return false;
    }

    public static BinaryPriorityQueue Syncronized(BinaryPriorityQueue P)
    {
        P.isSynch = true;
        return new BinaryPriorityQueue((ArrayList) Collections.synchronizedList(P.InnerList), P.Comparer, false);
    }

    public static BinaryPriorityQueue ReadOnly(BinaryPriorityQueue P)
    {
        return new BinaryPriorityQueue((ArrayList) Collections.unmodifiableCollection(P.InnerList), P.Comparer, false);
    }
    //</editor-fold>
}
