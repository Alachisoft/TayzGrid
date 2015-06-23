/* This software is provided 'as-is', without any expressed or implied warranty. In no event will the author(s) be held liable for any damages arising from the use of this software.
*Permission is granted to anyone to use this software for any purpose. If you use this software in a product, an acknowledgment in the product documentation would be deeply appreciated but is not required.

*In the case of the GOLD Parser Engine source code, permission is granted to anyone to alter it and redistribute it freely, subject to the following restrictions:

*	1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
*	2.	Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
*	3.	This notice may not be removed or altered from any source distribution
*/
package com.alachisoft.tayzgrid.parser;

import java.io.*;

// C# Translation of GoldParser, by Marcus Klimstra <klimstra@home.nl>.
// Based on GOLDParser by Devin Cook <http://www.devincook.com/goldparser>.
/**
 * This is the main class in the GoldParser Engine and is used to perform all duties required to the parsing of a source text string. This class contains the LALR(1) State Machine
 * code, the DFA State Machine code, character table (used by the DFA algorithm) and all other structures and methods needed to interact with the developer.
 */
public class Parser
{

    private java.util.HashMap m_parameters;
    private Symbol[] m_symbols;
    private String[] m_charsets;
    private Rule[] m_rules;
    private FAState[] m_DfaStates;
    private LRActionTable[] m_LalrTables;
    private boolean m_initialized;
    private boolean m_caseSensitive;
    private int m_startSymbol;
    private int m_initDfaState;
    private Symbol m_errorSymbol;
    private Symbol m_endSymbol;
    private LookAheadReader m_source;
    private int m_lineNumber;
    private boolean m_haveReduction;
    private boolean m_trimReductions;
    private int m_commentLevel;
    private int m_initLalrState;
    private int m_LalrState;
    private TokenStack m_inputTokens; // Stack of tokens to be analyzed
    private TokenStack m_outputTokens; // The set of tokens for 1. Expecting during error, 2. Reduction
    private TokenStack m_tempStack; // I often dont know what to call variables.

    /*
     * constructor
     */
    public Parser()
    {
    }

    /**
     * Creates a new <c>Parser</c> object for the specified CGT file.
     *
     * @param p_filename The name of the CGT file.
     */
    public Parser(String p_filename) throws IOException
    {
        LoadGrammar(p_filename);
    }

    /**
     * Creates a new <c>Parser</c> object for the specified CGT file.
     *
     * @param p_filename The name of the CGT file.
     */
    public Parser(FileInputStream stream) throws IOException
    {
        LoadGrammar(stream);
    }

    public final void LoadGrammar(String p_filename) throws IOException
    {
        m_parameters = new java.util.HashMap();
        m_inputTokens = new TokenStack();
        m_outputTokens = new TokenStack();
        m_tempStack = new TokenStack();
        m_initialized = false;
        m_trimReductions = false;

        LoadTables(new GrammarReader(p_filename));
    }

    public final void LoadGrammar(InputStream stream) throws IOException
    {
        m_parameters = new java.util.HashMap();
        m_inputTokens = new TokenStack();
        m_outputTokens = new TokenStack();
        m_tempStack = new TokenStack();
        m_initialized = false;
        m_trimReductions = false;

        LoadTables(new GrammarReader(stream));
    }

    /*
     * properties
     */
    /**
     * Gets or sets whether or not to trim reductions which contain only one non-terminal.
     */
    public final boolean getTrimReductions()
    {
        return m_trimReductions;
    }

    public final void setTrimReductions(boolean value)
    {
        m_trimReductions = value;
    }

    /**
     * Gets the current token.
     */
    public final Token getCurrentToken()
    {
        return m_inputTokens.PeekToken();
    }

    /**
     * Gets the <c>Reduction</c> made by the parsing engine. The value of this property is only valid when the Parse-method returns <c>ParseMessage.Reduction</c>.
     */
    public final Reduction getCurrentReduction()
    {
        if (m_haveReduction)
        {
            Token token = m_tempStack.PeekToken();
            Object tempVar = token.getData();
            return ((Reduction) ((tempVar instanceof Reduction) ? tempVar : null));
        }
        else
        {
            return null;
        }
    }

