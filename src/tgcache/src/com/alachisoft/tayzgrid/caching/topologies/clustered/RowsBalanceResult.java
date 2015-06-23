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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

public class RowsBalanceResult {

    private int[] _resultIndicies; //set of buckets to be given away.
    private long _distanceFromTarget; //to compare two sets. The set with least distance is the one to be selected.

    public RowsBalanceResult() {
    }

    public final int[] getResultIndicies() {
        return _resultIndicies;
    }

    public final void setResultIndicies(int[] value) {
        _resultIndicies = value;
    }

    public final long getTargetDistance() {
        return _distanceFromTarget;
    }

    public final void setTargetDistance(long value) {
        _distanceFromTarget = value;
    }
}
