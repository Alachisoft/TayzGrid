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

import com.alachisoft.tayzgrid.common.net.Address;

public final class BucketTxfrInfo {

    public java.util.ArrayList bucketIds;
    public boolean isSparsed;
    public Address owner;
    public boolean end;

    public BucketTxfrInfo() {
    }

    public BucketTxfrInfo(boolean end) {
        bucketIds = null;
        owner = null;
        isSparsed = false;
        this.end = end;
    }

    public BucketTxfrInfo(java.util.ArrayList bucketIds, boolean isSparsed, Address owner) {
        this.bucketIds = bucketIds;
        this.isSparsed = isSparsed;
        this.owner = owner;
        this.end = false;
    }

    public BucketTxfrInfo clone() {
        BucketTxfrInfo varCopy = new BucketTxfrInfo();

        varCopy.bucketIds = this.bucketIds;
        varCopy.isSparsed = this.isSparsed;
        varCopy.owner = this.owner;
        varCopy.end = this.end;

        return varCopy;
    }
}