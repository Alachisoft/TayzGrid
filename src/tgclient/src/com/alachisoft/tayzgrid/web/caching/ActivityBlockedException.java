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
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import java.io.Serializable;

	public class ActivityBlockedException extends CacheException implements Serializable
	{
		private Address _serverip = null;
		/**  
		 default constructor. 
		*/
		public ActivityBlockedException()
		{
		}

		/**  
		 overloaded constructor, takes the reason as parameter. 
		*/
		public ActivityBlockedException(String reason)
		{
			super(reason);
		}

		/**  
		 overloaded constructor, takes the reason as parameter. 
		*/
		public ActivityBlockedException(String reason, Address blockedServerIp)
		{
			super(reason);
			this._serverip = blockedServerIp;
		}

		/** 
		 overloaded constructor. 
		 
		 @param reason reason for exception
		 @param inner nested exception
		*/
		public ActivityBlockedException(String reason, RuntimeException inner)
		{
			super(reason, inner);
		}

		/** 
		 overloaded constructor. 
		 
		 @param reason reason for exception
		 @param inner nested exception
		*/
		public ActivityBlockedException(String reason, RuntimeException inner, Address blockedServerIp)
		{
			super(reason, inner);
			this._serverip = blockedServerIp;
		}

		/** <exclude/>
		*/
		public final Address getBlockedServerIp()
		{
			return _serverip;
		}

	}
