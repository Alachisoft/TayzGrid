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

import com.alachisoft.tayzgrid.common.Common;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;

public class Helper
{

    public static int getFreeTCPPort(InetAddress ipAddress)
    {
        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(0);
            return ss.getLocalPort();
        }
        catch (IOException iOE)
        {
        }
        finally
        {
            if (ss != null)
            {
                try
                {
                    ss.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        return 0;
    }

    public static int getFreePort(InetAddress ipAddress)
    {
        int port = getFreeTCPPort(ipAddress);
        int retry = 20;
        while(retry > 0)
        {
            if (isUDPPortFree(port, ipAddress))
            {
                return port;
            }
            retry -= 1;
        }                
        return 0;
    }
    public static boolean isTCPPortFree(int port, InetAddress addr)
    {
        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(port);
            //ss.setReuseAddress(true);
            return true;
        }
        catch (IOException io)
        {
        }
        finally
        {
            if (ss != null)
            {
                try
                {
                    ss.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        return false;

    }

    public static int getTotalAvailableMemoryInMBs()
    {
        com.sun.management.OperatingSystemMXBean mxbean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();   
        Double size = (mxbean.getTotalPhysicalMemorySize()/(1024*1024))*0.75;        
        return size.intValue();
    }
    public static boolean isUDPPortFree(int port, InetAddress addr)
    {
        DatagramSocket ds = null;
        try
        {
            ds = new DatagramSocket(port, addr);
            //ds.setReuseAddress(true);
            return true;
        }
        catch (IOException io)
        {
        }
        finally
        {
            if (ds != null)
            {
                ds.close();
            }
        }
        return false;
    }

    /**
     *
     * @param port
     * @param addr
     * @return true if port is not being used by any TCP and UDP Socket.
     */
    public static boolean isPortFree(int port, InetAddress addr)
    {
        return isTCPPortFree(port, addr) && isUDPPortFree(port, addr);
    }

    public static java.net.InetAddress getFirstNonLoopbackAddress()
    {
        try
        {
            java.util.Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements())
            {
                NetworkInterface networkdInterface = (NetworkInterface) networkInterfaces.nextElement();
                java.util.Enumeration inetAddresses = networkdInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements())
                {
                    java.net.InetAddress address = (java.net.InetAddress) inetAddresses.nextElement();
                    if (address.isLoopbackAddress())
                    {
                        continue;
                    }
                    if (Common.is(address, java.net.Inet4Address.class))
                    {
                        return address;
                    }
                }
            }
        } catch (IOException iOException) {
        }
        return null;
    }
}
