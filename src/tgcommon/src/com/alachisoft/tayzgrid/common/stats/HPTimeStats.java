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

package com.alachisoft.tayzgrid.common.stats;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Class that is useful in capturing statistics. It uses High performnace counters for the measurement of the time.
 */
public class HPTimeStats implements ICompactSerializable
{

    /**
     * Total number of samples collected for the statistics.
     */
    private long _runCount;
    /**
     * Timestamp for the begining of a sample.
     */
    private long _lastStart;
    /**
     * Timestamp for the end of a sample.
     */
    private long _lastStop;
    /**
     * Total time spent in sampling, i.e., acrued sample time.
     */
    private double _totalTime;
    /**
     * Best time interval mesaured during sampling.
     */
    private double _bestTime;
    /**
     * Worst time interval mesaured during sampling.
     */
    private double _worstTime;
    /**
     * Avg. time interval mesaured during sampling.
     */
    private double _avgTime;
    /**
     * Total number of samples collected for the statistics.
     */
    private long _totalRunCount;
    private float _avgCummulativeOperations;
    private double _worstThreshHole = Double.MAX_VALUE;
    private long _worstOccurance;
    private static double _frequency;

    static
    {
    }

    /**
     * Constructor
     */
    public HPTimeStats()
    {

        Reset();
    }

    /**
     * Constructor
     */
    public HPTimeStats(double worstThreshHoleValue)
    {
        Reset();
        _worstThreshHole = worstThreshHoleValue;
    }

    /**
     * Returns the total numbre of runs in the statistics capture.
     */
    public final long getRuns()
    {
        synchronized (this)
        {
            return _runCount;
        }
    }

    public final void setRuns(long value)
    {
        synchronized (this)
        {
            _runCount = value;
        }
    }

    /**
     * Gets or sets the threshhold value for worst case occurance count.
     */
    public final double getWorstThreshHoldValue()
    {
        return _worstThreshHole;
    }

    public final void setWorstThreshHoldValue(double value)
    {
        _worstThreshHole = value;
    }

    /**
     * Gets the number of total worst cases occured.
     */
    public final long getTotalWorstCases()
    {
        return _worstOccurance;
    }

    /**
     * Returns the total time iterval spent in sampling
     */
    public final double getTotal()
    {
        synchronized (this)
        {
            return _totalTime;
        }
    }

    /**
     * Returns the time interval for the last sample
     */
    public final double getCurrent()
    {
        synchronized (this)
        {
            return (double) (_lastStop - _lastStart) ;
        }
    }

    /**
     * Returns the best time interval mesaured during sampling
     */
    public final double getBest()
    {
        synchronized (this)
        {
            return _bestTime;
        }
    }

    /**
     * Returns the avg. time interval mesaured during sampling
     */
    public final double getAvg()
    {
        synchronized (this)
        {
            return _avgTime;
        }
    }

    public final float getAvgOperations()
    {
        synchronized (this)
        {
            return _avgCummulativeOperations;
        }
    }

    /**
     * Returns the worst time interval mesaured during sampling
     */
    public final double getWorst()
    {
        synchronized (this)
        {
            return _worstTime;
        }
    }

    /**
     * Resets the statistics collected so far.
     */
    public final void Reset()
    {
        _runCount = 0;
        _totalTime = _bestTime = _worstTime = _worstOccurance = 0;
        _avgTime = 0;
        _avgCummulativeOperations = 0;
    }

    /**
     * Timestamps the start of a sampling interval.
     */
    public final void BeginSample()
    {
        _lastStart = System.currentTimeMillis();
        _lastStart = _lastStart;
    }

    /**
     * Timestamps the end of interval and calculates the sample time
     */
    public final void EndSample()
    {
        synchronized (this)
        {
            _lastStop = System.currentTimeMillis();
            _lastStop = _lastStop;
            AddSampleTime(getCurrent());
        }
    }

