package org.vafer.jmx2snmp.snmp;

import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import javax.management.JMException;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.vafer.jmx2snmp.jmx.JmxAttribute;
import org.vafer.jmx2snmp.jmx.JmxIndex;
import org.vafer.jmx2snmp.jmx.JmxMib;
import org.vafer.jmx2snmp.jmx.JmxMib.Bean;
import org.vafer.jmx2snmp.jmx.JmxServer;
import org.weakref.jmx.MBeanExporter;

/**
 * The SnmpBridge starts a SNMP agent and provides access to the MBean objects. It looks up the JMX attribute path from the JmxMib mapping and looks up the JmxAttribute from the
 * JmxIndex.
 *
 * Calling report() on startup will log mapping incosistencies to System.err
 */
public final class SnmpBridge implements CommandResponder
{

    private final InetAddress address;
    private final int port;
    private final JmxIndex jmxIndex;
    private final JmxMib jmxMib;
    private Snmp snmp;

    public SnmpBridge(InetAddress pAddress, int pPort, JmxIndex pJmxIndex, JmxMib pJmxMib)
    {
        address = pAddress;
        port = pPort;
        jmxIndex = pJmxIndex;
        jmxMib = pJmxMib;
    }

    public void updateJmxIndex(String expression) throws Exception
    {
        jmxIndex.dynamicUpdate(expression);
    }

    public void updateJmxMib(String nodeName, boolean isServer)
    {
        jmxMib.dynamicUpdate(nodeName, isServer);
    }

    /**
     *
     */
    public void report()
    {

        final Map<String, Bean> mibMapping = jmxMib.getMapping();

        final Set<String> attributesInMib = new HashSet<String>();
        final Set<String> attributesInIndex = new HashSet<String>(jmxIndex.getAttributePaths());
        for (Map.Entry<String, Bean> entry : mibMapping.entrySet())
        {
            String oid = entry.getKey();
            Bean bean = entry.getValue();
            if (attributesInIndex.contains(bean.absolutePath))
            {
                if (attributesInMib.contains(bean.absolutePath))
                {
                    System.err.println("jmx2snmp: attribute mapping for [" + bean.absolutePath + "] found more than once");
                }
                attributesInMib.add(bean.absolutePath);
            }
            else
            {
                if (bean.leaf)
                {
                    System.err.println("jmx2snmp: attribute [" + bean.absolutePath + "] no longer exists at OID [" + oid + "]");
                }
            }
        }

        attributesInIndex.removeAll(attributesInMib);

        for (String attribute : attributesInIndex)
        {
            System.err.println("jmx2snmp: attribute not mapped yet: " + attribute);
        }

    }

    public void processPdu(CommandResponderEvent pRequest)
    {

        final PDU requestPdu = pRequest.getPDU();

        if (requestPdu == null)
        {
            return;
        }

        try
        {

            final PDU responsePdu = new PDU(requestPdu);
            responsePdu.setType(PDU.RESPONSE);

            if (requestPdu.getType() == PDU.GET)
            {

                for (VariableBinding binding : responsePdu.toArray())
                {
                    final OID oid = binding.getOid();
                    
                    
                    
                    OID oidClone = (OID) oid.clone();
                    oidClone.removeLast();
                    final String path = jmxMib.getPathFromOid(oidClone.toString());
                    //System.out.println("path is ==== >" + path);
//                    System.out.println(path);
                    if (path == null)
                    {
                        binding.setVariable(Null.noSuchObject);
                        continue;
                    }

                    final JmxAttribute attribute = jmxIndex.getAttributeAtPath(path);

                    if (attribute == null)
                    {
//                        binding.setVariable(Null.noSuchObject);
                        binding.setVariable(new OctetString(String.valueOf("N/A")));
                        continue;
                    }
                    //System.out.println("attribute name ==== >" + attribute.getName());

                    final Variable variable = getVariableFromJmxAttribute(attribute);
                    if (variable == null)
                    {
                        binding.setVariable(new OctetString(String.valueOf("N/A")));
                    }
                    else
                    {
                        binding.setVariable(variable);
                    }
                }

            }
            else if (requestPdu.getType() == PDU.GETNEXT)
            {
               
                for (VariableBinding binding : responsePdu.toArray())
                {
                    final OID oid = binding.getOid();
                    
                    if(oid.size() == 11)
                        oid.removeLast();
                    
                    final String next = jmxMib.getNextOidFromOid(oid.toString());

                    if (next == null)
                    {
                        //binding.setVariable(Null.noSuchObject);
                        binding.setVariable(new OctetString(String.valueOf("N/A")));
                       
                        continue;
                    }
                    
                    final OID nextOid = new OID(next);

                    binding.setOid(nextOid);

                    final String path = jmxMib.getPathFromOid(nextOid.toString());
                      
                    if (path == null)
                    {
                        binding.setVariable(Null.noSuchObject);
                        //binding.setVariable(new OctetString(String.valueOf("N/A")));
                        continue;                        
                    }
                     
                    
                    final JmxAttribute attribute = jmxIndex.getAttributeAtPath(path);

                    if (attribute == null)
                    {
                        binding.setVariable(Null.noSuchObject);
                        //binding.setVariable(new OctetString(String.valueOf("N/A")));
                        continue;
                    }

                    final Variable variable = getVariableFromJmxAttribute(attribute);

                    if (variable != null)
                    {
                        binding.setVariable(variable);
                    }
                    
                    
                }

            }
            else
            {
            }

            pRequest.getStateReference().setTransportMapping(pRequest.getTransportMapping());
            pRequest.getMessageDispatcher().returnResponsePdu(
                    pRequest.getMessageProcessingModel(),
                    pRequest.getSecurityModel(),
                    pRequest.getSecurityName(),
                    pRequest.getSecurityLevel(),
                    responsePdu,
                    pRequest.getMaxSizeResponsePDU(),
                    pRequest.getStateReference(),
                    new StatusInformation());

        }
        catch (Exception e)
        {
            System.out.println("Error occur in SNMP service:"+ e.getMessage());
            //e.printStackTrace();
        }
    }

