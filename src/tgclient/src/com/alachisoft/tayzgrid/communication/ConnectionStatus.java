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

package com.alachisoft.tayzgrid.communication;

public final class ConnectionStatus {

        /** The Manager is busy.*/
        public static final byte BUSY = 0x1;

        /** The Manager is connected.*/
        public static final byte CONNECTED = 0x2;

        /**The Manger is not connected.*/
        public static final byte DISCONNECTED = 0x4;

        /**This is in load balance state so don't wont queue up new request.*/
        public static final byte LOADBALANCE = 0x8;

        private final byte value;
        ConnectionStatus(byte value){ this.value = value; }
        public byte value(){ return value; }
}
