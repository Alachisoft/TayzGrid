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

public final class Timestamper {
  private static short counter = 0;
  private static long time;
  public static final short ONE_MS = 4096;

  public static long next()
  {
    synchronized (Timestamper.class) {
      long newTime = System.currentTimeMillis() << 12;
      if (time < newTime) {
        time = newTime;
        counter = 0;
      }
      else if (counter < 4095) {
        counter = (short)(counter + 1);
      }

      return time + counter;
    }
  }
}
