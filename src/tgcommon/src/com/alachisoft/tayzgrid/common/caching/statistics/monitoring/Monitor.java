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
package com.alachisoft.tayzgrid.common.caching.statistics.monitoring;


import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.StringCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.Operations;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.logger.JLevel;
import com.alachisoft.tayzgrid.common.snmp.MappingProperties;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Level;
import org.vafer.jmx2snmp.jmx.JmxIndex;
import org.vafer.jmx2snmp.jmx.JmxMib;
import org.vafer.jmx2snmp.jmx.JmxServer;
import org.vafer.jmx2snmp.snmp.SnmpBridge;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.Managed;
import javax.cache.management.*;

public abstract class Monitor implements MonitorMBean
{

    private int snmpPort;
    private static int jmxPort;
    private InetAddress statsServerIp;
    
    
    

    static
    {
        jmxPort = 0;
    }

    public InetAddress getStatsServerIp()
    {
        return statsServerIp;
    }

    public int getSnmpPort()
    {
        return snmpPort;
    }

     public void setSnmpPort(int port)
    {
         snmpPort=port;
    }

     abstract void setPort();


    public static int getJmxPort()
    {
        return jmxPort;
    }
    private String nodeName;
    private short isbridge = 0;
    private ILogger logger;
    private final String exportStringPrefix = "TayzGrid:name=tayzgrid.";
    static JmxServer jmxServer;
    private static boolean jmxStarted = false;
    MBeanExporter exporter;
    /*
     * dynamicMIBGeneration allows for using one port for all connections, the downside is client must have different Mib for each cache or server.
     * if disabled, then client can use just on mib for different caches or servers, but client must connect to different ports assigned to different caches.
     * When enabling or disabling please make sure you have commented or uncommented "dynamicMibGenerationEnabled = false/true" accordingly.
     *
     * [[Don't expect it to run with out changing code as a lot has changed]]
     *
     */
    private final static boolean dynamicMIBGenerationEnabled = false;
    protected SnmpBridge snmpBridge;
    protected boolean snmpStarted = false;

    public static boolean isDynamicMIBGenerationEnabled()
    {
        return dynamicMIBGenerationEnabled;
    }

    public String getExportStringPrefix()
    {
        return exportStringPrefix;
    }

    public Monitor(String nodeName)
    {
        this.nodeName = nodeName;
        this.statsServerIp = ServicePropValues.getStatsServerIp();
        if (jmxPort == 0)
        {
            jmxPort = PortPool.getInstance().getJMXPort(this.statsServerIp);
        }
    }

    public Monitor(String nodeName, ILogger logger)
    {
        this.nodeName = nodeName;
        this.logger = logger;
        this.statsServerIp = ServicePropValues.getStatsServerIp();
        if (jmxPort == 0)
        {
            jmxPort = PortPool.getInstance().getJMXPort(this.statsServerIp);
        }
    }

    @Override
    public void registerCounter(Operations operation, PerformanceCounter perfCounter) throws Exception
    {
        if (operation == null || perfCounter == null)
        {
            throw new Exception("Opertation or perfcounter cannot be null");
        }
        getCounterStore().put(operation, perfCounter);
    }
    public void registerServerCounter(Operations operation, PerformanceCounter perfCounter) throws Exception
    {
        if (operation == null || perfCounter == null)
        {
            throw new Exception("Opertation or perfcounter cannot be null");
        }
        ServerMonitor.getServerCounterStore().put(operation, perfCounter);
    }

    @Override
    public void unRegisterCounter(Operations operation)
    {
        getCounterStore().remove(operation);
    }

    @Override
    public void startJMX()
    {
        if (jmxStarted)
        {
            return;
        }
        try
        {
            jmxServer = new JmxServer(getStatsServerIp(), 0, jmxPort);
            jmxServer.start();
            jmxStarted = true;
        }
        catch (Exception exception)
        {
            this.Log(Monitor.class.getCanonicalName().toString() + "Error while attempting to start JMX", exception.getMessage(), JLevel.SEVERE);
        }
    }

