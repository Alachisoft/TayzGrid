/*
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
package com.alachisoft.tayzgrid.common;

import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author 
 */


public class PortPoolValidationTask implements TimeScheduler.Task  
{
    private boolean _cancel = false;
    private long _interval = 3000;
    
    public PortPoolValidationTask()
    {
    }
    
    @Override
    public boolean IsCancelled() 
    {
        return _cancel;
    }

    @Override
    public long GetNextInterval() {
        return _interval;
    }

    @Override
    public void Run() {
        try{
            Map<String, ArrayList<Integer>> snmpPort = PortPool.getInstance().getSNMPMap();
            for (Object entryObject : snmpPort.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObject;
                ArrayList<Integer> portList = (ArrayList<Integer>) entry.getValue();
                for (int i = 0; i < portList.size(); i++) {
                    //PortPool.getInstance().assignPort((String) entry.getKey(), (Integer) portList.get(i));
                    if(!isPortInUse(portList.get(i)))
                    {
                        PortPool.getInstance().disposeSNMPPort(entry.getKey().toString(), portList.get(i));
                        
                    }
                }
            }
        }catch(Exception ex){
            
        }
    }

    private boolean isPortInUse(Integer port) 
    {
        if(com.alachisoft.tayzgrid.common.net.Helper.isPortFree(port, ServicePropValues.getStatsServerIp()))
            return false;
        else 
            return true;
    }
    
}
