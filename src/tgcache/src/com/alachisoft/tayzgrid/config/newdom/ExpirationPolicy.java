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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.cache.expiry.Duration;



public class ExpirationPolicy implements Cloneable, InternalCompactSerializable{
    private String policyType = "none";
    private long duration;
    private String unit = "seconds";
    /**
     * @return the policyType
     */
    @ConfigurationAttributeAnnotation(value="default-policy",appendText = "")
    public String getPolicyType() {
        return policyType;
    }

    /**
     * @param policyType the policyType to set
     */
    @ConfigurationAttributeAnnotation(value="default-policy",appendText = "")
    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    /**
     * @return the defaultDuration
     */
    @ConfigurationAttributeAnnotation(value="duration",appendText = "")
    public long getDuration() {
        return duration;
    }

    /**
     * @param defaultDuration the defaultDuration to set
     */
    @ConfigurationAttributeAnnotation(value="duration",appendText = "")
    public void setDuration(long defaultDuration) {
        this.duration = defaultDuration;
    }

    /**
     * @return the unit
     */
    //@ConfigurationAttributeAnnotation(value="unit",appendText = "")
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    //@ConfigurationAttributeAnnotation(value="unit",appendText = "")
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        policyType =(String) Common.readAs(reader.ReadObject(), String.class);
        duration = reader.ReadInt64();
        unit = (String) Common.readAs(reader.ReadObject(),String.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(policyType);
        writer.Write(duration);
        writer.WriteObject(unit);
    }
    
    @Override
    public Object clone()
    {        
        ExpirationPolicy policy = new ExpirationPolicy();
        policy.setDuration(getDuration());
        policy.setPolicyType(getPolicyType());
        policy.setUnit(getUnit());
        return policy;
    }
    
}