    public final void setCurrentReduction(Reduction value)
    {
        if (m_haveReduction)
        {
            m_tempStack.PeekToken().setData(value);
        }
    }

    /**
     * Gets the line number that is currently being processed.
     */
    public final int getCurrentLineNumber()
    {
        return m_lineNumber;
    }

    /*
     * public methods
     */
    /**
     * Pushes the specified token onto the internal input queue. It will be the next token analyzed by the parsing engine.
     */
    public final void PushInputToken(Token p_token)
    {
        m_inputTokens.PushToken(p_token);
    }

    /**
     * Pops the next token from the internal input queue.
     */
    public final Token PopInputToken()
    {
        return m_inputTokens.PopToken();
    }
    /*
     * /// Returns the token at the specified index. public Token GetToken(int p_index) { return m_outputTokens.GetToken(p_index); }
     */

    /**
     * Returns a <c>TokenStack</c> containing the tokens for the reduced rule or the tokens that where expected when a syntax error occures.
     */
    public final TokenStack GetTokens()
    {
        return m_outputTokens;
    }

    /**
     * Returns a string containing the value of the specified parameter. These parameters include: Name, Version, Author, About, Case Sensitive and Start Symbol. If the name
     * specified is invalid, this method will return an empty string.
     */
    public final String GetParameter(String p_name)
    {
        String result = (String) m_parameters.get(p_name);
        return (result != null ? result : "");
    }

    /**
     * Opens the file with the specified name for parsing.
     */
    public final void OpenFile(String p_filename) throws FileNotFoundException
    {
        Reset();

        m_source = new LookAheadReader(new BufferedReader(new FileReader(p_filename)));

        PrepareToParse();
    }

    /**
     * Opens the file with the specified name for parsing.
     */
    public final void OpenStream(BufferedReader stream)
    {
        Reset();

        m_source = new LookAheadReader(stream);

        PrepareToParse();
    }

    /**
     * Closes the file opened with <c>OpenFile</c>.
     */
    public final void CloseFile() throws IOException
    {
        // This will automaticly close the FileStream (I think :))
        if (m_source != null)
        {
            m_source.Close();
        }

        m_source = null;
    }

    /**
     * Executes a parse-action. When this method is called, the parsing engine reads information from the source text and then reports what action was taken. This ranges from a
     * token being read and recognized from the source, a parse reduction, or some type of error.
     */
    public final ParseMessage Parse() throws IOException
    {
        while (true)
        {
            if (m_inputTokens.getCount() == 0)
            {
                // we must read a token.

                Token token = RetrieveToken();

                if (token == null)
                {
                    throw new ParserException("RetrieveToken returned null");
                }

                if (token.getKind() != SymbolType.Whitespace)
                {
                    m_inputTokens.PushToken(token);

                    if (m_commentLevel == 0 && !CommentToken(token))
                    {
                        return ParseMessage.TokenRead;
                    }
                }
            }
            else if (m_commentLevel > 0)
            {
                // we are in a block comment.

                Token token = m_inputTokens.PopToken();

                switch (token.getKind())
                {
                    case CommentStart:
                        m_commentLevel++;
                        break;
                    case CommentEnd:
                        m_commentLevel--;
                        break;
                    case End:
                        return ParseMessage.CommentError;
                }
            }
            else
            {
                // we are ready to parse.

                Token token = m_inputTokens.PeekToken();
                switch (token.getKind())
                {
                    case CommentStart:
                        m_inputTokens.PopToken();
                        m_commentLevel++;
                        break;
                    case CommentLine:
                        m_inputTokens.PopToken();
                        DiscardLine();
                        break;
                    default:
                        ParseResult result = ParseToken(token);
                        switch (result)
                        {
                            case Accept:
                                return ParseMessage.Accept;
                            case InternalError:
                                return ParseMessage.InternalError;
                            case ReduceNormal:
                                return ParseMessage.Reduction;
                            case Shift:
                                m_inputTokens.PopToken();
                                break;
                            case SyntaxError:
                                return ParseMessage.SyntaxError;
                        }
                        break;
                } // switch
            } // else
        } // while
    }

