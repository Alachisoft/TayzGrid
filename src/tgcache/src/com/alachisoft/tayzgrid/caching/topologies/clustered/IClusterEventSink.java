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

import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;

public interface IClusterEventSink {

    /**
     * Handles the function requests.
     *
     */
    Object HandleClusterMessage(Address src, Function func) throws OperationFailedException,  GeneralFailureException, StateTransferException, LockingException, CacheException, SuspectedException, TimeoutException, Exception;

    Object HandleClusterMessage(Address src, Function func, tangible.RefObject<Address> destination, tangible.RefObject<Message> replicationMsg) throws OperationFailedException,  GeneralFailureException, StateTransferException, LockingException, CacheException, SuspectedException, TimeoutException, Exception;
}