/*
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

package com.alachisoft.tayzgrid.cluster.stack;
 
 
public class Interval {
	private int nextInt = 0;
	private long[] interval = null;

	public Interval(long[] interval) {
		if (interval.length == 0) {
			throw new IllegalArgumentException("Interval()");
		}
		this.interval = interval;
	}

	public long first() {
		return interval[0];
	}

	/** @return  the next interval 
	 
	*/
	public long next() {
		synchronized (this) {
			if (nextInt >= interval.length) {
				return (interval[interval.length - 1]);
			} else {
				return (interval[nextInt++]);
			}
		}
	}

	public long[] getInterval() {
		return interval;
	}

	public void reset() {
		synchronized (this) {
			nextInt = 0;
		}
	}
}