    /*
     * private methods
     */
    private char FixCase(char p_char)
    {
        if (m_caseSensitive)
        {
            return p_char;
        }

        return Character.toLowerCase(p_char);
    }

    private String FixCase(String p_string)
    {
        if (m_caseSensitive)
        {
            return p_string;
        }

        return p_string.toLowerCase();
    }

    private void AddSymbol(Symbol p_symbol)
    {
        if (!m_initialized)
        {
            throw new ParserException("Table sizes not initialized");
        }

        int index = p_symbol.getTableIndex();
        m_symbols[index] = p_symbol;
    }

    private void AddCharset(int p_index, String p_charset)
    {
        if (!m_initialized)
        {
            throw new ParserException("Table sizes not initialized");
        }

        m_charsets[p_index] = FixCase(p_charset);
    }

    private void AddRule(Rule p_rule)
    {
        if (!m_initialized)
        {
            throw new ParserException("Table sizes not initialized");
        }

        int index = p_rule.getTableIndex();
        m_rules[index] = p_rule;
    }

    private void AddDfaState(int p_index, FAState p_fastate)
    {
        if (!m_initialized)
        {
            throw new ParserException("Table sizes not initialized");
        }

        m_DfaStates[p_index] = p_fastate;
    }

    private void AddLalrTable(int p_index, LRActionTable p_table)
    {
        if (!m_initialized)
        {
            throw new ParserException("Table counts not initialized");
        }

        m_LalrTables[p_index] = p_table;
    }

    private void LoadTables(GrammarReader reader)
    {
        Object obj;
        short index;
        while (reader.MoveNext())
        {
            byte id = (Byte) reader.RetrieveNext();

            switch (RecordId.forValue(id))
            {
                case Parameters:
                    m_parameters.put("Name", (String) reader.RetrieveNext());
                    m_parameters.put("Version", (String) reader.RetrieveNext());
                    m_parameters.put("Author", (String) reader.RetrieveNext());
                    m_parameters.put("About", (String) reader.RetrieveNext());
                    m_caseSensitive = (Boolean) reader.RetrieveNext();
                    m_startSymbol = (Short) reader.RetrieveNext();
                    break;

                case TableCounts:
                    m_symbols = new Symbol[(Short) reader.RetrieveNext()];
                    m_charsets = new String[(Short) reader.RetrieveNext()];
                    m_rules = new Rule[(Short) reader.RetrieveNext()];
                    m_DfaStates = new FAState[(Short) reader.RetrieveNext()];
                    m_LalrTables = new LRActionTable[(Short) reader.RetrieveNext()];
                    m_initialized = true;
                    break;

                case Initial:
                    m_initDfaState = (Short) reader.RetrieveNext();
                    m_initLalrState = (Short) reader.RetrieveNext();
                    break;

                case Symbols:
                    index = (Short) reader.RetrieveNext();
                    String name = (String) reader.RetrieveNext();
                    SymbolType kind = SymbolType.forValue((Short) reader.RetrieveNext());
                    Symbol symbol = new Symbol(index, name, kind);
                    AddSymbol(symbol);
                    break;

                case CharSets:
                    index = (Short) reader.RetrieveNext();
                    String charset = (String) reader.RetrieveNext();
                    AddCharset(index, charset);
                    break;

                case Rules:
                    index = (Short) reader.RetrieveNext();
                    Symbol head = m_symbols[(Short) reader.RetrieveNext()];
                    Rule rule = new Rule(index, head);

                    reader.RetrieveNext(); // reserved
                    while ((obj = reader.RetrieveNext()) != null)
                    {
                        rule.AddItem(m_symbols[(Short) obj]);
                    }

                    AddRule(rule);
                    break;

                case DFAStates:
                    FAState fastate = new FAState();
                    index = (Short) reader.RetrieveNext();

                    if ((Boolean) reader.RetrieveNext())
                    {
                        fastate.setAcceptSymbol((Short) reader.RetrieveNext());
                    }
                    else
                    {
                        reader.RetrieveNext();
                    }

                    reader.RetrieveNext(); // reserverd

                    while (!reader.RetrieveDone())
                    {
                        short ci = (Short) reader.RetrieveNext();
                        short ti = (Short) reader.RetrieveNext();
                        reader.RetrieveNext(); // reserved
                        fastate.AddEdge(m_charsets[ci], ti);
                    }

                    AddDfaState(index, fastate);
                    break;

                case LRTables:
                    LRActionTable table = new LRActionTable();
                    index = (Short) reader.RetrieveNext();
                    reader.RetrieveNext(); // reserverd

                    while (!reader.RetrieveDone())
                    {
                        short sid = (Short) reader.RetrieveNext();
                        short action = (Short) reader.RetrieveNext();
                        short tid = (Short) reader.RetrieveNext();
                        reader.RetrieveNext(); // reserved
                        table.AddItem(m_symbols[sid], Action.forValue(action), tid);
                    }

                    AddLalrTable(index, table);
                    break;

                case Comment:
                    break;

                default:
                    throw new ParserException("Wrong id for record");
            }
        }
    }

