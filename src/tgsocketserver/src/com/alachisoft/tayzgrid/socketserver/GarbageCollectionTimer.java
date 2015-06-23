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

package com.alachisoft.tayzgrid.socketserver;

import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.lang.Thread;
/**
 Follows Singleton patren. This class is responsible to do preodic collections of GEN#2.
 By default the due time and period is set to 5 mins. The minimum interval of 1 mins can be specified.
*/
public final class GarbageCollectionTimer 
{
	private static GarbageCollectionTimer _instance = null;
        private Thread timer;
	private static boolean stopped = true;
	/**
	 To prevent Object creation by user.
	*/
	private GarbageCollectionTimer() {
	}

	/**
	 Returns the GarbageCollectionThread instance. It ensures that only one instance
	 of this class exist.

	 @return GarbageCollectionThread instance
	*/
	public static GarbageCollectionTimer GetInstance() {
		if (_instance == null) {
			_instance = new GarbageCollectionTimer();
		}
		return _instance;
	}

	/**
	 priate reentrant mehtod used for the timercallback.
	*/
	private void StartColletion(Object[] objs) {
               TimeSpan dueTime=(TimeSpan)((objs[0] instanceof TimeSpan) ? objs[0] : null);
               TimeSpan period=(TimeSpan)((objs[0] instanceof TimeSpan) ? objs[0] : null);


               if(dueTime.getTotalMiliSeconds()>0)
               {
                   try
                   {
                   Monitor.wait(this, dueTime.getTotalMiliSeconds()); 
                   while (!stopped)
                   {
                       System.gc();
                       if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error("GarbageCollectionTimer.StartCollection");
			}

                       Monitor.wait(this,period.getTotalMiliSeconds());

                   }
                   }
                   catch (java.lang.InterruptedException e)
                   {

                   }
               }
               else if (dueTime.getTotalMiliSeconds() == 0)
               {
                   try
                   {
                   while (!stopped)
                   {
                       System.gc();
                       if (SocketServer.getLogger().getIsErrorLogsEnabled()) {
				SocketServer.getLogger().getCacheLog().Error("GarbageCollectionTimer.StartCollection");
			}

                       Monitor.wait(this,period.getTotalMiliSeconds());

                   }
                   }catch (java.lang.InterruptedException e)
                   {

                   }
               }
               else
                   return;


	}

	/**
	 Starts the timer.

	 @param dueTime The amount of time to delay before first collection, in minutes. minimum value is 0 mins which does the collection immediately.
	 @param period The time interval between tw oconsective collections, in minutes. minimus value is 1 mins.
	*/
	public void Start(int dueTime, int period) {
		if (dueTime < 0) {
			throw new IllegalArgumentException("The Value must be greater than equal to zero");
		}
		if (period < 1) {
			throw new IllegalArgumentException("The value must be greater than 0.");
		}

 		if (timer == null) {
                    final Object[] args = new Object[]
                    {
                        dueTime, period
                    };
                    timer = new Thread(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            StartColletion(args);
                        }
                    });
		}
		stopped = false;
	}

	public void Stop() {
		stopped = true;
		timer = null;
	}


	public void dispose() {
		Stop();
		_instance = null;
	}



}
