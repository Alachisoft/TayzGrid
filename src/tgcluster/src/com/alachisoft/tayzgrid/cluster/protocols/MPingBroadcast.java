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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;


public class MPingBroadcast
{

    private TCPPING enclosingInstance;
    private MulticastSocket mcast_send_sock;
    public int ip_ttl = 32;
    private Address mcast_addr;
    private int buff_size = 64000;

    public MPingBroadcast(TCPPING enclosingInstance)
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

           

            mcast_send_sock = new MulticastSocket(new InetSocketAddress(mcast_addr.getIpAddress(), mcast_addr.getPort()));//(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            mcast_send_sock.setTimeToLive(ip_ttl);
            mcast_send_sock.bind(null);
 
        }
        catch (Exception ex)
        {
            throw ex;
        }finally{

            setBufferSizes();
        }
    }

    public final void send(Message msg) throws IOException
    {
        msg.setSrc(getEnclosing_Instance().local_addr);
 
        byte[] buf = messageToBuffer(msg);
        if (mcast_send_sock != null)
        {
            mcast_send_sock.send(new DatagramPacket(buf, buf.length));
        }
    }

    public final boolean start()
    {
        try
        {
            createSockets();
        }
        catch (Exception e)
        {
            enclosingInstance.getStack().getCacheLog().Error("MPingBroadcast.start()", e.toString());
            return false;
        }
        if (enclosingInstance.getStack().getCacheLog().getIsInfoEnabled())
        {
            enclosingInstance.getStack().getCacheLog().Info("MPingBroadcast.Start()", " multicast sockets created successfully");
        }
        return true;
    }

    public final void stop()
    {
        if (mcast_send_sock != null)
        {
            mcast_send_sock.close();
            mcast_send_sock = null;
        }
    }

 
    private byte[] messageToBuffer(Message msg)
    {
 
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try
        {
            s.write(Version.version_id, 0, Version.version_id.length);
 
            byte[] buffer = s.toByteArray();


            return buffer;
        }
        finally
        {
            if(s != null){
                try{
                    s.close();
                }catch(Exception e){/*do nothing*/}
            }
        }
    }

    private void setBufferSizes()
    {
        if (mcast_send_sock != null)
        {
            try
            {
                mcast_send_sock.setSendBufferSize(buff_size);
                 
            }
            catch (Exception ex)
            {
                enclosingInstance.getStack().getCacheLog().Warn("failed setting mcast_send_buf_size in mcast_send_sock: " + ex);
            }

            try
            {
                mcast_send_sock.setReceiveBufferSize(buff_size);
                 
            }
            catch (Exception ex)
            {
                enclosingInstance.getStack().getCacheLog().Warn("failed setting mcast_recv_buf_size in mcast_send_sock: " + ex);
            }
        }
    }
}
