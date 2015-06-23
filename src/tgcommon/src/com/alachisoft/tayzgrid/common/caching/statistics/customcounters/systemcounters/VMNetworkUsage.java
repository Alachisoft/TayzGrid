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

package com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters;

import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hyperic.sigar.SigarException;

public class VMNetworkUsage extends SystemCounter
{
    public VMNetworkUsage(String name, String instance)
    {
        super(name, instance);
    }

    @Override
    protected void calculate(double value)
    {
        try {
            double usage = NetworkData.getNetworkUsage();
            setValue(usage);
        } catch (SigarException ex) {
            Logger.getLogger(VMNetworkUsage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
