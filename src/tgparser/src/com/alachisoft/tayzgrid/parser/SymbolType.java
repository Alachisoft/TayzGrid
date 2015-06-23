/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/

package com.alachisoft.tayzgrid.parser;

/** Respresents the type of a symbol.
*/
public enum SymbolType {
	/** A normal non-terminal.
	*/
	NonTerminal(0),

	/** A normal terminal.
	*/
	Terminal(1),

	/** This Whitespace symbol is a special terminal that is automatically 
	 ignored by the parsing engine. Any text accepted as whitespace is 
	 considered to be inconsequential and "meaningless".
	*/
	Whitespace(2),

	/** The End symbol is generated when the tokenizer reaches the end of 
	 the source text.
	*/
	End(3),

	/** This type of symbol designates the start of a block comment.
	*/
	CommentStart(4),

	/** This type of symbol designates the end of a block comment.
	*/
	CommentEnd(5),

	/** When the engine reads a token that is recognized as a line comment, 
	 the remaining characters on the line are automatically ignored by 
	 the parser.
	*/
	CommentLine(6),

	/** The Error symbol is a general-purpose means of representing characters 
	 that were not recognized by the tokenizer. In other words, when the 
	 tokenizer reads a series of characters that is not accepted by the DFA 
	 engine, a token of this type is created.
	*/
	Error(7);

	private int intValue;
	private static java.util.HashMap<Integer, SymbolType> mappings;
	private static java.util.HashMap<Integer, SymbolType> getMappings() {
		if (mappings == null) {
			synchronized (SymbolType.class) {
				if (mappings == null) {
					mappings = new java.util.HashMap<Integer, SymbolType>();
				}
			}
		}
		return mappings;
	}

	private SymbolType(int value) {
		intValue = value;
		SymbolType.getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static SymbolType forValue(int value) {
		return getMappings().get(value);
	}
}