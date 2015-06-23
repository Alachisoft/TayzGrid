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

public class KeyValuesContainer
{
	public KeyValuesContainer()
	{
		_valuesHT = new java.util.Hashtable();
	}

	private Object _key;
	public final Object getKey()
	{
		return _key;
	}
	public final void setKey(Object value)
	{
		_key = value;
	}

	//Hashtable containing attribute-value pair of current key
	private java.util.Hashtable _valuesHT;
	public final java.util.Hashtable getValues()
	{
		return _valuesHT;
	}

	public final int getCount()
	{
		return _valuesHT.size();
	}
}