    private void Reset()
    {
        for (Symbol symbol : m_symbols)
        {
            if (symbol.getKind() == SymbolType.Error)
            {
                m_errorSymbol = symbol;
            }
            else if (symbol.getKind() == SymbolType.End)
            {
                m_endSymbol = symbol;
            }
        }

        m_haveReduction = false;
        m_LalrState = m_initLalrState;
        m_lineNumber = 1;
        m_commentLevel = 0;

        m_inputTokens.Clear();
        m_outputTokens.Clear();
        m_tempStack.Clear();
    }

    private void PrepareToParse()
    {
        Token token = new Token();
        token.setState(m_initLalrState);
        token.SetParent(m_symbols[m_startSymbol]);
        m_tempStack.PushToken(token);
    }

    private void DiscardLine() throws IOException
    {
        m_source.DiscardLine();
        m_lineNumber++;
    }

    /**
     * Returns true if the specified token is a CommentLine or CommentStart-symbol.
     */
    private boolean CommentToken(Token p_token)
    {
        return (p_token.getKind() == SymbolType.CommentLine) || (p_token.getKind() == SymbolType.CommentStart);
    }

    /**
     * This function analyzes a token and either: 1. Makes a SINGLE reduction and pushes a complete Reduction object on the stack 2. Accepts the token and shifts 3. Errors and
     * places the expected symbol indexes in the Tokens list The Token is assumed to be valid and WILL be checked
     */
    private ParseResult ParseToken(Token p_token)
    {
        ParseResult result = ParseResult.InternalError;
        LRActionTable table = m_LalrTables[m_LalrState];
        LRAction action = table.GetActionForSymbol(p_token.getTableIndex());

        if (action != null)
        {
            m_haveReduction = false;
            m_outputTokens.Clear();

            switch (action.getAction())
            {
                case Accept:
                    m_haveReduction = true;
                    result = ParseResult.Accept;
                    break;
                case Shift:
                    p_token.setState(m_LalrState = action.getValue());
                    m_tempStack.PushToken(p_token);
                    result = ParseResult.Shift;
                    break;
                case Reduce:
                    result = Reduce(m_rules[action.getValue()]);
                    break;
            }
        }
        else
        {
            // syntax error - fill expected tokens.
            m_outputTokens.Clear();
            LRAction a;
            for (Object aObj : table.getMembers())
            {
                a = (LRAction) aObj;
                SymbolType kind = a.getSymbol().getKind();

                if (kind == SymbolType.Terminal || kind == SymbolType.End)
                {
                    m_outputTokens.PushToken(new Token(a.getSymbol()));
                }
            }
            result = ParseResult.SyntaxError;
        }

        return result;
    }

