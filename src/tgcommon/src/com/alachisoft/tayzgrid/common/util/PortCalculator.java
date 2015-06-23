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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.common.util;

/**
 *
 * @author 
 */


public class PortCalculator {
    public static final int PORT_JUMP = 6;

    public static int getManagementPort(int port){
        return port+1;
    }
    
    public static int getSNMPPort(int port){
        return port+2;
    }
    
    public static int getClusterPort(int port){
        return port+3;
    }
    
    public static int getClusterPortReplica(int port){
        return port+1;
    }
    
    public static int getSNMPPortReplica(int port){
        return port+5;
    }
    
}
