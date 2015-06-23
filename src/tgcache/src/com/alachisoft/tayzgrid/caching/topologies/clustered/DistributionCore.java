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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import java.util.Iterator;

public class DistributionCore {

    public DistributionCore() {
    }

    public enum BalanceAction {

        LoseWeight,
        GainWeight;

        public int getValue() {
            return this.ordinal();
        }

        public static BalanceAction forValue(int value) {
            return values()[value];
        }
    }

    public static class Tuple {

        private int _first, _second;

        public Tuple(int first, int second) {
            _first = first;
            _second = second;
        }

        public final int getFirst() {
            return _first;
        }

        public final int getSecond() {
            return _second;
        }
    }

    public static java.util.ArrayList Distribute(java.util.ArrayList currentMap, int bucketsPerNode, int weightPerNode) {
        return null;
    }

    public static java.util.ArrayList Merge(java.util.ArrayList currentMap, int bucketsPerNode) {
        return null;
    }

    public static java.util.ArrayList FilterBucketsByNode(java.util.HashMap currentMap) {
        return null;
    }

    //We have the matrix. We need no weight balancing, but we need shuffled indexes to be selected. Here comes the routine and algo.
    // Shuffle works the way of moving diagonally within a matrix. If we reach end column, move to first row but extended column.
    public static RowsBalanceResult ShuffleSelect(DistributionMatrix bMatrix) {
        RowsBalanceResult rbResult = new RowsBalanceResult();
        int[] selectedBuckets = new int[bMatrix.getMatrixDimension().getCols()];
        for (int i = 0; i < bMatrix.getMatrixDimension().getCols();) {
            for (int j = 0; j < bMatrix.getMatrixDimension().getRows() && i < bMatrix.getMatrixDimension().getCols(); j++, i++) {
                if (bMatrix.getMatrix()[j][i] == -1) {
                    break;
                } else {
                    selectedBuckets[i] = (j * bMatrix.getMatrixDimension().getCols()) + i;
                }

            }
        }
        rbResult.setResultIndicies(selectedBuckets);
        rbResult.setTargetDistance(0);
        return rbResult;
    }

    //returns selected array of indicies that are the resultant set to be sacrificed
    public static RowsBalanceResult CompareAndSelect(DistributionMatrix bMatrix) {
        int rowNum = IndividualSelect(bMatrix);

        if (rowNum >= 0) {
            int[] selectedBuckets = new int[bMatrix.getMatrixDimension().getCols()];
            for (int i = 0; i < bMatrix.getMatrixDimension().getCols(); i++) {
                selectedBuckets[i] = (rowNum * bMatrix.getMatrixDimension().getCols()) + i;
            }
            RowsBalanceResult rbResult = new RowsBalanceResult();
            rbResult.setResultIndicies(selectedBuckets);
            rbResult.setTargetDistance(Math.abs(bMatrix.getWeightPercentMatrix()[rowNum][1] - bMatrix.getWeightToSacrifice()));
            return rbResult;
        } else { //Second pass Compare all pairs.
            java.util.ArrayList allTuples = CandidateTuples(bMatrix);
            RowsBalanceResult rbResultCurr, rbResultToKeep;
            rbResultCurr = null;
            rbResultToKeep = null;
            for (Iterator it = allTuples.iterator(); it.hasNext();) {
                Tuple pair = (Tuple) it.next();
                rbResultCurr = BalanceWeight(pair, bMatrix);
                if (rbResultToKeep == null) {
                    rbResultToKeep = rbResultCurr;
                }
                if (rbResultCurr.getTargetDistance() < rbResultToKeep.getTargetDistance()) {
                    rbResultToKeep = rbResultCurr;
                }
            }
            return rbResultToKeep;
        }
    }

