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

import com.alachisoft.tayzgrid.caching.queries.filters.Predicate;
import com.alachisoft.tayzgrid.caching.util.ParserHelper;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.parser.ParseMessage;
import com.alachisoft.tayzgrid.parser.Reduction;
import com.alachisoft.tayzgrid.caching.util.MiscUtil;

public class PredicateHolder  implements java.io.Serializable
{
	private String _cmdText;
	private String _objectType;
	private String _queryId;
	private java.util.Map _attributeValues;
//	// this member is not serialzable.
	private transient Predicate _predicate;

	/**
	 Initializes the predicate from command text.
	*/
	public final void Initialize(ILogger NCacheLog) {

		ParserHelper parser = new ParserHelper(NCacheLog);
		if (parser.Parse(_cmdText) == ParseMessage.Accept) {
			Reduction reduction = parser.getCurrentReduction();
			Object tempVar = reduction.getTag();
			_predicate = (Predicate)((tempVar instanceof Predicate) ? tempVar : null);
		}
	}

	public final String getCommandText() {
		return _cmdText;
	}
	public final void setCommandText(String value) {
		_cmdText = value;
	}

	public final String getObjectType() {
		return _objectType;
	}
	public final void setObjectType(String value) {
		_objectType = value;
	}

	public final Predicate getPredicate() {
		return _predicate;
	}
	public final void setPredicate(Predicate value) {
		_predicate = value;
	}

	public final java.util.Map getAttributeValues() {
		return MiscUtil.DeepClone(_attributeValues);
	}
	public final void setAttributeValues(java.util.Map value) {
		_attributeValues = value;
	}

	public final String getQueryId() {
		return _queryId;
	}
	public final void setQueryId(String value) {
		_queryId = value;
	}

	public boolean equals(Object obj) {
		PredicateHolder other = (PredicateHolder)((obj instanceof PredicateHolder) ? obj : null);
		if (other != null) {
			if (this.getQueryId().equals(other.getQueryId())) {
				return true;
			}
		}
		return false;
	}
}