    @Override
    public void stopJMX()
    {
        try
        {
            if (jmxServer != null)
            {
                jmxServer.stop();
            }
        }
        catch (Exception exception)
        {
            this.Log(Monitor.class.getCanonicalName().toString() + "Error while attempting to stop JMX", exception.getMessage(), JLevel.Error);
        }
    }
    

    @Override
    public void startSNMP()
    {
        if (isDynamicMIBGenerationEnabled() && snmpStarted)
        {
            return;
        }

        try
        {
            final URL url = Monitor.class.getResource(ServicePropValues.getMapping_Properties());
            final JmxMib jmxMib = new JmxMib();
            final JmxIndex jmxIndex;
            if (!isDynamicMIBGenerationEnabled())
            {
                // Since mapping.properties is required anyone who is publishing on snmp and clients don't have access to it,
                // so now it is loaded from a hardcoded string instead.
                jmxMib.load(MappingProperties.getOids());
                jmxIndex = new JmxIndex(nodeName, getExportString());
            }
            else
            {
                jmxMib.dynamicLoad(new FileReader(url.getFile()));
                jmxIndex = new JmxIndex();
            }

            InetAddress address = null;
            address = InetAddress.getByName("0.0.0.0");
            snmpBridge = new SnmpBridge(address, this.snmpPort, jmxIndex, jmxMib);
            
            while(true){
                try{
                    snmpBridge.start();
                    snmpStarted = true;
                    break;
                    }
                catch(Exception ex){
                    this.snmpPort = this.snmpPort +6;
                    snmpBridge = new SnmpBridge(address, this.snmpPort, jmxIndex, jmxMib);
                }
            }
        }
        catch (Exception exception)
        {

        }
    }

    @Override
    public void stopSNMP() throws Exception
    {
        snmpBridge.stop();
    }

    @Override
    public Boolean registerNode()
    {
        try
        {
            String exportString = getExportString();
            exporter = new MBeanExporter(ManagementFactory.getPlatformMBeanServer());
            exporter.export(exportString, this);
        }
        catch (Throwable t)
        {
            Log(t.getMessage(), JLevel.Error);
            return false;
        }

        return true;
    }

    @Override
    public Boolean unRegisterNode()
    {
        try
        {
            exporter.unexport(getExportString());
        }
        catch (Exception exception)
        {
            Log(exception.getMessage(), JLevel.Error);
            return false;
        }
        return true;
    }

    public @Override
    double getCounter(Operations operation)
    {
        return ((PerformanceCounter) getCounterStore().get(operation)).getValue();
    }
    
    String getCounterString(Operations operation)
    {
        return ((StringCounter) getCounterStore().get(operation)).getStringValue();
    }
    

    protected PerformanceCounter getCounterObject(Operations operation)
    {
        return (PerformanceCounter)getCounterStore().get(operation);

    }

    public @Managed(description="Name of this running node. ",name="Node Name")
    @Override
    String getNodeName()
    {
        return nodeName;
    }

    public void Log(String message, Level level)
    {
        Log(null, message, level);
    }

    public void Log(String moduleName, String message, Level level)
    {
        if (this.logger != null)
        {
            if (level.equals(JLevel.ALL))
            {
                if (moduleName == null)
                {
                    this.logger.Debug(message);
                }
                else
                {
                    this.logger.Debug(moduleName, message);
                }
            }
            else if (level.equals(JLevel.CriticalInfo))
            {
                if (moduleName == null)
                {
                    this.logger.CriticalInfo(message);
                }
                else
                {
                    this.logger.CriticalInfo(moduleName, message);
                }
            }
            else if (level.equals(JLevel.Error))
            {
                if (moduleName == null)
                {
                    this.logger.Error(message);
                }
                else
                {
                    this.logger.Error(moduleName, message);
                }
            }
            else if (level.equals(JLevel.SEVERE))
            {
                if (moduleName == null)
                {
                    this.logger.Fatal(message);
                }
                else
                {
                    this.logger.Fatal(moduleName, message);
                }
            }
            else
            {
                if (moduleName == null)
                {
                    this.logger.Info(message);
                }
                else
                {
                    this.logger.Info(moduleName, message);
                }
            }
        }
    }
}
