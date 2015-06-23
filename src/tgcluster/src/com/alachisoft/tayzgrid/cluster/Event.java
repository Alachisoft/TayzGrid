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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.net.IRentableObject;
import java.io.Serializable;

//public enum Priority {Critical, Normal, Low};
/**
 * Used for intra-stack communication. <p><b>Author:</b> Chris Koiak, Bela Ban</p> <p><b>Date:</b> 12/03/2003</p>
 */
public class Event implements IRentableObject, Serializable
{

    public static final int MSG = 1;
    public static final int CONNECT = 2; // arg = group address (string)
    public static final int CONNECT_OK = 3; // arg = group multicast address (Address)
    public static final int DISCONNECT = 4; // arg = member address (Address)
    public static final int DISCONNECT_OK = 5;
    public static final int VIEW_CHANGE = 6; // arg = View (or MergeView in case of merge)
    public static final int GET_LOCAL_ADDRESS = 7;
    public static final int SET_LOCAL_ADDRESS = 8;
    public static final int SUSPECT = 9; // arg = Address of suspected member
    public static final int BLOCK = 10;
    public static final int BLOCK_OK = 11;
    public static final int FIND_INITIAL_MBRS = 12;
    public static final int FIND_INITIAL_MBRS_OK = 13; // arg = Vector of PingRsps
    public static final int MERGE = 14; // arg = Vector of Objects
    public static final int TMP_VIEW = 15; // arg = View
    public static final int BECOME_SERVER = 16; // sent when client has joined group

    public static final int GET_STATE = 19; // arg = StateTransferInfo
    public static final int GET_STATE_OK = 20; // arg = Object or Vector (state(s))

    public static final int START_QUEUEING = 22;
    public static final int STOP_QUEUEING = 23; // arg = Vector (event-list)
    public static final int SWITCH_NAK = 24;
    public static final int SWITCH_NAK_ACK = 25;
    public static final int SWITCH_OUT_OF_BAND = 26;
    public static final int FLUSH = 27; // arg = Vector (destinatinon for FLUSH)
    public static final int FLUSH_OK = 28; // arg = FlushRsp
    public static final int DROP_NEXT_MSG = 29;
    public static final int STABLE = 30; // arg = long[] (stable seqnos for mbrs)
    public static final int GET_MSG_DIGEST = 31; // arg = long[] (highest seqnos from mbrs)
    public static final int GET_MSG_DIGEST_OK = 32; // arg = Digest
    public static final int REBROADCAST_MSGS = 33; // arg = Vector (msgs with NakAckHeader)
    public static final int REBROADCAST_MSGS_OK = 34;
    public static final int GET_MSGS_RECEIVED = 35;
    public static final int GET_MSGS_RECEIVED_OK = 36; // arg = long[] (highest deliverable seqnos)
    public static final int GET_MSGS = 37; // arg = long[][] (range of seqnos for each m.)
    public static final int GET_MSGS_OK = 38; // arg = List
    public static final int GET_DIGEST = 39;
    public static final int GET_DIGEST_OK = 40; // arg = Digest (response to GET_DIGEST)
    public static final int SET_DIGEST = 41; // arg = Digest
    public static final int GET_DIGEST_STATE = 42; // see ./JavaStack/Protocols/pbcast/DESIGN for explanantion
    public static final int GET_DIGEST_STATE_OK = 43; // see ./JavaStack/Protocols/pbcast/DESIGN for explanantion
    public static final int SET_PARTITIONS = 44; // arg = HashMap of addresses and numbers
    public static final int MERGE_DENIED = 45; // Passed down from gms when a merge attempt fails
    public static final int EXIT = 46; // received when member was forced out of the group
    public static final int PERF_START = 47; // for performance measurements
    public static final int SUBVIEW_MERGE = 48; // arg = vector of addresses; see JGroups/EVS/Readme.txt
    public static final int SUBVIEWSET_MERGE = 49; // arg = vector of addresses; see JGroups/EVS/Readme.txt
    public static final int HEARD_FROM = 50; // arg = Vector (list of Addresses)
    public static final int UNSUSPECT = 51; // arg = Address (of unsuspected member)

