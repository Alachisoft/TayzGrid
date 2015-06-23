/*
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

package com.alachisoft.tayzgrid.cluster.blocks;

import com.alachisoft.tayzgrid.common.net.IRentableObject;
import com.alachisoft.tayzgrid.common.net.ObjectProvider;

public class RequestCorrelatorHeaderProvider extends ObjectProvider
{

    public RequestCorrelatorHeaderProvider()
    {
    }

    public RequestCorrelatorHeaderProvider(int initialsize)
    {
        super(initialsize);
    }

    @Override
    protected IRentableObject CreateObject()
    {
        return new RequestCorrelatorHDR();
    }

    @Override
    public String getName()
    {
        return "RequestCorrelatorHeaderProvider";
    }

    @Override
    protected void ResetObject(Object obj)
    {
        RequestCorrelatorHDR hdr = (RequestCorrelatorHDR) ((obj instanceof RequestCorrelatorHDR) ? obj : null);
        if (hdr != null)
        {
            hdr.Reset();
        }
    }

    @Override
    public java.lang.Class getObjectType()
    {
        if (_objectType == null)
        {
            _objectType = RequestCorrelatorHDR.class;
        }
        return _objectType;
    }
}
