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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class SortedMap<K,V> implements Map<K, V>
{
    //Maintains key order
    private LinkedList<K> _linkList = new LinkedList<K>();

    //HashMap for fastest fetch
    private HashMap<K,List<V>> _hashMap;

    Comparator<K> comp;
    public SortedMap(Comparator<K> comp)
    {
        this.comp = comp;
        _hashMap = new HashMap<K, List<V>>();
    }


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
    public boolean containsKey(Object o)
    {
        return _hashMap.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o)
    {
        return _hashMap.containsValue(o);
    }

    @Override
    @Deprecated
    public V get(Object o)
    {
        throw new UnsupportedOperationException();
    }
    public List<V> getValue(Object o)
    {
        return _hashMap.get(o);
    }
    @Override
    @Deprecated
    public V put(K k, V v)
    {
       throw new UnsupportedOperationException();
    }
     public List<V> putValue(K k, V v)
    {
        List<V> valueList = new ArrayList<V>();
        sortKey(k);
        if (_hashMap.containsKey(k))
        {
            valueList=_hashMap.get(k);
            valueList.add(v);
            return _hashMap.put(k, valueList);
        }
        else
        {
            valueList.add(v);
            return _hashMap.put(k, valueList);
        }
    }

    @Override
    @Deprecated
    public V remove(Object o)
    {
        throw new UnsupportedOperationException();
    }
    public List<V> removeVal(Object o)
    {
        _linkList.remove(o);
        return _hashMap.remove(o);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
    {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K,V> pair = (Map.Entry<K,V>) it.next();
            putValue(pair.getKey(), pair.getValue());
        }
    }

    @Override
    public void clear()
    {
        _linkList.clear();
        _hashMap.clear();
    }

    @Override
    public Set<K> keySet()
    {
        LinkedHashSet<K> link = new LinkedHashSet<K>(_linkList);
        return link;
    }

    @Override
    public Collection<V> values()
    {
        ArrayList<V> arr = new ArrayList<V>();
        for (int i = 0; i < _linkList.size(); i++)
        {
            if (_hashMap.get(_linkList.get(i)) != null)
            {
                List<V> intList=_hashMap.get(_linkList.get(i));
                for (int j = 0; j < intList.size(); j++)
                {
                    arr.add(intList.get(j));
                }
            }
        }
        return arr;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        LinkedHashSet<Map.Entry<K,V>> link = new LinkedHashSet<Map.Entry<K, V>>();

        for (int i = 0; i < _linkList.size(); i++)
        {
            HashMap.SimpleEntry simple = new AbstractMap.SimpleEntry(_linkList.get(i), _hashMap.get(_linkList.get(i)));
            link.add(simple);
        }
        return link;
    }


    private void sortKey(Object o)
    {

        BinarySearch.insertItem(_linkList, o, comp);

    }

    public V getByIndex(int index)
    {
         if(index >= size())
            throw new IndexOutOfBoundsException();
        int currentIndex = 0;
        V value = null;
        ArrayList tempList = new ArrayList();
        Iterator ie = _linkList.iterator();
        while(ie.hasNext())
        {
            Object tmpValue = ie.next();
            if(!tempList.contains(tmpValue))
                tempList.add(tmpValue);
        }
        ie = tempList.iterator();
        while(ie.hasNext())
        {
            List<V> list = _hashMap.get(ie.next());
            
            if(list != null && list.size()>1){
                for(int j =0; j< list.size(); j++ )
                {
                    value = list.get(j);
                    if(currentIndex == index)
                        return value;
                    currentIndex++;
                }
            }else{
                if(list != null && list.size() ==1)
                    value = list.get(0);
                 if(currentIndex == index)
                    return value;
                 
                 currentIndex++;
            }
            
           
        }
        
        return null;
        
        
        
    }

}
