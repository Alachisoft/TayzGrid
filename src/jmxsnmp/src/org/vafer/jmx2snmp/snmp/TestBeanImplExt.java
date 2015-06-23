/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vafer.jmx2snmp.snmp;

/**
 *
 * @author  
 */
public class TestBeanImplExt extends TestBeanImpl implements ncachediffExt
{
    private String getpersec = "";
    @Override
    public String getgetpersec()
    {
        return this.getpersec;
    }
}
