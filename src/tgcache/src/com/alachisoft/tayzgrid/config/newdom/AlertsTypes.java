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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class AlertsTypes implements Cloneable, InternalCompactSerializable
{

    private boolean _cacheStop;
    private boolean _cacheStart;
    private boolean _nodeLeft;
    private boolean _nodeJoined;
    private boolean _stateTransferStop;
    private boolean _stateTransferStarted;
    private boolean _stateTransferError;
    private boolean _serviceStartError;
    private boolean _cacheSize;
    private boolean _generalError;
    private boolean _licensingError;
    private boolean _configurationError;
    private boolean _generalInformation;
    private boolean _unhandledExceptions;

    @ConfigurationAttributeAnnotation(value = "cache-stop", appendText = "")
    public final boolean getCacheStop()
    {
        return _cacheStop;
    }

    @ConfigurationAttributeAnnotation(value = "cache-stop", appendText = "")
    public final void setCacheStop(boolean value)
    {
        _cacheStop = value;
    }

    @ConfigurationAttributeAnnotation(value = "cache-start", appendText = "")
    public final boolean getCacheStart()
    {
        return _cacheStart;
    }

    @ConfigurationAttributeAnnotation(value = "cache-start", appendText = "")
    public final void setCacheStart(boolean value)
    {
        _cacheStart = value;
    }

    @ConfigurationAttributeAnnotation(value = "node-left", appendText = "")
    public final boolean getNodeLeft()
    {
        return _nodeLeft;
    }

    @ConfigurationAttributeAnnotation(value = "node-left", appendText = "")
    public final void setNodeLeft(boolean value)
    {
        _nodeLeft = value;
    }

    @ConfigurationAttributeAnnotation(value = "node-joined", appendText = "")
    public final boolean getNodeJoined()
    {
        return _nodeJoined;
    }

    @ConfigurationAttributeAnnotation(value = "node-joined", appendText = "")
    public final void setNodeJoined(boolean value)
    {
        _nodeJoined = value;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-started", appendText = "")
    public final boolean getStartTransferStarted()
    {
        return _stateTransferStarted;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-started", appendText = "")
    public final void setStartTransferStarted(boolean value)
    {
        _stateTransferStarted = value;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-stop", appendText = "")
    public final boolean getStartTransferStop()
    {
        return _stateTransferStop;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-stop", appendText = "")
    public final void setStartTransferStop(boolean value)
    {
        _stateTransferStop = value;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-error", appendText = "")
    public final boolean getStartTransferError()
    {
        return _stateTransferError;
    }

    @ConfigurationAttributeAnnotation(value = "state-transfer-error", appendText = "")
    public final void setStartTransferError(boolean value)
    {
        _stateTransferError = value;
    }

    @ConfigurationAttributeAnnotation(value = "service-start-error", appendText = "")
    public final boolean getServiceStartError()
    {
        return _serviceStartError;
    }

    @ConfigurationAttributeAnnotation(value = "service-start-error", appendText = "")
    public final void setServiceStartError(boolean value)
    {
        _serviceStartError = value;
    }

    @ConfigurationAttributeAnnotation(value = "cache-size", appendText = "")
    public final boolean getCacheSize()
    {
        return _cacheSize;
    }

    @ConfigurationAttributeAnnotation(value = "cache-size", appendText = "")
    public final void setCacheSize(boolean value)
    {
        _cacheSize = value;
    }

    @ConfigurationAttributeAnnotation(value = "general-error", appendText = "")
    public final boolean getGeneralError()
    {
        return _generalError;
    }

    @ConfigurationAttributeAnnotation(value = "general-error", appendText = "")
    public final void setGeneralError(boolean value)
    {
        _generalError = value;
    }

    @ConfigurationAttributeAnnotation(value = "licensing-error", appendText = "")
    public final boolean getLicensingError()
    {
        return _licensingError;
    }

    @ConfigurationAttributeAnnotation(value = "licensing-error", appendText = "")
    public final void setLicensingError(boolean value)
    {
        _licensingError = value;
    }

    @ConfigurationAttributeAnnotation(value = "configuration-error", appendText = "")
    public final boolean getConfigurationError()
    {
        return _configurationError;
    }

    @ConfigurationAttributeAnnotation(value = "configuration-error", appendText = "")
    public final void setConfigurationError(boolean value)
    {
        _configurationError = value;
    }

    @ConfigurationAttributeAnnotation(value = "general-info", appendText = "")
    public final boolean getGeneralInfo()
    {
        return _generalInformation;
    }

    @ConfigurationAttributeAnnotation(value = "general-info", appendText = "")
    public final void setGeneralInfo(boolean value)
    {
        _generalInformation = value;
    }

    @ConfigurationAttributeAnnotation(value = "unhandled-exceptions", appendText = "")
    public final boolean getUnHandledException()
    {
        return _unhandledExceptions;
    }

    @ConfigurationAttributeAnnotation(value = "unhandled-exceptions", appendText = "")
    public final void setUnHandledException(boolean value)
    {
        _unhandledExceptions = value;
    }

    @Override
    public final Object clone()
    {
        AlertsTypes alertTypes = new AlertsTypes();

        alertTypes.setCacheStop(this.getCacheStop());
        alertTypes.setCacheStart(this.getCacheStart());
        alertTypes.setNodeLeft(this.getNodeLeft());
        alertTypes.setNodeJoined(this.getNodeJoined());
        alertTypes.setStartTransferStarted(this.getStartTransferStarted());
        alertTypes.setStartTransferStop(this.getStartTransferStop());
        alertTypes.setStartTransferError(this.getStartTransferError());
        alertTypes.setServiceStartError(this.getServiceStartError());
        alertTypes.setCacheSize(this.getCacheSize());
        alertTypes.setGeneralError(this.getGeneralError());
        alertTypes.setLicensingError(this.getLicensingError());
        alertTypes.setConfigurationError(this.getConfigurationError());
        alertTypes.setGeneralInfo(this.getGeneralInfo());
        alertTypes.setUnHandledException(this.getUnHandledException());

        return alertTypes;
    }
    @Override
    public void Deserialize(CompactReader reader) throws IOException
    {
        _cacheStop = reader.ReadBoolean();
        _cacheStart = reader.ReadBoolean();
        _nodeLeft = reader.ReadBoolean();
        _nodeJoined = reader.ReadBoolean();
        _stateTransferStop = reader.ReadBoolean();
        _stateTransferStarted = reader.ReadBoolean();
        _stateTransferError = reader.ReadBoolean();
        _serviceStartError = reader.ReadBoolean();
        _cacheSize = reader.ReadBoolean();
        _generalError = reader.ReadBoolean();
        _licensingError = reader.ReadBoolean();
        _configurationError = reader.ReadBoolean();
        _generalInformation = reader.ReadBoolean();
        _unhandledExceptions = reader.ReadBoolean();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(_cacheStop);
        writer.Write(_cacheStart);
        writer.Write(_nodeLeft);
        writer.Write(_nodeJoined);
        writer.Write(_stateTransferStop);
        writer.Write(_stateTransferStarted);
        writer.Write(_stateTransferError);
        writer.Write(_serviceStartError);
        writer.Write(_cacheSize);
        writer.Write(_generalError);
        writer.Write(_licensingError);
        writer.Write(_configurationError);
        writer.Write(_generalInformation);
        writer.Write(_unhandledExceptions);
    }
}
