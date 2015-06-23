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

package com.alachisoft.tayzgrid.common.util;

/**
 Summary description for JavaClrTypeMapping.
*/
public class JavaClrTypeMapping {
	private static java.util.HashMap _mappingTable;
	private static java.util.HashMap _predefinedTypes = new java.util.HashMap();
	private static java.util.HashMap _collections = new java.util.HashMap();
	private static java.util.HashMap _exceptions = new java.util.HashMap();

	public static void Initialize() {
		if (_mappingTable != null) {
			return;
		}
		_mappingTable = new java.util.HashMap();

		_predefinedTypes.put("JavaToClr", new java.util.HashMap());
		_predefinedTypes.put("ClrToJava", new java.util.HashMap());

		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("void", "void"); // void value
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("bool", "java.lang.Boolean"); // True/false value
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Boolean", "java.lang.Boolean"); // True/false value
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("char", "java.lang.Character"); // Unicode character (16 bit)
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Char", "java.lang.Character"); // Unicode character (16 bit)
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("string", "java.lang.String"); // Unicode String
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.String", "java.lang.String"); // Unicode String
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("float", "java.lang.Float"); // IEEE 32-bit float
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Single", "java.lang.Float"); // IEEE 32-bit float
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("double", "java.lang.Double"); // IEEE 64-bit float
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Double", "java.lang.Double"); // IEEE 64-bit float
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("short", "java.lang.Short"); // Signed 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Int16", "java.lang.Short"); // Signed 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("ushort", "java.lang.Short"); // Signed 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.UInt16", "java.lang.Short"); // Unsigned 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("int", "java.lang.Integer"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Int32", "java.lang.Integer"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("uint", "java.lang.Integer"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.UInt32", "java.lang.Long"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("long", "java.lang.Long"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Int64", "java.lang.Long"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("ulong", "java.lang.Long"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.UInt64", "java.math.BigInteger"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("byte", "java.lang.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Byte", "java.lang.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("sbyte", "java.lang.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.SByte", "java.lang.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Object", "java.lang.Object"); // Base class for all objects
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.MarshalByRefObject", "java.lang.Object"); // Base class for all objects passed by reference
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.DateTime", "java.util.Date"); // Dates will always be serialized (passed by value), according to .NET Remoting
		((java.util.HashMap)_predefinedTypes.get("ClrToJava")).put("System.Decimal", "java.math.BigDecimal"); // Will always be serialized (passed by value), according to .NET Remoting

		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("void", "void"); // void value
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("boolean", "System.Boolean"); // True/false value
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Boolean", "System.Boolean"); // True/false value
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("char", "System.Char"); // Unicode character (16 bit)
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Character", "System.Char"); // Unicode character (16 bit)
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.String", "System.String"); // Unicode String
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("float", "System.Single"); // IEEE 32-bit float
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Float", "System.Single"); // IEEE 32-bit float
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("double", "System.Double"); // IEEE 64-bit float
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Double", "System.Double"); // IEEE 64-bit float
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("int", "System.Int32"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Integer", "System.Int32"); // Signed 32-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("long", "System.Int64"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Long", "System.Int64"); // Signed 64-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("byte", "System.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Byte", "System.Byte"); // Unsigned 8-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("short", "System.Int16"); // Unsigned 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.lang.Short", "System.Int16"); // Unsigned 16-bit integer
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.util.Date", "System.DateTime"); // DateTime
		((java.util.HashMap)_predefinedTypes.get("JavaToClr")).put("java.math.BigDecimal", "System.Decimal");

		_collections.put("JavaToClr", new java.util.HashMap());
		_collections.put("ClrToJava", new java.util.HashMap());

		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.ArrayList", "System.Collections.IList");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.LinkedList", "System.Collections.IList");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.Vector", "System.Collections.IList");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.HashMap", "System.Collections.IDictionary");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.Hashmap", "System.Collections.IDictionary");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.TreeMap", "System.Collections.IDictionary");
		((java.util.HashMap)_collections.get("JavaToClr")).put("java.util.Properties", "System.Collections.IDictionary");

		((java.util.HashMap)_collections.get("ClrToJava")).put("System.Collections.ArrayList", "java.util.List");
		((java.util.HashMap)_collections.get("JavaToClr")).put("System.Collections.HashMap", "java.util.Map");
		_exceptions.put("JavaToClr", new java.util.HashMap());
		_exceptions.put("ClrToJava", new java.util.HashMap());

		_mappingTable.put("predefinedtypes", _predefinedTypes);
		_mappingTable.put("collections", _collections);
		_mappingTable.put("exceptions", _exceptions);

	}

	public static String ClrToJava(String clrType) {

		Initialize();

		if (((java.util.HashMap)_predefinedTypes.get("ClrToJava")).containsKey(clrType)) {
			return (String)(((java.util.HashMap)_predefinedTypes.get("ClrToJava")).get(clrType));
		} else if (((java.util.HashMap)_collections.get("ClrToJava")).containsKey(clrType)) {
			return (String)(((java.util.HashMap)_collections.get("ClrToJava")).get(clrType));
		} else if (((java.util.HashMap)_exceptions.get("ClrToJava")).containsKey(clrType)) {
			return (String)(((java.util.HashMap)_exceptions.get("ClrToJava")).get(clrType));
		} else {
			return null;
		}
	}

	public static String JavaToClr(String javaType) {
		Initialize();

		if (((java.util.HashMap)_predefinedTypes.get("JavaToClr")).containsKey(javaType)) {
			return (String)(((java.util.HashMap)_predefinedTypes.get("JavaToClr")).get(javaType));
		} else if (((java.util.HashMap)_collections.get("JavaToClr")).containsKey(javaType)) {
			return (String)(((java.util.HashMap)_collections.get("JavaToClr")).get(javaType));
		} else if (((java.util.HashMap)_exceptions.get("JavaToClr")).containsKey(javaType)) {
			return (String)(((java.util.HashMap)_exceptions.get("JavaToClr")).get(javaType));
		} else {
			return null;
		}
	}
}
