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

package com.alachisoft.tayzgrid.common;



public class ServiceConfiguration {
	private static String _port = "9800";
	private static String _sendBufferSize = "131072";
	private static String _receiveBufferSize = "131072";
	private static String _licenseLogging = "false";
	private static String _bindToClusterIP;
	private static String _bindToClientServerIP;
	private static String _enableDualSocket = "false";
	private static String _enableNaggling = "false";
	private static String _nagglingSize = "500";
	private static String _autoStartCaches;
	private static String _enableDebuggingCounters = "false";
	private static String _expirationBulkRemoveSize = "10";
	private static String _expirationBulkRemoveDelay = "0";
	private static String _evictionBulkRemoveSize = "10";
	private static String _evictionBulkRemoveDelay = "0";
	private static String _bulkItemsToReplicated = "300";

	static {
		Load();
	}

	public static void Load() {
		try {
                    } catch (RuntimeException ex) {
		}
	}

	public static String getPort() {
		return _port;
	}
	public static void setPort(String value) {
		_port = value;
	}

	public static String getSendBufferSize() {
		return _sendBufferSize;
	}
	public static void setSendBufferSize(String value) {
		_sendBufferSize = value;
	}

	public static String getReceiveBufferSize() {
		return _receiveBufferSize;
	}
	public static void setReceiveBufferSize(String value) {
		setReceiveBufferSize(value);
	}

	public static String getLicenseLogging() {
		return _licenseLogging;
	}
	public static void setLicenseLogging(String value) {
		_licenseLogging = value;
	}

	public static String getBindToClusterIP() {
		return _bindToClusterIP;
	}
	public static void setBindToClusterIP(String value) {
		_bindToClusterIP = value;
	}

	public static String getBindToClientServerIP() {
		return _bindToClientServerIP;
	}
	public static void setBindToClientServerIP(String value) {
		_bindToClientServerIP = value;
	}

	public static String getEnableDualSocket() {
		return _enableDualSocket;
	}
	public static void setEnableDualSocket(String value) {
		_enableDualSocket = value;
	}

	public static String getEnableNaggling() {
		return _enableNaggling;
	}
	public static void setEnableNaggling(String value) {
		_enableNaggling = value;
	}

	public static String getNagglingSize() {
		return _nagglingSize;
	}
	public static void setNagglingSize(String value) {
		_nagglingSize = value;
	}

	public static String getAutoStartCaches() {
		return _autoStartCaches;
	}
	public static void setAutoStartCaches(String value) {
		_autoStartCaches = value;
	}

	public static String getEnableDebuggingCounters() {
		return _enableDebuggingCounters;
	}
	public static void setEnableDebuggingCounters(String value) {
		_enableDebuggingCounters = value;
	}

	public static String getExpirationBulkRemoveSize() {
		return _expirationBulkRemoveSize;
	}
	public static void setExpirationBulkRemoveSize(String value) {
		_expirationBulkRemoveSize = value;
	}

	public static String getExpirationBulkRemoveDelay() {
		return _expirationBulkRemoveDelay;
	}
	public static void setExpirationBulkRemoveDelay(String value) {
		_expirationBulkRemoveDelay = value;
	}

	public static String getEvictionBulkRemoveSize() {
		return _evictionBulkRemoveSize;
	}
	public static void setEvictionBulkRemoveSize(String value) {
		_evictionBulkRemoveSize = value;
	}

	public static String getEvictionBulkRemoveDelay() {
		return _evictionBulkRemoveDelay;
	}
	public static void setEvictionBulkRemoveDelay(String value) {
		_evictionBulkRemoveDelay = value;
	}

	public static String getBulkItemsToReplicated() {
		return _bulkItemsToReplicated;
	}
	public static void setBulkItemsToReplicated(String value) {
		_bulkItemsToReplicated = value;
	}
}
