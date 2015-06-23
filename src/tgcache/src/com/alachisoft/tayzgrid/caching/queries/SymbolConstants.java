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

public enum SymbolConstants
{

    SYMBOL_EOF(0), // (EOF)
    SYMBOL_ERROR(1), // (Error)
    SYMBOL_WHITESPACE(2), // (Whitespace)
    SYMBOL_MINUS(3), // '-'
    SYMBOL_EXCLAMEQ(4), // '!='
    SYMBOL_DOLLARTEXTDOLLAR(5), // '$Text$'
    SYMBOL_LPARAN(6), // '('
    SYMBOL_RPARAN(7), // ')'
    SYMBOL_TIMES(8), // '*'
    SYMBOL_COMMA(9), // ','
    SYMBOL_DOT(10), // '.'
    SYMBOL_QUESTION(11), // '?'
    SYMBOL_LT(12), // '<'
    SYMBOL_LTEQ(13), // '<='
    SYMBOL_LTGT(14), // '<>'
    SYMBOL_EQ(15), // '='
    SYMBOL_EQEQ(16), // '=='
    SYMBOL_GT(17), // '>'
    SYMBOL_GTEQ(18), // '>='
    SYMBOL_AND(19), // AND
    SYMBOL_AVGLPARAN(20), // 'AVG('
    SYMBOL_COUNTLPARAN(21), // 'COUNT('
    SYMBOL_DATETIME(22), // DateTime
    SYMBOL_FALSE(23), // false
    SYMBOL_IDENTIFIER(24), // Identifier
    SYMBOL_IN(25), // IN
    SYMBOL_INTEGERLITERAL(26), // IntegerLiteral
    SYMBOL_IS(27), // IS
    SYMBOL_KEYWORD(28), // Keyword
    SYMBOL_LIKE(29), // LIKE
    SYMBOL_MAXLPARAN(30), // 'MAX('
    SYMBOL_MINLPARAN(31), // 'MIN('
    SYMBOL_NOT(32), // NOT
    SYMBOL_NOW(33), // now
    SYMBOL_NULL(34), // NULL
    SYMBOL_OR(35), // OR
    SYMBOL_REALLITERAL(36), // RealLiteral
    SYMBOL_SELECT(37), // SELECT
    SYMBOL_STRINGLITERAL(38), // StringLiteral
    SYMBOL_SUMLPARAN(39), // 'SUM('
    SYMBOL_TAG(40), // Tag
    SYMBOL_TRUE(41), // true
    SYMBOL_WHERE(42), // WHERE
    SYMBOL_AGGREGATEFUNCTION(43), // <AggregateFunction>
    SYMBOL_ANDEXPR(44), // <AndExpr>
    SYMBOL_ATRRIB(45), // <Atrrib>
    SYMBOL_AVERAGEFUNCTION(46), // <AverageFunction>
    SYMBOL_COMPAREEXPR(47), // <CompareExpr>
    SYMBOL_COUNTFUNCTION(48), // <CountFunction>
    SYMBOL_DATE(49), // <Date>
    SYMBOL_DATELIST(50), // <DateList>
    SYMBOL_EXPRESSION(51), // <Expression>
    SYMBOL_INLIST(52), // <InList>
    SYMBOL_LISTTYPE(53), // <ListType>
    SYMBOL_MAXFUNCTION(54), // <MaxFunction>
    SYMBOL_MINFUNCTION(55), // <MinFunction>
    SYMBOL_NUMLITERAL(56), // <NumLiteral>
    SYMBOL_NUMLITERALLIST(57), // <NumLiteralList>
    SYMBOL_OBJECTATTRIBUTE(58), // <ObjectAttribute>
    SYMBOL_OBJECTTYPE(59), // <ObjectType>
    SYMBOL_OBJECTVALUE(60), // <ObjectValue>
    SYMBOL_OREXPR(61), // <OrExpr>
    SYMBOL_PROPERTY(62), // <Property>
    SYMBOL_QUERY(63), // <Query>
    SYMBOL_STRLITERAL(64), // <StrLiteral>
    SYMBOL_STRLITERALLIST(65), // <StrLiteralList>
    SYMBOL_SUMFUNCTION(66), // <SumFunction>
    SYMBOL_TYPEPLUSATTRIBUTE(67), // <TypePlusAttribute>
    SYMBOL_UNARYEXPR(68), // <UnaryExpr>
    SYMBOL_VALUE(69); // <Value>
    private int intValue;
    private static java.util.HashMap<Integer, SymbolConstants> mappings;

    private static java.util.HashMap<Integer, SymbolConstants> getMappings()
    {
        if (mappings == null)
        {
            synchronized (SymbolConstants.class)
            {
                if (mappings == null)
                {
                    mappings = new java.util.HashMap<Integer, SymbolConstants>();
                }
            }
        }
        return mappings;
    }

    private SymbolConstants(int value)
    {
        intValue = value;
        SymbolConstants.getMappings().put(value, this);
    }

    public int getValue()
    {
        return intValue;
    }

    public static SymbolConstants forValue(int value)
    {
        return getMappings().get(value);
    }
}
