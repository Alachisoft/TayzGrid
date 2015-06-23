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

/** 
 Summary description for Class1
*/

public class FlagsByte {
		//[FlagsAttribute]
		public enum Flag 
		{ // Hexidecimal        Decimal        Binary
		Clear(0x00), // 0x...0000        0            ...00000000000000000
		TRANS(0x01), // 0x...0001        1            ...00000000000000001
		COR(0x01 << 1), // 0x...0002        2            ...00000000000000010
		TOTAL(0x01 << 1 << 1), // 0x...0004        4            ...00000000000000100
		TCP(0x01 << 1 << 1 << 1), // 0x...0008        8            ...00000000000001000
		f5(0x01 << 1 << 1 << 1 << 1), // 0x...0010        16            ...00000000000010000
		f6(0x01 << 1 << 1 << 1 << 1 << 1), // 0x...0020        32            ...00000000000100000
		f7(0x01 << 1 << 1 << 1 << 1 << 1 << 1), // 0x...0040        64            ...00000000001000000
		f8(0x01 << 1 << 1 << 1 << 1 << 1 << 1 << 1); // 0x...0080        128            ...00000000010000000

		private int intValue;
		private static java.util.HashMap<Integer, Flag> mappings;
		private static java.util.HashMap<Integer, Flag> getMappings() {
			if (mappings == null) {
				synchronized (Flag.class) {
					if (mappings == null) {
						mappings = new java.util.HashMap<Integer, Flag>();
					}
				}
			}
			return mappings;
		}

		private Flag(int value) {
			intValue = value;
			Flag.getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static Flag forValue(int value) {
			return getMappings().get(value);
		}
		}

	/** 
	 The Field that will store our 64 flags
	*/
	private byte _DataByte;

	/** 
	 Public property SET and GET to access the Field
	*/
	public final byte getDataByte() {
		return _DataByte;
	}
	public final void setDataByte(byte value) {
		_DataByte = value;
	}

	/** 
	 Contructor
	 Add all initialization here
	*/
	public FlagsByte() {
		ClearField();
	}

	/** 
	 ClearField clears all contents of the Field
	 Set all bits to zero using the clear flag
	*/
	public final void ClearField() {
		SetField(Flag.Clear);
	}

	/** 
	 FillField fills all contents of the Field
	 Set all bits to zero using the negation of clear
	*/
	public final void FillField() {
		SetField(Flag.Clear);
	}

	/** 
	 Setting the specified flag(s) and turning all other flags off.
	  - Bits that are set to 1 in the flag will be set to one in the Field.
	  - Bits that are set to 0 in the flag will be set to zero in the Field. 
	 
	 @param flg The flag to set in Field
	*/
	private void SetField(Flag flg) {
		setDataByte((byte)flg.getValue());
	}

	/** 
	 Setting the specified flag(s) and leaving all other flags unchanged.
	  - Bits that are set to 1 in the flag will be set to one in the Field.
	  - Bits that are set to 0 in the flag will be unchanged in the Field. 
	 
	 <example>
	 OR truth table
	 0 | 0 = 0
	 1 | 0 = 1
	 0 | 1 = 1
	 1 | 1 = 1
	 </example>
	 @param flg The flag to set in Field
	*/
	public final void SetOn(Flag flg) {
		setDataByte((byte)(getDataByte() | (byte)flg.getValue()));
	}

	/** 
	 Unsetting the specified flag(s) and leaving all other flags unchanged.
	  - Bits that are set to 1 in the flag will be set to zero in the Field.
	  - Bits that are set to 0 in the flag will be unchanged in the Field. 
	 
	 <example>
	 AND truth table
	 0 & 0 = 0
	 1 & 0 = 0
	 0 & 1 = 0
	 1 & 1 = 1
	 </example>
	 @param flg The flag(s) to unset in Field
	*/
	public final void SetOff(Flag flg) {
		setDataByte((byte)(getDataByte() & (byte)flg.getValue()));
	}

	/** 
	 Toggling the specified flag(s) and leaving all other bits unchanged.
	  - Bits that are set to 1 in the flag will be toggled in the Field. 
	  - Bits that are set to 0 in the flag will be unchanged in the Field. 
	 
	 <example>
	 XOR truth table
	 0 ^ 0 = 0
	 1 ^ 0 = 1
	 0 ^ 1 = 1
	 1 ^ 1 = 0
	 </example>
	 @param flg The flag to toggle in Field
	*/
	public final void SetToggle(Flag flg) {
		setDataByte((byte)(getDataByte() ^ (byte)flg.getValue()));
	}

	/** 
	 AnyOn checks if any of the specified flag are set/on in the Field.
	 
	 @param flg flag(s) to check
	 @return 
	 true if flag is set in Field
	 false otherwise
	 
	*/
	public final boolean AnyOn(Flag flg) {
		return (getDataByte() & (byte)flg.getValue()) != 0;
	}

	/** 
	 AllOn checks if all the specified flags are set/on in the Field.
	 
	 @param flg flag(s) to check
	 @return 
	 true if all flags are set in Field
	 false otherwise
	 
	*/
	public final boolean AllOn(Flag flg) {
		return (getDataByte() & (byte)flg.getValue()) == (byte)flg.getValue();
	}

	/** 
	 IsEqual checks if all the specified flags are the same as in the Field.
	 
	 @param flg flag(s) to check
	 @return 
	 true if all flags identical in the Field
	 false otherwise
	 
	*/
	public final boolean IsEqual(Flag flg) {
		return getDataByte() == (byte)flg.getValue();
	}
}
