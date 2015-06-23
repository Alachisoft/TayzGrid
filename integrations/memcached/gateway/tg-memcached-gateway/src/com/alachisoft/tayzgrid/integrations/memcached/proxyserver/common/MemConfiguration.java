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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common;

public final class MemConfiguration {
    
    private static String _cacheName = "";
    private static String _textProtocolIP = "";
    private static int _textProtocolPort = 0;
    private static String _binaryProtocolIP = "";
    private static int _binaryProtocolPort = 0;
    private static int _maxCommandLength = 1024 * 1024;
    
    static {
        loadConfigurations();
    }
    
    public static void loadConfigurations() {
        if (System.getProperty("CacheName") != null) {
            _cacheName = System.getProperty("CacheName");
        }
        
        if (System.getProperty("TextProtocolIP") != null) {
            _textProtocolIP = System.getProperty("TextProtocolIP");
        }
        
        if (System.getProperty("TextProtocolPort") != null) {
            try {
                _textProtocolPort = Integer.parseInt(System.getProperty("TextProtocolPort"));
            } catch (RuntimeException e) {
                LogManager.getLogger().Error("MemConfiguration", " Failed to parse port for text protocol. Using default value " + _textProtocolPort);
            }
        }
        
        if (System.getProperty("BinaryProtocolIP") != null) {
            _binaryProtocolIP = System.getProperty("BinaryProtocolIP");
        }
        
        if (System.getProperty("BinaryProtocolPort") != null) {
            try {
                _binaryProtocolPort = Integer.parseInt(System.getProperty("BinaryProtocolPort"));
            } catch (RuntimeException e) {
                LogManager.getLogger().Error("MemConfiguration", " Failed to parse port for binary protocol. Using default value " + _binaryProtocolPort);
            }
        }
        if (System.getProperty("MaxCommandLength") != null) {
            try {
                _maxCommandLength = 1024 * Integer.parseInt(System.getProperty("MaxCommandLength"));
            } catch (RuntimeException e4) {
                LogManager.getLogger().Error("MemConfiguration", " Failed to parse maximum command length. Using default value " + _maxCommandLength / 1024 + " kb.");
            }
        }
    }
    
    public static String getCacheName() {
        return _cacheName;
    }
    
    public static int getTextProtocolPort() {
        return _textProtocolPort;
    }
    
    public static int getBinaryProtocolPort() {
        return _binaryProtocolPort;
    }
    
    public static int getMaximumCommandLength() {
        return _maxCommandLength;
    }
    
    public static String getTextProtocolIP() {
        return _textProtocolIP;
    }
    
    public static String getBinaryProtocolIP() {
        return _binaryProtocolIP;
    }
}