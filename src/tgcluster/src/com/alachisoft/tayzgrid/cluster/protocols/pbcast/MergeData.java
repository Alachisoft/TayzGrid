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

import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.Serializable;

// $Id: MergeData.java,v 1.3 2004/09/06 13:55:40 belaban Exp $
 
/**
 * Encapsulates data sent with a MERGE_RSP (handleMergeResponse()) and INSTALL_MERGE_VIEW (handleMergeView()).
 *
 *
 * <author> Bela Ban Oct 22 2001 </author>
 */
public class MergeData implements ICompactSerializable, Serializable
{

    public final Address getSender()
    {
        return sender;
    }

    public final View getView()
    {
        return view;
    }

    public final void setView(View value)
    {
        view = value;
    }

    public final Digest getDigest()
    {
        return digest;
    }

    public final void setDigest(Digest value)
    {
        digest = value;
    }
    public Address sender = null;
    public boolean merge_rejected = false;
    public View view = null;
    public Digest digest = null;

    /**
     * Empty constructor needed for externalization
     */
    public MergeData()
    {
    }

    public MergeData(Address sender, View view, Digest digest)
    {
        this.sender = sender;
        this.view = view;
        this.digest = digest;
    }

    @Override
    public boolean equals(Object other)
    {
        return sender != null && other != null && other instanceof MergeData && ((MergeData) other).sender != null && ((MergeData) other).sender.equals(sender);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("sender=").append(sender);
        if (merge_rejected)
        {
            sb.append(" (merge_rejected)");
        }
        else
        {
            sb.append(", view=").append(view).append(", digest=").append(digest);
        }
        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

 
    ///#region ICompactSerializable Members
    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        sender = (Address) reader.readObject();
        merge_rejected = reader.readBoolean();
        if (!merge_rejected)
        {
            view = (View) reader.readObject();
            digest = (Digest) reader.readObject();
        }
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(sender);
        writer.writeBoolean(merge_rejected);
        if (!merge_rejected)
        {
            writer.writeObject(view);
            writer.writeObject(digest);
        }
    }
 
    ///#endregion
}
