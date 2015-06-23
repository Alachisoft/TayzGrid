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

 
package com.alachisoft.tayzgrid.web.net;

import java.net.InetAddress;

 
public class NodeInfo {

    private InetAddress _ip;
    private int _port;
    

    public NodeInfo(InetAddress ip, int port) {
        _ip = ip;
        _port = port;
    }

    /**
     * IPAddress of the node joining / leaving the cluster.
     */
    public final InetAddress getIpAddress() {
        return _ip;
    }

    /**
     * Port, the member uses for the cluster-wide communication.
     */
    public final int getPort() {
        return _port;
    }

    /**
     * provides the string representation of NodeInfo.
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (_ip == null) {
            sb.append("<null>");
        } else {
            String host_name = _ip.toString();
            appendShortName(host_name, sb);
        }

        sb.append(":" + _port);

        return sb.toString();
    }

    /**
     * Input: "daddy.nms.fnc.fujitsu.com", output: "daddy". Appends result to
     * string buffer 'sb'.
     *
     * @param hostname The hostname in long form. Guaranteed not to be null
     *
     * @param sb The string buffer to which the result is to be appended
     *
     */
    private void appendShortName(String hostname, StringBuilder sb) {
        if (hostname != null) {
            int index = hostname.indexOf((char) '.');

            if (index > 0 && !Character.isDigit(hostname.charAt(0))) {
                sb.append(hostname.substring(0, (index) - (0)));
            } else {
                sb.append(hostname);
            }
        }

    }
}
