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


package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Calendar;

public class ShutDownServerInfo
{
		private String _uniqueId;
		private Address _blockserverIP = null;
		private long _blockinterval;
		private Object _waitForBlockActivity = new Object();
		private java.util.Date _startBlockingTime = new java.util.Date(0);
                private long startTime;

		public ShutDownServerInfo()
		{
			_startBlockingTime = new java.util.Date();
                        startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;
		}

		public final String getUniqueBlockingId()
		{
			return _uniqueId;
		}
		public final void setUniqueBlockingId(String value)
		{
			_uniqueId = value;
		}

		public final Address getBlockServerAddress()
		{
			return _blockserverIP;
		}
		public final void setBlockServerAddress(Address value)
		{
			_blockserverIP = value;
		}

		public final long getBlockInterval()
		{
			return _blockinterval;
		}
		public final void setBlockInterval(long value)
		{
			_blockinterval = value;
		}

		public final Object getWaitForBlockedActivity()
		{
			return _waitForBlockActivity;
		}
		public final void setWaitForBlockedActivity(Object value)
		{
			_waitForBlockActivity = value;
		}

		public final long getStartBlockingTime()
		{
			return startTime;
		}
		public final void setStartBlockingTime(long value)
		{
			startTime = value;
		}
}
