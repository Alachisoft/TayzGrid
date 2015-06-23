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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.binaryprotocol;

 
public enum BinaryResponseStatus 
{
	no_error((short)0x0000),

	key_not_found((short)0x0001),

	key_exists((short)0x0002),

	value_too_large((short)0x0003),

	invalid_arguments((short)0x0004),

	item_not_stored((short)0x0005),

	incr_decr_on_nonnumeric_value((short)0x0006),

	unknown_commnad((short)0x0081),

	out_of_memory((short)0x0082);

	private short intValue;
	private static java.util.HashMap<Short, BinaryResponseStatus> mappings;
	private static java.util.HashMap<Short, BinaryResponseStatus> getMappings()
	{
		if (mappings == null)
		{
			synchronized (BinaryResponseStatus.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Short, BinaryResponseStatus>();
				}
			}
		}
		return mappings;
	}

	private BinaryResponseStatus(short value)
	{
		intValue = value;
		BinaryResponseStatus.getMappings().put(value, this);
	}

	public short getValue()
	{
		return intValue;
	}

	public static BinaryResponseStatus forValue(short value)
	{
		return getMappings().get(value);
	}
}