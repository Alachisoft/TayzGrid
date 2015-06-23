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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import java.util.Collections;
import java.util.Iterator;

//Keeps data that is related to ONE NODE. This consist of almost all the required data in required form.
public class DistributionMatrix
{

    private Address _address;
    private java.util.ArrayList _filteredWeightIdList;
    private long[][] _weightPercentMatrix; //Two-D matrix always of [Height,2] dimension. At 0th index Im keeping %age weight of each row, at 1st index Im keeping the weight corresponding to that %age
    private MatrixDimensions _mDimensions; //Keeps the matrix dimensions
    private long[][] _weightMatrix;
    private int[][] _idMatrix;
    private int _itemsCount;
    private long _totalWeight;
    private long _weightToSacrifice; //weight this node needs to give away
    private int _bucketsToSacrifice;
    private int _percentWeightToSacrifice; //weight to sacrifice in percent. (w.r.t the same node data).
    private int _percentWeightOfCluster; //%age weight of cluster, THIS node is keeping .This helps in calculating this node's share
    private int _cushionFactor; //Cushion +- to be considered as Algorithm is Aprroximate rather Exact.
    private DistributionData _distData; //provide information about buckets and other calculations.
    public static int WeightBalanceThresholdPercent = 10; //Percent weight threshold before balancing weight
    private long _maxCacheSize = 1073741824; //Default One GB = 1024 * 1024 * 1024 (Byte * KB * MB = GB) User provided in if specified at UI.
    private long _weightBalanceThreshold = 0; //at what weight should the node be treated as contributor to incoming nodes.

    public DistributionMatrix(java.util.ArrayList weightIdList, Address address, DistributionData distData, ILogger NCacheLog)
    {
        _address = address;
        _distData = distData;
        _filteredWeightIdList = new java.util.ArrayList();
        _itemsCount = weightIdList.size();
        _totalWeight = 1;
        _weightToSacrifice = 0;
        _cushionFactor = 10;
        _percentWeightToSacrifice = 0;
        _weightBalanceThreshold = (int) ((_maxCacheSize * WeightBalanceThresholdPercent) / 100); //10%, threshold at which we feel to balance weight for incoming nodes. its value is percent of MaxCacheSize
        if (NCacheLog.getIsInfoEnabled())
        {
            NCacheLog.Info("DistributionMatrix.ctor", "Address->" + address.toString() + ", DistributionData->" + distData.toString());
        }
        //this is the temp code just to put some trace...
        int bucketCount = 0;
        for (Iterator it = weightIdList.iterator(); it.hasNext();)
        {
            WeightIdPair wiPair = (WeightIdPair) it.next();
            if (wiPair.getAddress().compare(address) == 0)
            {
                if (NCacheLog.getIsInfoEnabled())
                {
                    NCacheLog.Info("DistributionMatrix.ctor", "waitPair" + wiPair.getAddress().toString() + ", wiPait->" + wiPair.getBucketId());
                }
                _filteredWeightIdList.add(wiPair);
                bucketCount++;
            }
        }
        if (NCacheLog.getIsInfoEnabled())
        {
            NCacheLog.Info("DistributionMatrix..ctor", address + " owns " + bucketCount + " buckets");
        }
        Collections.sort(_filteredWeightIdList);

        if (NCacheLog.getIsInfoEnabled())
        {
            NCacheLog.Info("DistributionMatrix.ctor", "_filterWeightIdList.Count:" + _filteredWeightIdList.size() + ", distData.BucketPerNode: " + distData.getBucketsPerNode());
        }

        //Current bucket count - bucketss count after division gives buckets count to be sacrificed.
        _bucketsToSacrifice = _filteredWeightIdList.size() - distData.getBucketsPerNode();
        if (_bucketsToSacrifice <= 0)
        {
            NCacheLog.Error("DistributionMatrix", "Address::" + address.toString() + " cant sacrifice any bucket. Buckets/Node = " + distData.getBucketsPerNode()
                    + " My Buckets Count = " + _filteredWeightIdList.size());
            return;
        }
        double _weightList = (double) _filteredWeightIdList.size() ;
        double _buckets = (double) _bucketsToSacrifice;
        int rows = (int) Math.ceil(_weightList / _buckets);
        int cols = _bucketsToSacrifice;
        InitializeMatrix(rows, cols);
    }

