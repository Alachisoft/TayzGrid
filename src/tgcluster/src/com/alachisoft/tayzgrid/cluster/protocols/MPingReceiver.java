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

package com.alachisoft.tayzgrid.cluster.protocols;

import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Version;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;

public class MPingReceiver implements Runnable
{

    private TCPPING enclosingInstance;
    private MulticastSocket mcast_recv_sock;
    public int ip_ttl = 32;
    private Address mcast_addr;
    private int buff_size = 64000;
    private boolean discard_incompatible_packets = true;
    public Thread mcast_receiver = null;

    public MPingReceiver(TCPPING enclosingInstance)
    {
        this.enclosingInstance = enclosingInstance;
    }

    public final TCPPING getEnclosing_Instance()
    {
        return enclosingInstance;
    }

    private void createSockets() throws UnknownHostException, Exception
    {
        InetAddress tmp_addr = null;
        tmp_addr = Address.Resolve(getEnclosing_Instance().discovery_addr);
        mcast_addr = new Address(tmp_addr, getEnclosing_Instance().discovery_port);

        try
        {
            
            mcast_recv_sock = new MulticastSocket();//(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            mcast_recv_sock.setTimeToLive(ip_ttl);
            mcast_recv_sock.bind(new InetSocketAddress(mcast_addr.getIpAddress(), mcast_addr.getPort()));
            mcast_recv_sock.setReuseAddress(true);
            mcast_recv_sock.joinGroup(mcast_addr.getIpAddress());
            
        }
        catch (Exception ex)
        {
            throw ex;
        }finally{
            setBufferSizes();
        }
    }

    private void setBufferSizes()
    {
        if (mcast_recv_sock != null)
        {
            try
            {
                mcast_recv_sock.setSendBufferSize(buff_size);
                
            }
            catch (Exception ex)
            {
                enclosingInstance.getStack().getCacheLog().Warn("failed setting mcast_send_buf_size in mcast_recv_sock: " + ex);
            }

            try
            {
                mcast_recv_sock.setReceiveBufferSize(buff_size);
               
            }
            catch (Exception ex)
            {
                enclosingInstance.getStack().getCacheLog().Warn("failed setting mcast_recv_buf_size in mcast_recv_sock: " + ex);
            }
        }
    }

 
    public  void run()
    {
        int len = 0;
 
        byte[] packet = new byte[buff_size];
       
        InetSocketAddress remoteIpEndPoint = new InetSocketAddress(0);

        while (mcast_receiver != null && mcast_recv_sock != null)
        {
            try
            {
                tangible.RefObject<InetSocketAddress> tempRef_remoteIpEndPoint = new tangible.RefObject<InetSocketAddress>(remoteIpEndPoint);
                mcast_recv_sock.receive(new DatagramPacket(packet, packet.length));
                remoteIpEndPoint = tempRef_remoteIpEndPoint.argvalue;

                if (packet[0] == 0)
                {
                    if (enclosingInstance.getStack().getCacheLog().getIsInfoEnabled())
                    {
                        enclosingInstance.getStack().getCacheLog().Info("UDP.Run()", "received dummy packet");
                    }
                    continue;
                }

                if (len > packet.length)
                {
                    enclosingInstance.getStack().getCacheLog().Error("UDP.Run()", "size of the received packet (" + len + ") is bigger than " + "allocated buffer ("
                            + packet.length + "): will not be able to handle packet. " + "Use the FRAG protocol and make its frag_size lower than " + packet.length);
                }

                if (Version.compareTo(packet) == false)
                {
                    if (discard_incompatible_packets)
                    {
                        continue;
                    }
                }

                handleIncomingPacket(packet, len);
            }
            catch (SocketException sock_ex)
            {
                enclosingInstance.getStack().getCacheLog().Error("MPingReceiver.Run()", "multicast socket is closed, exception=" + sock_ex);
                break;
            }
            catch (IOException ex)
            {
                enclosingInstance.getStack().getCacheLog().Error("MPingReceiver.Run()", "exception=" + ex);
              
            }
            catch (Exception ex)
            {
              
            }
        }
    }

 
    public final boolean start()
    {
        try
        {
            createSockets();
            if (mcast_receiver == null)
            {
                mcast_receiver = new Thread(this);
                mcast_receiver.setDaemon(true);
                mcast_receiver.start();
            }
        }
        catch (Exception e)
        {
            enclosingInstance.getStack().getCacheLog().Error("MPingReceiver.start()", e.toString());
            return (false);
        }
        return (true);
    }

    public final void stop()
    {
        mcast_receiver = null;
        mcast_recv_sock.close();
        mcast_recv_sock = null;
    }
 

    private void handleIncomingPacket(byte[] data, int dataLen)
    {
        ByteArrayOutputStream inp_stream;
        Message msg = null;

        try
        {
            inp_stream = new ByteArrayOutputStream();
            inp_stream.write(data, Version.getLength(), dataLen - Version.getLength());
       
        }
        catch (Exception e)
        {
            enclosingInstance.getStack().getCacheLog().Error("MpingReceiver.handleIncomingPacket()", "exception=" + e.toString() + "\n");
        }
    }

    
}
 
