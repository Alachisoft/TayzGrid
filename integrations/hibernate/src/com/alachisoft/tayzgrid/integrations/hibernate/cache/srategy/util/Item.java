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

import java.io.Serializable;
import java.util.Comparator;

public final class Item
    implements Serializable, Lockable
  {
    private final long freshTimestamp;
    private final Object value;
    private final Object version;

    public Item(Object value, Object version, long currentTimestamp)
    {
      this.value = value;
      this.version = version;
      this.freshTimestamp = currentTimestamp;
    }

    public long getFreshTimestamp()
    {
      return this.freshTimestamp;
    }

    public Object getValue()
    {
      return this.value;
    }

    @Override
    public Lock lock(long timeout, int id)
    {
      return new Lock(timeout, id, this.version);
    }

    @Override
    public boolean isLock()
    {
      return false;
    }

    @Override
    public boolean isGettable(long txTimestamp)
    {
      return this.freshTimestamp < txTimestamp;
    }

    @Override
    public boolean isPuttable(long txTimestamp, Object newVersion, Comparator comparator)
    {
      return (this.version != null) && (comparator.compare(this.version, newVersion) < 0);
    }

    @Override
    public String toString() {
      return "Item{version=" + this.version + ",freshTimestamp=" + this.freshTimestamp;
    }
  }
