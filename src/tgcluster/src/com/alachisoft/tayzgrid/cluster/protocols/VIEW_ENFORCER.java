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

import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.stack.Protocol;
import com.alachisoft.tayzgrid.cluster.stack.ProtocolStackType;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.net.Address;

// $Id: VIEW_ENFORCER.java,v 1.3 2004/04/23 19:36:13 belaban Exp $
 
public class VIEW_ENFORCER extends Protocol
{

    /**
     * All protocol names have to be unique !
     *
     * @return
     */
    @Override
    public String getName()
    {
        return "VIEW_ENFORCER";
    }

    public VIEW_ENFORCER()
    {
        this.up_thread = false;
        this.down_thread = false;
    }
    public Address local_addr = null;
    public boolean is_member = false;

    @Override
    public boolean setProperties(java.util.HashMap props)
    {
        if (stack.getStackType() == ProtocolStackType.TCP)
        {
            this.up_thread = false;
            this.down_thread = false;
            getStack().getCacheLog().Info(getName() + ".setProperties", "part of TCP stack");
        }
        return true;
    }

    @Override
    public void up(final Event evt)
    {

        switch (evt.getType())
        {
            case Event.VIEW_CHANGE:
                if (is_member)
                // pass the view change up if we are already a member of the group
                {
                    break;
                }

                java.util.List new_members = ((View) evt.getArg()).getMembers();
                if (new_members == null || new_members.isEmpty())
                {
                    break;
                }
                if (local_addr == null)
                {
                    getStack().getCacheLog().Error("VIEW_ENFORCER.up(VIEW_CHANGE): local address is null; cannot check "
                            + "whether I'm a member of the group; discarding view change");
                    return;
                }

                if (new_members.contains(local_addr))
                {
                    is_member = true;
                }
                else
                {
                    return;
                }

                break;

            case Event.SET_LOCAL_ADDRESS:
                local_addr = (Address) evt.getArg();
                break;

            case Event.MSG:
                if (!is_member)
                {
                    // drop message if we are not yet member of the group
                    getStack().getCacheLog().Info("dropping message " + evt.getArg());
                    return;
                }
                Object tempVar = evt.getArg();
                Message msg = (Message) ((tempVar instanceof Message) ? tempVar : null);
                if (msg != null && msg.getHandledAysnc())
                {
                    Thread workerThread = new Thread(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            AsyncPassUp(evt);
                        }
                    });
                    workerThread.start();
                     
                    return;
                }
                break;
        }
        passUp(evt); // Pass up to the layer above us
    }

    /**
     * Threadpool calls this method to pass data up the stack.
     *
     * @param evt
     */
    public final void AsyncPassUp(Object evt)
    {
        passUp((Event) evt);
    }
}
