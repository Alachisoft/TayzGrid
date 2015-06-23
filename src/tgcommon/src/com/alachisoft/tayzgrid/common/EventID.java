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

public class EventID
{

    public static final int CacheStart = 1000;
    public static final int CacheStop = 1001;
    public static final int CacheStartError = 1002;
    public static final int CacheStopError = 1003;
    public static final int ClientConnected = 1004;
    public static final int ClientDisconnected = 1005;
    public static final int NodeJoined = 1006;
    public static final int NodeLeft = 1007;
    public static final int StateTransferStart = 1008;
    public static final int StateTransferStop = 1009;
    public static final int StateTransferError = 1010;
    public static final int UnhandledException = 1011;
    public static final int LoginFailure = 1012;
    public static final int ServiceStartFailure = 1013;
    public static final int LoggingEnabled = 1014;
    public static final int LoggingDisabled = 1015;
    public static final int CacheSizeWarning = 1016;
    public static final int GeneralError = 1017;
    public static final int GeneralInformation = 1018;
    public static final int ConfigurationError = 1019;
    public static final int LicensingError = 1020;
    public static final int SecurityError = 1021;
    public static final int BadClientFound = 1022;

    public static String EventText(int eventID)
    {
        String text = "";
        switch (eventID)
        {
            case EventID.CacheStart:
                text = "Cache Start";
                break;
            case EventID.CacheStop:
                text = "Cache Stop";
                break;
            case EventID.CacheStartError:
                text = "Cache Start Error";
                break;
            case EventID.CacheStopError:
                text = "Cache Stop Error";
                break;
            case EventID.ClientConnected:
                text = "Client Connected";
                break;
            case EventID.ClientDisconnected:
                text = "Client Diconnected";
                break;
            case EventID.NodeJoined:
                text = "Node Joined";
                break;
            case EventID.NodeLeft:
                text = "Node Left";
                break;
            case EventID.StateTransferStart:
                text = "State Transfer Start";
                break;
            case EventID.StateTransferStop:
                text = "State Transfer Stop";
                break;
            case EventID.StateTransferError:
                text = "State Transfer Error";
                break;
            case EventID.UnhandledException:
                text = "Unhandled Exception";
                break;
            case EventID.LoginFailure:
                text = "Login Failure";
                break;
            case EventID.ServiceStartFailure:
                text = "Service Start Failure";
                break;
            case EventID.CacheSizeWarning:
                text = "Cache Size Warning";
                break;
            case EventID.GeneralError:
                text = "General Error";
                break;
            case EventID.GeneralInformation:
                text = "General Information";
                break;
            case EventID.ConfigurationError:
                text = "Configuration Error";
                break;
            case EventID.LicensingError:
                text = "Licensing Error";
                break;
            case EventID.SecurityError:
                text = "Security Error";
                break;
            case EventID.BadClientFound:
                text = "Bad Client Found";
                break;
            default:
                text = "Unknown";
                break;
        }

        return text;
    }
}
