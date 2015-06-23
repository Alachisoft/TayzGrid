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

package com.alachisoft.tayzgrid.web.caching;

/**
 * Provide method for notifying applications after a request for asynchronous clear operation
 * completes.
 */
public interface AsyncCacheClearedCallback {

    /**
     * This method is called when async clear operation completes
     * @param result The result of the Async Operation. If the operation completes successfully,
     * it contatins 1 otherwise it contains an OperationFailedException indicating
     * the cause of operation failure.
     */
    public void asyncCacheCleared(Object result);
}
