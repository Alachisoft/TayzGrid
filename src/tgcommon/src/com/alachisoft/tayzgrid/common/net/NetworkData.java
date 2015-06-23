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
package com.alachisoft.tayzgrid.common.net;

import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.systemcounters.SystemCounter;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.RefObject;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetFlags;

public class NetworkData
{

    static Map<String, Long> rxCurrentMap = new HashMap<String, Long>();
    static Map<String, List<Long>> rxChangeMap = new HashMap<String, List<Long>>();
    static Map<String, Long> txCurrentMap = new HashMap<String, Long>();
    static Map<String, List<Long>> txChangeMap = new HashMap<String, List<Long>>();

    private static Map<String, String> filteredIP = new HashMap<String, String>();
    private static Map<String, Integer> filteredSpeed = new HashMap<String, Integer>();
    public static boolean enabled = true;

    /**
     * @throws InterruptedException
     * @throws SigarException
     *
     */
    public static void registerIPToMonitor(String ip)
    {
        if (ip != null && !ip.isEmpty())
        {
           if (enabled)
            { 
                if(SystemCounter.getSigar() == null)
                     return ;
                try
                {
                    for (String ni : SystemCounter.getSigar().getNetInterfaceList())
                    {

                        NetInterfaceConfig ifConfig = null;
                        try
                        {
                            ifConfig = SystemCounter.getSigar().getNetInterfaceConfig(ni);
                        }
                        catch (SigarException ex)
                        {
                            Logger.getLogger(NetworkData.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if (ip != null && ip.equals(ifConfig.getAddress()))
                        {
                            synchronized (filteredIP)
                            {
                                if (!filteredIP.containsKey(ip))
                                {
                                    filteredIP.put(ip, ni);
                                }
                            }
                        }
                    }
                }
                catch (Throwable exc)
                {
                    enabled = false;
                }
            }
        }
    }

    /*
     * Gets the network usage of registered NIC's. Call registerIPToMonitor before
     * calling this function.
     */
    public static double getNetworkUsage() throws SigarException
    {
        if(SystemCounter.getSigar() == null)
            return 0;

        double speed = 1;
        double totalUsage = -1;
        for (String ni : filteredIP.values())
        {
            NetInterfaceStat netStat = SystemCounter.getSigar().getNetInterfaceStat(ni);
            NetInterfaceConfig ifConfig = SystemCounter.getSigar().getNetInterfaceConfig(ni);

            String hwaddr = null;
            speed = netStat.getSpeed();

            if(speed == -1)
            {
                if(!filteredSpeed.containsKey(ni))
                {
                    filteredSpeed.put(ni, getNICLinuxSpeed());
                }
                speed = filteredSpeed.get(ni);
            }

            if (!NetFlags.NULL_HWADDR.equals(ifConfig.getHwaddr()))
            {
                hwaddr = ifConfig.getHwaddr();
            }
            if (hwaddr != null)
            {
                double rxCurrenttmp = netStat.getRxBytes();
                saveChange(rxCurrentMap, rxChangeMap, hwaddr, (long) rxCurrenttmp, ni);
                double txCurrenttmp = netStat.getTxBytes();
                saveChange(txCurrentMap, txChangeMap, hwaddr, (long) txCurrenttmp, ni);

                long totalrxDown = getMetricData(rxChangeMap);
                long totaltxUp = getMetricData(txChangeMap);
                for (List<Long> l : rxChangeMap.values())
                {
                    l.clear();
                }
                for (List<Long> l : txChangeMap.values())
                {
                    l.clear();
                }
                Sigar.formatSize(totaltxUp);
                double usageForCurrentNIC = 0;
                if (speed != 0)
                {
                    usageForCurrentNIC = (((totalrxDown + totaltxUp) * 8) / speed) * 100;
                }
                else
                {
                    usageForCurrentNIC=0;
                }

                if (totalUsage == -1)
                {
                    totalUsage = usageForCurrentNIC;
                }
                else
                {
                    totalUsage = (totalUsage + usageForCurrentNIC) / 2;
                }
            }
        }

        if (totalUsage == -1)
        {
            totalUsage = 0;
        }
        if (totalUsage > 100)
        {
            totalUsage = 100;
        }

        return totalUsage;
    }

    public static int getNICLinuxSpeed()
    {
        try
        {
            String[] cmd =
            {
                "/bin/sh",
                "-c",
                "ethtool em1 2>/dev/null | grep Speed"
            };

            Process p = Runtime.getRuntime().exec(cmd);
            InputStream stdoutStream = new BufferedInputStream(p.getInputStream());

            StringBuffer buffer = new StringBuffer();
            for (;;)
            {
                int c = stdoutStream.read();
                if (c == -1)
                {
                    break;
                }
                buffer.append((char) c);
            }
            String outputText = buffer.toString();

            stdoutStream.close();

            //Brute Force with regex
            outputText = outputText.split(":")[1];
            String number = outputText.replaceAll("[^0-9]","");
            String rate = outputText.trim().replace(number, "");
            char metric = rate.charAt(0);
            int mult = 1;
            if(metric == 'M')
                mult=1000*1000;
            else if(metric == 'G')
                mult=1000*1000*1000;
            else if (metric == 'T')
                mult=1000*1000*1000*1000;

            return Integer.parseInt(number) * mult;
        }
        catch(Exception ex)
        {
            return 0;
        }
    }

    public static String getDefaultGateway() throws SigarException
    {
        if(SystemCounter.getSigar() == null)
            return "";
        return SystemCounter.getSigar().getNetInfo().getDefaultGateway();
    }

    private static long getMetricData(Map<String, List<Long>> rxChangeMap)
    {
        long total = 0;
        for (Entry<String, List<Long>> entry : rxChangeMap.entrySet())
        {
            int average = 0;
            for (Long l : entry.getValue())
            {
                average += l;
            }
            total += average / entry.getValue().size();
        }
        return total;
    }

    private static void saveChange(Map<String, Long> currentMap, Map<String, List<Long>> changeMap, String hwaddr, long current, String ni)
    {
        Long oldCurrent = currentMap.get(ni);
        if (oldCurrent != null)
        {
            List<Long> list = changeMap.get(hwaddr);
            if (list == null)
            {
                list = new LinkedList<Long>();
                changeMap.put(hwaddr, list);
            }
            list.add((current - oldCurrent));
        }
        currentMap.put(ni, current);
    }
}
