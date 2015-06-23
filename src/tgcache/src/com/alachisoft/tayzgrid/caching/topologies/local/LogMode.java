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

package com.alachisoft.tayzgrid.caching.topologies.local;

public enum LogMode {

    /**
     * This mode specifies that operation should be logged before an actual
     * operation is done on the cache. This logging mode is used for state
     * transfer among replicas
     */
    LogBeforeActualOperation,
    /**
     * This mode specifies that operation should be logged after an actual
     * operation is done on the cache. This logging mode is used for state
     * transfer among partitions
     */
    LogBeforeAfterActualOperation;

    public int getValue() {
        return this.ordinal();
    }

    public static LogMode forValue(int value) {
        return values()[value];
    }
}
