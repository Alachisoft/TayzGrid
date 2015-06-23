/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vafer.jmx2snmp.snmp;

import org.weakref.jmx.Managed;

/**
 *
 * @author 
 */
interface ncacheDiff
{

    @Managed
    String getcachecount();

    void setName(String name);
}
