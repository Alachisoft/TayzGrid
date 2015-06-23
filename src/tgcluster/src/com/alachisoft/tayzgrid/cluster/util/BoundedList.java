/*
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

package com.alachisoft.tayzgrid.cluster.util;

import java.io.Serializable;

/**  A bounded subclass of List, oldest elements are removed once max capacity is exceeded
 <author>  Bela Ban Nov 20, 2003
 </author>
 <version>  $Id: BoundedList.java,v 1.2 2004/07/26 15:23:26 belaban Exp $
 </version>
*/
public class BoundedList extends List implements Serializable {
	public int max_capacity = 10;



	public BoundedList() {
	}

	public BoundedList(int size) {
		super();
		max_capacity = size;
	}


	/**  Adds an element at the tail. Removes an object from the head if capacity is exceeded
	 @param obj The object to be added

	*/
	@Override
	public void add(Object obj) {
		if (obj == null) {
			return;
		}
		while (_size >= max_capacity && _size > 0) {
			removeFromHead();
		}
		super.add(obj);
	}


	/**  Adds an object to the head, removes an element from the tail if capacity has been exceeded
	 @param obj The object to be added

	*/
	@Override
	public void addAtHead(Object obj) {
		if (obj == null) {
			return;
		}
		while (_size >= max_capacity && _size > 0) {
			remove();
		}
		super.addAtHead(obj);
	}
}
