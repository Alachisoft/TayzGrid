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
import java.util.Collections;
import java.util.Iterator;

//This class keeps information related to each indivisual node.
public class BalanceDataForNode {
	private Address _address;
	private java.util.ArrayList _filteredWeightIdList;
	private int _percentData; //%age data of cluster this node is carrying.
	private int _itemsCount; // total buckets this node is keeping  with.
	private long _totalWeight; //total weight of this node.

	public BalanceDataForNode(java.util.ArrayList weightIdList, Address address, long clusterWeight) {
		_address = address;
		_filteredWeightIdList = new java.util.ArrayList();
		_totalWeight = 1;
        for (Iterator it = weightIdList.iterator(); it.hasNext();)
        {
            WeightIdPair wiPair = (WeightIdPair)it.next();
            if (wiPair.getAddress().compare(address) == 0) {
                    _filteredWeightIdList.add(wiPair);
                    _totalWeight += wiPair.getWeight();
            }
        }
                Collections.sort(_filteredWeightIdList);
		_itemsCount = _filteredWeightIdList.size();
		_percentData = (int)(((double)_totalWeight / (double)clusterWeight) * 100);
	}


	public final int getPercentData() {
		return _percentData;
	}

	public final long getTotalWeight() {
		return _totalWeight;
	}

	public final int getItemsCount() {
		return _itemsCount;
	}

	public final java.util.ArrayList getWeightIdList() {
		return _filteredWeightIdList;
	}
	public final void setWeightIdList(java.util.ArrayList value) {
		_filteredWeightIdList = value;
                Collections.sort(_filteredWeightIdList);
	}

	public final Address getNodeAddress() {
		return _address;
	}
	public final void setNodeAddress(Address value) {
		_address = value;
	}

}
