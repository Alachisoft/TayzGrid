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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util;

import java.util.Comparator;

public abstract interface Lockable {
    
    public abstract Lock lock(long paramLong, int paramInt);

    public abstract boolean isLock();

    public abstract boolean isGettable(long paramLong);

    public abstract boolean isPuttable(long paramLong, Object paramObject, Comparator paramComparator);
}
