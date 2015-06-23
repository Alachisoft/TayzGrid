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

package com.alachisoft.tayzgrid.common.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/** 
 Summary description for DnsCache.
*/
public class DnsCache {
	/**  map for forward lookups 
	*/
	private static java.util.HashMap _fwdMap;
	/**  map for reverse lookups 
	*/
	private static java.util.HashMap _bckMap;

	static {
		_fwdMap = new HashMap();
		_bckMap = new HashMap();
	}

	/** 
	 Does a DNS lookup on the hostname. updates the reverse cache to optimize reverse lookups.
	 
	 @param hostname
	 @return 
	*/
	public static InetAddress ResolveName(String hostname) throws UnknownHostException {
		hostname = hostname.toLowerCase();
		if (!_fwdMap.containsKey(hostname)) {
			InetAddress[] ie = InetAddress.getAllByName(hostname);
                        
			if (ie != null && ie.length > 0) {
				
					_fwdMap.put(hostname, ie[0]);
					_bckMap.put(ie[0], hostname);
				
			}
		}

		return (InetAddress)((_fwdMap.get(hostname) instanceof InetAddress) ? _fwdMap.get(hostname) : null);
	}


	/** 
	 Does a reverse DNS lookup on the address. updates the forward cache to optimize lookups.
	 
	 @param address
	 @return 
	*/
	public static String ResolveAddress(String address) throws UnknownHostException {
		try {
			return InetAddress.getByName(address).getHostAddress();
		} catch (RuntimeException ex) {
		}
		return null;
	}


	/** 
	 Does a reverse DNS lookup on the address. updates the forward cache to optimize lookups.
	 
	 @param address
	 @return 
	*/
	public static String ResolveAddress(InetAddress address) throws UnknownHostException {
		if (!_bckMap.containsKey(address)) {
			InetAddress ie = InetAddress.getByAddress(address.getAddress());
			
				String hostname = ie.getHostName();
				hostname = hostname.replace("is~","");
				if (hostname.indexOf('.') > 0) {
					hostname = hostname.substring(0, hostname.indexOf('.'));
				}

				
					_bckMap.put(address, hostname);
					_fwdMap.put(ie.getHostName(), address);
				
			
		}

		return (String)((_bckMap.get(address) instanceof String) ? _bckMap.get(address) : null);
	}


	/** 
	 Clears the caches 
	*/
	public static void FlushCache() {
		
			_fwdMap.clear();
			_bckMap.clear();
		
	}
}
