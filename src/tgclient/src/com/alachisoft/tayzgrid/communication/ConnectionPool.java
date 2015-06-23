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

import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.util.NewHashMap;
import com.alachisoft.tayzgrid.web.caching.ShutDownServerInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

public final class ConnectionPool {

    private HashMap<Address, ConnectionManager> _connections;
    private HashMap<Integer, Address> _hashMap;
    private int _bucketSize = 0;
    private final Object _connMutex = new Object();
    private final Object _mapMutex = new Object();

    private long lastViewId = -1;


    public ConnectionPool() {
        this(null);
    }

    public ConnectionPool(HashMap<Integer, Address> hashMap) {
        this._connections = new HashMap<Address, ConnectionManager>();
        if (hashMap != null) {
            this._hashMap = new HashMap<Integer, Address>(hashMap);
        } else {
            this._hashMap = new HashMap<Integer, Address>();
        }
    }

    /**
     *
     * @param ip
     * @return
     */
    public ConnectionManager getConnection(Address ip) {

        ConnectionManager connection = null;
        if (ip != null) {
            synchronized(_connMutex) {
                connection = this._connections.get(ip);
            }
        }
        return connection;
    }

    public int getPort(){
        int port =-1;
        synchronized(_connMutex) {
            if(_connections != null){
            for (ConnectionManager conn : _connections.values())
                {
                    if (conn.isConnected())
                    {
                        return conn.getPort();
                    }
                }
            }
        }
        return port;
    }
    
    /**
     *
     * @param ip
     * @param connection
     */
    public void setConnection(Address ip, ConnectionManager connection) {
        if (ip != null) {
            synchronized(_connMutex) {
                this._connections.put(ip, connection);
            }
        }
    }

    public Address[] getServers() {
         synchronized(_connMutex) {
            return this._connections.keySet().toArray(new Address[1]);
         }
    }
    public int getBucketSize() {
        return this._bucketSize;
    }

    /**
     *
     * @param bucketSize
     */
    public void setBucketSize(int bucketSize) {
        if (bucketSize > 0) {
            this._bucketSize = bucketSize;
        }
    }

    public int getConnectionPoolCount() {
        synchronized(_connMutex) {
            return this._connections.size();
        }
    }

    public HashMap<Address, ConnectionManager> getPool() {
        return this._connections;
    }

    /**
     *
     * @param key
     * @return
     */
    public ConnectionManager getConnectionForKey(Object key) {
        Address ip = getIp(key);

        if (ip != null) {
            return getConnection(ip);
        }

        return null;
    }

    /**
     *
     * @param key
     * @return
     */
    public Address getIp(Object key) {
        if (key == null || this._hashMap == null || this._bucketSize == 0) {
            return null;
        }

        //+Numan Hanif: Fix for 7419
        //int index = AppUtil.hashCode(key) / this._bucketSize;
        int index = AppUtil.hashCode(key) % AppUtil.TotalBuckets;
        //+Numan Hanif
        if (index < 0) {
            index *= -1;
        }

        synchronized(_mapMutex) {
            return this._hashMap.get(index);
        }
    }

    /**
     *
     * @param hashMap
     */
    public void setHashMap(NewHashMap hashMap) {
        synchronized(_mapMutex) {
            this._hashMap = hashMap.getMap();
            this.lastViewId = hashMap.getLastViewId();
        }
    }

    /**
     *
     * @param ip
     * @param connection
     */
    public void add(Address ip, ConnectionManager connection) {
        synchronized(_connMutex) {
            this._connections.put(ip, connection);
        }
    }

    public void remove(Address ip) {
        synchronized(_connMutex) {
            this._connections.remove(ip);
        }
    }

    /**
     *
     * @param ip
     * @return
     */
    public boolean contains(Address ip) {
        synchronized(_connMutex) {
            return this._connections.containsKey(ip);
        }
    }

    public HashMap cloneConnectionPool() {
        synchronized(_connMutex) {
            return (HashMap) this._connections.clone();
        }
    }

    private static int getHashCode(String str) {

        char[] array = str.toCharArray();
        int[] intArray = new int[(int) Math.ceil((double) (array.length) / 2)];

        for (int i = 0, j = 0; i < intArray.length; i++) {
            char[] toInt = new char[2];
            if (j < array.length) {
                toInt[0] = array[j++];
            }
            if (j < array.length) {
                toInt[1] = array[j++];
            }
            intArray[i] = charToInt(toInt);
        }

        int num = 0x15051505;
        int num2 = num;

        for (int i = array.length, j = 0; i > 0; i -= 4, j += 2) {

            num = (((num << 5) + num) + (num >> 0x1b)) ^ intArray[j];
            if (i <= 2) {
                break;
            }
            num2 = (((num2 << 5) + num2) + (num2 >> 0x1b)) ^ intArray[j + 1];
        }
        return (num + (num2 * 0x5d588b65));
    }

    private static int charToInt(char[] array) {
        return (array[0] | (array[1] << 16));
    }

    public long getLastViewId() {
        return lastViewId;
    }

    public boolean isFullyConnected() {
        synchronized(_connMutex) {
            for (ConnectionManager conn : _connections.values())
            {
                if (!conn.isConnected())
                {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isFullyDisConnected() {
        synchronized(_connMutex) {
            for (ConnectionManager conn : _connections.values())
            {
                if (conn.isConnected())
                {
                    return false;
                }
            }
            return true;
        }
    }

      public ConnectionManager GetAnyConnection()
      {
            ConnectionManager connection = null;
            for (ConnectionManager conn : _connections.values())
            {
                if (conn.isConnected())
                {
                    connection = conn;
                    break;
                }
            }
            return connection;
      }

    /*
     *
     * @param newConnections
     */
    public void cleanUpDisconnected(HashMap<Address, Address> newConnections, Hashtable shutdownServers) {
        synchronized(_connMutex) {
            ArrayList<Address> ipToRemove = new ArrayList<Address>();
            Set<Address> ips = this._connections.keySet();
            for (Address ip : ips)
            {
                if (!newConnections.containsKey(ip))
                {
                    ConnectionManager connection = this.getConnection(ip);
                    connection.disconnect();
                    ipToRemove.add(ip);
                    if (shutdownServers.containsKey(ip)) {
                        ShutDownServerInfo ssInfo = (ShutDownServerInfo)shutdownServers.get(ip);

                        synchronized (ssInfo.getWaitForBlockedActivity()) {
                            shutdownServers.remove(ip);
                            Monitor.pulse(ssInfo.getWaitForBlockedActivity());
                            
                        }

                    }
                }
            }

            for(Address ip : ipToRemove) {
                this._connections.remove(ip);
            }
        }
    }
}
