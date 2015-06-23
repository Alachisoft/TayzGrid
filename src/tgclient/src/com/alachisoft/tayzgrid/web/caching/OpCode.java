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



package com.alachisoft.tayzgrid.web.caching;

public enum OpCode {
        Add(0),
        Update(1),
        Remove(2),
        Clear(3);

        private final int index;

        OpCode(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        public static OpCode getOpCode(int index)
        {
            switch(index)
            {
                case 0:
                    return OpCode.Add;
                case 1:
                    return OpCode.Update;
                case 2:
                    return OpCode.Remove;
                 case 3:
                    return OpCode.Clear;
                default:
                    throw new RuntimeException("Unknown index:" + index);
            }


        }

}
