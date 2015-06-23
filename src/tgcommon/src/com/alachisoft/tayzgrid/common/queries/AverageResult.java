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

package com.alachisoft.tayzgrid.common.queries;


import java.io.Serializable;

public class AverageResult implements Serializable { //: ICompactSerializable
	private java.math.BigDecimal sum = new java.math.BigDecimal(0);
	private java.math.BigDecimal count = new java.math.BigDecimal(0);

	public final java.math.BigDecimal getSum() {
		return this.sum;
	}
	public final void setSum(java.math.BigDecimal value) {
		this.sum = value;
	}

	public final java.math.BigDecimal getCount() {
		return this.count;
	}
	public final void setCount(java.math.BigDecimal value) {
		this.count = value;
	}

	public final java.math.BigDecimal getAverage() {
		java.math.BigDecimal average = new java.math.BigDecimal(0);

		if (getCount().compareTo(average) > 0) {
			average = getSum().divide(getCount());
		}

		return average;
	}

}
