/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/

package com.alachisoft.tayzgrid.parser;

/** Represents the type of a record in the CGT file.
*/
public enum RecordId {
	/**
	*/
	Parameters(80),

	/**
	*/
	TableCounts(84),

	/**
	*/
	Initial(73),

	/**
	*/
	Symbols(83),

	/**
	*/
	CharSets(67),

	/**
	*/
	Rules(82),

	/**
	*/
	DFAStates(68),

	/**
	*/
	LRTables(76),

	/**
	*/
	Comment(33);

	private int intValue;
	private static java.util.HashMap<Integer, RecordId> mappings;
	private static java.util.HashMap<Integer, RecordId> getMappings() {
		if (mappings == null) {
			synchronized (RecordId.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, RecordId>();
				}
			}
		}
		return mappings;
	}

	private RecordId(int value) {
		intValue = value;
		RecordId.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static RecordId forValue(int value) {
		return getMappings().get(value);
	}
}