    private static int IndividualSelect(DistributionMatrix bMatrix) {
        for (int i = 0; i < bMatrix.getMatrixDimension().getRows(); i++) {
            long rowWeight = bMatrix.getWeightPercentMatrix()[i][0];
            if (rowWeight == bMatrix.getPercentWeightToSacrifice()) {
                return i; //this row;
            }

            //Lets see if if required data is within +- of cushion factor
            if (rowWeight < bMatrix.getPercentWeightToSacrifice()) {
                rowWeight += bMatrix.getCushionFactor();
                if (rowWeight >= bMatrix.getPercentWeightToSacrifice()) {
                    return i; //this row
                }
            } else {
                rowWeight -= bMatrix.getCushionFactor();
                if (rowWeight <= bMatrix.getPercentWeightToSacrifice()) {
                    return i; //this row
                }
            }
        }
        return -1; //We got no single row that can be selected under required criteria
    }

    
    //Backbone to be written now.//Two arrays ....both need to give a right combination against required weight.
    private static RowsBalanceResult BalanceWeight(Tuple rowPair, DistributionMatrix bMatrix) {
        BalanceAction balAction;
        long weightToMove = 0;
        long primaryRowWeight = bMatrix.getWeightPercentMatrix()[rowPair.getFirst()][0];
        long secondaryRowWeight = bMatrix.getWeightPercentMatrix()[rowPair.getSecond()][0];

        if (primaryRowWeight < bMatrix.getPercentWeightToSacrifice()) {
            weightToMove = bMatrix.getPercentWeightToSacrifice() - primaryRowWeight;
            weightToMove = (weightToMove * bMatrix.getTotalWeight()) / 100;
            balAction = BalanceAction.GainWeight;
        } else {
            weightToMove = primaryRowWeight - bMatrix.getPercentWeightToSacrifice();
            weightToMove = (weightToMove * bMatrix.getTotalWeight()) / 100;
            balAction = BalanceAction.LoseWeight;
        }

        long[] primaryRowData = new long[bMatrix.getMatrixDimension().getCols()];
        long[] secondaryRowData = new long[bMatrix.getMatrixDimension().getCols()];

        //Fills the local copy of two rows to be manipulated
        for (int i = 0; i < bMatrix.getMatrixDimension().getCols(); i++) {
            primaryRowData[i] = bMatrix.getMatrix()[rowPair.getFirst()][i];
            secondaryRowData[i] = bMatrix.getMatrix()[rowPair.getSecond()][i];
        }
        RowsBalanceResult rbResult = null;
        switch (balAction) {
            case GainWeight:
                rbResult = RowBalanceGainWeight(rowPair, primaryRowData, secondaryRowData, weightToMove, bMatrix);
                break;
            case LoseWeight:
                rbResult = RowBalanceLoseWeight(rowPair, weightToMove, bMatrix);
                break;
            default:
                break;
        }

        return rbResult;
    }

    //All 2-tuples of  the given set. Set is array of %weights against each row.
    private static java.util.ArrayList CandidateTuples(DistributionMatrix bMatrix) {
        // here we'll have n choose r scenario. Where we need to choose all possible pairs from the given set
        // for n choose r
        int n = bMatrix.getMatrixDimension().getRows();
        int r = 2;
        int tupleCount = (int) (Factorial(n) / (Factorial(r) * Factorial(n - r)));
        java.util.ArrayList listTuples = new java.util.ArrayList(tupleCount);

        for (int i = 0; i < bMatrix.getMatrixDimension().getRows(); i++) {
            for (int j = i + 1; j < bMatrix.getMatrixDimension().getRows(); j++) {
                listTuples.add(new Tuple(i, j));
            }
        }
        return listTuples;
    }

