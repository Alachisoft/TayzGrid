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

import com.alachisoft.tayzgrid.cluster.util.Util;
import com.alachisoft.tayzgrid.common.datastructures.QueueClosedException;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import java.lang.Thread;
import java.util.Iterator;

public class DedicatedMessageSender extends Thread {

    private com.alachisoft.tayzgrid.common.datastructures.Queue mq;
    private ConnectionTable.Connection peerConnection;
    private ConnectionTable connectionTable;
    private int id;
    private Object sync_lock;

    private ILogger _ncacheLog;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }
    private int sendBufferSize = 1024 * 1024;

    private byte[] sendBuffer;
    private long waitTimeout = 0;
    private boolean isNagglingEnabled = true;
    private int nagglingSize = 500 * 1024;

    public DedicatedMessageSender(com.alachisoft.tayzgrid.common.datastructures.Queue mq, ConnectionTable.Connection connection, Object syncLockObj, ILogger NCacheLog, boolean onPrimaryNIC, boolean doNaggling, int naglingSize) {
        this.mq = mq;
        this.peerConnection = connection;
        if (connection != null) {
            String primary = connection.IsPrimary() ? "p" : "s";
            String primaryNIC = onPrimaryNIC ? "p" : "s";
            super.setName("DmSender - " + connection.peer_addr.toString() + " - " + primary + primaryNIC);
        }

        super.setDaemon(true);
        sync_lock = syncLockObj;
        _ncacheLog = NCacheLog;
        isNagglingEnabled = doNaggling;
        nagglingSize = naglingSize;
        if (nagglingSize + 8 > sendBufferSize) {
            sendBufferSize = nagglingSize + 8;
        }
        sendBuffer = new byte[sendBufferSize];
    }

    public final void UpdateConnection(ConnectionTable.Connection newCon) {
        peerConnection = newCon;
    }

    /**
     * Removes events from mq and calls handler.down(evt)
     */
    @Override
    public void run() {
        try {
            java.util.ArrayList msgList = new java.util.ArrayList();

            byte[] tmpBuffer = null;
            int totalMsgSize = 4;
            int noOfMsgs = 0;
            int offset = 8;
            boolean resendMessage = false;
            java.util.ArrayList msgsTobeSent = new java.util.ArrayList();
            while (!mq.getClosed()) {
                try {
                    if (resendMessage) {
                        if (tmpBuffer != null && tmpBuffer.length > 0) {
                            peerConnection.send(tmpBuffer, null, totalMsgSize + 4);

                        }
                        resendMessage = false;
                        continue;
                    }
                    msgsTobeSent.clear();
                    synchronized (sync_lock) {
                        tmpBuffer = sendBuffer;
                        totalMsgSize = 4;
                        noOfMsgs = 0;
                        offset = 8;
                        while (true) {
                            BinaryMessage bMsg = (BinaryMessage) mq.remove();

                            if (bMsg != null) {
                                if (!peerConnection.IsPrimary()) {
                                    msgsTobeSent.add(bMsg);
                                }
                                noOfMsgs++;

                                totalMsgSize += bMsg.getSize();

                                if (totalMsgSize + 8 > sendBuffer.length) {
                                    sendBuffer = new byte[totalMsgSize + 8];
                                    System.arraycopy(tmpBuffer, 0, sendBuffer, 0, totalMsgSize - bMsg.getSize());

                                    tmpBuffer = sendBuffer;
                                }
                                System.arraycopy(bMsg.getBuffer(), 0, tmpBuffer, offset, bMsg.getBuffer().length);

                                offset += bMsg.getBuffer().length;
                                if (bMsg.getUserPayLoad() != null) {

                                    byte[] buf = null;
                                    for (int i = 0; i < bMsg.getUserPayLoad().length; i++) {
                                        Object tempVar = bMsg.getUserPayLoad()[i];

                                        buf = (byte[]) ((tempVar instanceof byte[]) ? tempVar : null);
                                        System.arraycopy(buf, 0, tmpBuffer, offset, buf.length);

                                        offset += buf.length;
                                    }
                                }
                            }
                            bMsg = null;
                            boolean success = false;
                            tangible.RefObject< Boolean> tempRef_success = new tangible.RefObject<Boolean>(success);
                            Object tempVar2 = mq.peek(waitTimeout, tempRef_success);
                            success = tempRef_success.argvalue;
                            bMsg = (BinaryMessage) ((tempVar2 instanceof BinaryMessage) ? tempVar2 : null);
                            if ((!isNagglingEnabled || bMsg == null || ((bMsg.getSize() + totalMsgSize + 8) > nagglingSize))) {
                                break;
                            }

                        }
                    }

                    byte[] bTotalLength = Util.WriteInt32(totalMsgSize);
                    System.arraycopy(bTotalLength, 0, tmpBuffer, 0, bTotalLength.length);

                    byte[] bNoOfMsgs = Util.WriteInt32(noOfMsgs);
                    System.arraycopy(bNoOfMsgs, 0, tmpBuffer, 4, bNoOfMsgs.length);

                    peerConnection.send(tmpBuffer, null, totalMsgSize + 4);

                } catch (ExtSocketException e) {
                    getCacheLog().Error(super.getName(), e.toString());

                    if (peerConnection.IsPrimary()) {
                        if (peerConnection.LeavingGracefully()) {
                            getCacheLog().Error("DmSender.Run", peerConnection.peer_addr + " left gracefully");
                            break;
                        }

                        {
                            getCacheLog().Error("DMSender.Run", "Connection broken with " + peerConnection.peer_addr + ". node left abruptly");
                            ConnectionTable.Connection connection = peerConnection.getEnclosing_Instance().Reconnect(peerConnection.peer_addr);
                            if (connection != null) {

                                Thread.sleep(3000);
                                resendMessage = true;
                                continue;
                            } else {
                                getCacheLog().Error("DMSender.Run", super.getName() + ". Failed to re-establish connection with " + peerConnection.peer_addr);
                                break;
                            }

                        }

                    } else {
                        getCacheLog().Error("DmSender.Run", "secondary connection broken; peer_addr : " + peerConnection.peer_addr);
                        try {
                            for (Iterator it = msgsTobeSent.iterator(); it.hasNext();) {
                                BinaryMessage bMsg = (BinaryMessage) it.next();
                                try {
                                    if (bMsg != null && mq != null && !mq.getClosed()) {
                                        mq.add(bMsg);
                                    }
                                } catch (Exception ex) {
                                    getCacheLog().Error("DmSender.Run", "an error occured while requing the messages. " + ex.toString());
                                }
                            }
                        } catch (Exception ex) {
                        }
                    }
                    break;
                } catch (QueueClosedException e) {
                    //NCacheLog.Error(Name, e.ToString());
                    break;
                } catch (InterruptedException e3) {
                    break;
                } catch (Exception ex) {
                    getCacheLog().Error(getName() + "", "exception=" + ex.toString());

                }
            }
        } catch (Exception ex) {
            getCacheLog().Error(getName() + "", "exception=" + ex.toString());
        }
        try {
            peerConnection.getEnclosing_Instance().notifyConnectionClosed(peerConnection.peer_addr);
            peerConnection.getEnclosing_Instance().remove(peerConnection.peer_addr, peerConnection.IsPrimary());
        } catch (Exception e) {
        }

    }

    public final void dispose() {

    }

}
