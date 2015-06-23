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

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class RemoteServer implements Comparable {
    private int _port;
    private String _name;
    private short _portRange = 1;
    private short _priority = 0;
    private boolean _userProvided;
    private boolean _do_node_balance = false;

    public RemoteServer(String name, int port) {
        setName(name);
        setPort(port);
    }
    public RemoteServer() { }

    public short getPortRange(){
        return _portRange;
    }
    public void setPortRange(int value){
        if (getPortRange() >0) setPortRange((short)value);
    }
    public short getPriority(){
        return _priority;
    }

    public void setPriority(short value) {
        if(value >0) _priority = value;
    }

    public boolean isUserProvided(){
        return _userProvided;
    }

    public void setUserProvided(boolean value){
        _userProvided = value;
    }

    public String getName(){
        return _name;
    }

    public void setName(String value){

        if (value == "") return;
        if (value != null && value.equalsIgnoreCase("localhost")){
            try {
                _name = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                return;
            }
        }else{
            _name = value;
        }
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        return (this.getName().equalsIgnoreCase(((RemoteServer)obj).getName()) && this.getPort() == ((RemoteServer)obj).getPort());
    }

    public String toString() {
        return getName()+ ":" + getPort();
    }

    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        this._port = port;
    }

    public void setPortRange(short portRange) {
        this._portRange = portRange;
    }

    void setPriority(int i) {
        _priority = (short)i;
    }

    public void setNodeBalance(boolean value){
        _do_node_balance = true;
    }

    public boolean getNodeBalance(){
        return _do_node_balance;
    }

//<editor-fold defaultstate="collapsed" desc="IComparable Members">

    public int compareTo(Object anotherRemoteServer) {
        if (anotherRemoteServer != null && anotherRemoteServer instanceof RemoteServer) {
            RemoteServer other = (RemoteServer)anotherRemoteServer;
            return _priority - other.getPriority();
        }
        return 0;
    }
}//</editor-fold>