    //Balances two rows in a way that primary need to add some more weight to get the resultant weight.
    private static RowsBalanceResult RowBalanceGainWeight(Tuple rowPair, long[] primaryRowData, long[] secondaryRowData, long weightToGain, DistributionMatrix bMatrix) {
        int[] primaryIndicies = new int[bMatrix.getMatrixDimension().getCols()];
        int[] secondaryIndicies = new int[bMatrix.getMatrixDimension().getCols()];
        int tmpIndex, primaryLockAt, secondaryLockAt;
        long primaryRowWeight = bMatrix.getWeightPercentMatrix()[rowPair.getFirst()][1];
        long secondaryRowWeight = bMatrix.getWeightPercentMatrix()[rowPair.getSecond()][1];
        long weightToAchieve, primaryDistance, secondaryDistance, tmpWeightPri, tmpWeightSec, weightDifference;

        boolean primarySelect = false;
        primaryLockAt = -1;
        secondaryLockAt = -1;
        primaryDistance = 0;
        secondaryDistance = 0;
        boolean bSecondaryNeedsToLoose = true;
        //total weight to be made
        weightToAchieve = primaryRowWeight + weightToGain;

        RowsBalanceResult rbResult = new RowsBalanceResult();

        // for example first-row weight = 1000, second-row weight = 2000, required weight = 3000,
        // in this case second row need not to lose weight, so no need to keep it as a candidate.
        if (secondaryRowWeight < weightToAchieve) {
            bSecondaryNeedsToLoose = false;
        }

        //lets first populated indicies list for each row.This would help in geting the final set of indicies.
        for (int i = 0; i < bMatrix.getMatrixDimension().getCols(); i++) {
            primaryIndicies[i] = (rowPair.getFirst() * bMatrix.getMatrixDimension().getCols()) + i;
            secondaryIndicies[i] = (rowPair.getSecond() * bMatrix.getMatrixDimension().getCols()) + i;
        }

        //in this loop I am checking both ways. The one that needs to gain weight and the one that looses
        //weight in result. So any row can match the required weight. After each loop, each swap I check
        //for the criteria against both rows. In the end I get two indexes against both rows along with
        //possible extra/deficient count.
        //In Loose weight, primary is already high from the target,so no chance of secondary
        for (int i = 0; i < bMatrix.getMatrixDimension().getCols(); i++) {
            tmpWeightPri = primaryRowData[i];
            tmpWeightSec = secondaryRowData[i];

            weightDifference = tmpWeightSec - tmpWeightPri;

            primaryRowWeight += weightDifference;
            secondaryRowWeight -= weightDifference;

            if (primaryRowWeight > weightToAchieve && primaryLockAt == -1) {
                long diffAfterSwap = primaryRowWeight - weightToAchieve;
                long diffBeforeSwap = weightToAchieve - (primaryRowWeight - weightDifference);
                if (diffAfterSwap >= diffBeforeSwap) {
                    primaryLockAt = i - 1;
                    primaryDistance = diffBeforeSwap;
                } else {
                    primaryLockAt = i;
                    primaryDistance = diffAfterSwap;
                }
            }

            //Do secondary really needs to loose weight ?? Not all the time.
            if (secondaryRowWeight < weightToAchieve && secondaryLockAt == -1 && bSecondaryNeedsToLoose) {
                long diffAfterSwap = weightToAchieve - secondaryRowWeight;
                long diffBeforeSwap = (secondaryRowWeight + weightDifference) - weightToAchieve;

                if (diffAfterSwap >= diffBeforeSwap) {
                    secondaryLockAt = i - 1;
                    secondaryDistance = diffBeforeSwap;
                } else {
                    secondaryLockAt = i;
                    secondaryDistance = diffAfterSwap;
                }
            }
        }

        if (primaryLockAt != -1 && secondaryLockAt != -1) { //if we found both rows be candidates then select one with less error
            if (primaryDistance <= secondaryDistance) {
                primarySelect = true;
            } else {
                primarySelect = false;
            }
        } else {
            if (primaryLockAt != -1) {
                primarySelect = true;
            }

            if (secondaryLockAt != -1) {
                primarySelect = false;
            }
        }

        //unfortunately we found nothing ... So give the first row back with overhead value
        if (primaryLockAt == -1 && secondaryLockAt == -1) {
            primarySelect = true;
            primaryDistance = weightToAchieve - primaryRowWeight;
        }

        int swapCount = (primarySelect == true) ? primaryLockAt : secondaryLockAt;

        //do the items swapping according to swap count value
        for (int i = 0; i <= swapCount; i++) {
            tmpIndex = primaryIndicies[i];
            primaryIndicies[i] = secondaryIndicies[i];
            secondaryIndicies[i] = tmpIndex;
        }

        if (primarySelect == true) {
            rbResult.setResultIndicies(primaryIndicies);
            rbResult.setTargetDistance(primaryDistance);
        } else {
            rbResult.setResultIndicies(secondaryIndicies);
            rbResult.setTargetDistance(secondaryDistance);
        }

        return rbResult;
    }

    //Balances two rows in a way that primary need to lose some weight to get the resultant weight.
    //As secondary is always higher weight then primary, so primary cant lose weight. This makes it
    //straight forward.
    private static RowsBalanceResult RowBalanceLoseWeight(Tuple rowPair, long weightToLose, DistributionMatrix bMatrix) {
        int[] primaryIndicies = new int[bMatrix.getMatrixDimension().getCols()];

        RowsBalanceResult rbResult = new RowsBalanceResult();
        //lets first populated indicies list for each row.This would help in geting the final set of indicies.
        for (int i = 0; i < bMatrix.getMatrixDimension().getCols(); i++) {
            primaryIndicies[i] = (rowPair.getFirst() * bMatrix.getMatrixDimension().getCols()) + i;
        }

        rbResult.setResultIndicies(primaryIndicies);
        rbResult.setTargetDistance(weightToLose);

        return rbResult;
    }

    /**
     * Returns the factorial of any UInt64 less than 22
     *
     * @param n The number to get a factorial for.
     * @return The factorial of n. Throws an exception if the number passed is
     * greater than 21
     */
    public static long Factorial(int n) {
        if (n < 0) { //error result - undefined
            return -1;
        }
        if (n > 256) { //error result - input is too big
            return -2;
        }

        if (n == 0) {
            return 1;
        }

        // Calculate the factorial iteratively rather than recursively:
        long tempResult = 1;
        for (int i = 1; i <= n; i++) {
            tempResult *= i;
        }
        return tempResult;
    }
}