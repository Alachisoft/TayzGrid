
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

package com.alachisoft.tayzgrid.tools.common;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeBase;

public class ArgumentAttribute extends ConfigurationAttributeBase
{
	private String shortNotation = "";
	private String fullName = "";
	private String appendedText = "";
	private Object defaultValue = "";

	public ArgumentAttribute(String attribName)
	{
		super(false, false);
		shortNotation = attribName;
	}

	public ArgumentAttribute(String attribName1, String attribName2)
	{
		super(false, false);
		shortNotation = attribName1;
		fullName = attribName2;
	}

	public ArgumentAttribute(String attribName, boolean isRequired, boolean isCollection, String appendedText)
	{
		super(isRequired, false);
		shortNotation = attribName;
		this.appendedText = appendedText;
	}

	public ArgumentAttribute(String attribName, Object defaultValue)
	{
		this(attribName, false, false, "");
		shortNotation = attribName;
		this.defaultValue = defaultValue;
	}

	public ArgumentAttribute(String attribName, String attribName2, Object defaultValue)
	{
		this(attribName, false, false, "");
		shortNotation = attribName;
		fullName = attribName2;
		this.defaultValue = defaultValue;
	}

	/**
	 Gets the attribute name.
	*/
	public final String getShortNotation()
	{
		return shortNotation;
	}

	public final String getFullName()
	{
		return fullName;
	}

	/**
	 Gets/sets the appended text.
	 A property value may have some appended extra text just for
	 describtion of the property e.g. size="250mb". Here 250 is the size
	 and mb is just for describtion that size is in mbs.
	*/
	public final String getAppendedText()
	{
		return appendedText;
	}

	public final Object getDefaultValue()
	{
		return defaultValue;
	}
}
