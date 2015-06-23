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
package com.alachisoft.tayzgrid.web.aggregation;

import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.common.enums.DataType;
import com.alachisoft.tayzgrid.runtime.aggregation.Aggregator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

public final class BuiltinAggregator {

    public static Aggregator integerSum() {
        return new IntegerAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator doubleSum() {
        return new DoubleAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator floatSum() {
        return new FloatAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator bigDecimalSum() {
        return new BigDecimalAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator bigIntegerSum() {
        return new BigIntegerAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator longSum() {
        return new LongAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator shortSum() {
        return new ShortAggregator(AggregateFunctionType.SUM);
    }

    public static Aggregator integerAvg() {
        return new IntegerAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator doubleAvg() {
        return new DoubleAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator floatAvg() {
        return new FloatAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator bigDecimalAvg() {
        return new BigDecimalAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator bigIntegerAvg() {
        return new BigIntegerAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator longAvg() {
        return new LongAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator shortAvg() {
        return new ShortAggregator(AggregateFunctionType.AVG);
    }

    public static Aggregator integerMin() {
        return new IntegerAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator doubleMin() {
        return new DoubleAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator floatMin() {
        return new FloatAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator bigDecimalMin() {
        return new BigDecimalAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator bigIntegerMin() {
        return new BigIntegerAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator longMin() {
        return new LongAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator shortMin() {
        return new ShortAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator integerMax() {
        return new IntegerAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator doubleMax() {
        return new DoubleAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator floatMax() {
        return new FloatAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator bigDecimalMax() {
        return new BigDecimalAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator bigIntegerMax() {
        return new BigIntegerAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator longMax() {
        return new LongAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator shortMax() {
        return new ShortAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator stringMax() {
        return new StringAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator stringMin() {
        return new StringAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator dataTimeMin() {
        return new DateAggregator(AggregateFunctionType.MIN);
    }

    public static Aggregator dataTimeMax() {
        return new DateAggregator(AggregateFunctionType.MAX);
    }

    public static Aggregator count() {
        return new CountAggregator();
    }

    public static Aggregator distinctValue() {
        return new DistinctAggregator();
    }

    static class IntegerAggregator implements Aggregator<Collection, Integer> {

        AggregateFunctionType aggregateType;
        Integer result = 0;

        public IntegerAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Integer aggragate(Collection value) {
            return calculate(value);
        }

        @Override
        public Integer aggragateAll(Collection value) {
            return calculate(value);
        }

        private Integer calculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (Integer) Calculation.sum(value, DataType.INTEGER);
                    break;
                case AVG:
                    result = (Integer) Calculation.avg(value, DataType.INTEGER);
                    break;
                case MIN:
                    result = (Integer) Calculation.min(value);
                    break;
                case MAX:
                    result = (Integer) Calculation.max(value);
                    break;
            }
            return result;
        }

    }

    static class DoubleAggregator implements Aggregator<Collection, Double> {

        AggregateFunctionType aggregateType;
        Double result = 0.0d;

        public DoubleAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Double aggragate(Collection value) {
            return calculate(value);
        }

        @Override
        public Double aggragateAll(Collection value) {
            return calculate(value);
        }

        private Double calculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (Double) Calculation.sum(value, DataType.DOUBLE);
                    break;
                case AVG:
                    result = (Double) Calculation.avg(value, DataType.DOUBLE);
                    break;
                case MIN:
                    result = (Double) Calculation.min(value);
                    break;
                case MAX:
                    result = (Double) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class FloatAggregator implements Aggregator<Collection, Float> {

        AggregateFunctionType aggregateType;
        Float result = 0F;

        public FloatAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Float aggragate(Collection value) {
            return calculate(value);
        }

        @Override
        public Float aggragateAll(Collection value) {
            return calculate(value);
        }

        private Float calculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (Float) Calculation.sum(value, DataType.FLOAT);
                    break;
                case AVG:
                    result = (Float) Calculation.avg(value, DataType.FLOAT);
                    break;
                case MIN:
                    result = (Float) Calculation.min(value);
                    break;
                case MAX:
                    result = (Float) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class BigDecimalAggregator implements Aggregator<Collection, BigDecimal> {

        AggregateFunctionType aggregateType;
        BigDecimal result = new BigDecimal(0);

        public BigDecimalAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public BigDecimal aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public BigDecimal aggragateAll(Collection value) {
            return caclculate(value);
        }

        private BigDecimal caclculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (BigDecimal) Calculation.sum(value, DataType.BIGDECIMAL);
                    break;
                case AVG:
                    result = (BigDecimal) Calculation.avg(value, DataType.BIGDECIMAL);
                    break;
                case MIN:
                    result = (BigDecimal) Calculation.min(value);
                    break;
                case MAX:
                    result = (BigDecimal) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class BigIntegerAggregator implements Aggregator<Collection, BigInteger> {

        AggregateFunctionType aggregateType;
        BigInteger result = BigInteger.valueOf(0);

        public BigIntegerAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public BigInteger aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public BigInteger aggragateAll(Collection value) {
            return caclculate(value);
        }

        private BigInteger caclculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (BigInteger) Calculation.sum(value, DataType.BIGINTEGER);
                    break;
                case AVG:
                    result = (BigInteger) Calculation.avg(value, DataType.BIGINTEGER);
                    break;
                case MIN:
                    result = (BigInteger) Calculation.min(value);
                    break;
                case MAX:
                    result = (BigInteger) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class LongAggregator implements Aggregator<Collection, Long> {

        AggregateFunctionType aggregateType;
        Long result = 0L;

        public LongAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Long aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public Long aggragateAll(Collection value) {
            return caclculate(value);
        }

        private Long caclculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (Long) Calculation.sum(value, DataType.LONG);
                    break;
                case AVG:
                    result = (Long) Calculation.avg(value, DataType.LONG);
                    break;
                case MIN:
                    result = (Long) Calculation.min(value);
                    break;
                case MAX:
                    result = (Long) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class ShortAggregator implements Aggregator<Collection, Short> {

        AggregateFunctionType aggregateType;
        Short result = 0;

        public ShortAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Short aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public Short aggragateAll(Collection value) {
            return caclculate(value);
        }

        private Short caclculate(Collection value) {
            switch (aggregateType) {
                case SUM:
                    result = (Short) Calculation.sum(value, DataType.SHORT);
                    break;
                case AVG:
                    result = (Short) Calculation.avg(value, DataType.SHORT);
                    break;
                case MIN:
                    result = (Short) Calculation.min(value);
                    break;
                case MAX:
                    result = (Short) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class StringAggregator implements Aggregator<Collection, String> {

        AggregateFunctionType aggregateType;
        String result = "";

        public StringAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public String aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public String aggragateAll(Collection value) {
            return caclculate(value);
        }

        private String caclculate(Collection value) {
            switch (aggregateType) {
                case MIN:
                    result = (String) Calculation.min(value);
                    break;
                case MAX:
                    result = (String) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class DateAggregator implements Aggregator<Collection, Date> {

        AggregateFunctionType aggregateType;
        Date result = new Date();

        public DateAggregator(AggregateFunctionType type) {
            this.aggregateType = type;
        }

        @Override
        public Date aggragate(Collection value) {
            return caclculate(value);
        }

        @Override
        public Date aggragateAll(Collection value) {
            return caclculate(value);
        }

        private Date caclculate(Collection value) {
            switch (aggregateType) {
                case MIN:
                    result = (Date) Calculation.min(value);
                    break;
                case MAX:
                    result = (Date) Calculation.max(value);
                    break;
            }
            return result;
        }
    }

    static class CountAggregator implements Aggregator<Collection, Long> {

        @Override
        public Long aggragate(Collection value) {
            return Calculation.count(value);
        }

        @Override
        public Long aggragateAll(Collection value) {
            return Calculation.countAll(value);
        }
    }

    static class DistinctAggregator implements Aggregator<Collection, HashSet> {

        @Override
        public HashSet aggragate(Collection value) {
            return Calculation.distinctValue(value);
        }

        @Override
        public HashSet aggragateAll(Collection value) {
            return Calculation.distinctAllValue(value);
        }
    }

    private static class Calculation {

        private static Object sum(Collection value, DataType dataType) {
            Integer result = 0;
            Double doubleRessult = 0.0d;
            Float floatResult = 0.0f;
            BigDecimal decimalResult = new BigDecimal(0);
            BigInteger integerResult = BigInteger.valueOf(0);
            Long longResult = 0L;
            Short shortResult = 0;
            if (value != null) {
                Iterator itr = value.iterator();
                while (itr.hasNext()) {
                    switch (dataType) {
                        case DOUBLE:
                            doubleRessult += (Double) itr.next();
                            break;
                        case FLOAT:
                            floatResult += (Float) itr.next();
                            break;
                        case BIGDECIMAL:
                            decimalResult = decimalResult.add((BigDecimal) itr.next());
                            break;
                        case INTEGER:
                            result += (Integer) itr.next();
                            break;
                        case BIGINTEGER:
                            integerResult = integerResult.add((BigInteger) itr.next());
                            break;
                        case LONG:
                            longResult += (Long) itr.next();
                            break;
                        case SHORT: {
                            Integer temp = 0;
                            temp = shortResult + (Short) itr.next();
                            shortResult = Short.parseShort(temp.toString());
                            break;
                        }

                    }
                }
                switch (dataType) {
                    case DOUBLE:
                        return doubleRessult;
                    case FLOAT:
                        return floatResult;
                    case BIGDECIMAL:
                        return decimalResult;
                    case INTEGER:
                        return result;
                    case BIGINTEGER:
                        return integerResult;
                    case LONG:
                        return longResult;
                    case SHORT:
                        return shortResult;
                }
            }
            return null;
        }

        private static Object avg(Collection value, DataType dataType) {
            Integer result = 0;
            Double doubleRessult = 0.0d;
            Float floatResult = 0.0f;
            BigDecimal decimalResult = new BigDecimal(0);
            BigInteger integerResult = BigInteger.valueOf(0);
            Long longResult = 0L;
            Short shortResult = 0;

            if (value != null) {
                int size = value.size();
                Iterator itr = value.iterator();
                while (itr.hasNext()) {

                    switch (dataType) {
                        case DOUBLE:
                            doubleRessult += (Double) itr.next();
                            break;
                        case FLOAT:
                            floatResult += (Float) itr.next();
                            break;
                        case BIGDECIMAL:
                            decimalResult = decimalResult.add((BigDecimal) itr.next());
                            break;
                        case INTEGER:
                            result += (Integer) itr.next();
                            break;
                        case BIGINTEGER:
                            integerResult = integerResult.add((BigInteger) itr.next());
                            break;
                        case LONG:
                            longResult += (Long) itr.next();
                            break;
                        case SHORT: {
                            Integer temp = 0;
                            temp = shortResult + (Short) itr.next();
                            shortResult = Short.parseShort(temp.toString());
                            break;
                        }
                    }
                }

                switch (dataType) {
                    case DOUBLE:
                        return doubleRessult / size;
                    case FLOAT:
                        return floatResult / size;
                    case BIGDECIMAL:
                        return decimalResult.divide(BigDecimal.valueOf(size));
                    case INTEGER:
                        return result / size;
                    case BIGINTEGER:
                        return integerResult.divide(BigInteger.valueOf(size));
                    case LONG:
                        return longResult / size;
                    case SHORT: {
                        Integer temp = 0;
                        temp = shortResult / size;
                        return Short.parseShort(temp.toString());

                    }
                }
            }
            return null;
        }

        private static Object min(Collection value) {
            if (value != null) {
                return Collections.min(value);
            }
            return null;
        }

        private static Object max(Collection value) {

            if (value != null) {
                return Collections.max(value);
            }
            return null;
        }

        private static Long count(Collection value) {
            if (value != null) {
                return (long) value.size();
            }
            return null;
        }

        private static Long countAll(Collection value) {
            long count = 0;

            if (value != null) {
                Iterator itr = value.iterator();
                while (itr.hasNext()) {
                    count += (Long) itr.next();
                }
            }
            return count;
        }

        private static HashSet distinctValue(Collection value) {
            if (value != null) {
                return new HashSet(value);
            }
            return null;
        }
        
        private static HashSet distinctAllValue(Collection value) {
            HashSet hashSet = new HashSet();
            if (value != null) {
                Iterator itr = value.iterator();
                while (itr.hasNext()) {
                    hashSet.addAll((HashSet) itr.next());
                }
            }
            return hashSet;
        }
    }
}