    public void start() throws Exception
    {
        snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress(address, port)));
        snmp.addCommandResponder(this);
        snmp.listen();
    }

    public void stop() throws Exception
    {
        snmp.close();
        snmp = null;
    }

    private Variable getVariableFromJmxAttribute(JmxAttribute pAttribute) throws JMException
    {
        Object value = null;
        try
        {
            value = pAttribute.getValue();
//            System.out.println(value);
        }
        catch(Exception iNE)
        {
            return null;
        }

        if (value == null)
        {
            return new Null();
        }

        final String type = pAttribute.getType();
        if ("double".equals(type))
        {
            final Number n = (Number) value;
            return new Integer32(n.intValue());
        }
        if ("int".equals(type))
        {
            final Number n = (Number) value;
            return new Integer32(n.intValue());
        }
        else if ("long".equals(type))
        {
            final Number n = (Number) value;
            return new Counter64(n.longValue());
        }
        else if ("boolean".equals(type))
        {
            final Boolean b = (Boolean) value;
            return new Integer32(b ? 1 : 0);
        }
        else if ("java.lang.String".equals(type))
        {
            return new OctetString(String.valueOf(value));
        }
        else
        {
            return new OctetString("Unsupported Type: " + pAttribute.getType());
        }


    }

    public static void main(String[] args) throws Exception
    {

        System.out.println("starting...");


        final MBeanExporter exporter = new MBeanExporter(ManagementFactory.getPlatformMBeanServer());
        ncacheDiff ob1 = new TestBeanImplExt();
        ob1.setName("changed");
        //TestBeanImpl testImp = new TestBeanImpl();
        exporter.export("bean - localCache:name=ncache.cache", ob1);

        exporter.export("bean:name=ncache.server", new TestBeanImplExt());

        final JmxServer jmxServer = new JmxServer(InetAddress.getByName("127.0.0.1"));
        jmxServer.start();

        final URL url = SnmpBridge.class.getResource("/org/vafer/jmx2snmp/snmp/mapping.properties");

        final JmxMib jmxMib = new JmxMib();
        jmxMib.load(new FileReader(url.getFile()));

        final JmxIndex jmxIndex = new JmxIndex("bean - localCache:name=ncache.cache");

        final SnmpBridge snmpBridge = new SnmpBridge(InetAddress.getByName("127.0.0.1"), 1617, jmxIndex, jmxMib);
        snmpBridge.start();
        System.out.println("enter 'quit' to stop...");
        final Scanner sc = new Scanner(System.in);
        while (!sc.nextLine().equals("quit"))
        {
        };



        snmpBridge.stop();
        exporter.unexportAllAndReportMissing();
        jmxServer.stop();
    }
}
