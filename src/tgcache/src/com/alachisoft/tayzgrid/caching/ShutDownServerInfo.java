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

import com.alachisoft.tayzgrid.common.net.Address;
import java.util.Calendar;

public class ShutDownServerInfo
 {
		private String _uniqueId;
		private Address _blockserverAddress = null;
		private Address _renderedAddress;
		private long _blockinterval;
		private java.util.Date _startShutDown = new java.util.Date();
                long startTime;
		public ShutDownServerInfo()
		{
			_startShutDown = new java.util.Date();
                        startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;
		}

		public final long getBlockInterval()
		{
			long timeout = (int)(_blockinterval * 1000) - (int)((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime);
			return (timeout / 1000);
		}
		public final void setBlockInterval(long value)
		{
			_blockinterval = value;
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
			return _blockserverAddress;
		}
		public final void setBlockServerAddress(Address value)
		{
			_blockserverAddress = value;
		}

		public final Address getRenderedAddress()
		{
			return _renderedAddress;
		}
		public final void setRenderedAddress(Address value)
		{
			_renderedAddress = value;
		}
 }
