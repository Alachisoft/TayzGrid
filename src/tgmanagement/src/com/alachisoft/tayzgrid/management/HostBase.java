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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.common.IDisposable;


public class HostBase implements IDisposable
{

    protected static String _appName = "TayzGrid";

    protected String _url;

    /**
     * Overloaded constructor.
     *
     * @param application
     */
    static
    {
        try
        {
        }
        catch (Exception e)
        {
        }
    }

    public HostBase()
    {
    }

    /**
     * Returns the application name of this session.
     */
    public static String getApplicationName()
    {
        return _appName;
    }

    protected int GetHttpPort()
    {
        return 0;
    }

    protected int GetTcpPort()
    {
        return 0;
    }


    /**
     * Set things in motion so your service can do its work.
     */
    public final void StartHosting(String tcpChannel, String httpChannel) throws Exception
    {
        StartHosting(tcpChannel, GetTcpPort(), httpChannel, GetHttpPort());
    }

    /**
     * Set things in motion so your service can do its work.
     */
    public final void StartHosting(String tcpChannel, String httpChannel, String ip) throws Exception
    {
        StartHosting(tcpChannel, GetTcpPort(), httpChannel, GetHttpPort(), ip);
    }

    public final void StartHosting(String tcpChannel, String ip, int port)
    {
        StartHosting(tcpChannel, port, ip);
    }

    public final void StartHosting(String tcpChannel, int tcpPort, String ip)
    {
    }

    /**
     * Set things in motion so your service can do its work.
     */
    public final void StartHosting(String tcpChannel, int tcpPort, String httpChannel, int httpPort, String ip) throws Exception
    {
    }

    /**
     * Set things in motion so your service can do its work.
     */
    public final void StartHosting(String tcpChannel, int tcpPort, String httpChannel, int httpPort) throws Exception
    {
        try
        {
            String monitorUri = _url + "/Monitor";
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    /**
     * Set things in motion so your service can do its work.
     */
    private void StartHosting(String tcpChannel, int tcpPort, String httpChannel, int httpPort, int sendBuffer, int receiveBuffer) throws Exception
    {
        try
        {

        }
        catch (Exception e)
        {
            throw e;
        }
    }
    /**
     * Stop this service.
     */
    public final void StopHosting() throws Exception
    {
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     *
     * @param disposing
     *
     *
     */
    private void dispose(boolean disposing)
    {
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public final void dispose()
    {
        dispose(true);
    }
}
