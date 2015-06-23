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

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal class used to hold the object as well as eviction and expiration
 * data.
 */
/**
 * CallbackEntry represents an item with callback.
 */
public class CallbackEntry implements ICompactSerializable, Cloneable, java.io.Serializable {

    private Object _value;
    private Object _onAsyncOperationCompleteCallback;
    private Object _onWriteBehindOperationCompletedCallback;
    private BitSet _flag;
    private java.util.List _itemRemovedListener = Collections.synchronizedList(new java.util.ArrayList(2));
    private java.util.List _itemUpdateListener = Collections.synchronizedList(new java.util.ArrayList(2));

    public CallbackEntry() {
    }

    /**
     * Creates a CallBackEntry.
     *
     * @param clientid
     * @param reqId
     * @param value Actual data
     * @param onCacheItemRemovedCallback OnCacheItemRemovedCallback
     * @param onCacheItemUpdateCallback OnCacheItemUpdateCallback
     * @param onAsyncOperationCompleteCallback
     * @param onWriteBehindOperationCompletedCallback
     * @param Flag
     * @param updateDatafilter
     * @param removeDatafilter
     */
    public CallbackEntry(String clientid, int reqId, Object value, short onCacheItemRemovedCallback, short onCacheItemUpdateCallback, short onAsyncOperationCompleteCallback, short onWriteBehindOperationCompletedCallback, BitSet Flag, EventDataFilter updateDatafilter, EventDataFilter removeDatafilter) {
        _value = value;
        _flag = Flag;
        if (onCacheItemUpdateCallback != -1) {
            _itemUpdateListener.add(new CallbackInfo(clientid, onCacheItemUpdateCallback, updateDatafilter));
        }
        if (onCacheItemRemovedCallback != -1) {
            _itemRemovedListener.add(new CallbackInfo(clientid, onCacheItemRemovedCallback, removeDatafilter));
        }
        if (onAsyncOperationCompleteCallback != -1) {
            _onAsyncOperationCompleteCallback = new AsyncCallbackInfo(reqId, clientid, onAsyncOperationCompleteCallback);
        }
        if (onWriteBehindOperationCompletedCallback != -1) {
            _onWriteBehindOperationCompletedCallback = new AsyncCallbackInfo(reqId, clientid, onWriteBehindOperationCompletedCallback);
        }
    }

    /**
     * Creates a CallBackEntry.
     *
     * @param clientid
     * @param reqId
     * @param value Actual data
     * @param onCacheItemRemovedCallback OnCacheItemRemovedCallback
     * @param onCacheItemUpdateCallback OnCacheItemUpdateCallback
     * @param onAsyncOperationCompleteCallback
     * @param updateDatafilter
     * @param removeDatafilter
     */
    public CallbackEntry(String clientid, int reqId, Object value, short onCacheItemRemovedCallback, short onCacheItemUpdateCallback, short onAsyncOperationCompleteCallback, EventDataFilter updateDatafilter, EventDataFilter removeDatafilter) { //, short onWriteBehindOperationCompletedCallback
        _value = value;
        if (onCacheItemUpdateCallback != -1) {
            _itemUpdateListener.add(new CallbackInfo(clientid, onCacheItemUpdateCallback, updateDatafilter));
        }
        if (onCacheItemRemovedCallback != -1) {
            _itemRemovedListener.add(new CallbackInfo(clientid, onCacheItemRemovedCallback, removeDatafilter));
        }
        if (onAsyncOperationCompleteCallback != -1) {
            _onAsyncOperationCompleteCallback = new AsyncCallbackInfo(reqId, clientid, onAsyncOperationCompleteCallback);
        }
    }

    /**
     * Creates a CallBackEntry.
     *
     * @param value Actual data
     * @param onCacheItemRemovedCallback OnCacheItemRemovedCallback
     * @param onCacheItemUpdateCallback OnCacheItemUpdateCallback
     * @param onAsyncOperationCompleteCallback
     * @param onWriteBehindOperationCompletedCallback
     */
    public CallbackEntry(Object value, CallbackInfo onCacheItemRemovedCallback, CallbackInfo onCacheItemUpdateCallback, AsyncCallbackInfo onAsyncOperationCompleteCallback, AsyncCallbackInfo onWriteBehindOperationCompletedCallback) {
        _value = value;
        if (onCacheItemRemovedCallback != null) {
            _itemRemovedListener.add(onCacheItemRemovedCallback);
        }
        if (onCacheItemUpdateCallback != null) {
            _itemUpdateListener.add(onCacheItemUpdateCallback);
        }

        _onAsyncOperationCompleteCallback = onAsyncOperationCompleteCallback;
        _onWriteBehindOperationCompletedCallback = onWriteBehindOperationCompletedCallback;
    }

    public final void AddItemRemoveCallback(String clientid, Object callback, EventDataFilter datafilter) {
        AddItemRemoveCallback(new CallbackInfo(clientid, callback, datafilter));
    }

