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

package com.alachisoft.tayzgrid.caching;

public class LockAccessTypeConverter {
	public static String ToString(LockAccessType accessType) {
		switch (accessType) {
			case ACQUIRE:
				return "1";

			case DONT_ACQUIRE:
				return "2";

			case RELEASE:
				return "3";

			case DONT_RELEASE:
				return "4";

			case IGNORE_LOCK:
				return "5";

			case COMPARE_VERSION:
				return "6";

			case GET_VERSION:
				return "7";

			case MATCH_VERSION:
				return "8";

			case PRESERVE_VERSION:
				return "9";
		}
		return "";
	}

	public static LockAccessType FromString(String str) {
		if (str.equals("1")) {
		return LockAccessType.ACQUIRE;
		}
		else if (str.equals("2")) {
		return LockAccessType.DONT_ACQUIRE;
		}
		else if (str.equals("3")) {
		return LockAccessType.RELEASE;
		}
		else if (str.equals("4")) {
		return LockAccessType.DONT_RELEASE;
		}
		else if (str.equals("5")) {
		return LockAccessType.IGNORE_LOCK;
		}
		else if (str.equals("6")) {
		return LockAccessType.COMPARE_VERSION;
		}
		else if (str.equals("7")) {
		return LockAccessType.GET_VERSION;
		}
		else if (str.equals("8")) {
		return LockAccessType.MATCH_VERSION;
		}
		else if (str.equals("9")) {
		return LockAccessType.PRESERVE_VERSION;
		}
		return LockAccessType.DEFAULT;
	}
}
