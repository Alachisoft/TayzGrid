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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import java.io.Serializable;

public class QueryDataFilters implements Serializable
{
	private EventDataFilter _addDF;
	private EventDataFilter _updateDF;
	private EventDataFilter _removeDF;

	public QueryDataFilters(int add, int update, int remove)
	{
		_addDF = EventDataFilter.forValue(add);
		_updateDF = EventDataFilter.forValue(update);
		_removeDF = EventDataFilter.forValue(remove);
	}

	public final EventDataFilter getAddDataFilter()
	{
		return _addDF;
	}
	public final void setAddDataFilter(EventDataFilter value)
	{
		_addDF = value;
	}

	public final EventDataFilter getUpdateDataFilter()
	{
		return _updateDF;
	}
	public final void setUpdateDataFilter(EventDataFilter value)
	{
		_updateDF = value;
	}

	public final EventDataFilter getRemoveDataFilter()
	{
		return _removeDF;
	}
	public final void setRemoveDataFilter(EventDataFilter value)
	{
		_removeDF = value;
	}
}