    /**
     * Timestamps the end of interval and calculates the sample time
     */
    public final void EndSample(int runcount)
    {
        synchronized (this)
        {
            _lastStop = System.currentTimeMillis();
            _lastStop = _lastStop;
            AddSampleTime(getCurrent(), runcount);
        }
    }

    /**
     * Adds a specified sample time to the statistics and updates the run count
     *
     * @param time sample time in milliseconds.
     */
    public final void AddSampleTime(double time)
    {
        synchronized (this)
        {

            _runCount++;
            _totalRunCount++;

            if (_runCount == 1)
            {
                _avgTime = _totalTime = _bestTime = _worstTime = time;
            }
            else
            {
                _totalTime += time;
                if (time < _bestTime)
                {
                    _bestTime = time;
                }
                if (time > _worstTime)
                {
                    _worstTime = time;
                }
                if (time > _worstThreshHole)
                {
                    _worstOccurance += 1;
                }
                _avgTime = (double) _totalTime / (double) _runCount;
            }


            if (_totalTime < 1000)
            {
                _avgCummulativeOperations = _runCount;
            }
            else
            {
                _avgCummulativeOperations = (float) _runCount * 1000 / (float) _totalTime;
            }

        }
    }

    /**
     * Adds a specified sample time to the statistics and updates the run count
     *
     * @param time sample time in milliseconds.
     */
    public final void AddSampleTime(double time, int runcount)
    {
        synchronized (this)
        {

            _runCount += runcount;
            _totalRunCount += runcount;

            if (_runCount == 1)
            {
                _avgTime = _totalTime = _bestTime = _worstTime = time;
            }
            else
            {
                _totalTime += time;
                if (time < _bestTime)
                {
                    _bestTime = time;
                }
                if (time > _worstTime)
                {
                    _worstTime = time;
                }
                if (time > _worstThreshHole)
                {
                    _worstOccurance += 1;
                }
                _avgTime = (float) _totalTime / (float) _runCount;
            }

            if (_totalTime < 1000)
            {
                _avgCummulativeOperations = _runCount;
            }
            else
            {
                _avgCummulativeOperations = (float) _runCount * 1000 / (float) _totalTime;
            }
        }
    }

    /**
     * Gets the total run count for the samples
     */
    public final long getTotalRunCount()
    {
        return _totalRunCount;
    }

    /**
     * Override converts to string equivalent.
     *
     * @return
     */
    @Override
    public String toString()
    {
        synchronized (this)
        {
            String retval = "[Runs: " + _runCount + ", ";
            retval += "Best(ms): " + _bestTime + ", ";
            retval += "Avg.(ms): " + _avgTime + ", ";
            retval += "Worst(ms): " + _worstTime + ", ";
            retval += "WorstThreshHole(ms): " + _worstThreshHole + ", ";
            retval += "Worst cases: " + _worstOccurance + "]";

            return retval;
        }
    }

    public final void deserialize(CacheObjectInput reader) throws IOException
    {
        _runCount = reader.readLong();
        _avgTime = reader.readDouble();
        _bestTime = reader.readDouble();
        _lastStart = reader.readLong();
        _lastStop = reader.readLong();
        _worstThreshHole = reader.readDouble();
        _worstTime = reader.readDouble();
        _totalRunCount = reader.readLong();
        _totalTime = reader.readDouble();
        _worstOccurance = reader.readLong();
        _avgCummulativeOperations = reader.readFloat();
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeLong(_runCount);
        writer.writeDouble(_avgTime);
        writer.writeDouble(_bestTime);
        writer.writeLong(_lastStart);
        writer.writeLong(_lastStop);
        writer.writeDouble(_worstThreshHole);
        writer.writeDouble(_worstTime);
        writer.writeLong(_totalRunCount);
        writer.writeDouble(_totalTime);
        writer.writeLong(_worstOccurance);
        writer.writeFloat(_avgCummulativeOperations);

    }
}
