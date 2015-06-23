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

package com.alachisoft.tayzgrid.cluster.protocols.pbcast;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

// $Id: Digest.java,v 1.6 2004/07/05 05:49:41 belaban Exp $
 
/**
 * A message digest, which is used e.g. by the PBCAST layer for gossiping (also used by NAKACK for keeping track of current seqnos for all members). It contains pairs of senders
 * and a range of seqnos (low and high), where each sender is associated with its highest and lowest seqnos seen so far. That is, the lowest seqno which was not yet
 * garbage-collected and the highest that was seen so far and is deliverable (or was already delivered) to the application. A range of [0 - 0] means no messages have been received
 * yet. <p> April 3 2001 (bela): Added high_seqnos_seen member. It is used to disseminate information about the last (highest) message M received from a sender P. Since we might be
 * using a negative acknowledgment message numbering scheme, we would never know if the last message was lost. Therefore we periodically gossip and include the last message seqno.
 * Members who haven't seen it (e.g. because msg was dropped) will request a retransmission. See DESIGN for details.
 *
 * <author> Bela Ban </author>
 */
public class Digest implements ICompactSerializable, Serializable
{

    public Address[] senders = null;
    public long[] low_seqnos = null; // lowest seqnos seen
    public long[] high_seqnos = null; // highest seqnos seen so far *that are deliverable*, initially 0
    public long[] high_seqnos_seen = null; // highest seqnos seen so far (not necessarily deliverable), initially -1
    public int index = 0; // current index of where next member is added

    public Digest()
    {
    } // used for externalization

    public Digest(int size)
    {
        reset(size);
    }

    public final void add(Address sender, long low_seqno, long high_seqno)
    {
        if (index >= senders.length)
        {
            
            return;
        }
        if (sender == null)
        {
            
            return;
        }
        senders[index] = sender;
        low_seqnos[index] = low_seqno;
        high_seqnos[index] = high_seqno;
        high_seqnos_seen[index] = - 1;
        index++;
    }

    public final void add(Address sender, long low_seqno, long high_seqno, long high_seqno_seen)
    {
        if (index >= senders.length)
        {
            
            return;
        }
        if (sender == null)
        {
             
            return;
        }
        senders[index] = sender;
        low_seqnos[index] = low_seqno;
        high_seqnos[index] = high_seqno;
        high_seqnos_seen[index] = high_seqno_seen;
        index++;
    }

    public final void add(Digest d)
    {
        Address sender;
        long low_seqno, high_seqno, high_seqno_seen;

        if (d != null)
        {
            for (int i = 0; i < d.size(); i++)
            {
                sender = d.senderAt(i);
                low_seqno = d.lowSeqnoAt(i);
                high_seqno = d.highSeqnoAt(i);
                high_seqno_seen = d.highSeqnoSeenAt(i);
                add(sender, low_seqno, high_seqno, high_seqno_seen);
            }
        }
    }

    /**
     * Adds a digest to this digest. This digest must have enough space to add the other digest; otherwise an error message will be written. For each sender in the other digest,
     * the merge() method will be called.
     */
    public final void merge(Digest d)
    {
        Address sender;
        long low_seqno, high_seqno, high_seqno_seen;

        if (d == null)
        {
            
            return;
        }
        for (int i = 0; i < d.size(); i++)
        {
            sender = d.senderAt(i);
            low_seqno = d.lowSeqnoAt(i);
            high_seqno = d.highSeqnoAt(i);
            high_seqno_seen = d.highSeqnoSeenAt(i);
            merge(sender, low_seqno, high_seqno, high_seqno_seen);
        }
    }

