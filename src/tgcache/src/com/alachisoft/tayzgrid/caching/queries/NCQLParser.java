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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.parser.ParseMessage;
import com.alachisoft.tayzgrid.parser.Reduction;
import com.alachisoft.tayzgrid.parser.Token;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

public class NCQLParser extends com.alachisoft.tayzgrid.parser.Parser {

    public enum SymbolConstants {

        SYMBOL_EOF(0),// (EOF)
        SYMBOL_ERROR(1),// (Error)
        SYMBOL_WHITESPACE(2),// (Whitespace)
        SYMBOL_MINUS(3),// '-'
        SYMBOL_EXCLAMEQ(4),// '!='
        SYMBOL_DOLLARTEXTDOLLAR(5),// '$Text$'
        SYMBOL_LPARAN(6),// '('
        SYMBOL_RPARAN(7),// ')'
        SYMBOL_TIMES(8),// '*'
        SYMBOL_COMMA(9),// ','
        SYMBOL_DOT(10),// '.'
        SYMBOL_QUESTION(11),// '?'
        SYMBOL_LT(12),// '<'
        SYMBOL_LTEQ(13),// '<='
        SYMBOL_LTGT(14),// '<>'
        SYMBOL_EQ(15),// '='
        SYMBOL_EQEQ(16),// '=='
        SYMBOL_GT(17),// '>'
        SYMBOL_GTEQ(18),// '>='
        SYMBOL_AND(19),// AND
        SYMBOL_AVGLPARAN(20),// 'AVG('
        SYMBOL_COUNTLPARAN(21),// 'COUNT('
        SYMBOL_DATETIME(22),// DateTime
        SYMBOL_DELETE(23),// DELETE
        SYMBOL_FALSE(24),// false
        SYMBOL_GROUPBY(25),// 'GROUP BY'
        SYMBOL_IDENTIFIER(26),// Identifier
        SYMBOL_IN(27),// IN
        SYMBOL_INTEGERLITERAL(28),// IntegerLiteral
        SYMBOL_IS(29),// IS
        SYMBOL_KEYWORD(30),// Keyword
        SYMBOL_LIKE(31),// LIKE
        SYMBOL_MAXLPARAN(32),// 'MAX('
        SYMBOL_MINLPARAN(33),// 'MIN('
        SYMBOL_NOT(34),// NOT
        SYMBOL_NOW(35),// now
        SYMBOL_NULL(36),// NULL
        SYMBOL_OR(37),// OR
        SYMBOL_REALLITERAL(38),// RealLiteral
        SYMBOL_SELECT(39),// SELECT
        SYMBOL_STRINGLITERAL(40),// StringLiteral
        SYMBOL_SUMLPARAN(41),// 'SUM('
        SYMBOL_TAG(42),// Tag
        SYMBOL_TRUE(43),// true
        SYMBOL_WHERE(44),// WHERE
        SYMBOL_AGGREGATEFUNCTION(45),// <AggregateFunction>
        SYMBOL_AGGREGATEFUNCTIONLIST(46),// <AggregateFunctionList>
        SYMBOL_ANDEXPR(47),// <AndExpr>
        SYMBOL_ATRRIB(48),// <Atrrib>
        SYMBOL_AVERAGEFUNCTION(49),// <AverageFunction>
        SYMBOL_COMPAREEXPR(50),// <CompareExpr>
        SYMBOL_COUNTFUNCTION(51),// <CountFunction>
        SYMBOL_DATE(52),// <Date>
        SYMBOL_DATELIST(53),// <DateList>
        SYMBOL_DELETEPARAMS(54),// <DeleteParams>
        SYMBOL_EXPRESSION(55),// <Expression>
        SYMBOL_GROUPBYVALUELIST(56),// <GroupByValueList>
        SYMBOL_INLIST(57),// <InList>
        SYMBOL_LISTTYPE(58),// <ListType>
        SYMBOL_MAXFUNCTION(59),// <MaxFunction>
        SYMBOL_MINFUNCTION(60),// <MinFunction>
        SYMBOL_NUMLITERAL(61),// <NumLiteral>
        SYMBOL_NUMLITERALLIST(62),// <NumLiteralList>
        SYMBOL_OBJECTATTRIBUTE(63),// <ObjectAttribute>
        SYMBOL_OBJECTATTRIBUTELIST(64),// <ObjectAttributeList>
        SYMBOL_OBJECTTYPE(65),// <ObjectType>
        SYMBOL_OBJECTVALUE(66),// <ObjectValue>
        SYMBOL_OREXPR(67),// <OrExpr>
        SYMBOL_PROPERTY(68),// <Property>
        SYMBOL_QUERY(69),// <Query>
        SYMBOL_STRLITERAL(70),// <StrLiteral>
        SYMBOL_STRLITERALLIST(71),// <StrLiteralList>
        SYMBOL_SUMFUNCTION(72),// <SumFunction>
        SYMBOL_TYPEPLUSATTRIBUTE(73),// <TypePlusAttribute>
        SYMBOL_UNARYEXPR(74),// <UnaryExpr>
        SYMBOL_VALUE(75) // <Value>
        ;
        private int intValue;
        private static java.util.HashMap<Integer, SymbolConstants> mappings;

