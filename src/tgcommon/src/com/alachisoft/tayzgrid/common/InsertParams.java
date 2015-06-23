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

package com.alachisoft.tayzgrid.common;

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;


public class InsertParams implements ICompactSerializable{
    //for a replace operation, a value corresponding to that key must already exist
    public boolean IsReplaceOperation = false;
    //True if existing value against specified key is needed to be returned
    public boolean ReturnExistingValue = false;
    //True if an old value passed must match with existing cached value for operation to be performed
    public boolean CompareOldValue = false;
    //Old value that is to be compared
    public Object OldValue;
    //Flags 
    public BitSet OldValueFlag;

    @Override
    public void serialize(CacheObjectOutput out) throws IOException {
        out.writeBoolean(IsReplaceOperation);
        out.writeBoolean(ReturnExistingValue);
        out.writeBoolean(CompareOldValue);
        out.writeObject(OldValue);
        if(OldValueFlag != null)
           out.writeByte(OldValueFlag.getData());
        else
           out.writeByte(0);

    }

    @Override
    public void deserialize(CacheObjectInput in) throws IOException, ClassNotFoundException {
        IsReplaceOperation = in.readBoolean();
        ReturnExistingValue = in.readBoolean();
        CompareOldValue = in.readBoolean();
        OldValue = in.readObject();
        OldValueFlag = new BitSet(in.readByte());
    }
}
