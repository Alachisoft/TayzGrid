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

package com.alachisoft.tayzgrid.common.configuration;
public class ConfigurationAttributeAttribute extends ConfigurationAttributeBase {
	private String _attributeName;
	private String _appendedText = "";

	public ConfigurationAttributeAttribute(String attribName) {
		super(false, false);
		_attributeName = attribName;
	}

	public ConfigurationAttributeAttribute(String attribName, String appendedText) {
		this(attribName, false, false, appendedText);
		_attributeName = attribName;
	}

	public ConfigurationAttributeAttribute(String attribName, boolean isRequired, boolean isCollection, String appendedText) {
		super(isRequired, false);
		_attributeName = attribName;
		_appendedText = appendedText;
	}

	/** 
	 Gets the attribute name.
	*/
	public final String getAttributeName() {
		return _attributeName;
	}

	/** 
	 Gets/sets the appended text.
	 A property value may have some appended extra text just for
	 describtion of the property e.g. size="250mb". Here 250 is the size
	 and mb is just for describtion that size is in mbs.
	*/
	public final String getAppendedText() {
		return _appendedText;
	}
}
