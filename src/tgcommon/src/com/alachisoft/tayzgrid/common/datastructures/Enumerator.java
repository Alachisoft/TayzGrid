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

import com.alachisoft.tayzgrid.common.stats.Clock;

public class Enumerator<T> implements java.util.Iterator<T>
{
    private java.util.ArrayList<InstantaneousIndex<T>> _index = new java.util.ArrayList<InstantaneousIndex<T>>();
    private java.util.Iterator<InstantaneousIndex<T>> _enumerator;
    private java.util.Iterator<T> _subIndexEnumerator;
    private T _current;

    public Enumerator(SlidingIndex<T> slidingIndex)
    {
        for (InstantaneousIndex<T> indexEntry : slidingIndex._mainIndex)
        {
            //for older enteries which are not supposed to change
            if (indexEntry.getClockTime() != Clock.getCurrentTimeInSeconds())
            {
                _index.add(indexEntry);
            }
            else
            {
                    //index being modified currently
                Object tempVar = indexEntry.clone();
                _index.add((InstantaneousIndex<T>)tempVar);
            }
        }

            _enumerator = _index.iterator();
    }

    public final T getCurrent()
    {
            return _current;
    }

    public final void dispose()
    {
    }

    public final boolean MoveNext()
    {
        do
        {
            if (_subIndexEnumerator == null)
            {
                if (_enumerator.hasNext())
                {
                    InstantaneousIndex<T> indexEntry = _enumerator.next();
                    _subIndexEnumerator = indexEntry.iterator();
                }
            }

            if (_subIndexEnumerator != null)
            {
                if (_subIndexEnumerator.hasNext())
                {
                    _current = _subIndexEnumerator.next();
                    return true;
                }
                else
                {
                    _subIndexEnumerator = null;
                }
            }
            else
            {
                return false;
            }
        } while (true);
    }

    public final void Reset()
    {
        _enumerator = _index.iterator();
        _subIndexEnumerator = null;
        _current = null;
    }

    @Override
    public boolean hasNext() 
    {
        return _enumerator.hasNext();
    }

    @Override
    public T next() 
    {
        do
        {
            if (_subIndexEnumerator == null)
            {
                if (_enumerator.hasNext())
                {
                    InstantaneousIndex<T> indexEntry = _enumerator.next();
                    _subIndexEnumerator = indexEntry.iterator();
                }
            }

            if (_subIndexEnumerator != null)
            {
                if (_subIndexEnumerator.hasNext())
                {
                    _current = _subIndexEnumerator.next();
                    //return true;
                }
                else
                {
                    _subIndexEnumerator = null;
                }
            }
            else
            {
            }
        } while (true);
    }

    @Override
    public void remove() 
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
}
