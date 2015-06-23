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

package com.alachisoft.tayzgrid.common.datastructures;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class GroupEnumerationPointer extends EnumerationPointer implements ICompactSerializable {
	private String _group;
	private String _subGroup;

	public GroupEnumerationPointer(String group, String subGroup) {
		super();
		_group = group;
		_subGroup = subGroup;
	}

	public GroupEnumerationPointer(String id, int chunkId, String group, String subGroup) {
		super(id, chunkId);
		_group = group;
		_subGroup = subGroup;
	}

	public final String getGroup() {
		return _group;
	}
	public final void setGroup(String value) {
		_group = value;
	}

	public final String getSubGroup() {
		return _subGroup;
	}
	public final void setSubGroup(String value) {
		_subGroup = value;
	}


    @Override
	public boolean isGroupPointer() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;

		if (obj instanceof GroupEnumerationPointer) {
			GroupEnumerationPointer other = (GroupEnumerationPointer)((obj instanceof GroupEnumerationPointer) ? obj : null);
			if (super.equals(obj)) {
				equals = _group.equals(other._group) && _subGroup.equals(other._subGroup);
			}
		}

		return equals;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}


	private void readExternalize(CacheObjectInput reader) throws IOException {
		_group = reader.readUTF();
		_subGroup = reader.readUTF();
	}

	private void writeExternalize(CacheObjectOutput writer) throws IOException {
		writer.writeUTF(_group);
		writer.writeUTF(_subGroup);
	}

}
