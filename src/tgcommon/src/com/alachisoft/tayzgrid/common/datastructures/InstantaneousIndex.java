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

public class InstantaneousIndex<T> implements Cloneable, Iterable<T>
{
    private java.util.ArrayList<T> _activitesList = new java.util.ArrayList<T>();
    private java.util.HashMap _table = new java.util.HashMap();
    private boolean _checkDuplication;

    private long privateClockTime;
    public final long getClockTime()
    {
        return privateClockTime;
    }
    public final void setClockTime(long value)
    {
        privateClockTime = value;
    }

    private long privateMinActivityId;
    public final long getMinActivityId()
    {
        return privateMinActivityId;
    }
    public final void setMinActivityId(long value)
    {
        privateMinActivityId = value;
    }

    private long privateMaxActivityId;
    public final long getMaxActivityId()
    {
        return privateMaxActivityId;
    }
    public final void setMaxActivityId(long value)
    {
        privateMaxActivityId = value;
    }

    public final boolean getEnableDuplicationCheck()
    {
        return _checkDuplication;
    }
    public final void setEnableDuplicationCheck(boolean value)
    {
        _checkDuplication = value;
    }

    public final void AddEntry(long entryId, T activity)
    {
        if (_checkDuplication)
        {
            _table.put(activity, entryId);
        }

        _activitesList.add(activity);
        if (_activitesList.size() == 1)
        {
            setMinActivityId(entryId);
        }

        setMaxActivityId(entryId);
    }

    public final boolean CheckDuplication(T activity)
    {
        return _table.containsKey(activity);
    }

    public final java.util.ArrayList<T> GetClientActivites(long lastEnteryId)
    {
        java.util.ArrayList<T> clientActivities = new java.util.ArrayList<T>();

        if (lastEnteryId < getMinActivityId())
        {
            return _activitesList;
        }
        else
        {
            if (lastEnteryId < getMaxActivityId())
            {
                int startingIndex = (int)(lastEnteryId - getMinActivityId()) + 1;
                int length = (int)(getMaxActivityId() - lastEnteryId);
                return getRange(_activitesList, startingIndex, length);
            }
        }
        return clientActivities;
    }
    
    private java.util.ArrayList<T> getRange(java.util.ArrayList<T> list, int start, int last)
    {
        java.util.ArrayList<T> temp = new java.util.ArrayList<T>();

        for (int x = start; x<=last; x++) 
        {

            temp.add(list.get(x));

        }

        return temp;
    }

    @Override
    public boolean equals(Object obj)
    {
        InstantaneousIndex<T> other = (InstantaneousIndex<T>)obj;

        if (other != null && other.getClockTime() == this.getClockTime())
        {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    public final Object clone()
    {
        InstantaneousIndex<T> clone = new InstantaneousIndex<T>();
        clone._activitesList.addAll(getRange(this._activitesList, 0, _activitesList.size()));
        return clone;
    }



    public final java.util.Iterator<T> GetEnumerator()
    {
        return _activitesList.iterator();
    }


    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