    /**
     * Similar to add(), but if the sender already exists, its seqnos will be modified (no new entry) as follows: <ol> <li>this.low_seqno=min(this.low_seqno, low_seqno)
     * <li>this.high_seqno=max(this.high_seqno, high_seqno) <li>this.high_seqno_seen=max(this.high_seqno_seen, high_seqno_seen) </ol> If the sender doesn not exist, a new entry
     * will be added (provided there is enough space)
     */
    public final void merge(Address sender, long low_seqno, long high_seqno, long high_seqno_seen)
    {
        int index;
        long my_low_seqno, my_high_seqno, my_high_seqno_seen;
        if (sender == null)
        {
           
            return;
        }
        index = getIndex(sender);
        if (index == - 1)
        {
            add(sender, low_seqno, high_seqno, high_seqno_seen);
            return;
        }

        my_low_seqno = lowSeqnoAt(index);
        my_high_seqno = highSeqnoAt(index);
        my_high_seqno_seen = highSeqnoSeenAt(index);
        if (low_seqno < my_low_seqno)
        {
            setLowSeqnoAt(index, low_seqno);
        }
        if (high_seqno > my_high_seqno)
        {
            setHighSeqnoAt(index, high_seqno);
        }
        if (high_seqno_seen > my_high_seqno_seen)
        {
            setHighSeqnoSeenAt(index, high_seqno_seen);
        }
    }

    public final int getIndex(Address sender)
    {
        int ret = - 1;

        if (sender == null)
        {
            return ret;
        }
        for (int i = 0; i < senders.length; i++)
        {
            if (sender.equals(senders[i]))
            {
                return i;
            }
        }
        return ret;
    }

    public final boolean contains(Address sender)
    {
        return getIndex(sender) != - 1;
    }

