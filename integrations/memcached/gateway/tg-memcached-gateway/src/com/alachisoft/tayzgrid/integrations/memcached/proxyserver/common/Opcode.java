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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common;

 
public enum Opcode {

    Get((byte) 0x00),
    Set((byte) 0x01),
    Add((byte) 0x02),
    Replace((byte) 0x03),
    Delete((byte) 0x04),
    Increment((byte) 0x05),
    Decrement((byte) 0x06),
    Quit((byte) 0x07),
    Flush((byte) 0x08),
    GetQ((byte) 0x09),
    No_op((byte) 0x0A),
    Version((byte) 0x0B),
    GetK((byte) 0x0C),
    GetKQ((byte) 0x0D),
    Append((byte) 0x0E),
    Prepend((byte) 0x0F),
    Stat((byte) 0x10),
    SetQ((byte) 0x11),
    AddQ((byte) 0x12),
    ReplaceQ((byte) 0x13),
    DeleteQ((byte) 0x14),
    IncrementQ((byte) 0x15),
    DecrementQ((byte) 0x16),
    QuitQ((byte) 0x17),
    FlushQ((byte) 0x18),
    AppendQ((byte) 0x19),
    PrependQ((byte) 0x1A),
    //for text protocol
    Gets((byte) 0x1B),
    CAS((byte) 0x1C),
    Touch((byte) 0x1D),
    Slabs_Reassign((byte) 0x1E),
    Slabs_Automove((byte) 0x1F),
    Verbosity((byte) 0x20),
    Invalid_Command((byte) 0x21),
    //binary unknown command
    unknown_command((byte) 0x22);
    private byte intValue;
    private static java.util.HashMap<Byte, Opcode> mappings;

    private static java.util.HashMap<Byte, Opcode> getMappings() {
        if (mappings == null) {
            synchronized (Opcode.class) {
                if (mappings == null) {
                    mappings = new java.util.HashMap<Byte, Opcode>();
                }
            }
        }
        return mappings;
    }

    private Opcode(byte value) {
        intValue = value;
        Opcode.getMappings().put(value, this);
    }

    public byte getValue() {
        return intValue;
    }

    public static Opcode forValue(byte value) {
        return getMappings().get(value);
    }
}