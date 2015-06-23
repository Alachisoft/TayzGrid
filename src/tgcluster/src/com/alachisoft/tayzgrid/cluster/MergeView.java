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
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

// $Id: MergeView.java,v 1.1.1.1 2003/09/09 01:24:08 belaban Exp $

public class MergeView extends View implements Cloneable, Serializable
{

    public java.util.List getSubgroups()
    {
        return subgroups;
    }
    protected java.util.List subgroups = null; // subgroups that merged into this single view (a list of Views)

    /**
     * Used by externalization
     */
    public MergeView()
    {
    }

    /**
     * Creates a new view
     *
     * @param vid The view id of this view (can not be null)
     *
     * @param members Contains a list of all the members in the view, can be empty but not null.
     *
     * @param subgroups A list of Views representing the former subgroups
     *
     */
    public MergeView(ViewId vid, java.util.List members, java.util.List subgroups)
    {
        super(vid, members);
        this.subgroups = subgroups;
    }

    /**
     * Creates a new view
     *
     * @param creator The creator of this view (can not be null)
     *
     * @param id The lamport timestamp of this view
     *
     * @param members Contains a list of all the members in the view, can be empty but not null.
     *
     * @param subgroups A list of Views representing the former subgroups
     *
     */
    public MergeView(Address creator, long id, java.util.List members, java.util.List subgroups)
    {
        super(creator, id, members);
        this.subgroups = subgroups;
    }

    /**
     * creates a copy of this view
     *
     * @return a copy of this view
     *
     */
    @Override
    public Object clone()
    {
        ViewId vid2 = getVid() != null ? (ViewId) getVid().clone() : null;
        java.util.List members2 = getMembers() != null ? (java.util.List) GenericCopier.DeepCopy(getMembers())/*.clone()*/ : null;
        java.util.List subgroups2 = subgroups != null ? (java.util.List) GenericCopier.DeepCopy(subgroups)/*.clone()*/ : null;
        return new MergeView(vid2, members2, subgroups2);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MergeView::").append(super.toString());
        sb.append(", subgroups=").append(Global.CollectionToString(subgroups));
        return sb.toString();
    }


    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        super.deserialize(reader);
        subgroups = (java.util.ArrayList) reader.readObject();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        super.serialize(writer);
        writer.writeObject(subgroups);
    }

}
