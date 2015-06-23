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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.communication.RemoteServer;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class CacheServerInfo implements java.lang.Comparable{
    
    private RemoteServer _serverInfo;
    
    public final RemoteServer getServerInfo()
    {
            return _serverInfo;
    }
    
    public final void setServerInfo(RemoteServer value)
    {
            _serverInfo = value;
    }
    
    public CacheServerInfo(String name,int port )
    {
      _serverInfo=new RemoteServer(name, port);
    }
    
    public CacheServerInfo()
    {
        _serverInfo=new RemoteServer();
        
    }
    
    public final int getPort()
    {
            return _serverInfo.getPort();
    }
    public final void setPort(int value)
    {
            _serverInfo.setPort(value);
    }
    
    public final boolean getIsUserProvided()
    {
            return _serverInfo.isUserProvided();
    }
    public final void setIsUserProvided(boolean value)
    {
            _serverInfo.setUserProvided(value);
    }
    
     public final String getName()
     {
             return _serverInfo.getName();
     }
     public final void setName(String value) throws UnknownHostException
     {
             if (value.equals(""))
             {
                     return;
             }
             if (value != null && value.toLowerCase().equals("localhost"))
             {
                     _serverInfo.setName(InetAddress.getLocalHost().getHostName());
             }
             else
             {
                     _serverInfo.setName(value);
             }
     }
 
    @Override
    public boolean equals(Object obj)
    {
          return _serverInfo.equals(obj);
    }

    @Override
    public String toString()
    {
            return _serverInfo.toString();
    }


 
//IComparable Member Function 

    @Override
    public int compareTo(Object o) {
      return 0; 
    }
          
    
    
}
