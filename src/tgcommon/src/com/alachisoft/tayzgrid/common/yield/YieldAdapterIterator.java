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
package com.alachisoft.tayzgrid.common.yield;


import java.util.Iterator;

/**
 * A version of a standard Iterator<> used by the yield adapter. The only addition is a dispose()
 * function to clear resources manually when required.
 */
public interface YieldAdapterIterator<T> extends Iterator<T> {

    /**
     * Because the Yield Adapter starts a separate thread for duration of the collection, this can
     * be left open if the calling code only reads part of the collection. If the iterator goes out
     * of scope, when it is GCed its finalize() will close the collection thread. However garbage
     * collection is sporadic and the VM will not trigger it simply because there is a lack of
     * available threads. So, if a lot of partial reads are happening, it will be wise to manually
     * close the iterator (which will clear the resources immediately).
     */
    void dispose();
}
