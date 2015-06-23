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

package com.alachisoft.tayzgrid.runtime.cachemanagement;

public class ServerNode {

    private String _serverIP;
    private int _port;
    private boolean _isReplica = false;

    public final void setServerIP(String value) {
        _serverIP = value;
    }

    public final String getServerIP() {
        return _serverIP;
    }

    public final void setPort(int value) {
        _port = value;
    }

    public final int getPort() {
        return _port;
    }

    public final void setIsReplica(boolean value) {
        _isReplica = value;
    }

    public final boolean getIsReplica() {
        return _isReplica;
    }
}