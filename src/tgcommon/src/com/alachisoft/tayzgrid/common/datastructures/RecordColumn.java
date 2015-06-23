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

import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class RecordColumn implements ICompactSerializable
{
	private String _name;
	private boolean _isHidden;
	private ColumnType _columnType = ColumnType.AttributeColumn;
	private ColumnDataType _dataType = ColumnDataType.Object;
	private AggregateFunctionType _aggregateFunctionType = getAggregateFunctionType().NOTAPPLICABLE;

	//rowID-Object
	private java.util.HashMap<Integer, Object> _data;


	public final java.util.HashMap<Integer, Object> getData()
	{
		return _data;
	}

	public RecordColumn(String name, boolean isHidden)
	{
		_name = name;
		_isHidden = isHidden;
		_data = new java.util.HashMap<Integer, Object>();
	}
	//data-type

	public final String getName()
	{
		return _name;
	}

	public final boolean getIsHidden()
	{
		return _isHidden;
	}
	public final void setIsHidden(boolean value)
	{
		_isHidden = value;
	}

	public final ColumnType getType()
	{
		return _columnType;
	}
	public final void setType(ColumnType value)
	{
		_columnType = value;
	}

	public final ColumnDataType getDataType()
	{
		return _dataType;
	}
	public final void setDataType(ColumnDataType value)
	{
		_dataType = value;
	}

	public final AggregateFunctionType getAggregateFunctionType()
	{
		return _aggregateFunctionType;
	}
	public final void setAggregateFunctionType(AggregateFunctionType value)
	{
		_aggregateFunctionType = value;
	}

	public final void Add(Object value, int rowID)
	{
		_data.put(rowID, value);
	}

	public final Object Get(int rowID)
	{
		return _data.get(rowID);
	}

    @Override
    public void serialize(CacheObjectOutput out) throws IOException {
        out.writeObject(_name);
	out.writeBoolean(_isHidden);
	out.write(_columnType.getValue());
	out.write(_data.size());
	for (java.util.Map.Entry<Integer, Object> de : _data.entrySet())
	{
		out.write(de.getKey());
		out.writeObject(de.getValue());
	}
    }

    @Override
    public void deserialize(CacheObjectInput in) throws IOException, ClassNotFoundException {
        Object tempVar = in.readObject();
	_name = (String)((tempVar instanceof String) ? tempVar : null);
	_isHidden = in.readBoolean();
	_columnType = ColumnType.forValue(in.readInt());

	int count = in.readInt();
	for (int i = 0; i < count; i++)
	{
		_data.put(in.readInt(), in.readObject());
	}
    }

}
