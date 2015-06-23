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

import com.alachisoft.tayzgrid.common.datastructures.RecordSet;

class QueryCacheReader implements ICacheReader
{
	private int _currentRow = -1;
	private RecordSet _recordSet;

	public QueryCacheReader(RecordSet recordSet)
	{
		_recordSet = recordSet;
	}

	public final int getColumnCount()
	{
		return _recordSet.getColumnCount() - _recordSet.getHiddenColumnCount();
	}

	public final void Close()
	{
		_recordSet = null;
	}

	public final Object getItem(int index)
	{
		return this.GetValue(index);
	}

	public final Object getItem(String columnName)
	{
		return this.GetValue(columnName);
	}

	public final boolean Read()
	{
		return ++_currentRow < _recordSet.getRowCount() ? true : false;
	}

	public final boolean GetBool(int index)
	{
		return Boolean.parseBoolean(this.GetValue(index).toString());
	}

	public final String GetString(int index)
	{
		return String.valueOf(this.GetValue(index));
	}

	public final java.math.BigDecimal GetDecimal(int index)
	{
		return new java.math.BigDecimal(this.GetValue(index).toString());
	}

	public final double GetDouble(int index)
	{
		return Double.parseDouble(this.GetValue(index).toString());
	}

	public final short GetInt16(int index)
	{
		return Short.parseShort(this.GetValue(index).toString());
	}

	public final int GetInt32(int index)
	{
		return Integer.parseInt(this.GetValue(index).toString());
	}

	public final long GetInt64(int index)
	{
		return Long.parseLong(this.GetValue(index).toString());
	}

	public final Object GetValue(int index)
	{
		Object obj = _recordSet.GetObject(_currentRow, index);
		if (!_recordSet.GetIsHiddenColumn(index))
		{
			if (obj instanceof com.alachisoft.tayzgrid.common.queries.AverageResult)
			{
				return ((com.alachisoft.tayzgrid.common.queries.AverageResult)obj).getAverage();
			}
			else
			{
				return obj;
			}
		}
		else
		{
			throw new IndexOutOfBoundsException();
		}
	}

	public final Object GetValue(String columnName)
	{
		Object obj = _recordSet.GetObject(_currentRow, columnName);
		if (!_recordSet.GetIsHiddenColumn(columnName))
		{
			if (obj instanceof com.alachisoft.tayzgrid.common.queries.AverageResult)
			{
				return ((com.alachisoft.tayzgrid.common.queries.AverageResult)obj).getAverage();
			}
			else
			{
				return obj;
			}
		}
		else
		{
			throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
		}
	}

	public final int GetValues(Object[] objects)
	{
            int index = 0;
            for (int i = 0; i < this.getColumnCount(); i++) {
                objects[index++] = this.getValue(i);
            }
            return index;
	}
        
        private final Object getValue(int index)
        {
            Object obj = _recordSet.GetObject(_currentRow, index);
            if (!_recordSet.GetIsHiddenColumn(index)) {
                if (obj instanceof com.alachisoft.tayzgrid.common.queries.AverageResult) {
                    return ((com.alachisoft.tayzgrid.common.queries.AverageResult) obj).getAverage();
                } else {
                    return obj;
                }
            }
            return null;
        }

	public final String GetName(int index)
	{
            if(_recordSet.GetIsHiddenColumn(index))
                throw new IllegalArgumentException("Invalid index. Specified index does not exist in RecordSet.");
            return _recordSet.GetColumnName(index);
	}

	public final int GetIndex(String columnName)
	{
            if(_recordSet.GetIsHiddenColumn(columnName))
                throw new IllegalArgumentException("Invalid column name. Specified column does not exist in RecordSet.");
            return _recordSet.getColumnIndex(columnName);
	}
}