    public static final int MERGE_DIGEST = 53; // arg = Digest
    public static final int BLOCK_SEND = 54; // arg = null
    public static final int UNBLOCK_SEND = 55; // arg = null
    public static final int CONFIG = 56; // arg = HashMap (config properties)
    public static final int GET_DIGEST_STABLE = 57;
    public static final int GET_DIGEST_STABLE_OK = 58; // response to GET_DIGEST_STABLE
    public static final int ACK = 59; // used to flush down events
    public static final int ACK_OK = 60; // response to ACK
    public static final int START = 61; // triggers start() - internal event, handled by Protocol
    public static final int START_OK = 62; // arg = exception of null - internal event, handled by Protocol
    public static final int STOP = 63; // triggers stop() - internal event, handled by Protocol
    public static final int STOP_OK = 64; // arg = exception or null - internal event, handled by Protocol
    public static final int SUSPEND_STABLE = 65; // arg = null
    public static final int RESUME_STABLE = 66; // arg = null
    public static final int VIEW_CHANGE_OK = 67; // arg = null
    public static final int TCPPING = 68;
    public static final int PERF_STOP = 69; // for performance measurements
    public static final int PERF_STOP_OK = 70; // for performance measurements
    public static final int USER_DEFINED = 1000; // arg = <user def., e.g. evt type + data>
    public static final int MSG_URGENT = 71; // for urgent messages
    public static final int GET_NODE_STATUS = 72; // to get the status of a node
    public static final int GET_NODE_STATUS_OK = 73; // returns the status of the
    public static final int CHECK_NODE_CONNECTED = 74; // to checks whether all this node is connected with all nodes or not;
    public static final int CHECK_NODE_CONNECTED_OK = 75; // returns the list of nodes to which this node can not make connection
    public static final int CONNECTION_FAILURE = 76; // tells about the nodes with which we can not establish connection
    public static final int VIEW_BCAST_MSG = 78; // view message to be broadcasted; it is handled differently;
    public static final int HASHMAP_REQ = 79; // Request for both HashMaps for Data Distribution and Mirror Mapping for dynamic mirroring.
    public static final int HASHMAP_RESP = 80; // Response of the above Hashmap and Mirror Mapping request.
    public static final int CONNECT_PHASE_2 = 81;
    public static final int CONNECT_OK_PHASE_2 = 82;
    public static final int CONFIGURE_NODE_REJOINING = 83;
    public static final int NODE_REJOINING = 84;
    public static final int RESET_SEQUENCE = 85;
    public static final int CONNECTION_BREAKAGE = 86;
    public static final int CONNECTION_RE_ESTABLISHED = 87;
    public static final int CONFIRM_CLUSTER_STARTUP = 88;
    public static final int HAS_STARTED = 89;
    public static final int ASK_JOIN = 90;
    public static final int ASK_JOIN_RESPONSE = 91;
    public static final int MARK_CLUSTER_IN_STATETRANSFER = 92;
    public static final int MARK_CLUSTER_STATETRANSFER_COMPLETED = 93;
    public static final int I_AM_LEAVING = 94;
    public static final int NOTIFY_LEAVING = 95;
    public static final int CONNECTION_NOT_OPENED = 96;
    
    /**
     * Current type of event
     */
    private int type = 0;
    /**
     * Object associated with the type
     */
    private Object arg = null; // must be serializable if used for inter-stack communication
    /**
     * Priority of this event.
     */
    private Priority priority = Priority.Normal;
    private String reason = "";
    private int rentId;

    /**
     * Constructor
     *
     * @param type Type of Event
     */
    public Event(int type)
    {
        this.type = type;
    }

    public Event()
    {
    }

    /**
     * Constructor
     *
     * @param type Type of Event
     * @param arg Object associated with type
     */
    public Event(int type, Object arg)
    {
        this.type = type;
        this.arg = arg;
    }