    /**
     * Compares two digests and returns true if the senders are the same, otherwise false
     *
     * @param other
     *
     * @return
     *
     */
    public final boolean sameSenders(Digest other)
    {
        Address a1, a2;
        if (other == null)
        {
            return false;
        }
        if (this.senders == null || other.senders == null)
        {
            return false;
        }
        if (this.senders.length != other.senders.length)
        {
            return false;
        }
        for (int i = 0; i < this.senders.length; i++)
        {
            a1 = this.senders[i];
            a2 = other.senders[i];
            if (a1 == null && a2 == null)
            {
                continue;
            }
            if (a1 != null && a2 != null && a1.equals(a2))
            {
                continue;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Increment the sender's high_seqno by 1
     */
    public final void incrementHighSeqno(Address sender)
    {
        if (sender == null)
        {
            return;
        }
        for (int i = 0; i < senders.length; i++)
        {
            if (senders[i] != null && senders[i].equals(sender))
            {
                high_seqnos[i] = high_seqnos[i] + 1;
                break;
            }
        }
    }

    public final int size()
    {
        return senders.length;
    }

    public final Address senderAt(int index)
    {
        if (index < size())
        {
            return senders[index];
        }
        else
        {
             
            return null;
        }
    }

    /**
     * Resets the seqnos for the sender at 'index' to 0. This happens when a member has left the group, but it is still in the digest. Resetting its seqnos ensures that no-one will
     * request a message retransmission from the dead member.
     */
    public final void resetAt(int index)
    {
        if (index < size())
        {
            low_seqnos[index] = 0;
            high_seqnos[index] = 0;
            high_seqnos_seen[index] = - 1;
        }
        
    }

    public final void reset(int size)
    {
        senders = new Address[size];
        low_seqnos = new long[size];
        high_seqnos = new long[size];
        high_seqnos_seen = new long[size];
        for (int i = 0; i < size; i++)
        {
            high_seqnos_seen[i] = - 1;
        }
        index = 0;
    }

    public final long lowSeqnoAt(int index)
    {
        if (index < size())
        {
            return low_seqnos[index];
        }
        else
        {
            
            return 0;
        }
    }

    public final long highSeqnoAt(int index)
    {
        if (index < size())
        {
            return high_seqnos[index];
        }
        else
        {
            
            return 0;
        }
    }

    public final long highSeqnoSeenAt(int index)
    {
        if (index < size())
        {
            return high_seqnos_seen[index];
        }
        else
        {
            
            return 0;
        }
    }

    public final long highSeqnoAt(Address sender)
    {
        long ret = - 1;
        int index;

        if (sender == null)
        {
            return ret;
        }
        index = getIndex(sender);
        if (index == - 1)
        {
            return ret;
        }
        else
        {
            return high_seqnos[index];
        }
    }

    public final long highSeqnoSeenAt(Address sender)
    {
        long ret = - 1;
        int index;

        if (sender == null)
        {
            return ret;
        }
        index = getIndex(sender);
        if (index == - 1)
        {
            return ret;
        }
        else
        {
            return high_seqnos_seen[index];
        }
    }

    public final void setLowSeqnoAt(int index, long low_seqno)
    {
        if (index < size())
        {
            low_seqnos[index] = low_seqno;
        }
        
    }

    public final void setHighSeqnoAt(int index, long high_seqno)
    {
        if (index < size())
        {
            high_seqnos[index] = high_seqno;
        }
       
    }

    public final void setHighSeqnoSeenAt(int index, long high_seqno_seen)
    {
        if (index < size())
        {
            high_seqnos_seen[index] = high_seqno_seen;
        }
        
    }

    public final void setHighSeqnoAt(Address sender, long high_seqno)
    {
        int index = getIndex(sender);
        if (index < 0)
        {
            return;
        }
        else
        {
            setHighSeqnoAt(index, high_seqno);
        }
    }

    public final void setHighSeqnoSeenAt(Address sender, long high_seqno_seen)
    {
        int index = getIndex(sender);
        if (index < 0)
        {
            return;
        }
        else
        {
            setHighSeqnoSeenAt(index, high_seqno_seen);
        }
    }

    public final Digest copy()
    {
        Digest ret = new Digest(senders.length);

        // changed due to JDK bug (didn't work under JDK 1.4.{1,2} under Linux, JGroups bug #791718
        // ret.senders=(Address[])senders.clone();
        if (senders != null)
        {
            System.arraycopy(senders, 0, ret.senders, 0, senders.length);
        }

        ret.low_seqnos = new long[low_seqnos.length];
        System.arraycopy(low_seqnos, 0, ret.low_seqnos, 0, low_seqnos.length);
        ret.high_seqnos = new long[high_seqnos.length];
        System.arraycopy(high_seqnos, 0, ret.high_seqnos, 0, high_seqnos.length);
        ret.high_seqnos_seen = new long[high_seqnos_seen.length];
        System.arraycopy(high_seqnos_seen, 0, ret.high_seqnos_seen, 0, high_seqnos_seen.length);
        return ret;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (senders == null)
        {
            return "[]";
        }
        for (int i = 0; i < senders.length; i++)
        {
            if (!first)
            {
                sb.append(", ");
            }
            else
            {
                sb.append('[');
                first = false;
            }
            sb.append(senders[i]).append(": ").append('[').append(low_seqnos[i]).append(" : ");
            sb.append(high_seqnos[i]);
            if (high_seqnos_seen[i] >= 0)
            {
                sb.append(" (").append(high_seqnos_seen[i]).append(")]");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public final String printHighSeqnos()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < senders.length; i++)
        {
            if (!first)
            {
                sb.append(", ");
            }
            else
            {
                sb.append('[');
                first = false;
            }
            sb.append(senders[i]);
            sb.append('#');
            sb.append(high_seqnos[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public final String printHighSeqnosSeen()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < senders.length; i++)
        {
            if (!first)
            {
                sb.append(", ");
            }
            else
            {
                sb.append('[');
                first = false;
            }
            sb.append(senders[i]);
            sb.append('#');
            sb.append(high_seqnos_seen[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException
    {
        senders = (Address[]) reader.readObject();
        low_seqnos = (long[]) reader.readObject();
        high_seqnos = (long[]) reader.readObject();
        high_seqnos_seen = (long[]) reader.readObject();
        index = reader.readInt();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(senders);
        writer.writeObject(low_seqnos);
        writer.writeObject(high_seqnos);
        writer.writeObject(high_seqnos_seen);
        writer.writeInt(index);
    }
    //</editor-fold>
}
