/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
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

package com.alachisoft.tayzgrid.common;

import com.alachisoft.tayzgrid.common.enums.CacheStatus;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class StatusInfo implements InternalCompactSerializable
{

    private static final String NODE_EXPIRED_MESSAGE = "Your license for using NCache has expired on {0}. Please contact sales@alachisoft.com for further terms and conditions.";
    private static final String NODE_EXPIRED_MESSAGE2 = "Your license for using NCache has expired. Please contact sales@alachisoft.com for further terms and conditions.";
    private String _info = "";
    /**
     * Status of the Cache.
     */
    public CacheStatus Status = CacheStatus.Unavailable;
    public boolean IsCoordinatorInternal = false;
    private double configID;

    /**
     * This property tells whether the node is active node.
     */
    public final boolean getIsCoordinator()
    {
        return IsCoordinatorInternal;
    }

    public final void setIsCoordinator(boolean value)
    {
        IsCoordinatorInternal = value;
    }

    /**
     * Tells the unique sequence number of the last applied configuration. <p>This helps in identifying the inconsistency b/w project file and cache.conf</p>
     */
    public final double getConfigID()
    {
        return configID;
    }

    public final void setConfigID(double value)
    {
        configID = value;
    }

    /**
     * Information about the current status of the cache.
     *
     * @param nodeName The name of the status node. This name is used to format the message string.
     * @return Formated message string.
     */
    public final String Info(String nodeName)
    {
        switch (Status)
        {
            case Expired:
                if (nodeName == null || nodeName.equals(""))
                {
                    _info = NODE_EXPIRED_MESSAGE2;
                }
                else
                {
                    _info = String.format(NODE_EXPIRED_MESSAGE, nodeName);
                }
                break;
            case Registered:
                _info = "Stopped";
                break;
            case Running:
            case Unavailable:
                _info = Status.toString();
                break;
            default:
                _info = "Stopped";
                break;
        }
        return _info;
    }

    public StatusInfo()
    {
        this(CacheStatus.Unavailable);
    }

    public StatusInfo(CacheStatus status)
    {
        this(status, "");
    }

    public StatusInfo(CacheStatus status, String info)
    {
        Status = status;
        _info = info;
    }

    public final boolean getIsRunning()
    {
        return Status == CacheStatus.Running;
    }

    public final boolean getIsUnavailable()
    {
        return Status == CacheStatus.Unavailable;
    }

    public final boolean getIsExpired()
    {
        return Status == CacheStatus.Expired;
    }

    //<editor-fold defaultstate="collapsed" desc="CompactS">
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _info = (String) Common.readAs(reader.ReadObject(),String.class);
        Status = CacheStatus.forValue(reader.ReadInt32());
        IsCoordinatorInternal = reader.ReadBoolean();
        configID = reader.ReadDouble();
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_info);
        //writer.Write((int) Status);
        writer.Write(Status.getValue());
        writer.Write(IsCoordinatorInternal);
        writer.Write(configID);
    }
    //</editor-fold>
}
