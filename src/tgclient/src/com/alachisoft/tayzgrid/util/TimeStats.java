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

package com.alachisoft.tayzgrid.util;

public class TimeStats {

    /** Total number of samples collected for the statistics. */
    private long _runCount;
    /** Timestamp for the begining of a sample. */
    private long _lastStart;
    /** Timestamp for the end of a sample. */
    private long _lastStop;
    /** Total time spent in sampling, i.e., acrued sample time. */
    private long _totalTime;
    /** Best time interval mesaured during sampling. */
    private long _bestTime;
    /** Worst time interval mesaured during sampling. */
    private long _worstTime;
    /** Avg. time interval mesaured during sampling. */
    private float _avgTime;
    /** Total number of samples collected for the statistics. */
    private long _totalRunCount;

    private long _worstThreshHole = Long.MAX_VALUE;

    private long _worstOccurance;

    /** Constructor */
    public TimeStats() {
        reset();
    }

    /**
     * Constructor
     * @param worstThreshHoleValue
     */
    public TimeStats(long worstThreshHoleValue) {
        reset();
        _worstThreshHole = worstThreshHoleValue;
    }

    /**
     * Returns the total numbre of runs in the statistics capture.
     * @return
     */
    public long getRuns() {
        return _runCount;
    }

    /**
     * Gets the threshhold value for worst case occurance count.
     * @return
     */
    public long getWorstThreshHoldValue() {
        return _worstThreshHole; }

    /**
     * Sets the threshhold value for worst case occurance count.
     * @param value
     */
    public void setWorstThreshHoldValue(long value) {
        _worstThreshHole = value;
    }

    /**
     * Gets the number of total worst cases occured.
     * @return
     */
    public long getTotalWorstCases() {
        return _worstOccurance;
    }

    /**
     * Returns the total time iterval spent in sampling
     * @return
     */
    public long getTotal() {
        return _totalTime;
    }

    /**
     * Returns the time interval for the last sample
     * @return
     */
    public long getCurrent() {
        return (_lastStop - _lastStart)/1000;
    }

    /**
     * Returns the best time interval mesaured during sampling
     * @return
     */
    public long getBest() {
        return _bestTime;
    }

 

    /**
     * Returns the avg. time interval mesaured during sampling
     * @return
     */
    public float getAvg() {
        return _avgTime;
        //set { lock(this){ _avgTime = value; } }
    }

 

    /**
     * Returns the worst time interval mesaured during sampling
     * @return
     */
    public long getWorst() {
        return _worstTime;
        //set { lock(this){ _worstTime = value; } }
    }

 

    /** Resets the statistics collected so far. */
    public void reset() {
        _runCount = 0;
        _lastStart = _lastStop = 0;
        _totalTime = _bestTime = _worstTime = _worstOccurance = 0;
        _avgTime = 0;
    }

    /** Timestamps the start of a sampling interval. */
    public void beginSample() {
        _lastStart = System.nanoTime();
    }


    /** Timestamps the end of interval and calculates the sample time */
    public void endSample() {
        _lastStop = System.nanoTime();
        addSampleTime(getCurrent());
    }

    /**
     * Adds a specified sample time to the statistics and updates the run count
     * @param time
     */
    public void addSampleTime(long time) {
        _runCount ++;
        _totalRunCount ++;
        if(_runCount == 1) {
            _avgTime = _totalTime = _bestTime = _worstTime = time;
        } else {
            _totalTime += time;
            if(time < _bestTime)	_bestTime = time;
            if(time > _worstTime)	_worstTime = time;
            if (time > _worstThreshHole) _worstOccurance += 1;
            _avgTime = (float)_totalTime / _runCount;
        }
    }

    /**
     * Gets the total run count for the samples
     * @return
     */
    public long getTotalRunCount() {
        return _totalRunCount;
    }

    /**
     * Converts to string equivalent.
     * @return
     */
    public String toString() {
        String retval = "[Runs: " + _runCount + ", ";
        retval += "Best(mili.micro): " + toMiliMicro(_bestTime) + ", ";
        retval += "Avg.(mili.micro): " + toMiliMicro((long)_avgTime) + ", ";
        retval += "Worst(mili.micro): " + toMiliMicro(_worstTime) + ", ";
        retval += "WorstThreshHole(mili): " + _worstThreshHole + ", ";
        retval += "Worst cases: " + _worstOccurance + "]";

        return retval;
    }

    /**
     *
     * @param microSecs
     * @return
     */
    public static String toMiliMicro(long microSecs){
        long mili = microSecs / 1000;
        long micro = microSecs - mili * 1000;
        return mili + "." + micro;
    }
}
