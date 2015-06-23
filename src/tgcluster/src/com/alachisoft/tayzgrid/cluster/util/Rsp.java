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

package com.alachisoft.tayzgrid.cluster.util;

import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.io.IOException;

// $Id: Rsp.java,v 1.1.1.1 2003/09/09 01:24:12 belaban Exp $

 
public class Rsp {
	public final Object getValue() {
		return retval;
	}
	public final Object getSender() {
		return sender;
	}

	/* flag that represents whether the response was received */
	public boolean received = false;
	/* flag that represents whether the response was suspected */
	public boolean suspected = false;
	/* The sender of this response */
	public Object sender = null;
	/* the value from the response */
	public Object retval = null;

	/* Time taken on the cluster for an operation*/
	public long cluserTimetaken = 0;
	/* Time taken by the application to perform the operation*/
	public long appTimetaken = 0;

	public Rsp(Object sender) {
		this.sender = sender;
	}

	public Rsp(Object sender, boolean suspected) {
		this.sender = sender;
		this.suspected = suspected;
	}

	public Rsp(Object sender, Object retval) {
		this.sender = sender;
		this.retval = retval;
		received = true;
	}

	public Rsp(Object sender, Object retval, long clusterTime, long appTime) {
		this.sender = sender;
		this.retval = retval;
		this.cluserTimetaken = clusterTime;
		this.appTimetaken = appTime;
		received = true;
	}

	public final void Deflate(String serializationContext) throws IOException, ClassNotFoundException {
		if (retval != null) {
			if (retval instanceof OperationResponse) {
				((OperationResponse)retval).SerializablePayload = CompactBinaryFormatter.fromByteBuffer((byte[])((OperationResponse)retval).SerializablePayload, serializationContext);
			} else if (retval instanceof byte[]) {
				retval = CompactBinaryFormatter.fromByteBuffer((byte[])retval, serializationContext);
			}
		}
	}
	public final boolean wasReceived() {
		return received;
	}

	public final boolean wasSuspected() {
		return suspected;
	}


	@Override
	public String toString() {
		return "sender=" + sender + ", retval=" + retval + ", received=" + received + ", suspected=" + suspected;
	}


	public final long getClusterTimeTaken() {
		return cluserTimetaken;
	}
	public final void setClusterTimeTaken(long value) {
		cluserTimetaken = value;
	}

	public final long getAppTimeTaken() {
		return appTimetaken;
	}
	public final void setAppTimeTaken(long value) {
		appTimetaken = value;
	}
}
