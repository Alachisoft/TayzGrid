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

package com.alachisoft.tayzgrid.web;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.log4j.Logger;

public class ProxyResponse extends HttpServletResponseWrapper
{
    private String tayzgridSessionId = null;
    private Logger logger = Logger.getLogger(ProxyRequest.class);

    public ProxyResponse(HttpServletResponse response)
    {
        super(response);
    }
    @Override
    public String encodeURL(String url)
    {
        String encUrl = super.encodeURL(url);
        if (!encUrl.equals(url) && tayzgridSessionId != null && tayzgridSessionId.length() > 0)
        {
            encUrl += ";" + ProxyRequest.TAYZGRID_SESSION_ID + "=" + tayzgridSessionId;
        }
        return encUrl;
    }
    @Override
    public String encodeRedirectURL(String url)
    {
        String encUrl = super.encodeRedirectURL(url);
        if (!encUrl.equals(url) && tayzgridSessionId != null && tayzgridSessionId.length() > 0)
        {
            encUrl += ";" + ProxyRequest.TAYZGRID_SESSION_ID + "=" + tayzgridSessionId;
        }
        return encUrl;
    }
    public void setRemoteSessionId(String id)
    {
        logger.debug("Remote session id is: " + id);
        this.tayzgridSessionId = id;
    }
}
