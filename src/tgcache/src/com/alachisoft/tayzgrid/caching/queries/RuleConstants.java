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

public enum RuleConstants
{
    RULE_QUERY_SELECT(0), // <Query> ::= SELECT <ObjectType>
    RULE_QUERY_SELECT_WHERE(1), // <Query> ::= SELECT <ObjectType> WHERE <Expression>
    RULE_EXPRESSION(2), // <Expression> ::= <OrExpr>
    RULE_OREXPR_OR(3), // <OrExpr> ::= <OrExpr> OR <AndExpr>
    RULE_OREXPR(4), // <OrExpr> ::= <AndExpr>
    RULE_ANDEXPR_AND(5), // <AndExpr> ::= <AndExpr> AND <UnaryExpr>
    RULE_ANDEXPR(6), // <AndExpr> ::= <UnaryExpr>
    RULE_UNARYEXPR_NOT(7), // <UnaryExpr> ::= NOT <CompareExpr>
    RULE_UNARYEXPR(8), // <UnaryExpr> ::= <CompareExpr>
    RULE_COMPAREEXPR_EQ(9), // <CompareExpr> ::= <Atrrib> '=' <Value>
    RULE_COMPAREEXPR_EXCLAMEQ(10), // <CompareExpr> ::= <Atrrib> '!=' <Value>
    RULE_COMPAREEXPR_EQEQ(11), // <CompareExpr> ::= <Atrrib> '==' <Value>
    RULE_COMPAREEXPR_LTGT(12), // <CompareExpr> ::= <Atrrib> '<>' <Value>
    RULE_COMPAREEXPR_LT(13), // <CompareExpr> ::= <Atrrib> '<' <Value>
    RULE_COMPAREEXPR_GT(14), // <CompareExpr> ::= <Atrrib> '>' <Value>
    RULE_COMPAREEXPR_LTEQ(15), // <CompareExpr> ::= <Atrrib> '<=' <Value>
    RULE_COMPAREEXPR_GTEQ(16), // <CompareExpr> ::= <Atrrib> '>=' <Value>
    RULE_COMPAREEXPR_LIKE_STRINGLITERAL(17), // <CompareExpr> ::= <Atrrib> LIKE StringLiteral
    RULE_COMPAREEXPR_LIKE_QUESTION(18), // <CompareExpr> ::= <Atrrib> LIKE '?'
    RULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL(19), // <CompareExpr> ::= <Atrrib> NOT LIKE StringLiteral
    RULE_COMPAREEXPR_NOT_LIKE_QUESTION(20), // <CompareExpr> ::= <Atrrib> NOT LIKE '?'
    RULE_COMPAREEXPR_IN(21), // <CompareExpr> ::= <Atrrib> IN <InList>
    RULE_COMPAREEXPR_NOT_IN(22), // <CompareExpr> ::= <Atrrib> NOT IN <InList>
    RULE_COMPAREEXPR_IS_NULL(23), // <CompareExpr> ::= <Atrrib> IS NULL
    RULE_COMPAREEXPR_IS_NOT_NULL(24), // <CompareExpr> ::= <Atrrib> IS NOT NULL
    RULE_COMPAREEXPR_LPARAN_RPARAN(25), // <CompareExpr> ::= '(' <Expression> ')'
    RULE_ATRRIB(26), // <Atrrib> ::= <ObjectValue>
    RULE_VALUE_MINUS(27), // <Value> ::= '-' <NumLiteral>
    RULE_VALUE(28), // <Value> ::= <NumLiteral>
    RULE_VALUE2(29), // <Value> ::= <StrLiteral>
    RULE_VALUE_TRUE(30), // <Value> ::= true
    RULE_VALUE_FALSE(31), // <Value> ::= false
    RULE_VALUE3(32), // <Value> ::= <Date>
    RULE_DATE_DATETIME_DOT_NOW(33), // <Date> ::= DateTime '.' now
    RULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN(34), // <Date> ::= DateTime '(' StringLiteral ')'
    RULE_STRLITERAL_STRINGLITERAL(35), // <StrLiteral> ::= StringLiteral
    RULE_STRLITERAL_NULL(36), // <StrLiteral> ::= NULL
    RULE_STRLITERAL_QUESTION(37), // <StrLiteral> ::= '?'
    RULE_NUMLITERAL_INTEGERLITERAL(38), // <NumLiteral> ::= IntegerLiteral
    RULE_NUMLITERAL_REALLITERAL(39), // <NumLiteral> ::= RealLiteral
    RULE_OBJECTTYPE_TIMES(40), // <ObjectType> ::= '*'
    RULE_OBJECTTYPE_DOLLARTEXTDOLLAR(41), // <ObjectType> ::= '$Text$'
    RULE_OBJECTTYPE(42), // <ObjectType> ::= <Property>
    RULE_OBJECTTYPE2(43), // <ObjectType> ::= <AggregateFunction>
    RULE_OBJECTATTRIBUTE_IDENTIFIER(44), // <ObjectAttribute> ::= Identifier
    RULE_PROPERTY_DOT_IDENTIFIER(45), // <Property> ::= <Property> '.' Identifier
    RULE_PROPERTY_IDENTIFIER(46), // <Property> ::= Identifier
    RULE_TYPEPLUSATTRIBUTE_DOT(47), // <TypePlusAttribute> ::= <Property> '.' <ObjectAttribute>
    RULE_AGGREGATEFUNCTION(48), // <AggregateFunction> ::= <SumFunction>
    RULE_AGGREGATEFUNCTION2(49), // <AggregateFunction> ::= <CountFunction>
    RULE_AGGREGATEFUNCTION3(50), // <AggregateFunction> ::= <MinFunction>
    RULE_AGGREGATEFUNCTION4(51), // <AggregateFunction> ::= <MaxFunction>
    RULE_AGGREGATEFUNCTION5(52), // <AggregateFunction> ::= <AverageFunction>
    RULE_SUMFUNCTION_SUMLPARAN_RPARAN(53), // <SumFunction> ::= 'SUM(' <TypePlusAttribute> ')'
    RULE_COUNTFUNCTION_COUNTLPARAN_RPARAN(54), // <CountFunction> ::= 'COUNT(' <Property> ')'
    RULE_MINFUNCTION_MINLPARAN_RPARAN(55), // <MinFunction> ::= 'MIN(' <TypePlusAttribute> ')'
    RULE_MAXFUNCTION_MAXLPARAN_RPARAN(56), // <MaxFunction> ::= 'MAX(' <TypePlusAttribute> ')'
    RULE_AVERAGEFUNCTION_AVGLPARAN_RPARAN(57), // <AverageFunction> ::= 'AVG(' <TypePlusAttribute> ')'
    RULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER(58), // <ObjectValue> ::= Keyword '.' Identifier
    RULE_OBJECTVALUE_KEYWORD_DOT_TAG(59), // <ObjectValue> ::= Keyword '.' Tag
    RULE_INLIST_LPARAN_RPARAN(60), // <InList> ::= '(' <ListType> ')'
    RULE_LISTTYPE(61), // <ListType> ::= <NumLiteralList>
    RULE_LISTTYPE2(62), // <ListType> ::= <StrLiteralList>
    RULE_LISTTYPE3(63), // <ListType> ::= <DateList>
    RULE_NUMLITERALLIST_COMMA(64), // <NumLiteralList> ::= <NumLiteral> ',' <NumLiteralList>
    RULE_NUMLITERALLIST(65), // <NumLiteralList> ::= <NumLiteral>
    RULE_STRLITERALLIST_COMMA(66), // <StrLiteralList> ::= <StrLiteral> ',' <StrLiteralList>
    RULE_STRLITERALLIST(67), // <StrLiteralList> ::= <StrLiteral>
    RULE_DATELIST_COMMA(68), // <DateList> ::= <Date> ',' <DateList>
    RULE_DATELIST(69); // <DateList> ::= <Date>
    private int intValue;
    private static java.util.HashMap<Integer, RuleConstants> mappings;

    private static java.util.HashMap<Integer, RuleConstants> getMappings()
    {
        if (mappings == null)
        {
            synchronized (RuleConstants.class)
            {
                if (mappings == null)
                {
                    mappings = new java.util.HashMap<Integer, RuleConstants>();
                }
            }
        }
        return mappings;
    }

    private RuleConstants(int value)
    {
        intValue = value;
        RuleConstants.getMappings().put(value, this);
    }

    public int getValue()
    {
        return intValue;
    }

    public static RuleConstants forValue(int value)
    {
        return getMappings().get(value);
    }
}
