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

/**
 * Specifies how data is to be transferred from a source node to a target node.
 */
public class StateTransferType {

    /**
     * Data is to be transferred from source node to target node and at the
     * completion of transfer it is removed from the source node.
     */
    public static final byte MOVE_DATA = 1;

    /**
     * Data is replicated from source node to the target node.
     */
    public static final byte REPLICATE_DATA = 2;
}