    private void InitializeMatrix(int rows, int cols)
    {
        _mDimensions = new MatrixDimensions(rows, cols);
        _weightMatrix = new long[rows][cols];
        _idMatrix = new int[rows][cols];
        _weightPercentMatrix = new long[rows][2];

        int nLoopCount = 0;

        for (int i = 0; i < rows; i++)
        {
            long rowSum = 0;
            for (int j = 0; j < cols; j++)
            {
                if (nLoopCount < _filteredWeightIdList.size())
                {
                    WeightIdPair tmpPair = (WeightIdPair) _filteredWeightIdList.get(nLoopCount);
                    _weightMatrix[i][j] = tmpPair.getWeight();
                    _idMatrix[i][j] = tmpPair.getBucketId();
                    rowSum += tmpPair.getWeight();
                }
                else
                {
                    _weightMatrix[i][j] = -1;
                    _idMatrix[i][j] = -1;
                }
                nLoopCount++;
            }
            _weightPercentMatrix[i][1] = rowSum; //populate weightPercent Matrix while populating the weight and Id matrices.
            _totalWeight += rowSum;
        }

        //Here I am calculationg sum along with %age weight each row is keeping in. This would help while finding the right
        // set of buckets to be given off.
        for (int i = 0; i < _mDimensions.getRows(); i++)
        {
            _weightPercentMatrix[i][0] = (int) Math.ceil(((double) _weightPercentMatrix[i][1] / (double) _totalWeight) * 100);
        }

        //Calculate how much %age weight THIS NODE is keeping w.r.t overall cluster.
        _percentWeightOfCluster = (int) ((_totalWeight * 100) / _distData.getCacheDataSum());


        // Although buckets are sacrificed equally, but data is not.
        // Every node would share w.r.t the percentage that it is keeping in the Cluster.
        // If a node is keeping 50% share of the data, it would give away 50% of the required weight for the coming node.
        _weightToSacrifice = (int) Math.ceil(((double) _distData.getWeightPerNode() * (double) _percentWeightOfCluster) / 100);
        _percentWeightToSacrifice = (int) Math.ceil(((double) _weightToSacrifice / (double) _totalWeight) * 100);

    }

    public final long[][] getWeightPercentMatrix()
    {
        return _weightPercentMatrix;
    }

    public final int[][] getIdMatrix()
    {
        return _idMatrix;
    }

    public final long[][] getMatrix()
    {
        return _weightMatrix;
    }

    public final long getWeightToSacrifice()
    {
        return _weightToSacrifice;
    }

    public final void setWeightToSacrifice(long value)
    {
        _weightToSacrifice = value;
    }

    public final int getPercentWeightToSacrifice()
    {
        return _percentWeightToSacrifice;
    }

    public final long getTotalWeight()
    {
        return _totalWeight;
    }

    public final int getPercentWeightOfCluster()
    {
        return _percentWeightOfCluster;
    }

    public final int getCushionFactor()
    {
        return (int) Math.ceil((double) ((double) _percentWeightToSacrifice / (double) _cushionFactor));
    }

    public final MatrixDimensions getMatrixDimension()
    {
        return _mDimensions;
    }

    //Do we really need to balance the weight while a node joins ?. This would let us know.
    //Addition of this property is to deal with the case when buckets are sequentially assigned while the cluster is in start.
    public final boolean getDoWeightBalance()
    {
        if (_totalWeight > this.getWeightBalanceThreshold())
        {
            return true;
        }
        return false;
    }

    public final long getMaxCacheSize()
    {
        return _maxCacheSize;
    }

    public final void setMaxCacheSize(long value)
    {
        if (value > 0)
        {
            _maxCacheSize = value;
        }
    }

    public final long getWeightBalanceThreshold()
    {
        _weightBalanceThreshold = (long) ((_maxCacheSize * WeightBalanceThresholdPercent) / 100); //10%, threshold at which we feel to balance weight for incoming nodes. its value is percent of MaxCacheSize ;
        return _weightBalanceThreshold;
    }

    public final void setWeightBalanceThreshold(long value)
    {
        _weightBalanceThreshold = value;
    }
}