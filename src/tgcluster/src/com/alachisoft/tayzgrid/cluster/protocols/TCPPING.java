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

import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.stack.ProtocolStackType;
import com.alachisoft.tayzgrid.cluster.protocols.pbcast.Digest;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.ThreadClass;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

// $Id: TOTAL.java,v 1.6 2004/07/05 14:17:16 belaban Exp $
 
/**
 * The TCPPING protocol layer retrieves the initial membership (used by the GMS when started by sending event FIND_INITIAL_MBRS down the stack). We do this by mcasting TCPPING
 * requests to an IP MCAST address (or, if gossiping is enabled, by contacting the router). The responses should allow us to determine the coordinator whom we have to contact, e.g.
 * in case we want to join the group. When we are a server (after having received the BECOME_SERVER event), we'll respond to TCPPING requests with a TCPPING response.<p> The
 * FIND_INITIAL_MBRS event will eventually be answered with a FIND_INITIAL_MBRS_OK event up the stack.
 *
 * <author> Bela Ban </author>
 */
public class TCPPING extends Protocol
{

    @Override
    public String getName()
    {
        return "TCPPING";
    }
    public java.util.List members = Collections.synchronizedList(new java.util.ArrayList(10)), initial_members = Collections.synchronizedList(new java.util.ArrayList(10));
   
    public Address local_addr = null;
    public String group_addr = null;
    public String subGroup_addr = null;
    public String groupname = null;
    public long timeout = 5000;
    public long num_initial_members = 20;
    public boolean twoPhaseConnect;
 
    public int port_range = 1; // number of ports to be probed for initial membership
    public ThreadClass mcast_receiver = null;
    public String discovery_addr = "228.8.8.8";
    public int discovery_port = 7700;
    public int tcpServerPort = 7500;
 
    public MPingBroadcast broadcaster = null;
    public MPingReceiver receiver = null;
    public boolean hasStarted = false;
 
    public boolean isStatic = false;
    public int startPort;
    /**
     * These two values are used to authorize a user so that he can not join to other nodes where he is not allowed to.
     *
     *
     */
 
    private static final String DEFAULT_USERID = "Ncache-Default-UserId";
    private static final String DEFAULT_PASSWORD = "Ncache-Default-Password";
    public String userId = DEFAULT_USERID;
    public String password = DEFAULT_PASSWORD;
 
    public byte[] secureUid = null;
 
    public byte[] securePwd = null;
 
    /**
     * List<Address>
     */
    public java.util.ArrayList initial_hosts = null; // hosts to be contacted for the initial membership
    public boolean is_server = false;
    private boolean mbrDiscoveryInProcess = false;
    

 
 
    @Override
    public boolean setProperties(java.util.HashMap props) throws Exception
    {
        super.setProperties(props);

        if (stack.getStackType() == ProtocolStackType.TCP)
        {
            this.up_thread = false;
            this.down_thread = false;
            if (getStack().getCacheLog().getIsInfoEnabled())
            {
                getStack().getCacheLog().Info(getName() + ".setProperties", "part of TCP stack");
            }

        }
        if (props.containsKey("timeout"))
        { // max time to wait for initial members
            timeout = Long.decode((String)props.get("timeout"));
            props.remove("timeout");
        }

        if (props.containsKey("port_range"))
        { // if member cannot be contacted on base port,
            // how many times can we increment the port
            port_range = Integer.decode((String)props.get("port_range"));
            if (port_range < 1)
            {
                port_range = 1;
            }
            props.remove("port_range");
        }
        if (props.containsKey("static"))
        {
            isStatic = Boolean.parseBoolean((String)props.get("static"));
            props.remove("static");
        }
        
        if (props.containsKey("start_port"))
        {
            startPort = Integer.decode((String)props.get("start_port"));
            props.remove("start_port");
        }
        if (props.containsKey("num_initial_members"))
        { // wait for at most n members
            num_initial_members = Integer.decode((String)props.get("num_initial_members"));
            props.remove("num_initial_members");
        }

        
        if (props.containsKey("initial_hosts"))
        {
            String tmp = (String) props.get("initial_hosts");
            if (tmp != null && tmp.length() > 0)
            {
                initial_hosts = createInitialHosts(String.valueOf(tmp));
            }
            if (initial_hosts != null)
            {
                if (num_initial_members != initial_hosts.size())
                {
                    num_initial_members = initial_hosts.size();
                }
            }
            if (num_initial_members > 5)
            {
                //Taim:We estimate the time for finding initital members
                //for every member we add 1 sec timeout.
                long temp = num_initial_members - 5;
                timeout += (temp * 1000);
            }
            props.remove("initial_hosts");
        }
        if (props.containsKey("discovery_addr"))
        {
            discovery_addr = String.valueOf(props.get("discovery_addr"));

            if (discovery_addr != null && discovery_addr.length() > 0)
            {
                isStatic = false;
            }
            else
            {
                isStatic = true;
            }

            props.remove("discovery_addr");
        }

        if (props.containsKey("discovery_port"))
        {
            discovery_port = Integer.decode((String)props.get("discovery_port"));
            props.remove("discovery_port");
        }

        if (props.size() > 0)
        {
            
            return true;
        }
        return true;
    }
 

