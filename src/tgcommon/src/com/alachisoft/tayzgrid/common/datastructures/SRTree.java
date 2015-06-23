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

import java.util.Iterator;
import java.util.Map;

public class SRTree implements Cloneable
{

    private java.util.ArrayList _leftList;
    private java.util.ArrayList _rightList;

    public SRTree()
    {
        _leftList = new java.util.ArrayList();
        _rightList = new java.util.ArrayList();
    }

    public final java.util.ArrayList getLeftList()
    {
        return _leftList;
    }

    public final void setLeftList(java.util.ArrayList value)
    {
        if (value != null)
        {
            _leftList = value;
        }
    }

    public final java.util.ArrayList getRightList()
    {
        return _rightList;
    }

    public final void setRightList(java.util.ArrayList value)
    {
        if (value != null)
        {
            _rightList = value;
        }
    }

    /**
     * Populates the trees' right list with objects contained in the enumerator.
     *
     * @param e
     */
    public final void Populate(Iterator e)
    {
        if (e != null)
        {
            if (_rightList == null)
            {
                _rightList = new java.util.ArrayList();
            }
            if (e instanceof RedBlackEnumerator)
            {
                RedBlackEnumerator redE = (RedBlackEnumerator) e;
                while (redE.hasNext())
                {
                    java.util.HashMap tbl = (java.util.HashMap) ((redE.getValue() instanceof java.util.HashMap) ? redE.getValue() : null);
                    _rightList.addAll(tbl.keySet());
                }
            }
            else
            {
                while (e.hasNext())
                {
                    Map.Entry pair = (Map.Entry)e.next();
                    _rightList.add(pair.getKey());
                }
            }
        }
    }

    /**
     * After reduction, the trees' right list becomes the left list and left list vanishes away.
     */
    public final void Reduce()
    {
        {
            Object tempVar = _rightList.clone();
            _leftList = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            _rightList.clear();
        }
    }

    /**
     * Shifts an object with the specified key from the left list to the right list.
     */
    public final void Shift(Object key)
    {
        if (_leftList.contains(key))
        {
            if (_rightList == null)
            {
                _rightList = new java.util.ArrayList();
            }

            _leftList.remove(key);
            _rightList.add(key);
        }
    }

    /**
     * returns an instance of SRTreeEnumerator.
     *
     * @return
     */
    public final SRTreeEnumerator GetEnumerator()
    {
        if (_leftList != null)
        {
            return new SRTreeEnumerator(_leftList);
        }
        return null;
    }

    /**
     * Merge the right lists of passed tree and current tree.
     *
     * @param tree
     */
    public final void Merge(SRTree tree)
    {
        if (tree == null)
        {
            tree = new SRTree();
        }

        if (tree.getRightList() == null)
        {
            tree.setRightList(new java.util.ArrayList());
        }

        if (this.getRightList() != null)
        {
            java.util.Iterator en = this.getRightList().iterator();
            while (en.hasNext())
            {
                if (!tree.getRightList().contains(en.next()))
                {
                    tree.getRightList().add(en.next());
                }
            }

        }
    }
    public final Object clone()
    {
        SRTree tmp = new SRTree();
        Object tempVar = null;
        Object tempVar2 = null;

        if (this.getLeftList() != null)
        {
            tempVar = this.getLeftList().clone();
        }
        {
            tmp.setLeftList((java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null));
        }
        if (this.getRightList() != null)
        {
            tempVar2 = this.getRightList().clone();
        }
        {
            tmp.setRightList((java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null));
        }

        return tmp;
    }
}
