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
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class JoinRsp implements ICompactSerializable
{

    public JoinRsp()
    {
    }

    public final View getView()
    {
        return view;
    }

    public final Digest getDigest()
    {
        return digest;
    }

    public final JoinResult getJoinResult()
    {
        return joinResult;
    }

    public final void setJoinResult(JoinResult value)
    {
        joinResult = value;
    }
    private View view = null;
    private Digest digest = null;
    private JoinResult joinResult = JoinResult.Success;

    public JoinRsp(View v, Digest d)
    {
        view = v;
        digest = d;
    }

    public JoinRsp(View v, Digest d, JoinResult result)
    {
        view = v;
        digest = d;
        joinResult = result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("view: ");
        if (view == null)
        {
            sb.append("<null>");
        }
        else
        {
            sb.append(view);
        }
        sb.append(", digest: ");
        if (digest == null)
        {
            sb.append("<null>");
        }
        else
        {
            sb.append(digest);
        }
        sb.append(", join result: ");
        switch (joinResult)
        {
            case Success:
                sb.append("success");
                break;
            case MaxMbrLimitReached:
                sb.append("more than 2 nodes can not join the cluster");
                break;
            case HandleJoinInProgress:
                sb.append("Handle Join called");
                break;
            case HandleLeaveInProgress:
                sb.append("Handle Join called");
                break;
            case MembershipChangeAlreadyInProgress:
                sb.append("Membership Change Already In Progress");
                break;

        }
        return sb.toString();
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        Object tempVar = reader.readObject();
        view = (View) ((tempVar instanceof View) ? tempVar : null);
        Object tempVar2 = reader.readObject();
        digest = (Digest) ((tempVar2 instanceof Digest) ? tempVar2 : null);
        joinResult = (JoinResult) reader.readObject();
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(view);
        writer.writeObject(digest);
        writer.writeObject(joinResult);
    }
    //</editor-fold>
}