    public final boolean getIsStatic()
    {
        return isStatic;
    }
 

    @Override
    public void up(Event evt)
    {
        Message msg, rsp_msg;
        Object obj;
        PingHeader hdr, rsp_hdr;
        PingRsp rsp;
        Address coord;
         

        switch (evt.getType())
        {

            case Event.MSG:
                msg = (Message) evt.getArg();

                obj = msg.getHeader(HeaderType.TCPPING);
                if (obj == null || !(obj instanceof PingHeader))
                {
                    passUp(evt);
                    return;
                }

                hdr = (PingHeader) msg.removeHeader(HeaderType.TCPPING);

                switch (hdr.type)
                {

                    case PingHeader.GET_MBRS_REQ: // return Rsp(local_addr, coord)
 
                        if (!hdr.group_addr.equals(group_addr))
                        {
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TcpPing.up()", "GET_MBRS_REQ from different group , so discarded");
                            }
                            return;
                        }
                        Address src = (Address) hdr.arg;
                        msg.setSrc(src);

                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("TCPPING.up()", "GET_MBRS_REQ from " + msg.getSrc().toString());
                        }
 
              
                        boolean authorized = true;
 

                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("TCPPING.up()", " before authorizing. I have received these credentials user-id = " + userId + ", password = " + password);
                        }


