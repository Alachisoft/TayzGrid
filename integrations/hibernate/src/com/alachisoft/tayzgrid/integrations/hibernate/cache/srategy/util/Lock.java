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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util;

import java.io.Serializable;
import java.util.Comparator;
import org.hibernate.cache.spi.access.SoftLock;

public class Lock implements Serializable, Lockable, SoftLock {

    private long unlockTimestamp = -1L;
    private int multiplicity = 1;
    private boolean concurrentLock = false;
    private long timeout;
    private final int id;
    private final Object version;

    public Lock(long timeout, int id, Object version) {
        this.timeout = timeout;
        this.id = id;
        this.version = version;
    }

    public long getUnlockTimestamp() {
        return this.unlockTimestamp;
    }

    public int getId() {
        return this.id;
    }

    public boolean wasLockedConcurrently() {
        return this.concurrentLock;
    }

    public String toString() {
        return "Lock{version=" + this.version + ",multiplicity=" + this.multiplicity + ",unlockTimestamp=" + this.unlockTimestamp;
    }

    @Override
    public Lock lock(long timeout, int lockId) {
        this.concurrentLock = true;
        this.multiplicity += 1;
        this.timeout = timeout;
        return this;
    }

    public void unlock(long currentTimestamp) {
        if (--this.multiplicity == 0) {
            this.unlockTimestamp = currentTimestamp;
        }
    }

    @Override
    public boolean isLock() {
        return true;
    }

    @Override
    public boolean isGettable(long txTimestamp) {
        return false;
    }

    @Override
    public boolean isPuttable(long txTimestamp, Object newVersion, Comparator comparator) {
        if (this.timeout < txTimestamp) {
            return true;
        }
        if (this.multiplicity > 0) {
            return false;
        }
        return this.unlockTimestamp < txTimestamp;
    }
}
