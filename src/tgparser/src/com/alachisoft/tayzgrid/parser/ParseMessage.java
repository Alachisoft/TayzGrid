/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/


package com.alachisoft.tayzgrid.parser;

// C# Translation of GoldParser, by Marcus Klimstra <klimstra@home.nl>.
// Based on GOLDParser by Devin Cook <http://www.devincook.com/goldparser>.
/** The result of the Parser.Parse method.
*/
public enum ParseMessage {
	/** This message is returned each time a token is read.
	*/
	TokenRead,

	/** When the engine is able to reduce a rule, this message is returned. 
	 The rule that was reduced is set in the <c>Parser.Reduction</c> property. 
	 The tokens that are reduced and correspond to the rule's definition 
	 can be acquired using the <c>GetToken</c> or <c>GetTokens</c> methods.
	*/
	Reduction,

	/** The engine will returns this message when the source text has been 
	 accepted as both complete and correct. In other words, the source 
	 text was successfully analyzed.
	*/
	Accept,

	/** The tokenizer will generate this message when it is unable to 
	 recognize a series of characters as a valid token. To recover, pop the 
	 invalid token from the input queue using <c>Parser.PopInputToken</c>.
	*/
	LexicalError,

	/** Often the parser will read a token that is not expected in the 
	 grammar. When this happens, you can acquire the expected tokens using
	 the <c>GetToken</c> or <c>GetTokens</c> methods. To recover, push 
	 one of the expected tokens onto the input queue.
	*/
	SyntaxError,

	/** The parser reached the end of the file while reading a comment. 
	 This is caused when the source text contains a "run-away" comment, 
	 or in other words, a block comment that lacks the end-delimiter.
	*/
	CommentError,

	/** Something is very wrong when this message is returned.
	*/
	InternalError;

	public int getValue() {
		return this.ordinal();
	}

	public static ParseMessage forValue(int value) {
		return values()[value];
	}
}