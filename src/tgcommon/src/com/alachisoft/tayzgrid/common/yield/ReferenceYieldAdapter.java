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
import java.util.ArrayList;
import java.util.Iterator;


/**
 * This reference adapter simply invokes the Collector<>, first gathering the results into a list.
 * It is provided to illustrate the simplicity of the function of the adapter, and to aid debugging
 * if threading issues are suspected as the cause of problems in the calling code.
 *
 * @author Jim Blackler (jimblackler@gmail.com)
 */
public class ReferenceYieldAdapter<T> implements YieldAdapter<T> {

    /**
     * Convert a method that implements the Collector<> class with a standard Iterable<>, by
     * collecting the results in a list, and returning an iterator to that list.
     */
    public YieldAdapterIterable<T> adapt(Collector<T> client) {

        final ArrayList<T> results = new ArrayList<T>();

        try {
            client.collect(new ResultHandler<T>() {
                public void handleResult(T value) {
                    results.add(value);
                }
            });
        } catch (CollectionAbortedException e) {
            // The process was aborted by calling code.
        }

        // Wrap container's iterator with yield adapter interface for compatibility
        return new YieldAdapterIterable<T>() {
            public YieldAdapterIterator<T> iterator() {
                final Iterator<T> iterator = results.iterator();
                return new YieldAdapterIterator<T>() {
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public T next() {
                        return iterator.next();
                    }

                    public void remove() {
                        iterator.remove();
                    }

                    public void dispose() {
                        // Does nothing in this implementation
                    }
                };
            }
        };

    }
}