    /**
     * Produces a reduction. Removes as many tokens as members in the rule and pushes a non-terminal token.
     */
    private ParseResult Reduce(Rule p_rule)
    {
        ParseResult result;
        Token head;

        if (m_trimReductions && p_rule.getContainsOneNonTerminal())
        {
            // The current rule only consists of a single nonterminal and can be trimmed from the
            // parse tree. Usually we create a new Reduction, assign it to the Data property
            // of Head and push it on the stack. However, in this case, the Data property of the
            // Head will be assigned the Data property of the reduced token (i.e. the only one
            // on the stack). In this case, to save code, the value popped of the stack is changed
            // into the head.
            head = m_tempStack.PopToken();
            head.SetParent(p_rule.getRuleNonTerminal());

            result = ParseResult.ReduceEliminated;
        }
        else
        {
            Reduction reduction = new Reduction();
            reduction.setParentRule(p_rule);

            m_tempStack.PopTokensInto(reduction, p_rule.getSymbolCount());

            head = new Token();
            head.setData(reduction);
            head.SetParent(p_rule.getRuleNonTerminal());

            m_haveReduction = true;
            result = ParseResult.ReduceNormal;
        }

        int index = m_tempStack.PeekToken().getState();
        LRAction action = m_LalrTables[index].GetActionForSymbol(p_rule.getRuleNonTerminal().getTableIndex());

        if (action != null)
        {
            head.setState(m_LalrState = action.getValue());
            m_tempStack.PushToken(head);
        }
        else
        {
            throw new ParserException("Action for LALR state is null");
        }

        return result;
    }

    /**
     * This method implements the DFA algorithm and returns a token to the LALR state machine.
     */
    private Token RetrieveToken() throws IOException
    {
        Token result;
        int currentPos = 0;
        int lastAcceptState = -1;
        int lastAcceptPos = -1;
        FAState currentState = m_DfaStates[m_initDfaState];

        try
        {
            while (true)
            {
                // This code searches all the branches of the current DFA state for the next
                // character in the input LookaheadStream. If found the target state is returned.
                // The InStr() function searches the string pCharacterSetTable.Member(CharSetIndex)
                // starting at position 1 for ch.  The pCompareMode variable determines whether
                // the search is case sensitive.
                int target = -1;
                char ch = FixCase(m_source.LookAhead(currentPos));
                //:
                FAEdge edge;
                for (Object edgeObj : currentState.getEdges())
                {
                    edge = (FAEdge) edgeObj;
                    String chars = edge.getCharacters();
                    if (chars.indexOf(ch) != -1)
                    {
                        target = edge.getTargetIndex();
                        break;
                    }
                }

                // This block-if statement checks whether an edge was found from the current state.
                // If so, the state and current position advance. Otherwise it is time to exit the main loop
                // and report the token found (if there was it fact one). If the LastAcceptState is -1,
                // then we never found a match and the Error Token is created. Otherwise, a new token
                // is created using the Symbol in the Accept State and all the characters that
                // comprise it.
                if (target != -1)
                {
                    // This code checks whether the target state accepts a token. If so, it sets the
                    // appropiate variables so when the algorithm is done, it can return the proper
                    // token and number of characters.
                    if (m_DfaStates[target].getAcceptSymbol() != -1)
                    {
                        lastAcceptState = target;
                        lastAcceptPos = currentPos;
                    }

                    currentState = m_DfaStates[target];
                    currentPos++;
                }
                else
                {
                    if (lastAcceptState == -1)
                    {
                        result = new Token(m_errorSymbol);
                        result.setData(m_source.Read(1));
                    }
                    else
                    {
                        Symbol symbol = m_symbols[m_DfaStates[lastAcceptState].getAcceptSymbol()];
                        result = new Token(symbol);
                        result.setData(m_source.Read(lastAcceptPos + 1));
                    }
                    break;
                }
            }
        }
        catch (StreamCorruptedException e)
        {
            result = new Token(m_endSymbol);
            result.setData("");
        }

        UpdateLineNumber((String) result.getData());

        return result;
    }

    private void UpdateLineNumber(String p_string)
    {
        int index, pos = 0;
        while ((index = p_string.indexOf('\n', pos)) != -1)
        {
            pos = index + 1;
            m_lineNumber++;
        }
    }
}