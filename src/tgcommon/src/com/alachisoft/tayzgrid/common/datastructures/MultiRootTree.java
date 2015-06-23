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

public class MultiRootTree
{
	//Act as attribute name
	private String _currentAttribute;
	private java.util.ArrayList<String> _attributeNames;
	//contains attributeValue-listofkeys/subtree
	private java.util.Hashtable _ht;
	private int _levels;

	public final int getLevels()
	{
		return _levels;
	}

	public MultiRootTree(int levels, java.util.ArrayList<String> attributeNames)
	{
		_ht = new java.util.Hashtable();
		_levels = levels;
		_attributeNames = attributeNames;
		_currentAttribute = attributeNames.get(attributeNames.size() - _levels);
	}

	public final void Add(KeyValuesContainer value)
	{
		if (_levels == 1)
		{
			if (_ht.get(value.getValues().get(_currentAttribute)) instanceof java.util.ArrayList)
			{
				((java.util.ArrayList)_ht.get(value.getValues().get(_currentAttribute))).add(value.getKey());
			}
			else
			{
				java.util.ArrayList al = new java.util.ArrayList();
				al.add(value.getKey());
				_ht.put(value.getValues().get(_currentAttribute), al);
			}
		}
		else
		{
			if (_ht.get(value.getValues().get(_currentAttribute)) instanceof MultiRootTree)
			{
				((MultiRootTree)_ht.get(value.getValues().get(_currentAttribute))).Add(value);
			}
			else
			{
				MultiRootTree mrt = new MultiRootTree(_levels - 1, _attributeNames);
				mrt.Add(value);
				_ht.put(value.getValues().get(_currentAttribute), mrt);

			}
		}
	}

	private int GenerateRecordSet(RecordSet recordSet)
	{
                java.util.Iterator iterator=_ht.entrySet().iterator();
		int rowsAdded = 0;
		while (iterator.hasNext())
                {
                    java.util.Map.Entry entry=(java.util.Map.Entry)iterator.next();
			if (entry.getValue() instanceof java.util.ArrayList)
			{
				rowsAdded++;
				recordSet.AddRow();
				if (recordSet.GetColumnDataType(_currentAttribute) == ColumnDataType.Object)
				{
					recordSet.SetColumnDataType(_currentAttribute, RecordSet.ToColumnDataType(entry.getKey()));
				}
				recordSet.Add(entry.getKey(), recordSet.getRowCount() - 1, _currentAttribute);
				recordSet.Add(entry.getValue(), recordSet.getRowCount() - 1, recordSet.getColumnCount() - 1);
			}
			else
			{
				rowsAdded = ((MultiRootTree)((entry.getValue() instanceof MultiRootTree) ? entry.getValue() : null)).GenerateRecordSet(recordSet);
				for (int i = 0; i < rowsAdded; i++)
				{
					recordSet.Add(entry.getKey(), recordSet.getRowCount() - i - 1, _currentAttribute);
					if (recordSet.GetColumnDataType(_currentAttribute) == ColumnDataType.Object)
					{
						recordSet.SetColumnDataType(_currentAttribute, RecordSet.ToColumnDataType(entry.getKey()));
					}
				}
			}
		}
		return rowsAdded;
	}

	public final void ToRecordSet(RecordSet recordSet)
	{
		recordSet.AddColumn("", true);
		GenerateRecordSet(recordSet);
	}
}
