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

package com.alachisoft.tayzgrid.caching.enumeration;

import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;

public class LightWeightEnumerationProvider implements IEnumerationProvider {

	public final void Initialize(EnumerationPointer pointer, IndexedLocalCache cache) throws Exception {
		throw new Exception("The method or operation is not implemented.");
	}

	public final EnumerationDataChunk GetNextChunk(EnumerationPointer pointer) throws Exception {
		throw new Exception("The method or operation is not implemented.");
	}

	public final void dispose() {
	}
}
