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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Membership
{

    /**
     * List of current members
     */
    private java.util.List members = null;

    /**
     * Constructor: Initialises with no initial members
     */
    public Membership()
    {
        members = Collections.synchronizedList(new java.util.ArrayList(11));

    }

    /**
     * Constructor: Initialises with the specified initial members
     *
     * @param initial_members Initial members of the membership
     */
    public Membership(java.util.List initial_members)
    {
        members = Collections.synchronizedList(new java.util.ArrayList());
        if (initial_members != null)
        {
            members = (java.util.List) GenericCopier.DeepCopy(initial_members);//.clone();
        }

    }

    /**
     * returns a copy (clone) of the members in this membership. the vector returned is immutable in reference to this object. ie, modifying the vector that is being returned in
     * this method will not modify this membership object.
     *
     *
     * @return a list of members,
     *
     */
    public java.util.List getMembers()
    {
        /*
         * clone so that this objects members can not be manipulated from the outside
         */
        return (List) GenericCopier.DeepCopy(members);//.clone();
    }

    /**
     * Sets the members to the specified list
     *
     * @param membrs The current members
     */
    public final void setMembers(java.util.List membrs)
    {
        members = membrs;
    }

    /**
     * If the member already exists then the member will not be added to the membership
     *
     *
     * Adds a new member to this membership.
     *
     * @param new_member
     */
    public final void add(Address new_member)
    {
        if (new_member != null && !members.contains(new_member))
        {
            members.add(new_member);
        }
    }

    /**
     * Adds a number of members to this membership
     *
     * @param v
     */
    public final void add(java.util.List v)
    {
        if (v != null)
        {
            for (int i = 0; i < v.size(); i++)
            {
                add((Address) v.get(i));
            }
        }
    }

    /**
     * Removes the specified member
     *
     * @param old_member Member that has left the group
     */
    public final void remove(Address old_member)
    {
        if (old_member != null)
        {
            members.remove(old_member);
        }
    }

    /**
     * merges membership with the new members and removes suspects The Merge method will remove all the suspects and add in the new members. It will do it in the order 1. Remove
     * suspects 2. Add new members the order is very important to notice.
     *
     *
     * @param new_mems - a vector containing a list of members (Address) to be added to this membership
     *
     * @param suspects - a vector containing a list of members (Address) to be removed from this membership
     *
     */
    public void merge(java.util.List new_mems, java.util.ArrayList suspects)
    {
        synchronized (this)
        {
            remove(suspects);
            add(new_mems);
        }
    }

    /*
     * Simple inefficient bubble sort, but not used very often (only when merging)
     */
    public void sort()
    {
        synchronized (this)
        {
            Collections.sort(members);
        }
    }

    /**
     * Removes a number of members from the membership
     *
     * @param v
     */
    public final void remove(java.util.List v)
    {
        if (v != null)
        {
            for (int i = 0; i < v.size(); i++)
            {
                remove((Address) v.get(i));
            }
        }
    }

    /**
     * Removes all members
     */
    public final void clear()
    {
        members.clear();
    }

    /**
     * Sets the membership to the members present in the list
     *
     * @param v New list of members
     */
    public final void set(java.util.List v)
    {
        clear();
        if (v != null)
        {
            add(v);
        }
    }

    /**
     * Sets the membership to the specified membership
     *
     * @param m New membership
     */
    public final void set(Membership m)
    {
        clear();
        if (m != null)
        {
            add(m.getMembers());
        }
    }

    /**
     * Returns true if the provided member belongs to this membership
     *
     * @param member Member to check
     * @return True if the provided member belongs to this membership, otherwise false
     */
    public final boolean contains(Address member)
    {
        if (member == null)
        {
            return false;
        }
        return members.contains(member);
    }

    /**
     * Returns a copy of this membership.
     *
     * @return A copy of this membership
     */
    public final Membership copy()
    {
        return (Membership) this.clone();
    }

    /**
     * Determines the seniority between two given nodes. Seniority is based on the joining time. If n1 has joined before than n2, n1 will be considered senior.
     *
     * @param n1 node 1
     * @param n2 node 2
     * @return senior node
     */
    public final Address DetermineSeniority(Address n1, Address n2)
    {
        int indexofn1 = members.indexOf(n1);
        int indexofn2 = members.indexOf(n2);

        if (indexofn1 == -1)
        {
            indexofn1 = Integer.MAX_VALUE;
        }
        if (indexofn2 == -1)
        {
            indexofn2 = Integer.MAX_VALUE;
        }

        //smaller the index of a node means that this node has joined first.
        return indexofn1 <= indexofn2 ? n1 : n2;
    }

    /**
     * Clones the membership
     *
     * @return A clone of the membership
     */
    @Override
    public final Object clone()
    {
        Membership m;
        m = new Membership();
        m.setMembers((java.util.List) GenericCopier.DeepCopy(members));//.clone());
        return (m);
    }

    /**
     * The number of members in the membership
     *
     * @return Number of members in the membership
     */
    public final int size()
    {
        return members.size();
    }

    /**
     * Gets a member at a specified index
     *
     * @param index Index of member
     * @return Address of member
     */
    public final Address elementAt(int index)
    {
        if (index < members.size())
        {
            return (Address) members.get(index);
        }
        else
        {
            return null;
        }
    }

    /**
     * String representation of the Membership object
     *
     * @return String representation of the Membership object
     */
    @Override
    public final String toString()
    {
        return members.toString();
    }

    //@UH
    @Override
    public boolean equals(Object obj)
    {
        boolean equal = true;
        Membership membership = (Membership) ((obj instanceof Membership) ? obj : null);
        if (membership != null && this.size() == membership.size())
        {
            for (Iterator it = membership.members.iterator(); it.hasNext();)
            {
                Address address = (Address) it.next();
                if (!this.contains(address))
                {
                    equal = false;
                    break;
                }
            }
        }
        else
        {
            equal = false;
        }
        return equal;
    }

    public final boolean ContainsIP(Address address)
    {
        if (address == null)
        {
            return false;
        }
        boolean contains = false;
        for (Iterator it = members.iterator(); it.hasNext();)
        {
            Address add = (Address)it.next();
            if (add.getIpAddress().equals(address.getIpAddress()))
            {
                contains = true;
            }
        }
        return contains;
    }
}
