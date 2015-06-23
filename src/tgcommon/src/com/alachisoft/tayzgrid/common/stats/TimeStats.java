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


import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import java.io.Serializable;
import java.util.Calendar;


/**
 Class that is useful in capturing statistics.
*/
public class TimeStats implements Serializable {
	/**  Total number of samples collected for the statistics.
	*/
	private long _runCount;
	/**  Timestamp for the begining of a sample.
	*/
	private long _lastStart;
	/**  Timestamp for the end of a sample.
	*/
	private long _lastStop;
	/**  Total time spent in sampling, i.e., acrued sample time.
	*/
	private long _totalTime;
	/**  Best time interval mesaured during sampling.
	*/
	private long _bestTime;
	/**  Worst time interval mesaured during sampling.
	*/
	private long _worstTime;
	/**  Avg. time interval mesaured during sampling.
	*/
	private float _avgTime;
	/**  Total number of samples collected for the statistics.
	*/
	private long _totalRunCount;

	private long _worstThreshHole = Long.MAX_VALUE;

	private long _worstOccurance;

	/**
	 Constructor
	*/
	public TimeStats() {
		Reset();
	}

	/**
	 Constructor
	*/
	public TimeStats(long worstThreshHoleValue) {
		Reset();
		_worstThreshHole = worstThreshHoleValue;
	}

	/**
	 Returns the total numbre of runs in the statistics capture.
	*/
	public final long getRuns() {
		synchronized (this) {
			return _runCount;
		}
	}

	/**
	 Gets or sets the threshhold value for worst case occurance count.
	*/
	public final long getWorstThreshHoldValue() {
		return _worstThreshHole;
	}
	public final void setWorstThreshHoldValue(long value) {
		_worstThreshHole = value;
	}

	/**
	 Gets the number of total worst cases occured.
	*/
	public final long getTotalWorstCases() {
		return _worstOccurance;
	}

	/**
	 Returns the total time iterval spent in sampling
	*/
	public final long getTotal() {
		synchronized (this) {
			return _totalTime;
		}
	}

	/**
	 Returns the time interval for the last sample
	*/
	public final long getCurrent() {
		synchronized (this) {
			return _lastStop - _lastStart;
		}
	}

	/**
	 Returns the best time interval mesaured during sampling
	*/
	public final long getBest() {
		synchronized (this) {
			return _bestTime;
		}
	}

	/**
	 Returns the avg. time interval mesaured during sampling
	*/
	public final float getAvg() {
		synchronized (this) {
			return _avgTime;
		}
	}

	/**
	 Returns the worst time interval mesaured during sampling
	*/
	public final long getWorst() {
		synchronized (this) {
			return _worstTime;
		}
	}
		//set { lock(this){ _worstTime = value; } }

	/**
	 Resets the statistics collected so far.
	*/
	public final void Reset() {
		_runCount = 0;
		//_lastStart = _lastStop = 0;
		_totalTime = _bestTime = _worstTime = _worstOccurance = 0;
		_avgTime = 0;
	}

	/**
	 Timestamps the start of a sampling interval.
	*/
	public final void BeginSample() {
		_lastStart = (new NCDateTime(Calendar.getInstance().getTimeInMillis()).getTicks() - 621355968000000000L) / 10000;
	}


	/**
	 Timestamps the end of interval and calculates the sample time
	*/
	public final void EndSample() {
		synchronized (this) {
			_lastStop = (new NCDateTime(Calendar.getInstance().getTimeInMillis()).getTicks() - 621355968000000000L) / 10000;
			AddSampleTime(getCurrent());
		}
	}

	/**
	 Timestamp the end of interval and calculates the sample time for bulk operations

	 @param runcount number of operations in bulk
	*/
	public final void EndSample(int runcount) {
		synchronized (this) {
			_lastStop = (new NCDateTime(Calendar.getInstance().getTimeInMillis()).getTicks() - 621355968000000000L) / 10000;
			AddSampleTime(getCurrent(), runcount);
		}
	}
	/**
	 Adds a specified sample time to the statistics and updates the run count

	 @param time sample time in milliseconds.
	*/
	public final void AddSampleTime(long time) {
		synchronized (this) {
			_runCount++;
			_totalRunCount++;
			if (_runCount == 1) {
				_avgTime = _totalTime = _bestTime = _worstTime = time;
			} else {
				_totalTime += time;
				if (time < _bestTime) {
					_bestTime = time;
				}
				if (time > _worstTime) {
					_worstTime = time;
				}
				if (time > _worstThreshHole) {
					_worstOccurance += 1;
				}
				_avgTime = (float)_totalTime / _runCount;
			}
		}
	}

	/**
	 Adds a specified sample time to the statistics and updates the run count

	 @param time sample time in milliseconds.
	 @param runcount num of runs in case of bulk operations
	*/
	public final void AddSampleTime(long time, int runcount) {
		synchronized (this) {
			_runCount += runcount;
			_totalRunCount += runcount;
			if (_runCount == 1) {
				_avgTime = _totalTime = _bestTime = _worstTime = time;
			} else {
				_totalTime += time;
				if (time < _bestTime) {
					_bestTime = time;
				}
				if (time > _worstTime) {
					_worstTime = time;
				}
				if (time > _worstThreshHole) {
					_worstOccurance += 1;
				}
				_avgTime = (float)_totalTime / _runCount;
			}
		}
	}

	/**
	 Gets the total run count for the samples
	*/
	public final long getTotalRunCount() {
		return _totalRunCount;
	}

	/**
	 Override converts to string equivalent.

	 @return
	*/
	@Override
	public String toString() {
		synchronized (this) {
			String retval = "[Runs: " + _runCount + ", ";
			retval += "Best(ms): " + _bestTime + ", ";
			retval += "Avg.(ms): " + _avgTime + ", ";
			retval += "Worst(ms): " + _worstTime + ", ";
			retval += "WorstThreshHole(ms): " + _worstThreshHole + ", ";
			retval += "Worst cases: " + _worstOccurance + "]";

			return retval;
		}
	}
}
