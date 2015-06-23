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

public class TagComparisonTypeConverter {
	public static String ToString(TagComparisonType comparisonType) {
		switch (comparisonType) {
			case BY_TAG:
				return "1";

			case ALL_MATCHING_TAGS:
				return "2";

			case ANY_MATCHING_TAG:
				return "3";
		}
		return "";
	}

	public static TagComparisonType FromString(String str) {
		if (str.equals("1")) {
                    return TagComparisonType.BY_TAG;
		}
		else if (str.equals("2")) {
                    return TagComparisonType.ALL_MATCHING_TAGS;
		}
		else if (str.equals("3")) {
                    return TagComparisonType.ANY_MATCHING_TAG;
		}
		return TagComparisonType.DEFAULT;
	}
}
