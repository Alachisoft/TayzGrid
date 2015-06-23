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

package com.alachisoft.tayzgrid.common.propagator;

public class AlertNotificationTypes {
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
	private boolean _securityError;
	private boolean _generalInformation;
	private boolean _unhandledExceptions;

	public final boolean getCacheStop() {
		return _cacheStop;
	}
	public final void setCacheStop(boolean value) {
		_cacheStop = value;
	}

	public final boolean getCacheStart() {
		return _cacheStart;
	}
	public final void setCacheStart(boolean value) {
		_cacheStart = value;
	}

	public final boolean getNodeLeft() {
		return _nodeLeft;
	}
	public final void setNodeLeft(boolean value) {
		_nodeLeft = value;
	}

	public final boolean getNodeJoined() {
		return _nodeJoined;
	}
	public final void setNodeJoined(boolean value) {
		_nodeJoined = value;
	}

	public final boolean getStartTransferStarted() {
		return _stateTransferStarted;
	}
	public final void setStartTransferStarted(boolean value) {
		_stateTransferStarted = value;
	}

	public final boolean getStartTransferStop() {
		return _stateTransferStop;
	}
	public final void setStartTransferStop(boolean value) {
		_stateTransferStop = value;
	}

	public final boolean getStartTransferError() {
		return _stateTransferError;
	}
	public final void setStartTransferError(boolean value) {
		_stateTransferError = value;
	}

	public final boolean getServiceStartError() {
		return _serviceStartError;
	}
	public final void setServiceStartError(boolean value) {
		_serviceStartError = value;
	}

	public final boolean getCacheSize() {
		return _cacheSize;
	}
	public final void setCacheSize(boolean value) {
		_cacheSize = value;
	}

	public final boolean getGeneralError() {
		return _generalError;
	}
	public final void setGeneralError(boolean value) {
		_generalError = value;
	}

	public final boolean getLicensingError() {
		return _licensingError;
	}
	public final void setLicensingError(boolean value) {
		_licensingError = value;
	}

	public final boolean getConfigurationError() {
		return _configurationError;
	}
	public final void setConfigurationError(boolean value) {
		_configurationError = value;
	}

	public final boolean getSecurityError() {
		return _securityError;
	}
	public final void setSecurityError(boolean value) {
		_securityError = value;
	}

	public final boolean getGeneralInfo() {
		return _generalInformation;
	}
	public final void setGeneralInfo(boolean value) {
		_generalInformation = value;
	}

	public final boolean getUnHandledException() {
		return _unhandledExceptions;
	}
	public final void setUnHandledException(boolean value) {
		_unhandledExceptions = value;
	}
}
