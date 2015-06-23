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

package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

/**
 * Each key based on the hashcode belongs to a bucket. This class keeps the overall stats for the bucket and points to its owner.
 */
public class HashMapBucket implements ICompactSerializable, Cloneable
{

    private int _bucketId;
    private Address _tempAddress;
    private Address _permanentAddress;
    private Latch _stateTxfrLatch = new Latch(BucketStatus.Functional);
    private Object _status_wait_mutex = new Object();

    public HashMapBucket(Address address, int id)
    {
        _tempAddress = _permanentAddress = address;
        _bucketId = id;
        _stateTxfrLatch = new Latch(BucketStatus.Functional);
    }

    public HashMapBucket(Address address, int id, byte status)
    {
        this(address, id);
        setStatus(status);
    }

    public final int getBucketId()
    {
        return _bucketId;
    }

    public final Address getTempAddress()
    {
        return _tempAddress;
    }

    public final void setTempAddress(Address value)
    {
        _tempAddress = value;
    }

    public final Address getPermanentAddress()
    {
        return _permanentAddress;
    }

    public final void setPermanentAddress(Address value)
    {
        _permanentAddress = value;
    }

    public final void WaitForStatus(Address tmpOwner, byte status) throws InterruptedException
    {
        if (tmpOwner != null)
        {

            while (tmpOwner == _tempAddress)
            {
                if (_stateTxfrLatch.IsAnyBitsSet(status))
                {
                    return;
                }
                synchronized (_status_wait_mutex)
                {
                    if ((tmpOwner == _tempAddress) || _stateTxfrLatch.IsAnyBitsSet(status))
                    {
                        return;
                    }
                    //: Interrupt exception thrown (added)
                    Monitor.wait(_status_wait_mutex); //_status_wait_mutex.wait();
                }
            }
        }
    }

    public final void NotifyBucketUpdate()
    {
        synchronized (_status_wait_mutex)
        {
            Monitor.pulse(_status_wait_mutex); //_status_wait_mutex.notifyAll();
        }
    }

    /**
     * Sets the status of the bucket. A bucket can have any of the following status 1- Functional 2- UnderStateTxfr 3- NeedStateTransfer.
     * @return 
     */
    public final byte getStatus()
    {
        return _stateTxfrLatch.getStatus().getData();
    }

    public final void setStatus(byte value)
    {
        switch (value)
        {
            case BucketStatus.Functional:
            case BucketStatus.NeedTransfer:
            case BucketStatus.UnderStateTxfr:
                //these are valid status,we allow them to be set.
                byte oldStatus = _stateTxfrLatch.getStatus().getData();
                if (oldStatus == value)
                {
                    return;
                }
                _stateTxfrLatch.SetStatusBit(value, oldStatus);
                break;
        }
    }

    public final Latch getStateTxfrLatch()
    {
        return _stateTxfrLatch;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof HashMapBucket)
        {
            return this.getBucketId() == ((HashMapBucket) obj).getBucketId();
        }
        return false;
    }

    @Override
    public final Object clone()
    {
        HashMapBucket hmBucket = new HashMapBucket(_permanentAddress, _bucketId);
        hmBucket.setTempAddress(_tempAddress);
        hmBucket.setStatus(getStatus());
        return hmBucket;
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _bucketId = reader.readInt();
        _tempAddress = (Address) reader.readObject();
        _permanentAddress = (Address) reader.readObject();
        byte status = reader.readByte();
        _stateTxfrLatch = new Latch(status);

    }


    /**
     *
     * @deprecated used only for ICompactSerializable
     */
    @Deprecated
    public HashMapBucket()
    {
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeInt(_bucketId);
        writer.writeObject(_tempAddress);
        writer.writeObject(_permanentAddress);
        writer.writeByte(_stateTxfrLatch.getStatus().getData());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Bucket[").append(_bucketId).append(" ; ");
        sb.append("owner = ").append(_permanentAddress).append(" ; ");
        sb.append("temp = ").append(_tempAddress).append(" ; ");
        String status = null;
        //object can be zero object(initialization without default values), which may cause exception.
        if (_stateTxfrLatch != null) {
            status = BucketStatus.StatusToString(_stateTxfrLatch.getStatus().getData());
        }
        sb.append(status).append(" ]");
        return sb.toString();

    }
}
