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

package com.alachisoft.tayzgrid.common.threading;

import java.util.LinkedList;
import java.util.List;

public class Monitor {

    static List<WaitableObject> s_watingObjects = new LinkedList<WaitableObject>();
    static final Object s_mutex = new Object();

    static class WaitableObject {

        private Object objectToWaitFor;
        private boolean pulsed;
        private int pulseCount;
        private int uniqueId;
        private String pulsingThread;
        private long waitingThreadId;
        private long waitTime;
        private short wakeupCount;
        
        private static int s_unique_id_generator;
        private static Object s_unique_id_mutex = new Object();

        public WaitableObject(Object objectToWaitFor) {
            this.objectToWaitFor = objectToWaitFor;
            synchronized (s_unique_id_mutex) {
                this.uniqueId = s_unique_id_generator++;
            }

        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof WaitableObject) {
                if (this.objectToWaitFor == ((WaitableObject) obj).objectToWaitFor) {
                    return true;
                }
            } else if (obj != null && this.objectToWaitFor == obj) {
                return true;
            }
            return false;
        }

        /*
         * Returns timeelapsed
         */
        public long waitObject(long timeout) throws InterruptedException {
            
            waitingThreadId = Thread.currentThread().getId();
            waitTime = timeout;
            synchronized (objectToWaitFor) {
                if (pulsed) {
                    return -1;
                }
                long start = System.currentTimeMillis();
                objectToWaitFor.wait(timeout);
                wakeupCount++;
                long end = System.currentTimeMillis();
                if(start >end ){
                    return -1;
                }
                return end -start;
            }
        }

        public void pulseObject() {
            synchronized (objectToWaitFor) {
                pulsed = true;
                pulseCount++;
                objectToWaitFor.notifyAll();
                objectToWaitFor.notify();
            }
        }

        public boolean isPulsed() {
            synchronized (objectToWaitFor) {
                return pulsed;
            }
        }

        public int getUniqueId() {
            return uniqueId;
        }
    }

    public static boolean wait(Object waitObject) throws InterruptedException {

        return wait(waitObject, Long.MAX_VALUE);

    }

    public static boolean wait(Object waitObject, long timeout) throws InterruptedException {

       boolean lockReacquired = true; 
       if(timeout <=0) timeout = Long.MAX_VALUE;
       
       WaitableObject waitingObject = new WaitableObject(waitObject);
        try {
            synchronized (s_mutex) {
                s_watingObjects.add(waitingObject);
            }
            long timeElapsed = 0;
            
            while (!waitingObject.isPulsed()) {
              
                timeout = timeout - timeElapsed;
                if(timeout <=0){
                    lockReacquired = false;
                    break;
                }
                
                timeElapsed = waitingObject.waitObject(timeout);
                if (timeElapsed <=0 && !waitingObject.isPulsed()) {
                    //timeout has elapsed
                    lockReacquired = false;
                    break;
                }
                
                
            }
        } finally {
            
            synchronized (s_mutex) {
                for (int i = 0; i < s_watingObjects.size(); i++) {
                    WaitableObject currentWaitingObject = s_watingObjects.get(i);
                    
                    if (currentWaitingObject.getUniqueId()== waitingObject.getUniqueId())
                    {
                        s_watingObjects.remove(i);
                        break;
                    }
                }
            }
        }
       
        return lockReacquired;
    }

    public static void pulse(Object waitObject) {

        WaitableObject waitingObject = null;
        synchronized (s_mutex) {
            for (int i = 0; i < s_watingObjects.size(); i++) {
                waitingObject = s_watingObjects.get(i);
                
                if (waitingObject != null && waitingObject.equals(waitObject)) {
                    waitingObject.pulseObject();
                    waitingObject.pulsingThread = Thread.currentThread().getName();
                }
            }
        }
    }
    
    public static void pulseAll() {

        WaitableObject waitingObject = null;
        synchronized (s_mutex) {
            for (int i = 0; i < s_watingObjects.size(); i++) {
                waitingObject = s_watingObjects.get(i);
                if (waitingObject != null ) {
                    waitingObject.pulseObject();
                    waitingObject.pulsingThread = Thread.currentThread().getName();
                }
            }
        }
    }
    
}
