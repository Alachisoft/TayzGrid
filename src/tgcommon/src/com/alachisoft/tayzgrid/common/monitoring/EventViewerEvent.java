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

package com.alachisoft.tayzgrid.common.monitoring;

import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.Date;

/**
 Contains the information about events in event viewer.
*/
public class EventViewerEvent implements InternalCompactSerializable {
	private long _instanceId;
	private NCDateTime _timeGenerated;
	private String _source;
	private String _message;
	private String _node;
	public static EventType _type = EventType.INFORMATION;

//	/**
//	 Default constructor.
//	*/
	public EventViewerEvent() {
	}

        /**
	 Gets the Id of the event.
	*/
	public final long getInstanceID() {
		return _instanceId;
	}

	/**
	 Gets the source of the event.
	*/
	public final String getSource() {
		return _source;
	}

        /**
         * Sets the source of the event.
         * @param source
         * @return _source
         */
        public final String setSource(String source) {
            return _source = source;
        }


	/**
	 Gets the time of the event.
	*/
	public final NCDateTime getTimeGenerated() {
		return _timeGenerated;
	}

	/**
	 Gets the detail of the event.
	*/

	public final String getMessage() {
		return _message;
	}

        /**
         *
         */
        public final void setMessage(String message) {
            _message = message;
        }


	public final String getMachine() {
		return _node;
	}

	public final void setMachine(String value) {
		_node = value;
	}

        public final void setEventType(EventType type)
        {
            _type = type;
        }

        public final void setGeneratedTime(NCDateTime dateTime)
        {
            _timeGenerated = dateTime;
        }

        public final void setInstanceId(long id)
        {
            _instanceId = id;
        }

        public final EventType getEventType()
        {
            return _type;
        }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
            writer.Write(_instanceId);
            writer.Write(_timeGenerated.getTicks());
            writer.WriteObject(_source);
            writer.WriteObject(_message);
            writer.WriteObject(_node);

            if ( _type == EventType.ERROR)
                writer.Write(1);
            else if ( _type == EventType.WARNING)
                writer.Write(2);
            else if ( _type == EventType.INFORMATION)
                writer.Write(4);
    }

}
