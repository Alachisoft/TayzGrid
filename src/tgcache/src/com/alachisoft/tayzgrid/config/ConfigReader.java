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

package com.alachisoft.tayzgrid.config;

/**
 Base configuration reader.
*/
public abstract class ConfigReader {
	public abstract java.util.HashMap getProperties();

	/**
	 Returns an xml config given a properties map.

	 @param properties properties map
	 @return xml config.
	*/
	public static String ToPropertiesXml(java.util.Map properties) {
		return ToPropertiesXml(properties, false);
	}

	/**
	 Returns an xml config given a properties map.

	 @param properties properties map
	 @return xml config.
	*/
	public static String ToPropertiesXml(java.util.Map properties, boolean formatted) {
		return ConfigHelper.CreatePropertiesXml(properties, 0, formatted);
	}

	public static String ToPropertiesXml2(java.util.Map properties, boolean formatted) {
		return ConfigHelper.CreatePropertiesXml2(properties, 0, formatted);
	}

	/**
	 Returns a property string given a properties map.

	 @param properties properties map
	 @return property string
	*/
	public static String ToPropertiesString(java.util.Map properties) {
		return ToPropertiesString(properties, false);
	}

	/**
	 Returns a property string given a properties map.

	 @param properties properties map
	 @return property string
	*/
	public static String ToPropertiesString(java.util.Map properties, boolean formatted) {
		return ConfigHelper.CreatePropertyString(properties, 0, formatted);
	}
}