                        if (!authorized)
                        {
                            rsp_msg = new Message(msg.getSrc(), null, null);
                            rsp_hdr = new PingHeader(PingHeader.GET_MBRS_RSP, new PingRsp(null, null, false, true));
                            rsp_msg.putHeader(HeaderType.TCPPING, rsp_hdr);
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.up()", "responding to GET_MBRS_REQ back to " + msg.getSrc().toString() + " with empty response");
                            }
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.up()", "some un-authorized user has tried to connect to the cluster");
                            }
                            passDown(new Event(Event.MSG, rsp_msg, Priority.Critical));
                        }
                        else
                        {
 
                            synchronized (members)
                            {
                                coord = members.size() > 0 ? (Address) members.get(0) : local_addr;
                            }
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.up()", "my coordinator is " + coord.toString());
                            }

                            rsp_msg = new Message(msg.getSrc(), null, null);
                            rsp_hdr = new PingHeader(PingHeader.GET_MBRS_RSP, new PingRsp(local_addr, coord, getStack().getIsOperational(), getStack().getIsOperational()));
                            rsp_msg.putHeader(HeaderType.TCPPING, rsp_hdr);
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.up()", "responding to GET_MBRS_REQ back to " + msg.getSrc().toString());
                            }
 
                            passDown(new Event(Event.MSG, rsp_msg, Priority.Critical));
 
                        }
                        return;
                    case PingHeader.GET_MBRS_RSP: // add response to vector and notify waiting thread
                       
                        if (getStack().getCacheLog().getIsInfoEnabled())
                        {
                            getStack().getCacheLog().Info("TCPPING.up()", "GET_MBRS_RSP from " + msg.getSrc().toString());
                        }
                        rsp = (PingRsp) hdr.arg;
 
                        if (rsp.getOwnAddress() == null && rsp.getCoordAddress() == null && rsp.getHasJoined() == false)
                        {
                            synchronized (initial_members)
                            {
                                if (getStack().getCacheLog().getIsInfoEnabled())
                                {
                                    getStack().getCacheLog().Info("TCPPING.up()", "I am not authorized to join to " + msg.getSrc().toString());
                                }
                                Monitor.pulse(initial_members);// initial_members.notify();
                            }
                        }
                        else
                        {
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.up()", "Before Adding initial members response");
                            }
                            synchronized (initial_members)
                            {
                                if (getStack().getCacheLog().getIsInfoEnabled())
                                {
                                    getStack().getCacheLog().Info("TCPPING.up()", "Adding initial members response");
                                }
                                if (!initial_members.contains(rsp))
                                {
                                    initial_members.add(rsp);
                                    if (getStack().getCacheLog().getIsInfoEnabled())
                                    {
                                        getStack().getCacheLog().Info("TCPPING.up()", "Adding initial members response for " + rsp.getOwnAddress());
                                    }
                                }
                                else
                                {
                                    if (getStack().getCacheLog().getIsInfoEnabled())
                                    {
                                        getStack().getCacheLog().Info("TcpPing.up()", "response already received");
                                    }
                                }
                                Monitor.pulse(initial_members);//initial_members.notify();
                            }
                        }
                       
                        return;


                    default:
                        getStack().getCacheLog().Warn("got TCPPING header with unknown type (" + hdr.type + ')');
                        return;

                }
        



            case Event.SET_LOCAL_ADDRESS:
                passUp(evt);
                local_addr = (Address) evt.getArg();
                // Add own address to initial_hosts if not present: we must always be able to ping ourself !
                if (initial_hosts != null && local_addr != null)
                {
                    if (!initial_hosts.contains(local_addr))
                    {
                        getStack().getCacheLog().Debug("[SET_LOCAL_ADDRESS]: adding my own address (" + local_addr + ") to initial_hosts; initial_hosts="
                                + Global.CollectionToString(initial_hosts));
                        initial_hosts.add(local_addr);
                    }
                }
                break;
            case Event.CONNECT_OK: //'s code
                obj = evt.getArg();

                if (obj != null && obj instanceof Address)
                {
                    tcpServerPort = ((Address) obj).getPort();
                }
                passUp(evt);
                break;
            case Event.CONNECTION_NOT_OPENED:
                if (mbrDiscoveryInProcess) {
                    Address node = Common.as(evt.getArg(), Address.class);
                    PingRsp response = new PingRsp(node, node, true, false);
                    synchronized(initial_members)
                    {
                        initial_members.add(response);
                        Monitor.pulse(initial_members);
                    }
                    getStack().getCacheLog().CriticalInfo(getName() + ".up", "connection failure with " + node);
                }
                break;
            // end services
            default:
                passUp(evt); // Pass up to the layer above us
                break;

        }
    }
     

    @Override
    public void down(Event evt)
    {
        Message msg;
        long time_to_wait, start_time;
        

        switch (evt.getType())
        {
            case Event.FIND_INITIAL_MBRS: // sent by GMS layer, pass up a GET_MBRS_OK event
                //We pass this event down to tcp so that it can take some measures.
                passDown(evt);
                initial_members.clear();
                msg = new Message(null, null, null);
 
                msg.putHeader(HeaderType.TCPPING, new PingHeader(PingHeader.GET_MBRS_REQ, (Object) local_addr, group_addr, secureUid, securePwd));
                mbrDiscoveryInProcess= true;
                synchronized (members)
                {
                    if (initial_hosts != null)
                    {
                        for (java.util.Iterator it = initial_hosts.iterator(); it.hasNext();)
                        {
                            Address addr = (Address) it.next();
                            msg.setDest(addr);
                            if (getStack().getCacheLog().getIsInfoEnabled())
                            {
                                getStack().getCacheLog().Info("TCPPING.down()", "[FIND_INITIAL_MBRS] sending PING request to " + msg.getDest());
                            }
                            passDown(new Event(Event.MSG_URGENT, msg.copy(), Priority.Critical));
                        }
                    }
                }

                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TcpPing.down()", "[FIND_INITIAL_MBRS] waiting for results...............");
                }
                synchronized (initial_members)
                {
                    start_time = System.currentTimeMillis();
                    time_to_wait = timeout;
                    while (initial_members.size() < num_initial_members && time_to_wait > 0)
                    {
                        try
                        {
                            if (getStack().getCacheLog().getIsErrorEnabled())
                            {
                                getStack().getCacheLog().Info("TcpPing.down()", "initial_members Count: " + initial_members.size() + "initialHosts Count: " + num_initial_members);
                            }
                            if (getStack().getCacheLog().getIsErrorEnabled())
                            {
                                getStack().getCacheLog().Info("TcpPing.down()", "Time to wait for next response: " + time_to_wait);
                            }
                            ///initial members will be pulsed in case connection is not available.
                            ///so here we dont have to wait till each member is timed out.
                            ///this significantly improves time for initial member discovery. 
                            boolean timeExpire = Monitor.wait(initial_members,time_to_wait);//initial_members.wait(time_to_wait);
                        }
                        catch (Exception e)
                        {
                            getStack().getCacheLog().Error("TCPPing.down(FIND_INITIAL_MBRS)", e.toString());
                        }
                        time_to_wait = timeout - (System.currentTimeMillis() - start_time);
                    }
                    mbrDiscoveryInProcess = false;
                }
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TcpPing.down()", "[FIND_INITIAL_MBRS] initial members are " + Global.CollectionToString(initial_members));
                }
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TcpPing.down()", "[FIND_INITIAL_MBRS] initial members count " + initial_members.size());
                }

                for (int i = initial_members.size() - 1; i >= 0; i--)
                {
                    PingRsp rsp = (PingRsp) ((initial_members.get(i) instanceof PingRsp) ? initial_members.get(i) : null);
                    if (!rsp.getIsStarted())
                    {
                        initial_members.remove(i);
                    }
                }

                // 3. Send response
                passUp(new Event(Event.FIND_INITIAL_MBRS_OK, initial_members));
                break;


            case Event.TMP_VIEW:
            case Event.VIEW_CHANGE:
                java.util.List tmp;
                if ((tmp = ((View) evt.getArg()).getMembers()) != null)
                {
                    synchronized (members)
                    {
                        members.clear();
                        members.addAll(tmp);
                    }
                }
                passDown(evt);
                break;
            /**
             * **************************After removal of NackAck ********************************
             */
            //TCPPING emulates a GET_DIGEST call, which is required by GMS. This is needed
            //since we have now removed NAKACK from the stack!
            case Event.GET_DIGEST:
                Digest digest = new Digest(members.size());
                for (int i = 0; i < members.size(); i++)
                {
                    Address sender = (Address) members.get(i);
                    digest.add(sender, 0, 0);
                }
                passUp(new Event(Event.GET_DIGEST_OK, digest));
                return;

            case Event.SET_DIGEST:
                // Not needed! Just here to let you know that it is needed by GMS!
                return;
            /**
             * *****************************************************************************
             */
            case Event.BECOME_SERVER: // called after client has joined and is fully working group member
                if (getStack().getCacheLog().getIsInfoEnabled())
                {
                    getStack().getCacheLog().Info("TcpPing.down()", "received BECOME_SERVER event");
                }
                passDown(evt);
                is_server = true;
                break;


            case Event.CONNECT:
                Object[] addrs = ((Object[]) evt.getArg());
                group_addr = (String) addrs[0];
                subGroup_addr = (String) addrs[1];
                
                twoPhaseConnect = (Boolean) addrs[3];
                if (twoPhaseConnect)
                {
                    timeout = 1000;
                }
                passDown(evt);
                break;


            case Event.DISCONNECT:
                passDown(evt);
                break;
 
            case Event.HAS_STARTED:
                hasStarted = true;
                passDown(evt);
                break;
 
            default:
                passDown(evt); // Pass on to the layer below us
                break;

        }
    }

    @Override
    public java.util.List providedUpServices()
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(6));
        retval.add((int) Event.FIND_INITIAL_MBRS);
        retval.add((int) Event.GET_DIGEST);
        retval.add((int) Event.SET_DIGEST);
        return retval;
    }


    /*
     * -------------------------- Private methods ----------------------------
     */
    /**
     * Input is "daddy[8880],sindhu[8880],camille[5555]. Return List of IpAddresses</summary>
     */
    private java.util.ArrayList createInitialHosts(String l) throws Exception
    {
        Global.Tokenizer tok = new Global.Tokenizer(l, ",");
        String t;
        Address addr;
        int port;
        java.util.ArrayList retval = new java.util.ArrayList();
        java.util.HashMap hosts = new java.util.HashMap();
 
        int j = 0;
 
        //to be removed later on
        while (tok.hasNext())
        {
            try
            {
                t = tok.next();
 
                String host = t.substring(0, (t.indexOf((char) '[')) - (0));
                host = host.trim();
 
                //port = Integer.parseInt(t.substring(t.indexOf((char) '[') + 1, t.indexOf((char) '[') + 1 + (t.indexOf((char) ']')) - (t.indexOf((char) '[') + 1)));
                hosts.put(host, startPort);
 
                j++;
 

            }
            catch (NumberFormatException e)
            {
                getStack().getCacheLog().Error("TCPPING.createInitialHosts" , "exeption is " + e.toString());
            }
            catch (Exception e)
            {
                getStack().getCacheLog().Error("TcpPing.createInitialHosts", "Error: " + e.toString());
                throw new Exception("Invalid initial members list");
            }
        }
        try
        {
            Iterator ide;
            for (int i = 0; i < port_range; i++)
            {
                ide = hosts.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry ent =  (Map.Entry)ide.next();
                    port = (Integer) ent.getValue();
                    
                    port = port + (i * PortCalculator.PORT_JUMP);
                    
                    addr = new Address((String) ent.getKey(), port);
                    retval.add(addr);
                }

            }
        }
        catch (Exception ex)
        {
            getStack().getCacheLog().Error("TcpPing.CreateInitialHosts()", "Error :" + ex);
            throw new Exception("Invalid initial memebers list");
        }
        return retval;
    }
}
