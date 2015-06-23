package org.vafer.jmx2snmp.snmp;

import org.weakref.jmx.Managed;



public abstract class TestBeanImpl implements ncacheDiff  {

    private String name = "testbean";
    @Managed
    @Override
    public String getcachecount() {
        return this.name;
    }

    @Managed
    public void setName(String name){
        this.name = name;
    }




}
