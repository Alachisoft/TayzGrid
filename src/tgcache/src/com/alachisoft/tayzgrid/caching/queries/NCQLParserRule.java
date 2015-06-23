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

import com.alachisoft.tayzgrid.caching.queries.filters.AggregateFunctionPredicate;
import com.alachisoft.tayzgrid.caching.queries.filters.CompositeFunction;
import com.alachisoft.tayzgrid.caching.queries.filters.ConstantValue;
import com.alachisoft.tayzgrid.caching.queries.filters.DateTimeConstantValue;
import com.alachisoft.tayzgrid.caching.queries.filters.DoubleConstantValue;
import com.alachisoft.tayzgrid.caching.queries.filters.ExpressionBuilder;
import com.alachisoft.tayzgrid.caching.queries.filters.FalseValue;
import com.alachisoft.tayzgrid.caching.queries.filters.GroupByPredicate;
import com.alachisoft.tayzgrid.caching.queries.filters.IFunctor;
import com.alachisoft.tayzgrid.caching.queries.filters.IdentityFunction;
import com.alachisoft.tayzgrid.caching.queries.filters.IntegerConstantValue;
import com.alachisoft.tayzgrid.caching.queries.filters.IsInListPredicate;
import com.alachisoft.tayzgrid.caching.queries.filters.IsNullPredicate;
import com.alachisoft.tayzgrid.caching.queries.filters.IsOfTypePredicate;
import com.alachisoft.tayzgrid.caching.queries.filters.MemberFunction;
import com.alachisoft.tayzgrid.caching.queries.filters.NullValue;
import com.alachisoft.tayzgrid.caching.queries.filters.Predicate;
import com.alachisoft.tayzgrid.caching.queries.filters.RuntimeValue;
import com.alachisoft.tayzgrid.caching.queries.filters.StringConstantValue;
import com.alachisoft.tayzgrid.caching.queries.filters.TrueValue;
import com.alachisoft.tayzgrid.common.exceptions.RuntimeParsingException;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.parser.Reduction;
import com.alachisoft.tayzgrid.parser.Token;
import java.util.Collections;

