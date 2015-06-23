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

import com.alachisoft.tayzgrid.common.datastructures.BinaryPriorityQueue;

public class EventQueue extends BinaryPriorityQueue
{

    public EventQueue()
    {
        super(new EventQueueComparer());
    }

    /**
     * Checks if the list is empty
     *
     * @return
     */
    public final boolean getIsEmpty()
    {
        synchronized (this)
        {
            return (getCount() == 0);
        }
    }

    public final QueuedEvent Pop()
    {
        synchronized (this)
        {
            Object tempVar = super.pop();
            return (QueuedEvent) ((tempVar instanceof QueuedEvent) ? tempVar : null);
        }
    }

    public final QueuedEvent Peek()
    {
        synchronized (this)
        {
            Object tempVar = super.peek();
            return (QueuedEvent) ((tempVar instanceof QueuedEvent) ? tempVar : null);
        }
    }

    public final int Push(Object O)
    {
        synchronized (this)
        {
            int res = super.push(O);
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);//this.notify();
            return res;
        }
    }

    /**
     * does what it says
     */
    @Override
    public final void Clear()
    {
        synchronized (this)
        {
            super.Clear();
            com.alachisoft.tayzgrid.common.threading.Monitor.pulse(this);//this.notify();
        }
    }
}