    public final void AddItemRemoveCallback(CallbackInfo cbInfo) {
        if (_itemRemovedListener != null && !_itemRemovedListener.contains(cbInfo)) {
            _itemRemovedListener.add(cbInfo);
        }
    }

    public final void RemoveItemRemoveCallback(CallbackInfo cbInfo) {
        if (_itemRemovedListener != null && _itemRemovedListener.contains(cbInfo)) {
            _itemRemovedListener.remove(cbInfo);
        }
    }

    public final void AddItemUpdateCallback(String clientid, Object callback, EventDataFilter datafilter) {
        AddItemUpdateCallback(new CallbackInfo(clientid, callback, datafilter));
    }

    public final void AddItemUpdateCallback(CallbackInfo cbInfo) {
        if (_itemUpdateListener != null && !_itemUpdateListener.contains(cbInfo)) {
            _itemUpdateListener.add(cbInfo);
        }
    }

    public final void RemoveItemUpdateCallback(CallbackInfo cbInfo) {
        if (_itemUpdateListener != null && _itemUpdateListener.contains(cbInfo)) {
            _itemUpdateListener.remove(cbInfo);
        }
    }

    public final java.util.List getItemUpdateCallbackListener() {
        return _itemUpdateListener;
    }

    public final java.util.List getItemRemoveCallbackListener() {
        return _itemRemovedListener;
    }

    /**
     * Gets Caller id i.e. Client application id
     */
    /**
     * Gets/Sets the actual object.
     * @return 
     */
    public final Object getValue() {
        return _value;
    }

    public final void setValue(Object value) {
        _value = value;
    }

    public final Object[] getUserData() {
        Object[] userData = null;
        if (_value != null) {
            userData = ((UserBinaryObject) _value).getData();
        }
        return userData;
    }

    public final BitSet getFlag() {
        return _flag;
    }

    public final void setFlag(BitSet value) {
        _flag = value;
    }

    public final Object getAsyncOperationCompleteCallback() {
        return _onAsyncOperationCompleteCallback;
    }

    public final void setAsyncOperationCompleteCallback(Object value) {
        _onAsyncOperationCompleteCallback = value;
    }

    public final Object getWriteBehindOperationCompletedCallback() {
        return _onWriteBehindOperationCompletedCallback;
    }

    public final void setWriteBehindOperationCompletedCallback(Object value) {
        _onWriteBehindOperationCompletedCallback = value;
    }

    /**
     * Deserializes the CallbackEntry.
     *
     * @param reader
     * @throws java.lang.ClassNotFoundException
     * @throws java.io.IOException
     */
    @Override
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        _value = reader.readObject();
        Object tempVar = reader.readObject();
        _flag = (BitSet) ((tempVar instanceof BitSet) ? tempVar : null);
        int len = reader.readInt();
        Object item;
        for (int i = 0; i < len; i++) {
            item = reader.readObject();
            _itemUpdateListener.add(item);
        }
        len = reader.readInt();
        for (int i = 0; i < len; i++) {
            item = reader.readObject();
            _itemRemovedListener.add(item);
        }
        _onAsyncOperationCompleteCallback = reader.readObject();
        _onWriteBehindOperationCompletedCallback = reader.readObject();
    }

    /**
     * Serializes the CallbackEntry
     *
     * @param writer
     * @throws java.io.IOException
     */
    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeObject(_value);
        writer.writeObject(_flag);
        synchronized (_itemUpdateListener) {
            int len = _itemUpdateListener.size();
            writer.writeInt(len);
            Object[] obj = _itemUpdateListener.toArray();
            for (int i = 0; i < obj.length; i++) {
                writer.writeObject(obj[i]);
            }
        }
        synchronized (_itemRemovedListener) {
            int len = _itemRemovedListener.size();
            writer.writeInt(len);
            Object[] obj = _itemRemovedListener.toArray();
            for (int i = 0; i < obj.length; i++) {
                writer.writeObject(obj[i]);
            }
        }
        writer.writeObject(_onAsyncOperationCompleteCallback);
        writer.writeObject(_onWriteBehindOperationCompletedCallback);
    }

    public final Object clone() {
        CallbackEntry cloned = new CallbackEntry();
        cloned._flag = this.getFlag() != null? (BitSet) this._flag.Clone():null;
        cloned._value = this._value;

        List list = Collections.synchronizedList(new ArrayList(2));

        //Clone Too coslty
        cloned._itemRemovedListener = _itemRemovedListener;
        cloned._itemUpdateListener = _itemUpdateListener;

        cloned._onAsyncOperationCompleteCallback = this._onAsyncOperationCompleteCallback;
        cloned._onWriteBehindOperationCompletedCallback = this._onWriteBehindOperationCompletedCallback;
        return cloned;
    }
}
