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

public interface ICacheReader
{
	/** 
	 Gets number of columns.
	*/
	int getColumnCount();

	/** 
	 Closes IDataReader
	*/
	void Close();

	/** 
	 Returns object at specified index
	 
	 @param index Index of column
	 @return object value of specified index
	*/
	Object getItem(int index);

	/** 
	 Returns value at specified column
	 
	 @param index Name of column
	 @return object value of specified index
	*/
	Object getItem(String columnName);



	/** 
	 Advances ICacheReader to next record
	 
	 @return true if there are more rows; otherwise false 
	*/
	boolean Read();

	/** 
	 Gets value of specified column as bool
	 
	 @param index Index of column
	 @return bool value of specified column
	*/
	boolean GetBool(int index);

	/** 
	 Gets value of specified column as string
	 
	 @param index Index of column
	 @return string value of specified column
	*/
	String GetString(int index);

	/** 
	 Gets value of specified column as decimal
	 
	 @param index Index of column
	 @return decimal value of specified column
	*/
	java.math.BigDecimal GetDecimal(int index);

	/** 
	 Gets value of specified column as double
	 
	 @param index Index of column
	 @return double value of specified column
	*/
	double GetDouble(int index);

	/** 
	 Gets value of specified column as 16 bit integer
	 
	 @param index Index of column
	 @return Int16 value of specified column
	*/
	short GetInt16(int index);

	/** 
	 Gets value of specified column as 32 bit integer
	 
	 @param index Index of column
	 @return Int32 value of specified column
	*/
	int GetInt32(int index);

	/** 
	 Gets value of specified column as 64 bit integer
	 
	 @param index Index of column
	 @return Int64 value of specified column
	*/
	long GetInt64(int index);

	/** 
	 Gets value at specified column index
	 
	 @param index Index of column
	 @return Value at specified column
	*/
	Object GetValue(int index);

	/** 
	 Gets value at specified column index
	 
	 @param columnName
	 @return 
	*/
	Object GetValue(String columnName);

	/** 
	 Populates array of objects with values in current row
	 
	 @param objects array of objects to be populated
	 @return No of objects copied in specified array
	*/
	int GetValues(Object[] objects);

	/** 
	 Returns name of specidied column index
	 
	 @param index Index of column
	 @return Name of column
	*/
	String GetName(int index);

	/** 
	 Returns index of specified column name
	 
	 @param columnName Name of column
	 @return Index of column
	*/
	int GetIndex(String columnName);

}
