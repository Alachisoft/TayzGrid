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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class CallbackInfo implements InternalCompactSerializable, java.io.Serializable {

    protected String theClient;
    protected Object theCallback;
    protected boolean notifyOnItemExpiration = true;

    protected EventDataFilter _dataFilter = EventDataFilter.None;

    @Deprecated
    public CallbackInfo() {
    }

    @Deprecated
    public CallbackInfo(String client, Object callback) {
        this(client, callback, true);

    }

    @Deprecated
    public CallbackInfo(String client, Object callback, boolean notifyOnItemExpiration) {
        this(client, callback, EventDataFilter.None, notifyOnItemExpiration);
    }

    public CallbackInfo(String client, Object callback, EventDataFilter datafilter) {
        this(client, callback, datafilter, true);
    }

    public CallbackInfo(String client, Object callback, EventDataFilter datafilter, boolean notifyOnItemExpiration) {
        this.theClient = client;
        this.theCallback = callback;
        this.notifyOnItemExpiration = notifyOnItemExpiration;
        this._dataFilter = datafilter;
    }

    public final EventDataFilter getDataFilter() {
        return _dataFilter;
    }

    public final void setDataFilter(EventDataFilter value) {
        _dataFilter = value;
    }

    /**
     * Gets/sets the client intended to listen for the event
     * @return 
     */
    public final String getClient() {
        return theClient;
    }

    public final void setClient(String value) {
        theClient = value;
    }

    public boolean getNotifyOnExpiration() {
        return notifyOnItemExpiration;
    }

    /**
     * Gets/sets the callback.
     * @return 
     */
    public final Object getCallback() {
        return theCallback;
    }

    public final void setCallback(Object value) {
        theCallback = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CallbackInfo) {
            CallbackInfo other = (CallbackInfo) ((obj instanceof CallbackInfo) ? obj : null);
            if (!other.getClient().equals(theClient)) {
                return false;
            }
            if (other.getCallback() instanceof Short && theCallback instanceof Short) {
                if (!other.getCallback().equals(theCallback)) {
                    return false;
                }
            } else if (other.getCallback().equals(theCallback)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String cnt = theClient != null ? theClient : "NULL";
        String cback = theCallback != null ? theCallback.toString() : "NULL";
        return cnt + ":" + cback;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        theClient = Common.as(reader.ReadObject(), String.class);
        theCallback = Common.as(reader.ReadObject(), Object.class);
        notifyOnItemExpiration = reader.ReadBoolean();
        _dataFilter = EventDataFilter.forValue(reader.ReadInt32());
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        try {
            writer.WriteObject(theClient);
            writer.WriteObject(theCallback);
            writer.Write(notifyOnItemExpiration);
            writer.Write(_dataFilter.getValue());
        } catch (IOException iOException) {
            this.Serialize(writer);
        }
    }
}
