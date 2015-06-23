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

import java.io.Serializable;

public class QueueChangedState implements Serializable
{

    public QueueChangedState(QueueState oldState, QueueState newState)
    {
        _oldState = oldState;
        _newState = newState;
    }
    private QueueState _newState = QueueState.values()[0];

    public final QueueState getNewState()
    {
        return _newState;
    }

    public final void setNewState(QueueState value)
    {
        _newState = value;
    }
    private QueueState _oldState = QueueState.values()[0];

    public final QueueState getOldState()
    {
        return _oldState;
    }

    public final void setOldState(QueueState value)
    {
        _oldState = value;
    }
}