        private static java.util.HashMap<Integer, SymbolConstants> getMappings() {
            if (mappings == null) {
                synchronized (SymbolConstants.class) {
                    if (mappings == null) {
                        mappings = new java.util.HashMap<Integer, SymbolConstants>();
                    }
                }
            }
            return mappings;
        }

        private SymbolConstants(int value) {
            intValue = value;
            SymbolConstants.getMappings().put(value, this);
        }

        public int getValue() {
            return intValue;
        }

        public static SymbolConstants forValue(int value) {
            return getMappings().get(value);
        }
    }

    public enum RuleConstants {

        RULE_QUERY_SELECT(0),// <Query> ::= SELECT <ObjectType>
        RULE_QUERY_SELECT_WHERE(1),// <Query> ::= SELECT <ObjectType> WHERE <Expression>
        RULE_QUERY_DELETE(2),// <Query> ::= DELETE <DeleteParams>
        RULE_QUERY_DELETE_WHERE(3),// <Query> ::= DELETE <DeleteParams> WHERE <Expression>
        RULE_QUERY_SELECT2(4),// <Query> ::= SELECT <AggregateFunction>
        RULE_QUERY_SELECT_WHERE2(5),// <Query> ::= SELECT <AggregateFunction> WHERE <Expression>
        RULE_QUERY_SELECT_GROUPBY(6),// <Query> ::= SELECT <AggregateFunction> 'GROUP BY' <ObjectAttributeList>
        RULE_QUERY_SELECT_WHERE_GROUPBY(7),// <Query> ::= SELECT <AggregateFunction> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
        RULE_QUERY_SELECT_GROUPBY2(8),// <Query> ::= SELECT <GroupByValueList> 'GROUP BY' <ObjectAttributeList>
        RULE_QUERY_SELECT_WHERE_GROUPBY2(9),// <Query> ::= SELECT <GroupByValueList> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
        RULE_EXPRESSION(10),// <Expression> ::= <OrExpr>
        RULE_OREXPR_OR(11),// <OrExpr> ::= <OrExpr> OR <AndExpr>
        RULE_OREXPR(12),// <OrExpr> ::= <AndExpr>
        RULE_ANDEXPR_AND(13),// <AndExpr> ::= <AndExpr> AND <UnaryExpr>
        RULE_ANDEXPR(14),// <AndExpr> ::= <UnaryExpr>
        RULE_UNARYEXPR_NOT(15),// <UnaryExpr> ::= NOT <CompareExpr>
        RULE_UNARYEXPR(16),// <UnaryExpr> ::= <CompareExpr>
        RULE_COMPAREEXPR_EQ(17),// <CompareExpr> ::= <Atrrib> '=' <Value>
        RULE_COMPAREEXPR_EXCLAMEQ(18),// <CompareExpr> ::= <Atrrib> '!=' <Value>
        RULE_COMPAREEXPR_EQEQ(19),// <CompareExpr> ::= <Atrrib> '==' <Value>
        RULE_COMPAREEXPR_LTGT(20),// <CompareExpr> ::= <Atrrib> '<>' <Value>
        RULE_COMPAREEXPR_LT(21),// <CompareExpr> ::= <Atrrib> '<' <Value>
        RULE_COMPAREEXPR_GT(22),// <CompareExpr> ::= <Atrrib> '>' <Value>
        RULE_COMPAREEXPR_LTEQ(23),// <CompareExpr> ::= <Atrrib> '<=' <Value>
        RULE_COMPAREEXPR_GTEQ(24),// <CompareExpr> ::= <Atrrib> '>=' <Value>
        RULE_COMPAREEXPR_LIKE_STRINGLITERAL(25),// <CompareExpr> ::= <Atrrib> LIKE StringLiteral
        RULE_COMPAREEXPR_LIKE_QUESTION(26),// <CompareExpr> ::= <Atrrib> LIKE '?'
        RULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL(27),// <CompareExpr> ::= <Atrrib> NOT LIKE StringLiteral
        RULE_COMPAREEXPR_NOT_LIKE_QUESTION(28),// <CompareExpr> ::= <Atrrib> NOT LIKE '?'
        RULE_COMPAREEXPR_IN(29),// <CompareExpr> ::= <Atrrib> IN <InList>
        RULE_COMPAREEXPR_NOT_IN(30),// <CompareExpr> ::= <Atrrib> NOT IN <InList>
        RULE_COMPAREEXPR_IS_NULL(31),// <CompareExpr> ::= <Atrrib> IS NULL
        RULE_COMPAREEXPR_IS_NOT_NULL(32),// <CompareExpr> ::= <Atrrib> IS NOT NULL
        RULE_COMPAREEXPR_LPARAN_RPARAN(33),// <CompareExpr> ::= '(' <Expression> ')'
        RULE_ATRRIB(34),// <Atrrib> ::= <ObjectValue>
        RULE_VALUE_MINUS(35),// <Value> ::= '-' <NumLiteral>
        RULE_VALUE(36),// <Value> ::= <NumLiteral>
        RULE_VALUE2(37),// <Value> ::= <StrLiteral>
        RULE_VALUE_TRUE(38),// <Value> ::= true
        RULE_VALUE_FALSE(39),// <Value> ::= false
        RULE_VALUE3(40),// <Value> ::= <Date>
        RULE_DATE_DATETIME_DOT_NOW(41),// <Date> ::= DateTime '.' now
        RULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN(42),// <Date> ::= DateTime '(' StringLiteral ')'
        RULE_STRLITERAL_STRINGLITERAL(43),// <StrLiteral> ::= StringLiteral
        RULE_STRLITERAL_NULL(44),// <StrLiteral> ::= NULL
        RULE_STRLITERAL_QUESTION(45),// <StrLiteral> ::= '?'
        RULE_NUMLITERAL_INTEGERLITERAL(46),// <NumLiteral> ::= IntegerLiteral
        RULE_NUMLITERAL_REALLITERAL(47),// <NumLiteral> ::= RealLiteral
        RULE_OBJECTTYPE_TIMES(48),// <ObjectType> ::= '*'
        RULE_OBJECTTYPE_DOLLARTEXTDOLLAR(49),// <ObjectType> ::= '$Text$'
        RULE_OBJECTTYPE(50),// <ObjectType> ::= <Property>
        RULE_OBJECTATTRIBUTE_IDENTIFIER(51),// <ObjectAttribute> ::= Identifier
        RULE_DELETEPARAMS_DOLLARTEXTDOLLAR(52),// <DeleteParams> ::= '$Text$'
        RULE_DELETEPARAMS(53),// <DeleteParams> ::= <Property>
        RULE_PROPERTY_DOT_IDENTIFIER(54),// <Property> ::= <Property> '.' Identifier
        RULE_PROPERTY_IDENTIFIER(55),// <Property> ::= Identifier
        RULE_TYPEPLUSATTRIBUTE_DOT(56),// <TypePlusAttribute> ::= <Property> '.' <ObjectAttribute>
        RULE_AGGREGATEFUNCTION(57),// <AggregateFunction> ::= <SumFunction>
        RULE_AGGREGATEFUNCTION2(58),// <AggregateFunction> ::= <CountFunction>
        RULE_AGGREGATEFUNCTION3(59),// <AggregateFunction> ::= <MinFunction>
        RULE_AGGREGATEFUNCTION4(60),// <AggregateFunction> ::= <MaxFunction>
        RULE_AGGREGATEFUNCTION5(61),// <AggregateFunction> ::= <AverageFunction>
        RULE_SUMFUNCTION_SUMLPARAN_RPARAN(62),// <SumFunction> ::= 'SUM(' <TypePlusAttribute> ')'
        RULE_COUNTFUNCTION_COUNTLPARAN_RPARAN(63),// <CountFunction> ::= 'COUNT(' <Property> ')'
        RULE_MINFUNCTION_MINLPARAN_RPARAN(64),// <MinFunction> ::= 'MIN(' <TypePlusAttribute> ')'
        RULE_MAXFUNCTION_MAXLPARAN_RPARAN(65),// <MaxFunction> ::= 'MAX(' <TypePlusAttribute> ')'
        RULE_AVERAGEFUNCTION_AVGLPARAN_RPARAN(66),// <AverageFunction> ::= 'AVG(' <TypePlusAttribute> ')'
        RULE_OBJECTATTRIBUTE_KEYWORD_DOT_IDENTIFIER(67),// <ObjectAttribute> ::= Keyword '.' Identifier
        RULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER(68),// <ObjectValue> ::= Keyword '.' Identifier
        RULE_OBJECTVALUE_KEYWORD_DOT_TAG(69),// <ObjectValue> ::= Keyword '.' Tag
        RULE_INLIST_LPARAN_RPARAN(70),// <InList> ::= '(' <ListType> ')'
        RULE_LISTTYPE(71),// <ListType> ::= <NumLiteralList>
        RULE_LISTTYPE2(72),// <ListType> ::= <StrLiteralList>
        RULE_LISTTYPE3(73),// <ListType> ::= <DateList>
        RULE_NUMLITERALLIST_COMMA(74),// <NumLiteralList> ::= <NumLiteral> ',' <NumLiteralList>
        RULE_NUMLITERALLIST(75),// <NumLiteralList> ::= <NumLiteral>
        RULE_STRLITERALLIST_COMMA(76),// <StrLiteralList> ::= <StrLiteral> ',' <StrLiteralList>
        RULE_STRLITERALLIST(77),// <StrLiteralList> ::= <StrLiteral>
        RULE_DATELIST_COMMA(78),// <DateList> ::= <Date> ',' <DateList>
        RULE_DATELIST(79),// <DateList> ::= <Date>
        RULE_GROUPBYVALUELIST_COMMA(80),// <GroupByValueList> ::= <ObjectAttribute> ',' <GroupByValueList>
        RULE_GROUPBYVALUELIST(81),// <GroupByValueList> ::= <AggregateFunctionList>
        RULE_AGGREGATEFUNCTIONLIST_COMMA(82),// <AggregateFunctionList> ::= <AggregateFunction> ',' <AggregateFunctionList>
        RULE_AGGREGATEFUNCTIONLIST(83),// <AggregateFunctionList> ::= <AggregateFunction>
        RULE_OBJECTATTRIBUTELIST_COMMA(84),// <ObjectAttributeList> ::= <ObjectAttribute> ',' <ObjectAttributeList>
        RULE_OBJECTATTRIBUTELIST(85) // <ObjectAttributeList> ::= <ObjectAttribute>
        ;
        private int intValue;
        private static java.util.HashMap<Integer, RuleConstants> mappings;