    /**
     * Constructor
     *
     * @param type Type of Event
     * @param arg Object associated with type
     */
    public Event(int type, Object arg, Priority priority)
    {
        this.type = type;
        this.arg = arg;
        this.priority = priority;
    }

    /**
     * Gets and sets the type of the Event
     */
    public final int getType()
    {
        return type;
    }

    public final void setType(int value)
    {
        type = value;
    }

    /**
     * Gets and sets the object associated with the Event
     */
    public final Object getArg()
    {
        return arg;
    }

    public final void setArg(Object value)
    {
        arg = value;
    }

    /**
     * Gets and sets the type of the Event
     */
    public final Priority getPriority()
    {
        return priority;
    }

    public final void setPriority(Priority value)
    {
        priority = value;
    }

    public final String getReason()
    {
        return reason;
    }

    public final void setReason(String value)
    {
        reason = value;
    }

    /**
     * Converts an Event type to a string representation
     *
     * @param t Type of event
     * @return A string representatio nof the Event type
     */
    public static String type2String(int t)
    {
        switch (t)
        {
            case MSG:
                return "MSG";
            case CONNECT:
                return "CONNECT";
            case CONNECT_OK:
                return "CONNECT_OK";
            case DISCONNECT:
                return "DISCONNECT";
            case DISCONNECT_OK:
                return "DISCONNECT_OK";
            case VIEW_CHANGE:
                return "VIEW_CHANGE";
            case GET_LOCAL_ADDRESS:
                return "GET_LOCAL_ADDRESS";
            case SET_LOCAL_ADDRESS:
                return "SET_LOCAL_ADDRESS";
            case SUSPECT:
                return "SUSPECT";
            case BLOCK:
                return "BLOCK";
            case BLOCK_OK:
                return "BLOCK_OK";
            case FIND_INITIAL_MBRS:
                return "FIND_INITIAL_MBRS";
            case FIND_INITIAL_MBRS_OK:
                return "FIND_INITIAL_MBRS_OK";
            case TMP_VIEW:
                return "TMP_VIEW";
            case BECOME_SERVER:
                return "BECOME_SERVER";

            case GET_STATE:
                return "GET_STATE";
            case GET_STATE_OK:
                return "GET_STATE_OK";

            case START_QUEUEING:
                return "START_QUEUEING";
            case STOP_QUEUEING:
                return "STOP_QUEUEING";
            case SWITCH_NAK:
                return "SWITCH_NAK";
            case SWITCH_NAK_ACK:
                return "SWITCH_NAK_ACK";
            case SWITCH_OUT_OF_BAND:
                return "SWITCH_OUT_OF_BAND";
            case FLUSH:
                return "FLUSH";
            case FLUSH_OK:
                return "FLUSH_OK";
            case DROP_NEXT_MSG:
                return "DROP_NEXT_MSG";
            case STABLE:
                return "STABLE";
            case GET_MSG_DIGEST:
                return "GET_MSG_DIGEST";
            case GET_MSG_DIGEST_OK:
                return "GET_MSG_DIGEST_OK";
            case REBROADCAST_MSGS:
                return "REBROADCAST_MSGS";
            case REBROADCAST_MSGS_OK:
                return "REBROADCAST_MSGS_OK";
            case GET_MSGS_RECEIVED:
                return "GET_MSGS_RECEIVED";
            case GET_MSGS_RECEIVED_OK:
                return "GET_MSGS_RECEIVED_OK";
            case GET_MSGS:
                return "GET_MSGS";
            case GET_MSGS_OK:
                return "GET_MSGS_OK";
            case GET_DIGEST:
                return "GET_DIGEST";
            case GET_DIGEST_OK:
                return "GET_DIGEST_OK";
            case SET_DIGEST:
                return "SET_DIGEST";
            case GET_DIGEST_STATE:
                return "GET_DIGEST_STATE";
            case GET_DIGEST_STATE_OK:
                return "GET_DIGEST_STATE_OK";
            case SET_PARTITIONS: // Added by gianlucac@tin.it to support PARTITIONER
                return "SET_PARTITIONS";
            case MERGE: // Added by gianlucac@tin.it to support partitions merging in GMS
                return "MERGE";
            case MERGE_DENIED: // as above
                return "MERGE_DENIED";
            case EXIT:
                return "EXIT";
            case PERF_START:
                return "PERF_START";
            case PERF_STOP:
                return "PERF_STOP";
            case SUBVIEW_MERGE:
                return "SUBVIEW_MERGE";
            case SUBVIEWSET_MERGE:
                return "SUBVIEWSET_MERGE";
            case HEARD_FROM:
                return "HEARD_FROM";
            case UNSUSPECT:
                return "UNSUSPECT";

            case MERGE_DIGEST:
                return "MERGE_DIGEST";
            case BLOCK_SEND:
                return "BLOCK_SEND";
            case UNBLOCK_SEND:
                return "UNBLOCK_SEND";
            case CONFIG:
                return "CONFIG";
            case GET_DIGEST_STABLE:
                return "GET_DIGEST_STABLE";
            case GET_DIGEST_STABLE_OK:
                return "GET_DIGEST_STABLE_OK";
            case ACK:
                return "ACK";
            case ACK_OK:
                return "ACK_OK";
            case START:
                return "START";
            case START_OK:
                return "START_OK";
            case STOP:
                return "STOP";
            case STOP_OK:
                return "STOP_OK";
            case SUSPEND_STABLE:
                return "SUSPEND_STABLE";
            case RESUME_STABLE:
                return "RESUME_STABLE";
            case VIEW_CHANGE_OK:
                return "VIEW_CHANGE_OK";
            case MSG_URGENT:
                return "MSG_URGENT";
            case CHECK_NODE_CONNECTED:
                return "CHECK_NODE_CONNECTED";
            case CHECK_NODE_CONNECTED_OK:
                return "CHECK_NODE_CONNECTED_OK";
            case GET_NODE_STATUS:
                return "GET_NODE_STATUS";
            case GET_NODE_STATUS_OK:
                return "GET_NODE_STATUS_OK";
            case VIEW_BCAST_MSG:
                return "VIEW_BCAST_MSG";

            case USER_DEFINED:
                return "USER_DEFINED";
            case CONNECT_PHASE_2:
                return "CONNECT_PHASE_2";
            case CONNECT_OK_PHASE_2:
                return "CONNECT_OK_PHASE_2";
            case CONFIGURE_NODE_REJOINING:
                return "CONFIGURE_NODE_REJOINING";
            case NODE_REJOINING:
                return "NODE_REJOINING";
            case RESET_SEQUENCE:
                return "RESET_SEQUENCE";
            case CONNECTION_BREAKAGE:
                return "CONNECTION_BREAKAGE";
            case CONNECTION_RE_ESTABLISHED:
                return "CONNECTION_RE_ESTABLISHED";
            case CONFIRM_CLUSTER_STARTUP:
                return "CONFIRM_CLUSTER_STARTUP";
            case HAS_STARTED:
                return "HAS_STARTED";
            case ASK_JOIN:
                return "ASK_JOIN";
            case ASK_JOIN_RESPONSE:
                return "ASK_JOIN_RESPONSE";
            case I_AM_LEAVING:
                return "I_AM_LEAVING";
            default:
                return "UNDEFINED";
        }
    }

    public final void Reset()
    {
        type = 0;
        arg = null;
        priority = Priority.Normal;
    }


    /**
     * Returns a string representation of the Event
     *
     * @return A string representation of the Event
     */
    @Override
    public String toString()
    {
        return "Event[type=" + type2String(type) + ", arg=" + arg + "]";
    }

    //<editor-fold defaultstate="collapsed" desc="IRentableObject Members">
    @Override
    public final int getRentId()
    {
        return rentId;
    }

    @Override
    public final void setRentId(int value)
    {
        rentId = value;
    }
    //</editor-fold>
}
