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

package com.alachisoft.tayzgrid.cluster.util;

import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Collections;

// $Id: RspList.java,v 1.3 2004/03/30 06:47:28 belaban Exp $
 
public class RspList
{

    public final Object getFirst()
    {
        return rsps.size() > 0 ? ((Rsp) rsps.get(0)).getValue() : null;
    }

    /**
     * Returns the results from non-suspected members that are not null.
     */
    public final java.util.List getResults()
    {
        java.util.List ret = Collections.synchronizedList (new java.util.ArrayList(10));
        Rsp rsp;
        Object val;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);
            if (rsp.wasReceived() && (val = rsp.getValue()) != null)
            {
                ret.add(val);
            }
        }
        return ret;
    }

    public final java.util.List getSuspectedMembers()
    {
        java.util.List retval = Collections.synchronizedList (new java.util.ArrayList(10));
        Rsp rsp;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);
            if (rsp.wasSuspected())
            {
                retval.add(rsp.getSender());
            }
        }
        return retval;
    }

    public java.util.List rsps = (java.util.List) Collections.synchronizedList (new java.util.ArrayList(10));


    public final void reset()
    {
        rsps.clear();
    }

 

    public final void addRsp(Address sender, Object retval)
    {
        Rsp rsp = find(sender);

        if (rsp != null)
        {
            rsp.sender = sender;
            rsp.retval = retval;
            rsp.received = true;
            rsp.suspected = false;
            return;
        }
        rsp = new Rsp(sender, retval);
        rsps.add(rsp);
    }
 

    public final void addNotReceived(Address sender)
    {
        Rsp rsp = find(sender);

        if (rsp == null)
        {
            rsps.add(new Rsp(sender));
        }
    }

    public final void addSuspect(Address sender)
    {
        Rsp rsp = find(sender);

        if (rsp != null)
        {
            rsp.sender = sender;
            rsp.retval = null;
            rsp.received = false;
            rsp.suspected = true;
            return;
        }
        rsps.add(new Rsp(sender, true));
    }

    public final boolean isReceived(Address sender)
    {
        Rsp rsp = find(sender);

        if (rsp == null)
        {
            return false;
        }
        return rsp.received;
    }

    public final int numSuspectedMembers()
    {
        int num = 0;
        Rsp rsp;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);
            if (rsp.wasSuspected())
            {
                num++;
            }
        }
        return num;
    }

    public final boolean isSuspected(Address sender)
    {
        Rsp rsp = find(sender);

        if (rsp == null)
        {
            return false;
        }
        return rsp.suspected;
    }

    public final Object get(Address sender)
    {
        Rsp rsp = find(sender);

        if (rsp == null)
        {
            return null;
        }
        return rsp.retval;
    }

    public final int size()
    {
        return rsps.size();
    }

    public final Object elementAt(int i)
    {
        return rsps.get(i);
    }

    public final void removeElementAt(int i)
    {
        rsps.remove(i);
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        Rsp rsp;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);
            ret.append("[" + rsp + "]\n");
        }
        return ret.toString();
    }

    private boolean contains(Address sender)
    {
        Rsp rsp;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);

            if (rsp.sender != null && sender != null && rsp.sender.equals(sender))
            {
                return true;
            }
        }
        return false;
    }

    public final Rsp find(Address sender)
    {
        Rsp rsp;

        for (int i = 0; i < rsps.size(); i++)
        {
            rsp = (Rsp) rsps.get(i);
            if (rsp.sender != null && sender != null && rsp.sender.equals(sender))
            {
                return rsp;
            }
        }
        return null;
    }
}