        private static java.util.HashMap<Integer, RuleConstants> getMappings() {
            if (mappings == null) {
                synchronized (RuleConstants.class) {
                    if (mappings == null) {
                        mappings = new java.util.HashMap<Integer, RuleConstants>();
                    }
                }
            }
            return mappings;
        }

        private RuleConstants(int value) {
            intValue = value;
            RuleConstants.getMappings().put(value, this);
        }

        public int getValue() {
            return intValue;
        }

        public static RuleConstants forValue(int value) {
            return getMappings().get(value);
        }
    }

    private ILogger _ncacheLog;
    NCQLParserRule _parserRule;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }

    public NCQLParser(String resourceName, ILogger NCacheLog) throws IOException {
        this._ncacheLog = NCacheLog;
        _parserRule = new NCQLParserRule(NCacheLog);
        URL resource = this.getClass().getResource(resourceName);
        InputStream in = resource.openStream();
        super.LoadGrammar(in);
    }

    /**
     * This procedure starts the GOLD Parser Engine and handles each of the
     * messages it returns. Each time a reduction is made, a new custom object
     * can be created and used to store the rule. Otherwise, the system will use
     * the Reduction object that was returned.
     *
     * The resulting tree will be a pure representation of the language and will
     * be ready to implement.
     */
    public final ParseMessage Parse(BufferedReader Source, boolean GenerateContext) throws IOException {
        ParseMessage Response = null;
        boolean done = false;

        OpenStream(Source);
        setTrimReductions(true);

        do {
            Response = Parse();

            switch (Response) {
                case LexicalError:
                    //Cannot recognize token
                    done = true;
                    break;

                case SyntaxError:
                    //Expecting a different token
                    Iterator ide = GetTokens().GetEnumerator();
                    while (ide.hasNext()) {
                        Token t = (Token) ide.next();
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info(t.getName());
                        }
                    }
                    done = true; // stop if there are multiple errors on one line
                    break;

                case Reduction:
                    //Create a customized object to store the reduction
                    if (GenerateContext) {
                        setCurrentReduction(CreateNewObject(getCurrentReduction()));
                    }
                    break;

                case Accept:
                    //Success!
                    done = true;
                    break;

                case TokenRead:
                    //You don't have to do anything here.
                    break;

                case InternalError:
                    //INTERNAL ERROR! Something is horribly wrong.
                    done = true;
                    break;

                case CommentError:
                    //COMMENT ERROR! Unexpected end of file
                    done = true;
                    break;
            }
        } while (!done);

        CloseFile();
        return Response;
    }

    private Reduction CreateNewObject(Reduction reduction) {
        Reduction result = null;
        RuleConstants rule = RuleConstants.forValue(reduction.getParentRule().getTableIndex());
        switch (rule) {
            case RULE_QUERY_SELECT:
                ////<Query> ::= SELECT <ObjectType>
                result = _parserRule.CreateRULE_QUERY_SELECT(reduction);
                break;
            case RULE_QUERY_SELECT_WHERE:
                ////<Query> ::= SELECT <ObjectType> WHERE <Expression>
                result = _parserRule.CreateRULE_QUERY_SELECT_WHERE(reduction);
                break;
            case RULE_QUERY_DELETE:
                ////<Query> ::= DELETE <DeleteParams>
                result = _parserRule.CreateRULE_QUERY_DELETE(reduction);
                break;
            case RULE_QUERY_DELETE_WHERE:
                ////<Query> ::= DELETE <DeleteParams> WHERE <Expression>
                result = _parserRule.CreateRULE_QUERY_DELETE_WHERE(reduction);
                break;
            case RULE_QUERY_SELECT2:
                ////<Query> ::= SELECT <AggregateFunction>
                result = _parserRule.CreateRULE_QUERY_SELECT2(reduction);
                break;
            case RULE_QUERY_SELECT_WHERE2:
                ////<Query> ::= SELECT <AggregateFunction> WHERE <Expression>
                result = _parserRule.CreateRULE_QUERY_SELECT_WHERE2(reduction);
                break;
            case RULE_QUERY_SELECT_GROUPBY:
                ////<Query> ::= SELECT <AggregateFunction> 'GROUP BY' <ObjectAttributeList>
                result = _parserRule.CreateRULE_QUERY_SELECT_GROUPBY(reduction);
                break;
            case RULE_QUERY_SELECT_WHERE_GROUPBY:
                ////<Query> ::= SELECT <AggregateFunction> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
                result = _parserRule.CreateRULE_QUERY_SELECT_WHERE_GROUPBY(reduction);
                break;
            case RULE_QUERY_SELECT_GROUPBY2:
                ////<Query> ::= SELECT <GroupByValueList> 'GROUP BY' <ObjectAttributeList>
                result = _parserRule.CreateRULE_QUERY_SELECT_GROUPBY2(reduction);
                break;
            case RULE_QUERY_SELECT_WHERE_GROUPBY2:
                ////<Query> ::= SELECT <GroupByValueList> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
                result = _parserRule.CreateRULE_QUERY_SELECT_WHERE_GROUPBY2(reduction);
                break;
            case RULE_EXPRESSION:
                ////<Expression> ::= <OrExpr>
                result = _parserRule.CreateRULE_EXPRESSION(reduction);
                break;
            case RULE_OREXPR_OR:
                ////<OrExpr> ::= <OrExpr> OR <AndExpr>
                result = _parserRule.CreateRULE_OREXPR_OR(reduction);
                break;
            case RULE_OREXPR:
                ////<OrExpr> ::= <AndExpr>
                result = _parserRule.CreateRULE_OREXPR(reduction);
                break;
            case RULE_ANDEXPR_AND:
                ////<AndExpr> ::= <AndExpr> AND <UnaryExpr>
                result = _parserRule.CreateRULE_ANDEXPR_AND(reduction);
                break;
            case RULE_ANDEXPR:
                ////<AndExpr> ::= <UnaryExpr>
                result = _parserRule.CreateRULE_ANDEXPR(reduction);
                break;
            case RULE_UNARYEXPR_NOT:
                ////<UnaryExpr> ::= NOT <CompareExpr>
                result = _parserRule.CreateRULE_UNARYEXPR_NOT(reduction);
                break;
            case RULE_UNARYEXPR:
                ////<UnaryExpr> ::= <CompareExpr>
                result = _parserRule.CreateRULE_UNARYEXPR(reduction);
                break;
            case RULE_COMPAREEXPR_EQ:
                ////<CompareExpr> ::= <Atrrib> '=' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_EQ(reduction);
                break;
            case RULE_COMPAREEXPR_EXCLAMEQ:
                ////<CompareExpr> ::= <Atrrib> '!=' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_EXCLAMEQ(reduction);
                break;
            case RULE_COMPAREEXPR_EQEQ:
                ////<CompareExpr> ::= <Atrrib> '==' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_EQEQ(reduction);
                break;
            case RULE_COMPAREEXPR_LTGT:
                ////<CompareExpr> ::= <Atrrib> '<>' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_LTGT(reduction);
                break;
            case RULE_COMPAREEXPR_LT:
                ////<CompareExpr> ::= <Atrrib> '<' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_LT(reduction);
                break;
            case RULE_COMPAREEXPR_GT:
                ////<CompareExpr> ::= <Atrrib> '>' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_GT(reduction);
                break;
            case RULE_COMPAREEXPR_LTEQ:
                ////<CompareExpr> ::= <Atrrib> '<=' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_LTEQ(reduction);
                break;
            case RULE_COMPAREEXPR_GTEQ:
                ////<CompareExpr> ::= <Atrrib> '>=' <Value>
                result = _parserRule.CreateRULE_COMPAREEXPR_GTEQ(reduction);
                break;
            case RULE_COMPAREEXPR_LIKE_STRINGLITERAL:
                ////<CompareExpr> ::= <Atrrib> LIKE StringLiteral
                result = _parserRule.CreateRULE_COMPAREEXPR_LIKE_STRINGLITERAL(reduction);
                break;
            case RULE_COMPAREEXPR_LIKE_QUESTION:
                ////<CompareExpr> ::= <Atrrib> LIKE '?'
                result = _parserRule.CreateRULE_COMPAREEXPR_LIKE_QUESTION(reduction);
                break;
            case RULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL:
                ////<CompareExpr> ::= <Atrrib> NOT LIKE StringLiteral
                result = _parserRule.CreateRULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL(reduction);
                break;
            case RULE_COMPAREEXPR_NOT_LIKE_QUESTION:
                ////<CompareExpr> ::= <Atrrib> NOT LIKE '?'
                result = _parserRule.CreateRULE_COMPAREEXPR_NOT_LIKE_QUESTION(reduction);
                break;
            case RULE_COMPAREEXPR_IN:
                ////<CompareExpr> ::= <Atrrib> IN <InList>
                result = _parserRule.CreateRULE_COMPAREEXPR_IN(reduction);
                break;
            case RULE_COMPAREEXPR_NOT_IN:
                ////<CompareExpr> ::= <Atrrib> NOT IN <InList>
                result = _parserRule.CreateRULE_COMPAREEXPR_NOT_IN(reduction);
                break;
            case RULE_COMPAREEXPR_IS_NULL:
                ////<CompareExpr> ::= <Atrrib> IS NULL
                result = _parserRule.CreateRULE_COMPAREEXPR_IS_NULL(reduction);
                break;
            case RULE_COMPAREEXPR_IS_NOT_NULL:
                ////<CompareExpr> ::= <Atrrib> IS NOT NULL
                result = _parserRule.CreateRULE_COMPAREEXPR_IS_NOT_NULL(reduction);
                break;
            case RULE_COMPAREEXPR_LPARAN_RPARAN:
                ////<CompareExpr> ::= '(' <Expression> ')'
                result = _parserRule.CreateRULE_COMPAREEXPR_LPARAN_RPARAN(reduction);
                break;
            case RULE_ATRRIB:
                ////<Atrrib> ::= <ObjectValue>
                result = _parserRule.CreateRULE_ATRRIB(reduction);
                break;
            case RULE_VALUE_MINUS:
                ////<Value> ::= '-' <NumLiteral>
                result = _parserRule.CreateRULE_VALUE_MINUS(reduction);
                break;
            case RULE_VALUE:
                ////<Value> ::= <NumLiteral>
                result = _parserRule.CreateRULE_VALUE(reduction);
                break;
            case RULE_VALUE2:
                ////<Value> ::= <StrLiteral>
                result = _parserRule.CreateRULE_VALUE2(reduction);
                break;
            case RULE_VALUE_TRUE:
                ////<Value> ::= true
                result = _parserRule.CreateRULE_VALUE_TRUE(reduction);
                break;
            case RULE_VALUE_FALSE:
                ////<Value> ::= false
                result = _parserRule.CreateRULE_VALUE_FALSE(reduction);
                break;
            case RULE_VALUE3:
                ////<Value> ::= <Date>
                result = _parserRule.CreateRULE_VALUE3(reduction);
                break;
            case RULE_DATE_DATETIME_DOT_NOW:
                ////<Date> ::= DateTime '.' now
                result = _parserRule.CreateRULE_DATE_DATETIME_DOT_NOW(reduction);
                break;
            case RULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN:
                ////<Date> ::= DateTime '(' StringLiteral ')'
                result = _parserRule.CreateRULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN(reduction);
                break;
            case RULE_STRLITERAL_STRINGLITERAL:
                ////<StrLiteral> ::= StringLiteral
                result = _parserRule.CreateRULE_STRLITERAL_STRINGLITERAL(reduction);
                break;
            case RULE_STRLITERAL_NULL:
                ////<StrLiteral> ::= NULL
                result = _parserRule.CreateRULE_STRLITERAL_NULL(reduction);
                break;
            case RULE_STRLITERAL_QUESTION:
                ////<StrLiteral> ::= '?'
                result = _parserRule.CreateRULE_STRLITERAL_QUESTION(reduction);
                break;
            case RULE_NUMLITERAL_INTEGERLITERAL:
                ////<NumLiteral> ::= IntegerLiteral
                result = _parserRule.CreateRULE_NUMLITERAL_INTEGERLITERAL(reduction);
                break;
            case RULE_NUMLITERAL_REALLITERAL:
                ////<NumLiteral> ::= RealLiteral
                result = _parserRule.CreateRULE_NUMLITERAL_REALLITERAL(reduction);
                break;
            case RULE_OBJECTTYPE_TIMES:
                ////<ObjectType> ::= '*'
                result = _parserRule.CreateRULE_OBJECTTYPE_TIMES(reduction);
                break;
            case RULE_OBJECTTYPE_DOLLARTEXTDOLLAR:
                ////<ObjectType> ::= '$Text$'
                result = _parserRule.CreateRULE_OBJECTTYPE_DOLLARTEXTDOLLAR(reduction);
                break;
            case RULE_OBJECTTYPE:
                ////<ObjectType> ::= <Property>
                result = _parserRule.CreateRULE_OBJECTTYPE(reduction);
                break;
            case RULE_OBJECTATTRIBUTE_IDENTIFIER:
                ////<ObjectAttribute> ::= Identifier
                result = _parserRule.CreateRULE_OBJECTATTRIBUTE_IDENTIFIER(reduction);
                break;
            case RULE_DELETEPARAMS_DOLLARTEXTDOLLAR:
                ////<DeleteParams> ::= '$Text$'
                result = _parserRule.CreateRULE_DELETEPARAMS_DOLLARTEXTDOLLAR(reduction);
                break;
            case RULE_DELETEPARAMS:
                ////<DeleteParams> ::= <Property>
                result = _parserRule.CreateRULE_DELETEPARAMS(reduction);
                break;
            case RULE_PROPERTY_DOT_IDENTIFIER:
                ////<Property> ::= <Property> '.' Identifier
                result = _parserRule.CreateRULE_PROPERTY_DOT_IDENTIFIER(reduction);
                break;
            case RULE_PROPERTY_IDENTIFIER:
                ////<Property> ::= Identifier
                result = _parserRule.CreateRULE_PROPERTY_IDENTIFIER(reduction);
                break;
            case RULE_TYPEPLUSATTRIBUTE_DOT:
                ////<TypePlusAttribute> ::= <Property> '.' <ObjectAttribute>
                result = _parserRule.CreateRULE_TYPEPLUSATTRIBUTE_DOT(reduction);
                break;
            case RULE_AGGREGATEFUNCTION:
                ////<AggregateFunction> ::= <SumFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTION(reduction);
                break;
            case RULE_AGGREGATEFUNCTION2:
                ////<AggregateFunction> ::= <CountFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTION2(reduction);
                break;
            case RULE_AGGREGATEFUNCTION3:
                ////<AggregateFunction> ::= <MinFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTION3(reduction);
                break;
            case RULE_AGGREGATEFUNCTION4:
                ////<AggregateFunction> ::= <MaxFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTION4(reduction);
                break;
            case RULE_AGGREGATEFUNCTION5:
                ////<AggregateFunction> ::= <AverageFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTION5(reduction);
                break;
            case RULE_SUMFUNCTION_SUMLPARAN_RPARAN:
                ////<SumFunction> ::= 'SUM(' <TypePlusAttribute> ')'
                result = _parserRule.CreateRULE_SUMFUNCTION_SUMLPARAN_RPARAN(reduction);
                break;
            case RULE_COUNTFUNCTION_COUNTLPARAN_RPARAN:
                ////<CountFunction> ::= 'COUNT(' <Property> ')'
                result = _parserRule.CreateRULE_COUNTFUNCTION_COUNTLPARAN_RPARAN(reduction);
                break;
            case RULE_MINFUNCTION_MINLPARAN_RPARAN:
                ////<MinFunction> ::= 'MIN(' <TypePlusAttribute> ')'
                result = _parserRule.CreateRULE_MINFUNCTION_MINLPARAN_RPARAN(reduction);
                break;
            case RULE_MAXFUNCTION_MAXLPARAN_RPARAN:
                ////<MaxFunction> ::= 'MAX(' <TypePlusAttribute> ')'
                result = _parserRule.CreateRULE_MAXFUNCTION_MAXLPARAN_RPARAN(reduction);
                break;
            case RULE_AVERAGEFUNCTION_AVGLPARAN_RPARAN:
                ////<AverageFunction> ::= 'AVG(' <TypePlusAttribute> ')'
                result = _parserRule.CreateRULE_AVERAGEFUNCTION_AVGLPARAN_RPARAN(reduction);
                break;
            case RULE_OBJECTATTRIBUTE_KEYWORD_DOT_IDENTIFIER:
                ////<ObjectAttribute> ::= Keyword '.' Identifier
                result = _parserRule.CreateRULE_OBJECTATTRIBUTE_KEYWORD_DOT_IDENTIFIER(reduction);
                break;
            case RULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER:
                ////<ObjectValue> ::= Keyword '.' Identifier
                result = _parserRule.CreateRULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER(reduction);
                break;
            case RULE_OBJECTVALUE_KEYWORD_DOT_TAG:
                ////<ObjectValue> ::= Keyword '.' Tag
                result = _parserRule.CreateRULE_OBJECTVALUE_KEYWORD_DOT_TAG(reduction);
                break;
            case RULE_INLIST_LPARAN_RPARAN:
                ////<InList> ::= '(' <ListType> ')'
                result = _parserRule.CreateRULE_INLIST_LPARAN_RPARAN(reduction);
                break;
            case RULE_LISTTYPE:
                ////<ListType> ::= <NumLiteralList>
                result = _parserRule.CreateRULE_LISTTYPE(reduction);
                break;
            case RULE_LISTTYPE2:
                ////<ListType> ::= <StrLiteralList>
                result = _parserRule.CreateRULE_LISTTYPE2(reduction);
                break;
            case RULE_LISTTYPE3:
                ////<ListType> ::= <DateList>
                result = _parserRule.CreateRULE_LISTTYPE3(reduction);
                break;
            case RULE_NUMLITERALLIST_COMMA:
                ////<NumLiteralList> ::= <NumLiteral> ',' <NumLiteralList>
                result = _parserRule.CreateRULE_NUMLITERALLIST_COMMA(reduction);
                break;
            case RULE_NUMLITERALLIST:
                ////<NumLiteralList> ::= <NumLiteral>
                result = _parserRule.CreateRULE_NUMLITERALLIST(reduction);
                break;
            case RULE_STRLITERALLIST_COMMA:
                ////<StrLiteralList> ::= <StrLiteral> ',' <StrLiteralList>
                result = _parserRule.CreateRULE_STRLITERALLIST_COMMA(reduction);
                break;
            case RULE_STRLITERALLIST:
                ////<StrLiteralList> ::= <StrLiteral>
                result = _parserRule.CreateRULE_STRLITERALLIST(reduction);
                break;
            case RULE_DATELIST_COMMA:
                ////<DateList> ::= <Date> ',' <DateList>
                result = _parserRule.CreateRULE_DATELIST_COMMA(reduction);
                break;
            case RULE_DATELIST:
                ////<DateList> ::= <Date>
                result = _parserRule.CreateRULE_DATELIST(reduction);
                break;
            case RULE_GROUPBYVALUELIST_COMMA:
                ////<GroupByValueList> ::= <ObjectAttribute> ',' <GroupByValueList>
                result = _parserRule.CreateRULE_GROUPBYVALUELIST_COMMA(reduction);
                break;
            case RULE_GROUPBYVALUELIST:
                ////<GroupByValueList> ::= <AggregateFunctionList>
                result = _parserRule.CreateRULE_GROUPBYVALUELIST(reduction);
                break;
            case RULE_AGGREGATEFUNCTIONLIST_COMMA:
                ////<AggregateFunctionList> ::= <AggregateFunction> ',' <AggregateFunctionList>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTIONLIST_COMMA(reduction);
                break;
            case RULE_AGGREGATEFUNCTIONLIST:
                ////<AggregateFunctionList> ::= <AggregateFunction>
                result = _parserRule.CreateRULE_AGGREGATEFUNCTIONLIST(reduction);
                break;
            case RULE_OBJECTATTRIBUTELIST_COMMA:
                ////<ObjectAttributeList> ::= <ObjectAttribute> ',' <ObjectAttributeList>
                result = _parserRule.CreateRULE_OBJECTATTRIBUTELIST_COMMA(reduction);
                break;
            case RULE_OBJECTATTRIBUTELIST:
                ////<ObjectAttributeList> ::= <ObjectAttribute>
                result = _parserRule.CreateRULE_OBJECTATTRIBUTELIST(reduction);
                break;
        }
        if (result == null) {
            result = reduction;
        }
        return result;
    }
}
