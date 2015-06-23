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


import com.alachisoft.tayzgrid.common.searchalgorithms.BinarySearch;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SortedList<V> implements List<V>
{
    Comparator comp;
    LinkedList<V> _linkList = new LinkedList<V>();


    /**
     * Constructor
     * @param comp cannot be Null
     */
    public SortedList(Comparator comp)
    {
        if(comp == null)
            throw new NullPointerException("Comparator cannot be null");
        this.comp = comp;
    }

    //<editor-fold defaultstate="collapsed" desc="List Interface">


    @Override
    public int size()
    {
        return _linkList.size();
    }

    @Override
    public boolean isEmpty()
    {
        return _linkList.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return BinarySearch.searchItem(_linkList, o, comp) >= 0;
    }

    @Override
    public Iterator<V> iterator()
    {
        return _linkList.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return _linkList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts)
    {
        return _linkList.toArray(ts);
    }

    @Override
    public boolean add(V e)
    {
        BinarySearch.insertItem(_linkList, e, comp);
        return true;

    }

    @Override
    @Deprecated
    public boolean remove(Object o)
    {
        int pos = BinarySearch.searchItem(_linkList, o, comp);
        if(pos >= 0)
        {
            remove(pos);
            return true;
        }
        else
            return false;

    }

    @Override
    public boolean containsAll(Collection<?> clctn)
    {
        Iterator ite = clctn.iterator();
        while (ite.hasNext())
        {
            Object object = ite.next();
            if (!this.contains((V)object))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends V> clctn)
    {
        Iterator it = clctn.iterator();
        while (it.hasNext())
        {
            Object pair = it.next();
            add((V)pair);
        }
        return true;
    }

    @Override
    @Deprecated
    public boolean addAll(int i, Collection<? extends V> clctn)
    {
        throw new UnsupportedOperationException("Not possible to force sort the array");
    }

    @Override
    public boolean removeAll(Collection<?> clctn)
    {
        Iterator it = clctn.iterator();
        while (it.hasNext())
        {
            Object pair = it.next();
            remove(pair);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> clctn)
    {
        return _linkList.retainAll(clctn);
    }

    @Override
    public void clear()
    {
        _linkList.clear();;
    }

    @Override
    public V get(int i)
    {
        return _linkList.get(i);
    }

    @Override
    @Deprecated
    public V set(int i, V e)
    {
        throw new UnsupportedOperationException("Not possible to force sort the array");
    }

    @Override
    @Deprecated
    public void add(int i, V e)
    {
        throw new UnsupportedOperationException("Not possible to force sort the array");
    }

    @Override
    public V remove(int i)
    {
        return _linkList.remove(i);
    }

    @Override
    public int indexOf(Object o)
    {
        return _linkList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return BinarySearch.searchItem(_linkList, o, comp);
    }

    @Override
    public ListIterator<V> listIterator()
    {
        return _linkList.listIterator();
    }

    @Override
    public ListIterator<V> listIterator(int i)
    {
        return _linkList.listIterator(i);
    }

    @Override
    public List<V> subList(int i, int i1)
    {
        return _linkList.subList(i1, i1);
    }

    //</editor-fold>
}