public class NCQLParserRule 
{
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }

    public NCQLParserRule() 
    {
        
    }

    public NCQLParserRule(ILogger NCacheLog)
    {
        this._ncacheLog = NCacheLog;
    }

    /**
     * Implements <Query> ::= SELECT <TypeIdentifier>
     */
    protected Reduction CreateRULE_QUERY_SELECT(Reduction reduction)
    {
        Object selectType = ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();

        Predicate selectTypePredicate = (Predicate) ((selectType instanceof Predicate) ? selectType : null);

        if (selectTypePredicate == null)
        {
            reduction.setTag(new IsOfTypePredicate(selectType.toString()));
        }
        else
        {
            reduction.setTag(selectTypePredicate);
        }

        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT");
        }
        return null;
    }
    
    ///Implements <Query> ::= SELECT <AggregateFunction>
    protected Reduction CreateRULE_QUERY_SELECT2(Reduction reduction)
    {
        return CreateRULE_QUERY_SELECT(reduction);
    }

    /**
     * Implements <Query> ::= SELECT <TypeIdentifier> WHERE <Expression>
     */
    protected Reduction CreateRULE_QUERY_SELECT_WHERE(Reduction reduction)
    {
        //selectType can be one of the following depending on the query text: -
        //1. A plain string that is the name of Type; we can build IsOfTypePredicate from this.
        //2. AggregateFunctionPredicate that has IsOfTypePredicate set as its ChildPredicate
        //3. IsOfTypePredicate

        Object selectType = ((Reduction) reduction.GetToken(1).getData()).getTag();

        Predicate lhs = null;
        Predicate rhs = (Predicate) ((Reduction) reduction.GetToken(3).getData()).getTag();
        Predicate selectTypePredicate = (Predicate) ((selectType instanceof Predicate) ? selectType : null);
        Predicate result = null;

        if (selectTypePredicate == null)
        {
            lhs = new IsOfTypePredicate(selectType.toString());
            result = ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs);
        }
        else
        {
            lhs = selectTypePredicate;
            result = ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs);
        }

        reduction.setTag(result);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_WHERE");
        }
        return null;
    }
    
    ///Implements <Query> ::= SELECT <AggregateFunction> WHERE <Expression>
    protected Reduction CreateRULE_QUERY_SELECT_WHERE2(Reduction reduction)
    {
        //selectType can be one of the following depending on the query text: -
        // AggregateFunctionPredicate that has IsOfTypePredicate set as its ChildPredicate

        Object selectType = ((Reduction)reduction.GetToken(1).getData()).getTag();

        Predicate lhs = null;
        Predicate rhs = (Predicate)((Reduction)reduction.GetToken(3).getData()).getTag();
        Predicate selectTypePredicate = (Predicate)selectType;
        Predicate result = null;

        AggregateFunctionPredicate parentPredicate = (AggregateFunctionPredicate)selectTypePredicate;
        lhs = parentPredicate.getChildPredicate();
        parentPredicate.setChildPredicate(ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs));
        result = parentPredicate;

        reduction.setTag(result);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_WHERE2");
        }
        return null;
    }
    
    /// Implements <Query> ::= DELETE <TypeIdentifier>
    protected Reduction CreateRULE_QUERY_DELETE(Reduction reduction) 
    {
        Object selectType = ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();

        Predicate selectTypePredicate = selectType instanceof Predicate ? (Predicate) (selectType) : null;

        if (selectTypePredicate == null) {
            reduction.setTag(new IsOfTypePredicate(selectType.toString()));
        } else {
            reduction.setTag(selectTypePredicate);
        }

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("CreateRULE_QUERY_DELETE");
        }
        return null;
    }
    
    /// Implements <Query> ::= DELETE <TypeIdentifier> WHERE <Expression>      
    public Reduction CreateRULE_QUERY_DELETE_WHERE(Reduction reduction) 
    {
        //deleteType can be one of the following depending on the query text: -
        //1. A plain string that is the name of Type; we can build IsOfTypePredicate from this.
        //2. IsOfTypePredicate

        Object selectType = ((Reduction) reduction.GetToken(1).getData()).getTag();

        Predicate lhs = null;
        Predicate rhs = (Predicate) ((Reduction) reduction.GetToken(3).getData()).getTag();
        Predicate selectTypePredicate = selectType instanceof Predicate ? (Predicate) (selectType) : null;
        Predicate result = null;

        if (selectTypePredicate == null) {
            lhs = new IsOfTypePredicate(selectType.toString());
            result = ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs);
        } 
        else {
            lhs = selectTypePredicate;
            result = ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs);
        }

        reduction.setTag(result);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_DELETE_WHERE");
        }
        return null;
    }
    
    ///Implements <Query> ::= SELECT <AggregateFunction> 'GROUP BY' <ObjectAttributeList>
    protected Reduction CreateRULE_QUERY_SELECT_GROUPBY(Reduction reduction) 
    {
        Object selectType = ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();

        AggregateFunctionPredicate aggregateFunctionPredicate = (AggregateFunctionPredicate) selectType;

        if (aggregateFunctionPredicate == null) {
            throw new RuntimeParsingException("Invalid query. GROUP BY can only be used with Aggregate functions.");
        }
        GroupByPredicate gbp = null;
        if (((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag() instanceof GroupByPredicate) {
            gbp = (GroupByPredicate) ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag();
        } else {
            gbp = new GroupByPredicate();
            gbp.getAttributeNamesList().add(((MemberFunction) ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag()).getMemberName());
        }
        gbp.setChildPredicate(aggregateFunctionPredicate.getChildPredicate());
        aggregateFunctionPredicate.setChildPredicate(null);
        gbp.setGroupByValueList(new GroupByValueList());

        gbp.getGroupByValueList().getAggregateFunctionsList().add(aggregateFunctionPredicate);
        reduction.setTag(gbp);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_GROUPBY");
        }
        return null;
    }
    
    ///Implements<Query> ::= SELECT <GroupByValueList> 'GROUP BY' <ObjectAttributeList>
    public Reduction CreateRULE_QUERY_SELECT_GROUPBY2(Reduction reduction) 
    {
        Object groupBy = ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag();
        GroupByPredicate groupByPredicate = groupBy instanceof GroupByPredicate?(GroupByPredicate) groupBy:null;;
        if (groupByPredicate == null) {
            groupByPredicate = new GroupByPredicate();
            groupByPredicate.getAttributeNamesList().add(((MemberFunction) groupBy).getMemberName());
        }

        groupByPredicate.setGroupByValueList((GroupByValueList) ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag());
        //parsed in reverse order
        Collections.reverse(groupByPredicate.getGroupByValueList().getAggregateFunctionsList());
        Collections.reverse(groupByPredicate.getGroupByValueList().getObjectAttributesList());
        groupByPredicate.setChildPredicate(groupByPredicate.getGroupByValueList().getAggregateFunctionsList().get(0).getChildPredicate());

            for(String attribute : groupByPredicate.getGroupByValueList().getObjectAttributesList())
            {
                if (!groupByPredicate.getAttributeNamesList().contains(attribute))
                    throw new RuntimeParsingException("Invalid query. " + attribute + " must be specified in GROUP BY clause.");
            }
        reduction.setTag(groupByPredicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_GROUPBY2");
        }
        return null;
    }
        
    ///Implements <Query> ::= SELECT <AggregateFunction> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
    protected Reduction CreateRULE_QUERY_SELECT_WHERE_GROUPBY(Reduction reduction) 
    {
        Object groupBy = ((Reduction) ((Token) reduction.GetToken(5)).getData()).getTag();
        GroupByPredicate groupByPredicate = null;
        if(groupBy instanceof GroupByPredicate)
        {
            groupByPredicate = (GroupByPredicate)groupBy;
        }
        else 
        {
            groupByPredicate = new GroupByPredicate();
            groupByPredicate.getAttributeNamesList().add(((MemberFunction) groupBy).getMemberName());
        }

        GroupByValueList gbv = new GroupByValueList();
        AggregateFunctionPredicate afp = (AggregateFunctionPredicate) ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();
        Predicate lhs = afp.getChildPredicate();
        gbv.getAggregateFunctionsList().add(afp);
        Predicate rhs = (Predicate) ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag();
        groupByPredicate.setChildPredicate(ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs));
        groupByPredicate.setGroupByValueList(gbv);
        for(String attribute : groupByPredicate.getGroupByValueList().getObjectAttributesList())
        {
            if (!groupByPredicate.getAttributeNamesList().contains(attribute))
                throw new RuntimeParsingException(attribute + " must be specified in GROUP BY clause.");
        }
        reduction.setTag(groupByPredicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_WHERE_GROUPBY");
        }
        return null;
    }
    
    ///Implements <Query> ::= SELECT <GroupByValueList> WHERE <Expression> 'GROUP BY' <ObjectAttributeList>
    protected Reduction CreateRULE_QUERY_SELECT_WHERE_GROUPBY2(Reduction reduction) 
    {
        Object groupBy = ((Reduction) ((Token) reduction.GetToken(5)).getData()).getTag();
        GroupByPredicate groupByPredicate = null;
        if (groupBy instanceof GroupByPredicate) {
            groupByPredicate = (GroupByPredicate) groupBy;
        } else {
            groupByPredicate = new GroupByPredicate();
            groupByPredicate.getAttributeNamesList().add(((MemberFunction) groupBy).getMemberName());
        }

        groupByPredicate.setGroupByValueList((GroupByValueList) ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag());
        //parsed in reverse order
        Collections.reverse(groupByPredicate.getGroupByValueList().getAggregateFunctionsList());
        Collections.reverse(groupByPredicate.getGroupByValueList().getObjectAttributesList());

        Predicate lhs = groupByPredicate.getGroupByValueList().getAggregateFunctionsList().get(0).getChildPredicate();
        Predicate rhs = (Predicate) ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag();
        groupByPredicate.setChildPredicate(ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs));

        for (String attribute : groupByPredicate.getGroupByValueList().getObjectAttributesList()) 
        {
            if (!groupByPredicate.getAttributeNamesList().contains(attribute)) 
            {
                throw new RuntimeParsingException(attribute + " must be specified in GROUP BY clause.");
            }
        }
        reduction.setTag(groupByPredicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_QUERY_SELECT_WHERE_GROUPBY2");
        }
        return null;
    }

    /**
     * Implements <Expression> ::= <OrExpr>
     */
    protected Reduction CreateRULE_EXPRESSION(Reduction reduction)
    {
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_EXPRESSION");
        }
        return null;
    }

    /**
     * Implements <OrExpr> ::= <OrExpr> OR <AndExpr>
     */
    protected Reduction CreateRULE_OREXPR_OR(Reduction reduction)
    {
        Predicate lhs = (Predicate) ((Reduction) reduction.GetToken(0).getData()).getTag();
        Predicate rhs = (Predicate) ((Reduction) reduction.GetToken(2).getData()).getTag();
        reduction.setTag(ExpressionBuilder.CreateLogicalOrPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_OREXPR_OR");
        }
        return null;
    }

    /**
     * Implements <OrExpr> ::= <AndExpr>
     */
    protected Reduction CreateRULE_OREXPR(Reduction reduction)
    {
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_OREXPR");
        }
        return null;
    }

    /**
     * Implements <AndExpr> ::= <AndExpr> AND <UnaryExpr>
     */
    protected Reduction CreateRULE_ANDEXPR_AND(Reduction reduction)
    {
        Predicate lhs = (Predicate) ((Reduction) reduction.GetToken(0).getData()).getTag();
        Predicate rhs = (Predicate) ((Reduction) reduction.GetToken(2).getData()).getTag();
        reduction.setTag(ExpressionBuilder.CreateLogicalAndPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_ANDEXPR_AND");
        }
        return null;
    }

    /**
     * Implements <AndExpr> ::= <UnaryExpr>
     */
    protected Reduction CreateRULE_ANDEXPR(Reduction reduction)
    {
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_ANDEXPR");
        }
        return null;
    }

    /**
     * Implements <UnaryExpr> ::= NOT <CompareExpr>
     */
    protected Reduction CreateRULE_UNARYEXPR_NOT(Reduction reduction)
    {
        Predicate pred = (Predicate) ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();
        pred.Invert();
        reduction.setTag(pred);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_UNARYEXPR_NOT");
        }
        return null;
    }

    /**
     * Implements <UnaryExpr> ::= <CompareExpr>
     */
    protected Reduction CreateRULE_UNARYEXPR(Reduction reduction)
    {
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_UNARYEXPR");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '=' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_EQ(Reduction reduction)
    {
        return CreateRULE_COMPAREEXPR_EQEQ(reduction);
    }

    /**
     * Implements <CompareExpr> ::= <Value> '!=' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_EXCLAMEQ(Reduction reduction)
    {
        return CreateRULE_COMPAREEXPR_LTGT(reduction);
    }

    /**
     * Implements <CompareExpr> ::= <Value> '==' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_EQEQ(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateEqualsPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_EQEQ");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '<>' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_LTGT(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateNotEqualsPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LTGT");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '<' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_LT(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateLesserPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LT");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '>' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_GT(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateGreaterPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_GT");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '<=' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_LTEQ(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateLesserEqualsPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LTEQ");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> '>=' <Value>
     */
    protected Reduction CreateRULE_COMPAREEXPR_GTEQ(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object rhs = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();

        reduction.setTag(ExpressionBuilder.CreateGreaterEqualsPredicate(lhs, rhs));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_GTEQ");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> LIKE StringLiteral
     */
    protected Reduction CreateRULE_COMPAREEXPR_LIKE_STRINGLITERAL(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        RuntimeValue rhs = new RuntimeValue();
        Predicate predicate = ExpressionBuilder.CreateLikePatternPredicate(lhs, rhs);
        reduction.setTag(predicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LIKE_STRINGLITERAL");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> NOT LIKE StringLiteral
     */
    protected Reduction CreateRULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        RuntimeValue rhs = new RuntimeValue();
        Predicate predicate = ExpressionBuilder.CreateLikePatternPredicate(lhs, rhs);
        predicate.Invert();
        reduction.setTag(predicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LIKE_STRINGLITERAL");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> IN <InList>
     */
    protected Reduction CreateRULE_COMPAREEXPR_IN(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Object tempVar = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();
        IsInListPredicate pred = (IsInListPredicate) ((tempVar instanceof IsInListPredicate) ? tempVar : null);
        pred.setFunctor((IFunctor) ((lhs instanceof IFunctor) ? lhs : null));
        reduction.setTag(pred);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_IN");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> NOT IN <InList>
     */
    protected Reduction CreateRULE_COMPAREEXPR_NOT_IN(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        IsInListPredicate pred = (IsInListPredicate) ((Reduction) ((Token) reduction.GetToken(3)).getData()).getTag();
        pred.Invert();
        pred.setFunctor((IFunctor) ((lhs instanceof IFunctor) ? lhs : null));
        reduction.setTag(pred);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_NOT_IN");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> IS NUll
     */
    protected Reduction CreateRULE_COMPAREEXPR_IS_NULL(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Predicate predicate = new IsNullPredicate((IFunctor) ((lhs instanceof IFunctor) ? lhs : null));
        reduction.setTag(predicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_COMPAREEXPR_IS_NULL");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= <Value> IS NOT NUll
     */
    protected Reduction CreateRULE_COMPAREEXPR_IS_NOT_NULL(Reduction reduction)
    {
        Object lhs = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        Predicate predicate = new IsNullPredicate((IFunctor) ((lhs instanceof IFunctor) ? lhs : null));
        predicate.Invert();
        reduction.setTag(predicate);
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_COMPAREEXPR_IS_NOT_NULL");
        }
        return null;
    }

    /**
     * Implements <CompareExpr> ::= '(' <Expression> ')'
     */
    protected Reduction CreateRULE_COMPAREEXPR_LPARAN_RPARAN(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_COMPAREEXPR_LPARAN_RPARAN");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag());
        return null;
    }

    /**
     * Implements <Value> ::= <ObjectValue>
     */
    protected Reduction CreateRULE_VALUE(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <Value> ::= '-' <NumLiteral>
     */
    protected Reduction CreateRULE_VALUE_MINUS(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_VALUE_MINUS");
        }
        Object functor = ((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag();
        if (functor instanceof IntegerConstantValue)
        {
            reduction.setTag(new IntegerConstantValue("-" + reduction.GetToken(1).getData().toString()));
        }
        else
        {
            reduction.setTag(new DoubleConstantValue("-" + reduction.GetToken(1).getData().toString()));
        }
        return null;
    }

    /**
     * Implements <Value> ::= <NumLiteral>
     */
    protected Reduction CreateRULE_VALUE2(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE2");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <Value> ::= <StrLiteral>
     */
    protected Reduction CreateRULE_VALUE3(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE3");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <Value> ::= true
     */
    protected Reduction CreateRULE_VALUE_TRUE(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE_TRUE");
        }
        reduction.setTag(new TrueValue());
        return null;
    }

    /**
     * Implements <Value> ::= false
     */
    protected Reduction CreateRULE_VALUE_FALSE(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE_FALSE");
        }
        reduction.setTag(new FalseValue());
        return null;
    }

    /**
     * Implements <Value> ::= <Date>
     */
    protected Reduction CreateRULE_VALUE4(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_VALUE4");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <Date> ::= DateTime '.' now
     */
    protected Reduction CreateRULE_DATE_DATETIME_DOT_NOW(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_DATE_DATETIME_DOT_NOW");
        }
        reduction.setTag(new DateTimeConstantValue());
        return null;
    }

    /**
     * Implements <Date> ::= DateTime '(' <StrLiteral> ')'
     */
    protected Reduction CreateRULE_DATE_DATETIME_LPARAN_RPARAN(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_DATE_DATETIME_LPARAN_RPARAN");
        }
        reduction.setTag(new DateTimeConstantValue(reduction.GetToken(2).getData().toString()));
        return null;
    }

    /**
     * Implements <StrLiteral> ::= StringLiteral
     */
    protected Reduction CreateRULE_STRLITERAL_STRINGLITERAL(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_STRLITERAL_STRINGLITERAL");
        }
        reduction.setTag(new StringConstantValue(reduction.GetToken(0).getData().toString()));
        return null;
    }

    /**
     * Implements <StrLiteral> ::= NUll
     */
    protected Reduction CreateRULE_STRLITERAL_NULL(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_STRLITERAL_NULL");
        }
        reduction.setTag(new NullValue());
        return null;
    }

    /**
     * Implements <StrLiteral> ::= ?
     */
    protected Reduction CreateRULE_STRLITERAL_QUESTION(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_STRLITERAL_QUESTION");
        }
        reduction.setTag(new RuntimeValue());
        return null;
    }

    /**
     * Implements <NumLiteral> ::= IntegerLiteral
     */
    protected Reduction CreateRULE_NUMLITERAL_INTEGERLITERAL(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_NUMLITERAL_INTEGERLITERAL");
        }
        reduction.setTag(new IntegerConstantValue(reduction.GetToken(0).getData().toString()));
        return null;
    }

    /**
     * Implements <NumLiteral> ::= RealLiteral
     */
    protected Reduction CreateRULE_NUMLITERAL_REALLITERAL(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_NUMLITERAL_REALLITERAL");
        }
        reduction.setTag(new DoubleConstantValue(reduction.GetToken(0).getData().toString()));
        return null;
    }

    /**
     * Implements <TypeIdentifier> ::= '*'
     */
    protected Reduction CreateRULE_OBJECTTYPE_TIMES(Reduction reduction)
    {
        //reduction.Tag = ExpressionBuilder.TRUE_PREDICATE;
        reduction.setTag("*");
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_OBJECTTYPE_TIMES");
        }
        return null;
    }

    /**
     * Implements <TypeIdentifier> ::= '$Text$'
     */
    protected Reduction CreateRULE_OBJECTTYPE_DOLLARTEXTDOLLAR(Reduction reduction)
    {
        reduction.setTag("java.lang.String");
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_OBJECTTYPE_DOLLARTEXTDOLLAR");
        }
        return null;
    }

    /**
     * Implements <TypeIdentifier> ::= <Identifier>
     */
    protected Reduction CreateRULE_OBJECTTYPE_IDENTIFIER(Reduction reduction)
    {
        reduction.setTag(((Token) reduction.GetToken(0)).getData());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_TYPEIDENTIFIER_IDENTIFIER");
        }
        return null;
    }

    /**
     * Implements <TypeIdentifier> ::= <TypeIdentifier> '.' <Identifier>
     */
    protected Reduction CreateRULE_OBJECTTYPE_IDENTIFIER_DOT(Reduction reduction)
    {
        String lhs = ((Reduction) reduction.GetToken(0).getData()).getTag().toString();
        String rhs = reduction.GetToken(2).getData().toString();
        reduction.setTag(lhs + "." + rhs);
        return null;
    }

    /**
     * Implements <ObjectType> ::= <AggregateFunction>
     */
    protected Reduction CreateRULE_OBJECTTYPE2(Reduction reduction)
    {
        return null;
    }

    /**
     * Implements <ObjectAttribute> ::= Identifier
     */
    protected Reduction CreateRULE_OBJECTATTRIBUTE_IDENTIFIER(Reduction reduction)
    {
        String memberName = reduction.GetToken(0).getData().toString();
        reduction.setTag(memberName);
        return null;
    }
    
    protected Reduction CreateRULE_DELETEPARAMS_DOLLARTEXTDOLLAR(Reduction reduction)
    {
        reduction.setTag("System.String");
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("CreateRULE_OBJECTTYPE_DOLLARTEXTDOLLAR");
        }
        return null;
    }
    
    protected Reduction CreateRULE_DELETEPARAMS(Reduction reduction) 
    {
        return null;
    }

    /**
     * Implements <ObjectValue> ::= Keyword
     */
    protected Reduction CreateRULE_OBJECTVALUE_KEYWORD(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_OBJECTVALUE_KEYWORD");
        }
        reduction.setTag(new IdentityFunction());
        return null;
    }

    /**
     * Implements <ObjectValue> ::= Keyword '.' <Property>
     */
    protected Reduction CreateRULE_OBJECTVALUE_KEYWORD_DOT(Reduction reduction)
    {
        Object pred = ((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag();
        if (pred instanceof IFunctor)
        {
            reduction.setTag(pred);
        }
        else
        {
            reduction.setTag(new MemberFunction(pred.toString()));
        }
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_IDENTIFIER_KEYWORD");
        }
        return null;
    }

    /**
     * Implements <Property> ::= <Property> '.' <Identifier>
     */
    protected Reduction CreateRULE_PROPERTY_DOT(Reduction reduction)
    {
        IFunctor nested = new MemberFunction(((Reduction) ((Token) reduction.GetToken(2)).getData()).getTag().toString());
        IFunctor func = new MemberFunction(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag().toString());

        reduction.setTag(new CompositeFunction(func, nested));
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_PROPERTY_DOT -> " + reduction.getTag());
        }
        return null;
    }

    /**
     * Implements <Property> ::= <Identifier>
     */
    protected Reduction CreateRULE_PROPERTY(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_PROPERTY");
        }
        reduction.setTag(((Token) reduction.GetToken(0)).getData());
        return null;
    }

    /**
     * Implements <Identifier> ::= Identifier
     */
    protected Reduction CreateRULE_IDENTIFIER_IDENTIFIER(Reduction reduction)
    {
        reduction.setTag(((Token) reduction.GetToken(0)).getData().toString());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_IDENTIFIER_IDENTIFIER -> " + reduction.getTag());
        }
        return null;
    }

    /**
     * Implements <Identifier> ::= Keyword
     */
    protected Reduction CreateRULE_IDENTIFIER_KEYWORD(Reduction reduction)
    {
        reduction.setTag(((Token) reduction.GetToken(0)).getData());
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_IDENTIFIER_KEYWORD -> " + reduction.getTag());
        }
        return null;
    }

    /**
     * Implements <InList> ::= '(' <ListType> ')'
     */
    protected Reduction CreateRULE_INLIST_LPARAN_RPARAN(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_INLIST_LPARAN_RPARAN");
        }
        Object obj = ((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag();

        if (obj instanceof ConstantValue || obj instanceof RuntimeValue)
        {
            IsInListPredicate pred = new IsInListPredicate();
            pred.Append(obj);
            reduction.setTag(pred);
        }
        else
        {
            reduction.setTag(((Reduction) ((Token) reduction.GetToken(1)).getData()).getTag());
        }

        return null;
    }

    /**
     * Implements <ListType> ::= <NumLiteralList>
     */
    protected Reduction CreateRULE_LISTTYPE(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_LISTTYPE");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <ListType> ::= <StrLiteralList>
     */
    protected Reduction CreateRULE_LISTTYPE2(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_LISTTYPE2");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <ListType> ::= <DateList>
     */
    protected Reduction CreateRULE_LISTTYPE3(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_LISTTYPE3");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <NumLiteralList> ::= <NumLiteral> ',' <NumLiteralList>
     */
    protected Reduction CreateRULE_NUMLITERALLIST_COMMA(Reduction reduction)
    {
        return CreateInclusionList(reduction);
    }

    /**
     * Implements <NumLiteralList> ::= <NumLiteral>
     */
    protected Reduction CreateRULE_NUMLITERALLIST(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_NUMLITERALLIST");
        }
        IsInListPredicate pred = new IsInListPredicate();
        pred.Append(((Reduction) reduction.GetToken(0).getData()).getTag());
        reduction.setTag(pred);
        return null;
    }

    /**
     * Implements <StrLiteralList> ::= <StrLiteral> ',' <StrLiteralList>
     */
    protected Reduction CreateRULE_STRLITERALLIST_COMMA(Reduction reduction)
    {
        return CreateInclusionList(reduction);
    }

    /**
     * Implements <StrLiteralList> ::= <StrLiteral>
     */
    protected Reduction CreateRULE_STRLITERALLIST(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_STRLITERALLIST");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    /**
     * Implements <DateList> ::= <Date> ',' <DateList>
     */
    protected Reduction CreateRULE_DATELIST_COMMA(Reduction reduction)
    {
        return CreateInclusionList(reduction);
    }

    /**
     * Implements <DateList> ::= <Date>
     */
    protected Reduction CreateRULE_DATELIST(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_DATELIST");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    //self create by muds:
    //=========================
    protected Reduction CreateRULE_ATRRIB(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("CreateRULE_ATRRIB");
        }
        reduction.setTag(((Reduction) ((Token) reduction.GetToken(0)).getData()).getTag());
        return null;
    }

    protected Reduction CreateRULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_DATE_DATETIME_LPARAN_STRINGLITERAL_RPARAN");
        }
        String dateTime = tangible.DotNetToJavaStringHelper.trim(reduction.GetToken(2).getData().toString(), '\'');
        reduction.setTag(new DateTimeConstantValue(dateTime));
        return null;
    }

    protected Reduction CreateRULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER");
        }
        String memName = reduction.GetToken(2).getData().toString();
        reduction.setTag(new MemberFunction(memName));
        return null;
    }

    protected Reduction CreateRULE_OBJECTVALUE_KEYWORD_DOT_TAG(Reduction reduction)
    {
        if (getCacheLog().getIsInfoEnabled())
        {
            getCacheLog().Info("RULE_OBJECTVALUE_KEYWORD_DOT_TAG");
        }
        String memName = "$Tag$";
        reduction.setTag(new MemberFunction(memName));
        return null;
    }

    /**
     * Implements <SumFunction> ::= 'SUM(' <TypePlusAttribute> ')'
     */
    protected Reduction CreateRULE_SUMFUNCTION_SUMLPARAN_RPARAN(Reduction reduction)
    {
        Reduction typePlusAttributeReduction = (Reduction) reduction.GetToken(1).getData();

        Object tempVar = ((Reduction) typePlusAttributeReduction.GetToken(0).getData()).getTag();
        String typeName = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = ((Reduction) typePlusAttributeReduction.GetToken(2).getData()).getTag();
        String memberName = (String) ((tempVar2 instanceof String) ? tempVar2 : null);

        Predicate childPredicate = new IsOfTypePredicate(typeName);
        Predicate tempVar3 = ExpressionBuilder.CreateSumFunctionPredicate(memberName);
        AggregateFunctionPredicate sumFunctionPredicate = (AggregateFunctionPredicate) ((tempVar3 instanceof AggregateFunctionPredicate) ? tempVar3 : null);
        sumFunctionPredicate.setChildPredicate(childPredicate);
        reduction.setTag(sumFunctionPredicate);
        return null;
    }

    /**
     * Implements <CountFunction> ::= 'COUNT(' <Property> ')'
     */
    protected Reduction CreateRULE_COUNTFUNCTION_COUNTLPARAN_TIMES_RPARAN(Reduction reduction)
    {
        Object tempVar = ((Reduction) reduction.GetToken(1).getData()).getTag();
        String typeName = (String) ((tempVar instanceof String) ? tempVar : null);
        Predicate childPredicate = new IsOfTypePredicate(typeName);
        Predicate tempVar2 = ExpressionBuilder.CreateCountFunctionPredicate();
        AggregateFunctionPredicate countFunctionPredicate = (AggregateFunctionPredicate) ((tempVar2 instanceof AggregateFunctionPredicate) ? tempVar2 : null);
        countFunctionPredicate.setChildPredicate(childPredicate);
        reduction.setTag(countFunctionPredicate);
        return null;
    }

    /**
     * Implements <MinFunction> ::= 'MIN(' <TypePlusAttribute> ')'
     */
    protected Reduction CreateRULE_MINFUNCTION_MINLPARAN_RPARAN(Reduction reduction)
    {
        Reduction typePlusAttributeReduction = (Reduction) reduction.GetToken(1).getData();

        Object tempVar = ((Reduction) typePlusAttributeReduction.GetToken(0).getData()).getTag();
        String typeName = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = ((Reduction) typePlusAttributeReduction.GetToken(2).getData()).getTag();
        String memberName = (String) ((tempVar2 instanceof String) ? tempVar2 : null);

        Predicate childPredicate = new IsOfTypePredicate(typeName);
        Predicate tempVar3 = ExpressionBuilder.CreateMinFunctionPredicate(memberName);
        AggregateFunctionPredicate minFunctionPredicate = (AggregateFunctionPredicate) ((tempVar3 instanceof AggregateFunctionPredicate) ? tempVar3 : null);
        minFunctionPredicate.setChildPredicate(childPredicate);
        reduction.setTag(minFunctionPredicate);
        return null;
    }

    /**
     * Implements <MaxFunction> ::= 'MAX(' <TypePlusAttribute> ')'
     */
    protected Reduction CreateRULE_MAXFUNCTION_MAXLPARAN_RPARAN(Reduction reduction)
    {
        Reduction typePlusAttributeReduction = (Reduction) reduction.GetToken(1).getData();

        Object tempVar = ((Reduction) typePlusAttributeReduction.GetToken(0).getData()).getTag();
        String typeName = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = ((Reduction) typePlusAttributeReduction.GetToken(2).getData()).getTag();
        String memberName = (String) ((tempVar2 instanceof String) ? tempVar2 : null);

        Predicate childPredicate = new IsOfTypePredicate(typeName);
        Predicate tempVar3 = ExpressionBuilder.CreateMaxFunctionPredicate(memberName);
        AggregateFunctionPredicate maxFunctionPredicate = (AggregateFunctionPredicate) ((tempVar3 instanceof AggregateFunctionPredicate) ? tempVar3 : null);
        maxFunctionPredicate.setChildPredicate(childPredicate);
        reduction.setTag(maxFunctionPredicate);
        return null;
    }

    /**
     * Implements <AverageFunction> ::= 'AVG(' <TypePlusAttribute> ')'
     */
    protected Reduction CreateRULE_AVERAGEFUNCTION_AVGLPARAN_RPARAN(Reduction reduction)
    {
        Reduction typePlusAttributeReduction = (Reduction) reduction.GetToken(1).getData();

        Object tempVar = ((Reduction) typePlusAttributeReduction.GetToken(0).getData()).getTag();
        String typeName = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = ((Reduction) typePlusAttributeReduction.GetToken(2).getData()).getTag();
        String memberName = (String) ((tempVar2 instanceof String) ? tempVar2 : null);

        Predicate childPredicate = new IsOfTypePredicate(typeName);
        Predicate tempVar3 = ExpressionBuilder.CreateAverageFunctionPredicate(memberName);
        AggregateFunctionPredicate avgFunctionPredicate = (AggregateFunctionPredicate) ((tempVar3 instanceof AggregateFunctionPredicate) ? tempVar3 : null);
        avgFunctionPredicate.setChildPredicate(childPredicate);
        reduction.setTag(avgFunctionPredicate);
        return null;
    }
    
  /// Implements <CompareExpr> ::= <Value> LIKE Question      
    public Reduction CreateRULE_COMPAREEXPR_LIKE_QUESTION(Reduction reduction)
    {
        return CreateRULE_COMPAREEXPR_LIKE_STRINGLITERAL(reduction);
    }

    /// Implements <CompareExpr> ::= <Value> NOT LIKE Question      
    public Reduction CreateRULE_COMPAREEXPR_NOT_LIKE_QUESTION(Reduction reduction)
    {
        return CreateRULE_COMPAREEXPR_NOT_LIKE_STRINGLITERAL(reduction);
    }

      /// Implements <CompareExpr> ::= <Value> ObjectType   
        public Reduction CreateRULE_OBJECTTYPE(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Property DOT Identifier 
        public Reduction CreateRULE_PROPERTY_DOT_IDENTIFIER(Reduction reduction)
        {
            return CreateRULE_OBJECTTYPE_IDENTIFIER_DOT(reduction);
        }

        /// Implements <CompareExpr> ::= <Value> Property Identifier
        public Reduction CreateRULE_PROPERTY_IDENTIFIER(Reduction reduction)
        {
            return CreateRULE_OBJECTTYPE_IDENTIFIER(reduction);
        }

        /// Implements <CompareExpr> ::= <Value> Type Plus Attribute DOT   
        public Reduction CreateRULE_TYPEPLUSATTRIBUTE_DOT(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Aggregate Function
        public Reduction CreateRULE_AGGREGATEFUNCTION(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Aggregate Function2  
        public Reduction CreateRULE_AGGREGATEFUNCTION2(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Aggregate Function3
        public Reduction CreateRULE_AGGREGATEFUNCTION3(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Aggregate Function4
        public Reduction CreateRULE_AGGREGATEFUNCTION4(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> Aggregate Function5
        public Reduction CreateRULE_AGGREGATEFUNCTION5(Reduction reduction)
        {
            return null;
        }

        /// Implements <CompareExpr> ::= <Value> COUNT Function COUNT LParan RParan
        public Reduction CreateRULE_COUNTFUNCTION_COUNTLPARAN_RPARAN(Reduction reduction)
        {
            return CreateRULE_COUNTFUNCTION_COUNTLPARAN_TIMES_RPARAN(reduction);
        }
        
        ///Implements <ObjectAttribute> ::= Keyword '.' Identifier
        public Reduction CreateRULE_OBJECTATTRIBUTE_KEYWORD_DOT_IDENTIFIER(Reduction reduction)
        {
            return CreateRULE_OBJECTVALUE_KEYWORD_DOT_IDENTIFIER(reduction);
        }
        
        ///Implements <GroupByValueList> ::= <ObjectAttribute> ',' <GroupByValueList>
        public Reduction CreateRULE_GROUPBYVALUELIST_COMMA(Reduction reduction)
        {
            Object tag=((Reduction)reduction.GetToken(2).getData()).getTag();
            GroupByValueList groupByValueList=null;
            if(tag instanceof GroupByValueList)
            {
                groupByValueList=(GroupByValueList)tag;
            }
            else
            {
                groupByValueList=new GroupByValueList();
                groupByValueList.getAggregateFunctionsList().add((AggregateFunctionPredicate)tag);
            }
            String attributeName = ((MemberFunction)((Reduction)reduction.GetToken(0).getData()).getTag()).getMemberName();
            if(groupByValueList.getObjectAttributesList().contains(attributeName))
                throw new RuntimeParsingException("Invalid query. Same column cannot be selected twice.");
            groupByValueList.getObjectAttributesList().add(attributeName);
            reduction.setTag(groupByValueList);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("CreateRULE_GROUPBYVALUELIST_COMMA");
            }
            return null;  
        }
        
        ///Implements <GroupByValueList> ::= <AggregateFunctionList>
        public Reduction CreateRULE_GROUPBYVALUELIST(Reduction reduction)
        {
            //never called by parser
            return null;
        }
        
        ///Implements <AggregateFunctionList> ::= <AggregateFunction> ',' <AggregateFunctionList>
        public Reduction CreateRULE_AGGREGATEFUNCTIONLIST_COMMA(Reduction reduction)
        {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("CreateRULE_AGGREGATEFUNCTIONLIST_COMMA");
            }
            return CreateGroupByValueList(reduction);
        }
        
        ///Implements <AggregateFunctionList> ::= <AggregateFunction>
        public Reduction CreateRULE_AGGREGATEFUNCTIONLIST(Reduction reduction)
        {
            //never called by parser
            return null;
        }
        
        ///Implements <ObjectAttributeList> ::= <ObjectAttribute> ',' <ObjectAttributeList>
        public Reduction CreateRULE_OBJECTATTRIBUTELIST_COMMA(Reduction reduction)
        {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("CreateRULE_OBJECTATTRIBUTELIST_COMMA");
            }
            return CreateObjectAttributeList(reduction);
        }
        
        ///Implements <ObjectAttributeList> ::= <ObjectAttribute>
        public Reduction CreateRULE_OBJECTATTRIBUTELIST(Reduction reduction)
        {
            //never called by parser
            return null;
        }
        
        protected Reduction CreateGroupByValueList(Reduction reduction)
        {
            Object tag = ((Reduction)reduction.GetToken(2).getData()).getTag();
            GroupByValueList groupByValueList = null;
            if (tag instanceof GroupByValueList)
            {
                groupByValueList = (GroupByValueList)tag;
            }
            else
            {
                groupByValueList = new GroupByValueList();
                groupByValueList.getAggregateFunctionsList().add((AggregateFunctionPredicate)tag);
            }

            AggregateFunctionPredicate afp = (AggregateFunctionPredicate)((Reduction)reduction.GetToken(0).getData()).getTag();
            if (((IsOfTypePredicate)afp.getChildPredicate()).getTypeName().equals(((IsOfTypePredicate)groupByValueList.getAggregateFunctionsList().get(0).getChildPredicate()).getTypeName()))
            {
                groupByValueList.getAggregateFunctionsList().add(afp);
            }
            else
                throw new RuntimeParsingException("Invalid query. Same class should be specified in all aggregate functions.");

            reduction.setTag(groupByValueList);
            return null;
        }
        
        protected Reduction CreateObjectAttributeList(Reduction reduction)
        {
            Object tag = ((Reduction)reduction.GetToken(2).getData()).getTag();
            GroupByPredicate gb = null;
            if (tag instanceof GroupByPredicate)
            {
                gb = (GroupByPredicate)tag;
            }
            else
            {
                gb = new GroupByPredicate();
                gb.getAttributeNamesList().add(((MemberFunction)tag).getMemberName());
            }
            gb.getAttributeNamesList().add(((MemberFunction)((Reduction)reduction.GetToken(0).getData()).getTag()).getMemberName());

            reduction.setTag(gb);
            return null;
        }

    protected final Reduction CreateInclusionList(Reduction reduction)
    {
        Object tag = ((Reduction) reduction.GetToken(2).getData()).getTag();
        IsInListPredicate inc = null;
        if (tag instanceof IsInListPredicate)
        {
            inc = (IsInListPredicate) ((tag instanceof IsInListPredicate) ? tag : null);
        }
        else
        {
            inc = new IsInListPredicate();
            inc.Append(tag);
        }
        inc.Append(((Reduction) reduction.GetToken(0).getData()).getTag());
        reduction.setTag(inc);
        return null;
    }
}
