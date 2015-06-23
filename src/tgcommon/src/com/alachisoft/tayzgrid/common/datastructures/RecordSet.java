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

import com.alachisoft.tayzgrid.common.queries.AverageResult;
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.common.util.JvDateFormatter;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordSet implements ICompactSerializable
{
	/** 
	 Gets <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of object
	 
	 @param obj Object whose <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> is required
	 @return <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of object
	*/
	public static ColumnDataType ToColumnDataType(Object obj)
	{
		if (obj instanceof String)
		{
			return ColumnDataType.String;
		}
		else if (obj instanceof java.math.BigDecimal)
		{
			return ColumnDataType.Decimal;
		}
		else if (obj instanceof Short)
		{
			return ColumnDataType.Int16;
		}
		else if (obj instanceof Integer)
		{
			return ColumnDataType.Int32;
		}
		else if (obj instanceof Long)
		{
			return ColumnDataType.Int64;
		}
		else if (obj instanceof Short)
		{
			return ColumnDataType.UInt16;
		}
		else if (obj instanceof Integer)
		{
			return ColumnDataType.UInt32;
		}
		else if (obj instanceof Long)
		{
			return ColumnDataType.UInt64;
		}
		else if (obj instanceof Double)
		{
			return ColumnDataType.Double;
		}
		else if (obj instanceof Float)
		{
			return ColumnDataType.Float;
		}
		else if (obj instanceof Byte)
		{
			return ColumnDataType.Byte;
		}
		else if (obj instanceof Byte)
		{
			return ColumnDataType.SByte;
		}
		else if (obj instanceof Boolean)
		{
			return ColumnDataType.Bool;
		}
		else if (obj instanceof Character)
		{
			return ColumnDataType.Char;
		}
		else if (obj instanceof java.util.Date)
		{
			return ColumnDataType.DateTime;
		}
		else if (obj instanceof AverageResult)
		{
			return ColumnDataType.AverageResult;
		}
		else
		{
			return ColumnDataType.Object;
		}
	}

	/** 
	 Converts String represenation to appropriate object of specified <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/>
	 
	 @param stringValue String representation of object
	 @param dataType <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of object
	 @return 
	*/
	public static Object ToObject(String stringValue, ColumnDataType dataType)
	{
		switch (dataType)
		{
			case Bool:
				return Boolean.parseBoolean(stringValue);
			case Byte:
				return Byte.parseByte(stringValue);
			case Char:
				return Character.valueOf(stringValue.charAt(0));
			case DateTime:
                            try
                            {
                            java.text.DateFormat formatter=new JvDateFormatter("dd/MM/yyyy/HH/mm/ss/SSSS/ZZZZ");
                            formatter.parse(stringValue);
                            return formatter.getCalendar().getTime();
                            }
                            catch(Exception e)
                            {
                                throw new RuntimeException(e.getMessage(),e);
                            }
			case Decimal:
				return new java.math.BigDecimal(stringValue);
			case Double:
				return Double.parseDouble(stringValue);
			case Float:
				return Float.parseFloat(stringValue);
			case Int16:
				return Short.parseShort(stringValue);
			case Int32:
				return Integer.parseInt(stringValue);
			case Int64:
				return Long.parseLong(stringValue);
			case SByte:
				return Byte.parseByte(stringValue);
			case String:
				return stringValue;
			case UInt16:
				return Short.valueOf(stringValue);
			case UInt32:
				return Integer.valueOf(stringValue);
			case UInt64:
				return Long.valueOf(stringValue);
			default:
                            throw new java.lang.ClassCastException();
		}
	}

	public static String GetString(Object obj, ColumnDataType dataType)
	{
		if (dataType == com.alachisoft.tayzgrid.common.datastructures.ColumnDataType.DateTime)
		{
                    java.text.DateFormat formatter = new JvDateFormatter("dd/MM/yyyy/HH/mm/ss/SSSS/ZZZZ");
                    String str= formatter.format((Date)obj);
                    return str;
		}
		else
		{
			return obj.toString();
		}
	}

	private int _rowCount;
        private int _hiddenColumnCount;
	private java.util.HashMap<Integer, RecordColumn> _dataIntIndex;
	private java.util.HashMap<String, RecordColumn> _dataStringIndex;

	/** 
	 Gets Dictionary containg all column indexes and <see cref="Alachisoft.NCache.Common.DataStructures.RecordColumn"/>
	*/
	public final java.util.HashMap<Integer, RecordColumn> getData()
	{
		return _dataIntIndex;
	}

	/** 
	 Initializes new <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/>
	*/
	public RecordSet()
	{
            _rowCount = 0;
            _hiddenColumnCount = 0;
            _dataIntIndex = new java.util.HashMap<Integer, RecordColumn>();
            _dataStringIndex = new java.util.HashMap<String, RecordColumn>();
	}

	/** 
	 Adds specified value in speicified cell
	 
	 @param value value to be added
	 @param rowID Zero base row index in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	 @param columnID Zero base column index in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/></param>
	*/
	public final void Add(Object value, int rowID, int columnID)
	{
		if (_rowCount <= rowID)
		{
			throw new IllegalArgumentException("Invalid rowID. No of rows in RecordSet are less than specified rowID.");
		}
		if (this.getColumnCount() <= columnID)
		{
			throw new IllegalArgumentException("Invalid columnID. No of columns in RecordSet are less than specified columnID.");
		}
		_dataIntIndex.get(columnID).Add(value, rowID);
	}

	/** 
	 Adds specified value in speicified cell
	 
	 @param value value to be added
	 @param rowID Zero base row index in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	 @param columnName Column name in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/></param>
	*/
	public final void Add(Object value, int rowID, String columnName)
	{
		if (_rowCount <= rowID)
		{
			throw new IllegalArgumentException("Invalid rowID. No of rows in RecordSet are less than specified rowID.");
		}
		if (!_dataStringIndex.containsKey(columnName))
		{
			throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
		}
		_dataStringIndex.get(columnName).Add(value, rowID);
	}

	/** 
	 Adds a new row in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	*/
	public final void AddRow()
	{
		_rowCount++;
	}

	/** 
	 Adds a new column in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	 
	 @param columnName Name of column
	 @param isHidden IsHidden property of column
	 @return fasle if column with same name already exists, true otherwise
	*/
	public final boolean AddColumn(String columnName, boolean isHidden)
	{
		return this.AddColumn(columnName, isHidden, ColumnType.AttributeColumn, AggregateFunctionType.NOTAPPLICABLE);
	}

	/** 
	 Adds a new column in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	 
	 @param columnName Name of column
	 @param isHidden IsHidden property of column
	 @param aggregateFunctionType
	 @return fasle if column with same name already exists, true otherwise
	*/
	public final boolean AddColumn(String columnName, boolean isHidden, AggregateFunctionType aggregateFunctionType)
	{
		return this.AddColumn(columnName, isHidden, ColumnType.AttributeColumn, aggregateFunctionType);
	}

	/** 
	 Adds a new column in <see cref="Alachisoft.NCache.Common.DataStructures.Recordset"/>
	 
	 @param columnName Name of column
	 @param isHidden IsHidden property of column
	 @param columnType
	 @param aggregateFunctionType
	 @return fasle if column with same name already exists, true otherwise
	*/
	public final boolean AddColumn(String columnName, boolean isHidden, ColumnType columnType, AggregateFunctionType aggregateFunctionType)
	{
            if (_dataStringIndex.containsKey(columnName))
            {
                return false;
            }  
            RecordColumn column = new RecordColumn(columnName, isHidden);
            column.setType(columnType);
            column.setAggregateFunctionType(aggregateFunctionType);
            _dataIntIndex.put(this.getColumnCount(), column);
            _dataStringIndex.put(columnName, column);
            if(isHidden)
                _hiddenColumnCount++;
            return true;
	}

	/** 
	 Gets object at specified cell
	 
	 @param rowID Zero based row ID
	 @param columnID Zero based column ID
	 @return Object at specified row and column
	*/
	public final Object GetObject(int rowID, int columnID)
	{
		if (_rowCount <= rowID)
		{
			throw new IllegalArgumentException("Invalid rowID. No of rows in RecordSet are less than specified rowID.");
		}
		if (this.getColumnCount() <= columnID)
		{
			throw new IllegalArgumentException("Invalid columnID. No of columns in RecordSet are less than specified columnID.");
		}
		return _dataIntIndex.get(columnID).Get(rowID);
	}

	/** 
	 Gets object at specified cell
	 
	 @param rowID Zero based row ID
	 @param columnName Name of column
	 @return Object at specified row and column
	*/
	public final Object GetObject(int rowID, String columnName)
	{
		if (_rowCount <= rowID)
		{
			throw new IllegalArgumentException("Invalid rowID. No of rows in RecordSet are less than specified rowID.");
		}
		if (!_dataStringIndex.containsKey(columnName))
		{
			throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
		}
		return _dataStringIndex.get(columnName).Get(rowID);
	}

	/** 
	 Gets column name of specified column ID
	 
	 @param columnID Zero based ID of column
	 @return Name of column
	*/
	public final String GetColumnName(int columnID)
	{
		if (this.getColumnCount() <= columnID)
		{
			throw new IllegalArgumentException("Invalid columnID. No of columns in RecordSet are less than specified columnID.");
		}
		return _dataIntIndex.get(columnID).getName();
	}

        public final int GetColumnIndex(String columnName)
        {
            if(!_dataStringIndex.containsKey(columnName))
            {
                throw new IllegalArgumentException("Invalid column name. Specified column does not exist in RecordSet.");
            }  
            for (int i = 0; i < _dataIntIndex.size(); i++)
            {
                if (_dataIntIndex.get(i).getName() == columnName)
                    return i;
            }
            throw new IllegalArgumentException("Invalid column name. Specified column does not exist in RecordSet.");

        }
        
	/** 
	 Sets IsHidden property of specified column name true
	 
	 @param columnName Name of column
	*/
	public final void HideColumn(String columnName)
	{
            if (_dataStringIndex.containsKey(columnName))
            {
                if(_dataStringIndex.get(columnName).getIsHidden())
                    return;
                else
                    {
                        _dataStringIndex.get(columnName).setIsHidden(true);
                        _hiddenColumnCount++;
                    }
            }                 
	}

	/** 
	 Sets IsHidden property of specified column false
	 
	 @param columnName Name of column
	*/
	public final void UnHideColumn(String columnName)
	{
            if (_dataStringIndex.containsKey(columnName))
            {
                if(!_dataStringIndex.get(columnName).getIsHidden())
                    return;
                else
                    {
                        _dataStringIndex.get(columnName).setIsHidden(false);
                        _hiddenColumnCount--;
                    }
            }   
            else
            {
		throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
            }
	}
        
    public final boolean GetIsHiddenColumn(int columnID) 
    {
        if (this.getColumnCount() <= columnID) {
            throw new IllegalArgumentException("Invalid columnID. No of columns in RecordSet are less than specified columnID.");
        } else {
            return _dataIntIndex.get(columnID).getIsHidden();
        }
    }

    public final boolean GetIsHiddenColumn(String columnName) 
    {
        if (_dataStringIndex.containsKey(columnName)) {
            return _dataStringIndex.get(columnName).getIsHidden();
        } else {
            throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
        }
    }

	/** 
	 Sets <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of specified column
	 
	 @param columnName Name of column
	 @param dataType <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> to set
	*/
	public final void SetColumnDataType(String columnName, ColumnDataType dataType)
	{
		if (_dataStringIndex.containsKey(columnName))
		{
			_dataStringIndex.get(columnName).setDataType(dataType);
		}
		else
		{
			throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
		}
	}

	/** 
	 Sets <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of specified column
	 
	 @param index Zero based index of column
	 @param dataType <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> to set
	*/
	public final void SetColumnDataType(int index, ColumnDataType dataType)
	{
		if (_dataIntIndex.containsKey(index))
		{
			_dataIntIndex.get(index).setDataType(dataType);
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index. Specified column does not exist in RecordSet.");
		}
	}

	/** 
	 Gets <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of specified column
	 
	 @param columnName Name of column
	 @return <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of column
	*/
	public final ColumnDataType GetColumnDataType(String columnName)
	{
		if (_dataStringIndex.containsKey(columnName))
		{
			return _dataStringIndex.get(columnName).getDataType();
		}
		else
		{
			throw new IllegalArgumentException("Invalid columnName. Specified column does not exist in RecordSet.");
		}
	}

	/** 
	 Gets <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of specified column
	 
	 @param index Zero based index of column
	 @return <see cref="Alachisoft.NCache.Common.DataStructures.ColumnDataType"/> of column
	*/
	public final ColumnDataType GetColumnDataType(int index)
	{
		if (_dataIntIndex.containsKey(index))
		{
			return _dataIntIndex.get(index).getDataType();
		}
		else
		{
			throw new IllegalArgumentException("Invalid column index. Specified column does not exist in RecordSet.");
		}
	}

	/** 
	 Sets <see cref="Alachisoft.NCache.Common.DataStructures.AggregateFunctionType"/> of specifeid column
	 
	 @param columnName Name of column
	 @param type <see cref="Alachisoft.NCache.Common.DataStructures.AggregateFunctionType"/> to set
	*/
	public final void SetAggregateFunctionType(String columnName, AggregateFunctionType type)
	{
		_dataStringIndex.get(columnName).setAggregateFunctionType(type);
	}

	/** 
	 Gets <see cref="Alachisoft.NCache.Common.DataStructures.AggregateFunctionType"/> of specified column
	 
	 @param columnName Name of column
	 @return <see cref="Alachisoft.NCache.Common.DataStructures.AggregateFunctionType"/> of column
	*/
	public final AggregateFunctionType GetAggregateFunctionType(String columnName)
	{
		return _dataStringIndex.get(columnName).getAggregateFunctionType();
	}

	/** 
	 Removes last column from <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/>
	*/
	public final void RemoveLastColumn()
	{
		_dataStringIndex.remove(this.GetColumnName(this.getColumnCount() - 1));
                if(_dataIntIndex.get(_dataIntIndex.size()-1).getIsHidden())
                    _hiddenColumnCount--;
		_dataIntIndex.remove(_dataIntIndex.size() - 1);
	}

	/** 
	 Gets number of rows in <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/>
	*/
	public final int getRowCount()
	{
		return _rowCount;
	}

	/** 
	 Gets number of columns in <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/>
	*/
	public final int getColumnCount()
	{
		return _dataStringIndex.size();
	}
        
        public final int getHiddenColumnCount()
        {
            return _hiddenColumnCount;
        }

        public final int getColumnIndex(String columnName)
        {
            if(!_dataStringIndex.containsKey(columnName))
                throw new IllegalArgumentException("Invalid column name. Specified column does not exist in RecordSet.");
            for(int i=0;i<_dataIntIndex.size();i++)
            {
                if(_dataIntIndex.get(i).getName().equals(columnName))
                    return i;
            }
            throw new IllegalArgumentException("Invalid column name. Specified column does not exist in RecordSet.");
        }
        
	/** 
	 Merges <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/>
	 
	 @param recordSet <see cref="Alachisoft.NCache.Common.DataStructures.RecordSet"/> to merge
	*/
	public final void Union(RecordSet recordSet)
	{
		if (this.getColumnCount() != recordSet.getColumnCount())
		{
			throw new UnsupportedOperationException("Cannot compute union of two RecordSet with different number of columns.");
		}

		int thisRowCount = this._rowCount;
		for (int i = 0; i < recordSet.getRowCount(); i++)
		{
			boolean recordMatch = false;

			for (int l = 0; l < thisRowCount; l++)
			{
				boolean rowMatch = true;
				java.util.ArrayList<String> aggregateColumns = new java.util.ArrayList<String>();

				for (java.util.Map.Entry<String, RecordColumn> kvp : _dataStringIndex.entrySet())
				{

					if (kvp.getValue().getType() == ColumnType.AggregateResultColumn)
					{
						aggregateColumns.add(kvp.getKey());
						continue;
					}

					if (recordSet.GetObject(i, kvp.getKey()).equals(this.GetObject(l, kvp.getKey())))
					{
						continue;
					}

					rowMatch = false;
					break;
				}

				if (rowMatch)
				{
					//Rows matched, merging aggregate result columns
					for (String column : aggregateColumns)
					{
						switch (this.GetAggregateFunctionType(column))
						{
							case SUM:
								java.math.BigDecimal a = new java.math.BigDecimal(0);
								java.math.BigDecimal b = new java.math.BigDecimal(0);

								Object thisVal = this.GetObject(i, column);
								Object otherVal = recordSet.GetObject(i, column);

								java.math.BigDecimal sum = null;

								if (thisVal == null && otherVal != null)
								{
									sum = (java.math.BigDecimal)otherVal;
								}
								else if (thisVal != null && otherVal == null)
								{
									sum = (java.math.BigDecimal)thisVal;
								}
								else if (thisVal != null && otherVal != null)
								{
									a = (java.math.BigDecimal)thisVal;
									b = (java.math.BigDecimal)otherVal;
									sum = a.add(b);
								}

								if (sum != null)
								{
									this.Add(sum, i, column);
								}
								else
								{
									this.Add(null, i, column);
								}
								break;

							case COUNT:
								a = (java.math.BigDecimal)this.GetObject(i, column);
								b = (java.math.BigDecimal)recordSet.GetObject(i, column);
								java.math.BigDecimal count = a.add(b);

								this.Add(count, i, column);
								break;

							case MIN:
								java.lang.Comparable thisValue = (java.lang.Comparable)this.GetObject(i, column);
								java.lang.Comparable otherValue = (java.lang.Comparable)recordSet.GetObject(i, column);
								java.lang.Comparable min = thisValue;

								if (thisValue == null && otherValue != null)
								{
									min = otherValue;
								}
								else if (thisValue != null && otherValue == null)
								{
									min = thisValue;
								}
								else if (thisValue == null && otherValue == null)
								{
									min = null;
								}
								else if (otherValue.compareTo(thisValue) < 0)
								{
									min = otherValue;
								}

								this.Add(min, i, column);
								break;

							case MAX:
								thisValue = (java.lang.Comparable)this.GetObject(i, column);
								otherValue = (java.lang.Comparable)recordSet.GetObject(i, column);
								java.lang.Comparable max = thisValue;

								if (thisValue == null && otherValue != null)
								{
									max = otherValue;
								}
								else if (thisValue != null && otherValue == null)
								{
									max = thisValue;
								}
								else if (thisValue == null && otherValue == null)
								{
									max = null;
								}
								else if (otherValue.compareTo(thisValue) > 0)
								{
									max = otherValue;
								}

								this.Add(max, i, column);
								break;

							case AVG:
								thisVal = this.GetObject(i, column);
								otherVal = recordSet.GetObject(i, column);

								AverageResult avg = null;
								if (thisVal == null && otherVal != null)
								{
									avg = (AverageResult)otherVal;
								}
								else if (thisVal != null && otherVal == null)
								{
									avg = (AverageResult)thisVal;
								}
								else if (thisVal != null && otherVal != null)
								{
									AverageResult thisResult = (AverageResult)thisVal;
									AverageResult otherResult = (AverageResult)otherVal;

									avg = new AverageResult();
									avg.setSum(thisResult.getSum().add(otherResult.getSum()));
									avg.setCount(thisResult.getCount().add(otherResult.getCount()));
								}

								if (avg != null)
								{
									this.Add(avg, i, column);
								}
								else
								{
									this.Add(null, i, column);
								}
								break;
						}
					}
					recordMatch = true;
					break;
				}
			}

			if (recordMatch == true)
			{
				continue;
			}
			this.AddRow();
			//Append data to current record set
			for (int j = 0; j < this.getColumnCount(); j++)
			{
				this.Add(recordSet.GetObject(i, j), _rowCount - 1, j);
			}
		}
	}

    @Override
    public void serialize(CacheObjectOutput out) throws IOException {
        out.write(_rowCount);
        for (java.util.Map.Entry<String, RecordColumn> kvp : _dataStringIndex.entrySet())
	{
		out.writeObject(kvp.getKey());
		out.writeObject(kvp.getValue());
	}
    }

    @Override
    public void deserialize(CacheObjectInput in) throws IOException, ClassNotFoundException {
        _rowCount =in.readInt();
	for (int i = 0; i < this.getColumnCount(); i++)
	{
            Object tempVar = in.readObject();
            String key = (String)((tempVar instanceof String) ? tempVar : null);
            Object tempVar2 = in.readObject();
            RecordColumn col = (RecordColumn)((tempVar2 instanceof RecordColumn) ? tempVar2 : null);
            _dataStringIndex.put(key, col);
            _dataIntIndex.put(i, col);
	}
    }
}
