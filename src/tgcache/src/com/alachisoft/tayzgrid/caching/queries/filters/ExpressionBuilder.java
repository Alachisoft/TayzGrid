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

package com.alachisoft.tayzgrid.caching.queries.filters;

public class ExpressionBuilder {

    public static final Predicate TRUE_PREDICATE = new AlwaysTruePredicate();
    public static final Predicate FALSE_PREDICATE = new AlwaysFalsePredicate();

    public static Predicate CreateSumFunctionPredicate(String attributeName) {
        AggregateFunctionPredicate predicate = new SumPredicate();
        predicate.setAttributeName(attributeName);
        return predicate;
    }

    public static Predicate CreateCountFunctionPredicate() {
        AggregateFunctionPredicate predicate = new CountPredicate();
        return predicate;
    }

    public static Predicate CreateAverageFunctionPredicate(String attributeName) {
        AggregateFunctionPredicate predicate = new AveragePredicate();
        predicate.setAttributeName(attributeName);
        return predicate;
    }

    public static Predicate CreateMinFunctionPredicate(String attributeName) {
        AggregateFunctionPredicate predicate = new MinPredicate();
        predicate.setAttributeName(attributeName);
        return predicate;
    }

    public static Predicate CreateMaxFunctionPredicate(String attributeName) {
        AggregateFunctionPredicate predicate = new MaxPredicate();
        predicate.setAttributeName(attributeName);
        return predicate;
    }

    public static Predicate CreateLogicalAndPredicate(Predicate lhsPred, Predicate rhsPred) {
        if (lhsPred.equals(FALSE_PREDICATE) || rhsPred.equals(FALSE_PREDICATE)) {
            return FALSE_PREDICATE;
        }
        if (lhsPred.equals(TRUE_PREDICATE)) {
            return rhsPred;
        }
        if (rhsPred.equals(TRUE_PREDICATE)) {
            return lhsPred;
        }

        LogicalAndPredicate inc = null;
        if (lhsPred instanceof LogicalAndPredicate) {
            inc = (LogicalAndPredicate) ((lhsPred instanceof LogicalAndPredicate) ? lhsPred : null);
        }

        if (inc == null || inc.getInverse()) {
            inc = new LogicalAndPredicate();
            inc.getChildren().add(lhsPred);
        }
        inc.getChildren().add(rhsPred);

        return inc;
    }

    public static Predicate CreateLogicalOrPredicate(Predicate lhsPred, Predicate rhsPred) {
        if (lhsPred.equals(TRUE_PREDICATE) || rhsPred.equals(TRUE_PREDICATE)) {
            return TRUE_PREDICATE;
        }
        if (lhsPred.equals(FALSE_PREDICATE)) {
            return rhsPred;
        }
        if (rhsPred.equals(FALSE_PREDICATE)) {
            return lhsPred;
        }

        LogicalAndPredicate inc = null;
        if (lhsPred instanceof LogicalAndPredicate) {
            inc = (LogicalAndPredicate) ((lhsPred instanceof LogicalAndPredicate) ? lhsPred : null);
        }

        if (inc == null || !inc.getInverse()) {
            inc = new LogicalAndPredicate();
            inc.Invert();
            inc.getChildren().add(lhsPred);
        }
        inc.getChildren().add(rhsPred);

        return inc;
    }

    public static Predicate CreateEqualsPredicate(Object o, Object v) {
        boolean lhsIsGen = o instanceof IGenerator;
        boolean rhsIsGen = v instanceof IGenerator;

        if (lhsIsGen || rhsIsGen) {
            if (lhsIsGen && rhsIsGen) {
                Object lhs = ((IGenerator) o).Evaluate();
                Object rhs = ((IGenerator) v).Evaluate();
                return lhs.equals(rhs) ? TRUE_PREDICATE : FALSE_PREDICATE;
            }

            IFunctor func = lhsIsGen ? (IFunctor) v : (IFunctor) o;
            IGenerator gen = lhsIsGen ? (IGenerator) o : (IGenerator) v;

            return new FunctorEqualsGeneratorPredicate(func, gen);
        }

        return new FunctorEqualsFunctorPredicate((IFunctor) o, (IFunctor) v);
    }

    public static Predicate CreateNotEqualsPredicate(Object o, Object v) {
        Predicate pred = CreateEqualsPredicate(o, v);
        if (pred.equals(TRUE_PREDICATE)) {
            return FALSE_PREDICATE;
        }
        if (pred.equals(FALSE_PREDICATE)) {
            return TRUE_PREDICATE;
        }

        pred.Invert();
        return pred;
    }

    public static Predicate CreateGreaterPredicate(Object o, Object v) {
        boolean lhsIsGen = o instanceof IGenerator;
        boolean rhsIsGen = v instanceof IGenerator;

        if (lhsIsGen || rhsIsGen) {
            if (lhsIsGen && rhsIsGen) {
                Object lhs = ((IGenerator) o).Evaluate();
                Object rhs = ((IGenerator) v).Evaluate();
                return (lhs.toString().compareTo(rhs.toString()) < 0) ? TRUE_PREDICATE : FALSE_PREDICATE;
            }

            IFunctor func = lhsIsGen ? (IFunctor) v : (IFunctor) o;
            IGenerator gen = lhsIsGen ? (IGenerator) o : (IGenerator) v;

            return new FunctorGreaterGeneratorPredicate(func, gen);
        }

        return new FunctorGreaterFunctorPredicate((IFunctor) o, (IFunctor) v);
    }

    public static Predicate CreateGreaterEqualsPredicate(Object o, Object v) {
        Predicate pred = CreateLesserPredicate(o, v);
        if (pred.equals(TRUE_PREDICATE)) {
            return FALSE_PREDICATE;
        }
        if (pred.equals(FALSE_PREDICATE)) {
            return TRUE_PREDICATE;
        }

        pred.Invert();
        return pred;
    }

    public static Predicate CreateLikePatternPredicate(Object o, Object pattern) {
        IFunctor functor = (IFunctor) ((o instanceof IFunctor) ? o : null);
        IGenerator generator = (IGenerator) ((pattern instanceof IGenerator) ? pattern : null);
        return new FunctorLikePatternPredicate(functor, generator);
    }

    public static Predicate CreateLesserPredicate(Object o, Object v) {
        boolean lhsIsGen = o instanceof IGenerator;
        boolean rhsIsGen = v instanceof IGenerator;

        if (lhsIsGen || rhsIsGen) {
            if (lhsIsGen && rhsIsGen) {
                Object lhs = ((IGenerator) o).Evaluate();
                Object rhs = ((IGenerator) v).Evaluate();
                return (lhs.toString().compareTo(rhs.toString()) < 0) ? TRUE_PREDICATE : FALSE_PREDICATE;
            }

            IFunctor func = lhsIsGen ? (IFunctor) v : (IFunctor) o;
            IGenerator gen = lhsIsGen ? (IGenerator) o : (IGenerator) v;

            return new FunctorLesserGeneratorPredicate(func, gen);
        }

        return new FunctorLesserFunctorPredicate((IFunctor) o, (IFunctor) v);
    }

    public static Predicate CreateLesserEqualsPredicate(Object o, Object v) {
        Predicate pred = CreateGreaterPredicate(o, v);
        if (pred.equals(TRUE_PREDICATE)) {
            return FALSE_PREDICATE;
        }
        if (pred.equals(FALSE_PREDICATE)) {
            return TRUE_PREDICATE;
        }

        pred.Invert();
        return pred;
    }
}