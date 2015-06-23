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
public class SlidingIndex<T> 
{
    private static final int MAX_OBSERVATION_INTERVAL = 300;
    private static final int MIN_OBSERVATION_INTERVAL = 1;

    private int _observationInterval = MIN_OBSERVATION_INTERVAL; //in seocnds;
    private long _entryIdSequence;
    private boolean _checkDuplication;

    java.util.ArrayList<InstantaneousIndex<T>> _mainIndex = new java.util.ArrayList<InstantaneousIndex<T>>();
    
    public SlidingIndex(int interval)
    {
        this(interval,false);
    }

    public SlidingIndex(int interval, boolean checkDuplication)
    {
        if (interval > MIN_OBSERVATION_INTERVAL)
        {
                _observationInterval = interval;
        }
        _checkDuplication = checkDuplication;
    }
    public int GetInterval()
    {
        return _observationInterval;
    }
        
    public final boolean AddToIndex(T indexValue)
    {
        synchronized (this)
        {
            long currentTime = com.alachisoft.tayzgrid.common.stats.Clock.getCurrentTimeInSeconds();
            long entryId = _entryIdSequence++;
            InstantaneousIndex<T> indexEntry = null;

            if (_mainIndex.size() == 0)
            {
                    indexEntry = new InstantaneousIndex<T>();
                    indexEntry.setEnableDuplicationCheck(_checkDuplication);
                    indexEntry.setClockTime(currentTime);
                    _mainIndex.add(indexEntry);
            }
            else
            {
                if (_checkDuplication && CheckForDuplication(indexValue))
                {
                        return false;
                }

                ExpireOldEnteries(currentTime);
               
                InstantaneousIndex<T> matchEntry = null;
                for (InstantaneousIndex<T> match : _mainIndex)
                {
                    if (match.getClockTime() == currentTime)
                    {
                        matchEntry = match;
                        break;
                    }
                }

                boolean newEntry = false;
                if (matchEntry != null)
                {
                        indexEntry = matchEntry;
                }
                else
                {
                        newEntry = true;
                        indexEntry = new InstantaneousIndex<T>();
                        indexEntry.setEnableDuplicationCheck(_checkDuplication);
                        indexEntry.setClockTime(currentTime);
                        _mainIndex.add(indexEntry);
                }

            }
            indexEntry.AddEntry(entryId, indexValue);
        }
        return true;
    }

    private boolean CheckForDuplication(T activity)
    {
        if (!_checkDuplication)
        {
                return false;
        }

        for (InstantaneousIndex<T> currentIndexEntry : _mainIndex)
        {
                if (currentIndexEntry.CheckDuplication(activity))
                {
                        return true;
                }
        }
        return false;
    }
     
    public final java.util.Iterator<T> GetCurrentData()
    {
        synchronized (this)
        {
                java.util.Iterator<T> enumerator = new com.alachisoft.tayzgrid.common.datastructures.Enumerator<T>(this);    
                return enumerator;
        }
    }
    
    private void ExpireOldEnteries(long currentTime)
    {
        synchronized (this)
        {

                java.util.ArrayList<InstantaneousIndex<T>> enteriesTobeRemoved = new java.util.ArrayList<InstantaneousIndex<T>>();
                for (InstantaneousIndex<T> currentIndexEntry : _mainIndex)
                {
                        long windowStartTime = currentTime - _observationInterval;
                        if (windowStartTime > 0 && currentIndexEntry.getClockTime() < windowStartTime)
                        {
                                enteriesTobeRemoved.add(currentIndexEntry);
                        }
                        else
                        {
                                break;
                        }
                }
                for (InstantaneousIndex<T> currentIndexEntry : enteriesTobeRemoved)
                {
                        _mainIndex.remove(currentIndexEntry);
                }
        }
    }
}
