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

package com.alachisoft.tayzgrid.common.locking;


import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

/**
 LockManager is responsible for maintaining locks for cache items.
*/
public class LockManager implements ICompactSerializable {

	private static class LockHandle implements ICompactSerializable {
		private String _lockId;
		private java.util.Date _lockTime = new java.util.Date(0);

		public LockHandle(String lockId) {
			_lockId = lockId;
			_lockTime = new java.util.Date();
		}

		public final String getLockId() {
			return _lockId;
		}

		public final java.util.Date getLockTime() {
			return _lockTime;
		}

		@Override
		public boolean equals(Object obj) {
			LockHandle other = (LockHandle)((obj instanceof LockHandle) ? obj : null);
			if (other != null && other._lockId.equals(_lockId)) {
				return true;
			} else if (obj instanceof String) {
				return _lockId.equals((String)obj);
			} else {
				return false;
			}
		}



            @Override
		public final void deserialize(CacheObjectInput reader)throws ClassNotFoundException,IOException {
			Object tempVar = reader.readObject();
			_lockId = (String)((tempVar instanceof String) ? tempVar : null);
		}

            @Override
		public final void serialize(CacheObjectOutput writer)throws IOException {
			writer.writeObject(_lockId);
		}

	}


	private java.util.ArrayList<LockHandle> _readerLocks = new java.util.ArrayList<LockHandle>();
	private LockHandle _writerLock;
	private LockMode _lockMode = LockMode.None;

	private String GenerateLockId() {

                    return UUID.randomUUID().toString() +
                            new NCDateTime(Calendar.getInstance().getTimeInMillis()).getTicks();
	}

	/**
	 Gets the current locking mode.
	*/
	public final LockMode getMode() {
		return _lockMode;
	}
	/**
	 Acquires the reader lock for a cache item. Reader lock is assigned
	 only if no writer lock exists. Multiple reader locks can be acquired.

	 @return Unique lock id
	*/
	public final boolean AcquireReaderLock(String lockHandle) {
		synchronized (this) {
			if (_lockMode == LockMode.None || _lockMode == LockMode.Reader) {
				_readerLocks.add(new LockHandle(lockHandle));
				_lockMode = LockMode.Reader;
				return true;
			}
		}
		return false;
	}

	/**
	 Releases a reader lock on a cache item.

	 @param lockId
	*/
	public final void ReleaseReaderLock(String lockId) {
		if (lockId == null) {
			return;
		}
		synchronized (this) {
			if (_lockMode == LockMode.Reader) {
				if (_readerLocks.contains(new LockHandle(lockId))) {
					_readerLocks.remove(new LockHandle(lockId));
					if (_readerLocks.isEmpty()) {
						_lockMode = LockMode.None;
					}
				}
			}
		}
	}

	/**
	 Acquires the writer lock on a cache item. Writer lock is acquired only if
	 no reader or wirter lock exists on the item.

	 @return Lockid against which lock is acquired.
	*/
	public final boolean AcquireWriterLock(String lockHandle) {
		synchronized (this) {
			if (_lockMode == LockMode.None) {
				_writerLock = new LockHandle(lockHandle);
				_lockMode = LockMode.Write;
				return true;
			}
		}
		return false;
	}

	/**
	 Releases a writer lock on the cache item.

	 @param lockId
	*/
	public final void ReleaseWriterLock(String lockId) {
		synchronized (this) {
			if (_lockMode == LockMode.Write) {
				if (_writerLock.equals(lockId)) {
					_writerLock = null;
					_lockMode = LockMode.None;
				}
			}
		}
	}

	/**
	 Validates whether a valid lock is acquired by a lock holder.

	 @param mode Locking mode.
	 @param lockId LockId for which lock is to be validated.
	 @return Returns true if a lock holder still holds lock on the item.
	*/
	public final boolean ValidateLock(LockMode mode, String lockId) {
		if (lockId == null && mode != LockMode.None) {
			return false;
		}

		synchronized (this) {
			switch (mode) {
				case Reader:
					return _readerLocks.contains(new LockHandle(lockId));

				case Write:
					return _writerLock.equals(lockId);

				case None:
					return true;
			}
		}
		return false;
	}

	public final boolean ValidateLock(String lockId) {
		return ValidateLock(_lockMode, lockId);
	}



    @Override
	public final void deserialize(CacheObjectInput reader)throws ClassNotFoundException , IOException {
		_lockMode = LockMode.forValue(reader.readByte());
		Object tempVar = reader.readObject();
		String writerLockId = (String)((tempVar instanceof String) ? tempVar : null);
		if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(writerLockId)) {
			_writerLock = new LockHandle(writerLockId);
		}

		int readLockCount = reader.readInt();
		_readerLocks = new java.util.ArrayList<LockHandle>();
                Object tempVar2 = null;
		for (int i = 0; i < readLockCount; i++){
			tempVar2 = reader.readObject();
			_readerLocks.add(new LockHandle((String)((tempVar2 instanceof String) ? tempVar2 : null)));
			}
	}

    @Override
	public final void serialize(CacheObjectOutput writer)throws IOException {
		writer.write((byte)_lockMode.getValue());

		if (_writerLock != null) {
			writer.writeObject(_writerLock.getLockId());
		} else {
			writer.writeObject(null);
		}

		writer.writeInt(_readerLocks.size());
		for (LockHandle handle : _readerLocks) {
			writer.writeObject(handle.getLockId());
		}
	}

}
