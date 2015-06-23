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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class PerfCounters implements Cloneable, InternalCompactSerializable
{
    private boolean enabled = true;
    private int snmpPort = 0;

    public PerfCounters()
    {
    }

    @ConfigurationAttributeAnnotation(value = "snmp-port", appendText = "")
    public int getSnmpPort()
    {
        return getSnmpPort(null);
    }

    public int getSnmpPort(String cacheName)
    {
        if ((snmpPort == 0) && !(cacheName == null || cacheName.isEmpty()))
        {
            setSnmpPort(PortPool.getInstance().getNextSNMPPort(cacheName));
        }
        return snmpPort;
    }

    @ConfigurationAttributeAnnotation(value = "snmp-port", appendText = "")
    public void setSnmpPort(int snmpPort)
    {
        this.snmpPort = snmpPort;
    }

    @ConfigurationAttributeAnnotation(value = "enable-counters", appendText = "") //Changes for Dom enabled
    public final boolean getEnabled()
    {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value = "enable-counters", appendText = "") //Changes for Dom enabled
    public final void setEnabled(boolean value)
    {
        enabled = value;
    }

    @Override
    public final Object clone()
    {
        PerfCounters perfCounters = new PerfCounters();
        perfCounters.setEnabled(getEnabled());
        perfCounters.setSnmpPort(getSnmpPort());
        return perfCounters;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException
    {
        enabled = reader.ReadBoolean();
        snmpPort = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(enabled);
        writer.Write(snmpPort);
    }
}
