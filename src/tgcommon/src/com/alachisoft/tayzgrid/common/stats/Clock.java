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
package com.alachisoft.tayzgrid.common.stats;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

public class Clock implements Runnable {

    private static int _updateInterval = 1; //in seconds;
    private static long _currentTime;
    private static boolean _started;
    private static int _refCount;
    private static Thread _timerThread;

    public static void StartClock()
    {
        synchronized (Clock.class)
        {
            if (!_started) 
            {               
                _timerThread = new Thread(new Clock());
                _timerThread.start();
                _started = true;
            }
            _refCount++;
        }
        
    }

    public static void StopClock() 
    {
        synchronized (Clock.class) 
        {
            _refCount--;
            if (_started && _refCount == 0) 
            {
                _timerThread.interrupt();
                _started = false;
                _currentTime = 0;
            }
        }
    }

    private static void TimerElapsed(Object sender) 
    {
        tangible.RefObject<Long> tempRef_currentTime = new tangible.RefObject<Long>(_currentTime);
        _currentTime = tempRef_currentTime.argvalue++;
    }

    public static long getCurrentTimeInSeconds() {
        return _currentTime;
    }

    @Override
    public void run()
    {
        while (true && !Thread.currentThread().isInterrupted())
        {
            Clock.TimerElapsed(this);
            try 
            {
                Thread.sleep(_updateInterval * 1000);
            } 
            catch (InterruptedException ex) 
            {
               break; 
            }
        }
    }
}
