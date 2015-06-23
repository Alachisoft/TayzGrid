/*
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

package com.alachisoft.tayzgrid.cluster.blocks;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.enums.Priority;
import java.util.Iterator;

public class DedicatedMessageSendManager 
{

    private com.alachisoft.tayzgrid.common.datastructures.Queue mq;
    private Object sync_lock = new Object();
    private final java.util.ArrayList senderList = new java.util.ArrayList();
    private ILogger _ncacheLog;

    public DedicatedMessageSendManager(ILogger NCacheLog)
    {
        mq = new com.alachisoft.tayzgrid.common.datastructures.Queue();

        _ncacheLog = NCacheLog;
    }

    public final void AddDedicatedSenderThread(ConnectionTable.Connection connection, boolean onPrimaryNIC, boolean doNaggling, int nagglingSize)
    {
        DedicatedMessageSender dmSender = new DedicatedMessageSender(mq, connection, sync_lock, _ncacheLog, onPrimaryNIC, doNaggling, nagglingSize);
        dmSender.start();
        synchronized (senderList)
        {
            senderList.add(dmSender);
        }

    }

    public final void UpdateConnection(ConnectionTable.Connection newCon)
    {
        synchronized (senderList)
        {
            for (Iterator it = senderList.iterator(); it.hasNext();)
            {
                DedicatedMessageSender dmSender = (DedicatedMessageSender)it.next();
                dmSender.UpdateConnection(newCon);
            }
        }
    }


    public final int QueueMessage(byte[] buffer, Object[] userPayLoad, Priority prt)
    {
        return QueueMessage(new BinaryMessage(buffer, userPayLoad), prt);
    }

    public final int QueueMessage(BinaryMessage bmsg, Priority prt)
    {
        int queueCount = 0;
        if (mq != null)
        {
            try
            {
                mq.add(bmsg, prt);
                queueCount = mq.getCount();
            }
            catch (Exception e)
            {
            }
        }
        return queueCount;
    }


    public final void dispose()
    {

        synchronized (senderList)
        {
            for (Iterator it = senderList.iterator(); it.hasNext();)
            {
                DedicatedMessageSender dmSender = (DedicatedMessageSender)it.next();
                if (dmSender.isAlive())
                {
                    try
                    {
                       
                        _ncacheLog.Flush();
                        
                        dmSender.dispose();
                  ;
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
            senderList.clear();
            mq.close(false);
        }
    }

}
