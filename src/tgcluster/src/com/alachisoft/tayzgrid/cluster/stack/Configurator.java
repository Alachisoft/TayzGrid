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

package com.alachisoft.tayzgrid.cluster.stack;

import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Global;
import java.util.ArrayList;
import java.util.Collections;

// $Id: Configurator.java,v 1.6 2004/08/12 15:43:11 belaban Exp $
/**
 * The task if this class is to setup and configure the protocol stack. A string describing the desired setup, which is both the layering and the configuration of each layer, is
 * given to the configurator which creates and configures the protocol stack and returns a reference to the top layer (Protocol).<p> Future functionality will include the
 * capability to dynamically modify the layering of the protocol stack and the properties of each layer.
 *
 * <author> Bela Ban </author>
 */
public class Configurator
{

    /**
     * The configuration string has a number of entries, separated by a ':' (colon). Each entry consists of the name of the protocol, followed by an optional configuration of that
     * protocol. The configuration is enclosed in parentheses, and contains entries which are name/value pairs connected with an assignment sign (=) and separated by a semicolon.
     * <pre>UDP(in_port=5555;out_port=4445):FRAG(frag_size=1024)</pre><p> The <em>first</em> entry defines the <em>bottommost</em> layer, the string is parsed left to right and the
     * protocol stack constructed bottom up. Example: the string "UDP(in_port=5555):FRAG(frag_size=32000):DEBUG" results is the following stack:
     * <pre>
     *
     * -----------------------
     * | DEBUG                 |
     * |-----------------------|
     * | FRAG frag_size=32000  |
     * |-----------------------|
     * | UDP in_port=32000     |
     * -----------------------
     * </pre>
     */
    public Protocol setupProtocolStack(String configuration, ProtocolStack st) throws Exception
    {
        Protocol protocol_stack = null;
        java.util.List protocol_configs;
        java.util.List protocols;

        protocol_configs = parseConfigurations(configuration);
        protocols = createProtocols(protocol_configs, st);
        if (protocols == null)
        {
            return null;
        }
        protocol_stack = connectProtocols(protocols);
        return protocol_stack;
    }

    public void startProtocolStack(Protocol bottom_prot)
    {
        while (bottom_prot != null)
        {
            bottom_prot.startDownHandler();
            bottom_prot.startUpHandler();
            bottom_prot = bottom_prot.getUpProtocol();
        }
    }

    public void stopProtocolStack(Protocol start_prot)
    {
        while (start_prot != null)
        {
            start_prot.stopInternal();
            start_prot.destroy();
            start_prot = start_prot.getDownProtocol();
        }
    }

    public Protocol findProtocol(Protocol prot_stack, String name)
    {
        String s;
        Protocol curr_prot = prot_stack;

        while (true)
        {
            s = curr_prot.getName();
            if (s == null)
            {
                continue;
            }
            if (s.equals(name))
            {
                return curr_prot;
            }
            curr_prot = curr_prot.getDownProtocol();
            if (curr_prot == null)
            {
                break;
            }
        }
        return null;
    }

    public Protocol getBottommostProtocol(Protocol prot_stack)
    {
        Protocol tmp = null, curr_prot = prot_stack;

        while (true)
        {
            if ((tmp = curr_prot.getDownProtocol()) == null)
            {
                break;
            }
            curr_prot = tmp;
        }
        return curr_prot;
    }

    /**
     * Creates a new protocol given the protocol specification. Initializes the properties and starts the up and down handler threads.
     *
     * @param prot_spec The specification of the protocol. Same convention as for specifying a protocol stack. An exception will be thrown if the class cannot be created. Example:
     * <pre>"VERIFY_SUSPECT(timeout=1500)"</pre> Note that no colons (:) have to be specified
     *
     * @param stack The protocol stack
     *
     * @return Protocol The newly created protocol
     *
     * @exception Exception Will be thrown when the new protocol cannot be created
     *
     */
    public Protocol createProtocol(String prot_spec, ProtocolStack stack) throws Exception
    {
        ProtocolConfiguration config;
        Protocol prot;

        if (prot_spec == null)
        {
            throw new Exception("Configurator.createProtocol(): prot_spec is null");
        }

        // parse the configuration for this protocol
        config = new ProtocolConfiguration(this, prot_spec);

        // create an instance of the protocol class and configure it
        prot = config.createLayer(stack);

        // start the handler threads (unless down_thread or up_thread are set to false)
        prot.startDownHandler();
        prot.startUpHandler();

        return prot;
    }

    /**
     * Inserts an already created (and initialized) protocol into the protocol list. Sets the links to the protocols above and below correctly and adjusts the linked list of
     * protocols accordingly.
     *
     * @param prot The protocol to be inserted. Before insertion, a sanity check will ensure that none of the existing protocols have the same name as the new protocol.
     *
     * @param position Where to place the protocol with respect to the neighbor_prot (ABOVE, BELOW)
     *
     * @param neighbor_prot The name of the neighbor protocol. An exception will be thrown if this name is not found
     *
     * @param stack The protocol stack
     *
     * @exception Exception Will be thrown when the new protocol cannot be created, or inserted.
     *
     */
    public void insertProtocol(Protocol prot, int position, String neighbor_prot, ProtocolStack stack) throws Exception
    {
        if (neighbor_prot == null)
        {
            throw new Exception("Configurator.insertProtocol(): neighbor_prot is null");
        }
        if (position != ProtocolStack.ABOVE && position != ProtocolStack.BELOW)
        {
            throw new Exception("Configurator.insertProtocol(): position has to be ABOVE or BELOW");
        }


        // find the neighbors below and above



        // connect to the protocol layer below and above
    }

    /**
     * Removes a protocol from the stack. Stops the protocol and readjusts the linked lists of protocols.
     *
     * @param prot_name The name of the protocol. Since all protocol names in a stack have to be unique (otherwise the stack won't be created), the name refers to just 1 protocol.
     *
     * @exception Exception Thrown if the protocol cannot be stopped correctly.
     *
     */
    public void removeProtocol(String prot_name)
    {
    }

    /*
     * ------------------------------- Private Methods -------------------------------------
     */
    /**
     * Creates a protocol stack by iterating through the protocol list and connecting adjacent layers. The list starts with the topmost layer and has the bottommost layer at the
     * tail. When all layers are connected the algorithms traverses the list once more to call startInternal() on each layer.
     *
     * @param protocol_list List of Protocol elements (from top to bottom)
     *
     * @return Protocol stack
     *
     */
    private Protocol connectProtocols(java.util.List protocol_list)
    {
        Protocol current_layer = null, next_layer = null;

        for (int i = 0; i < protocol_list.size(); i++)
        {
            current_layer = (Protocol) protocol_list.get(i);
            if (i + 1 >= protocol_list.size())
            {
                break;
            }
            next_layer = (Protocol) protocol_list.get(i + 1);
            current_layer.setUpProtocol(next_layer);
            next_layer.setDownProtocol(current_layer);
        }
        return current_layer;
    }

    /**
     * Get a string of the form "P1(config_str1):P2:P3(config_str3)" and return ProtocolConfigurations for it. That means, parse "P1(config_str1)", "P2" and "P3(config_str3)"
     *
     * @param config_str Configuration string
     *
     * @param delimiter
     * @return Vector of ProtocolConfigurations
     *
     */
    public java.util.List parseComponentStrings(String config_str, String delimiter)
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(10));
        Global.Tokenizer tok;
        String token;

        tok = new Global.Tokenizer(config_str, delimiter);
        while (tok.hasNext())
        {
            token = tok.next();
            retval.add(token);
        }

        return retval;
    }

    /**
     * Return a number of ProtocolConfigurations in a vector
     *
     * @param configuration protocol-stack configuration string
     *
     * @return Vector of ProtocolConfigurations
     *
     */
    public java.util.List parseConfigurations(String configuration) throws Exception
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(10));
        java.util.List component_strings = parseComponentStrings(configuration, ":");
        String component_string;
        ProtocolConfiguration protocol_config;

        if (component_strings == null)
        {
            return null;
        }
        for (int i = 0; i < component_strings.size(); i++)
        {
            component_string = ((String) component_strings.get(i));
            protocol_config = new ProtocolConfiguration(this, component_string);
            retval.add(protocol_config);
        }
        return retval;
    }

    /**
     * Takes vector of ProtocolConfigurations, iterates through it, creates Protocol for each ProtocolConfiguration and returns all Protocols in a vector.
     *
     * @param protocol_configs Vector of ProtocolConfigurations
     *
     * @param stack The protocol stack
     *
     * @return Vector of Protocols
     *
     */
    private java.util.List createProtocols(java.util.List protocol_configs, ProtocolStack stack) throws Exception
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(10));
        ProtocolConfiguration protocol_config;
        Protocol layer;
        stack.setStackType(ProtocolStackType.TCP);
        for (int i = 0; i < protocol_configs.size(); i++)
        {
            protocol_config = (ProtocolConfiguration) protocol_configs.get(i);
            if (protocol_config != null)
            {
                if (protocol_config.getProtocolName().equals("UDP"))
                {
                    stack.setStackType(ProtocolStackType.UDP);
                    break;
                }
            }
        }

        for (int i = 0; i < protocol_configs.size(); i++)
        {
            protocol_config = (ProtocolConfiguration) protocol_configs.get(i);
            layer = protocol_config.createLayer(stack);
            if (layer == null)
            {
                return null;
            }
            retval.add(layer);
        }

        sanityCheck(retval);
        return retval;
    }

    /**
     * Throws an exception if sanity check fails. Possible sanity check is uniqueness of all protocol names.
     * @param protocols
     */
    public void sanityCheck(java.util.List protocols) throws Exception
    {
        java.util.List names = Collections.synchronizedList(new java.util.ArrayList(10));
        Protocol prot;
        String name;
        ProtocolReq req;
        java.util.List req_list = Collections.synchronizedList(new java.util.ArrayList(10));
        int evt_type;

        // Checks for unique names
        for (int i = 0; i < protocols.size(); i++)
        {
            prot = (Protocol) protocols.get(i);
            name = prot.getName();
            for (int j = 0; j < names.size(); j++)
            {
                if (name.equals(names.get(j)))
                {
                    throw new Exception("Configurator.sanityCheck(): protocol name " + name + " has been used more than once; protocol names have to be unique !");
                }
            }
            names.add(name);
        }


        // Checks whether all requirements of all layers are met
        for (int i = 0; i < protocols.size(); i++)
        {
            prot = (Protocol) protocols.get(i);
            req = new ProtocolReq(prot.getName());
            req.up_reqs = prot.requiredUpServices();
            req.down_reqs = prot.requiredDownServices();
            req.up_provides = prot.providedUpServices();
            req.down_provides = prot.providedDownServices();
            req_list.add(req);
        }


        for (int i = 0; i < req_list.size(); i++)
        {
            req = (ProtocolReq) req_list.get(i);

            // check whether layers above this one provide corresponding down services
            if (req.up_reqs != null)
            {
                for (int j = 0; j < req.up_reqs.size(); j++)
                {
                    evt_type = ((Integer) req.up_reqs.get(j)).intValue();

                    if (!providesDownServices(i, req_list, evt_type))
                    {
                        throw new Exception("Configurator.sanityCheck(): event " + Event.type2String(evt_type) + " is required by " + req.name
                                + ", but not provided by any of the layers above");
                    }
                }
            }

            // check whether layers below this one provide corresponding up services
            if (req.down_reqs != null)
            {
                // check whether layers above this one provide up_reqs
                for (int j = 0; j < req.down_reqs.size(); j++)
                {
                    evt_type = ((Integer) req.down_reqs.get(j)).intValue();

                    if (!providesUpServices(i, req_list, evt_type))
                    {
                        throw new Exception("Configurator.sanityCheck(): event " + Event.type2String(evt_type) + " is required by " + req.name
                                + ", but not provided by any of the layers below");
                    }
                }
            }
        }
    }

    /**
     * Check whether any of the protocols 'below' end_index provide evt_type
     * @param end_index
     * @param req_list
     * @param evt_type
     * @return
     */
    public boolean providesUpServices(int end_index, java.util.List req_list, int evt_type)
    {
        ProtocolReq req;

        for (int i = 0; i < end_index; i++)
        {
            req = (ProtocolReq) req_list.get(i);
            if (req.providesUpService(evt_type))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether any of the protocols 'above' start_index provide evt_type
     */
    public boolean providesDownServices(int start_index, java.util.List req_list, int evt_type)
    {
        ProtocolReq req;

        for (int i = start_index; i < req_list.size(); i++)
        {
            req = (ProtocolReq) req_list.get(i);
            if (req.providesDownService(evt_type))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * --------------------------- End of Private Methods ----------------------------------
     */
    private static class ProtocolReq
    {

        public java.util.List up_reqs = null;
        public java.util.List down_reqs = null;
        public java.util.List up_provides = null;
        public java.util.List down_provides = null;
        public String name = null;

        public ProtocolReq(String name)
        {
            this.name = name;
        }

        public boolean providesUpService(int evt_type)
        {
            int type;

            if (up_provides != null)
            {
                for (int i = 0; i < up_provides.size(); i++)
                {
                    type = ((Integer) up_provides.get(i)).intValue();
                    if (type == evt_type)
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean providesDownService(int evt_type)
        {
            int type;

            if (down_provides != null)
            {
                for (int i = 0; i < down_provides.size(); i++)
                {
                    type = ((Integer) down_provides.get(i)).intValue();
                    if (type == evt_type)
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String toString()
        {
            StringBuilder ret = new StringBuilder();
            ret.append('\n' + name + ':');
            if (up_reqs != null)
            {
                ret.append("\nRequires from above: ").append(printUpReqs());
            }

            if (down_reqs != null)
            {
                ret.append("\nRequires from below: ").append(printDownReqs());
            }

            if (up_provides != null)
            {
                ret.append("\nProvides to above: ").append(printUpProvides());
            }

            if (down_provides != null)
            {
                ret.append("\nProvides to below: ").append(printDownProvides());
            }
            return ret.toString();
        }

        public String printUpReqs()
        {
            StringBuilder ret = new StringBuilder("[");
            if (up_reqs != null)
            {
                for (int i = 0; i < up_reqs.size(); i++)
                {
                    ret.append(Event.type2String(((Integer) up_reqs.get(i)).intValue())).append(' ');
                }
            }
            return ret.toString() + ']';
        }

        public String printDownReqs()
        {
            StringBuilder ret = new StringBuilder("[");
            if (down_reqs != null)
            {
                for (int i = 0; i < down_reqs.size(); i++)
                {
                    ret.append(Event.type2String(((Integer) down_reqs.get(i)).intValue())).append(' ');
                }
            }
            return ret.toString() + ']';
        }

        public String printUpProvides()
        {
            StringBuilder ret = new StringBuilder("[");
            if (up_provides != null)
            {
                for (int i = 0; i < up_provides.size(); i++)
                {
                    ret.append(Event.type2String(((Integer) up_provides.get(i)).intValue())).append(' ');
                }
            }
            return ret.toString() + ']';
        }

        public String printDownProvides()
        {
            StringBuilder ret = new StringBuilder("[");
            if (down_provides != null)
            {
                for (int i = 0; i < down_provides.size(); i++)
                {
                    ret.append(Event.type2String(((Integer) down_provides.get(i)).intValue())).append(' ');
                }
            }
            return ret.toString() + ']';
        }
    }

    /**
     * Parses and encapsulates the specification for 1 protocol of the protocol stack, e.g.
     * <code>UNICAST(timeout=5000)</code>
     */
    public static class ProtocolConfiguration
    {

        private void InitBlock(Configurator enclosingInstance)
        {
            this.enclosingInstance = enclosingInstance;
        }
        private Configurator enclosingInstance;

        public String getProtocolName()
        {
            return protocol_name;
        }

        public java.util.HashMap getProperties()
        {
            return properties;
        }

        public void setContents(String value) throws Exception
        {
            int index = value.indexOf((char) '('); // e.g. "UDP(in_port=3333)"
            int end_index = value.lastIndexOf((char) ')');

            if (index == - 1)
            {
                protocol_name = value;
            }
            else
            {
                if (end_index == - 1)
                {
                    throw new Exception("Configurator.ProtocolConfiguration.setContents(): closing ')' " + "not found in " + value + ": properties cannot be set !");
                }
                else
                {
                    properties_str = value.substring(index + 1, index + 1 + (end_index) - (index + 1));
                    protocol_name = value.substring(0, (index) - (0));
                }
            }

            /*
             * "in_port=5555;out_port=6666"
             */
            if (properties_str != null)
            {
                java.util.List components = getEnclosing_Instance().parseComponentStrings(properties_str, ";");
                if (components.size() > 0)
                {
                    for (int i = 0; i < components.size(); i++)
                    {
                        String name, valu, comp = (String) components.get(i);
                        index = comp.indexOf((char) '=');
                        if (index == - 1)
                        {
                            throw new Exception("Configurator.ProtocolConfiguration.setContents(): " + "'=' not found in " + comp);
                        }
                        name = comp.substring(0, (index) - (0));
                        valu = comp.substring(index + 1, index + 1 + (comp.length()) - (index + 1));
                        properties.put((String) name, (String) valu);
                    }
                }
            }
        }

        public final Configurator getEnclosing_Instance()
        {
            return enclosingInstance;
        }

        public final ProtocolStackType getStackType()
        {
            return stackType;
        }

        public final void setStackType(ProtocolStackType value)
        {
            stackType = value;
        }
        private String protocol_name = null;
        private String properties_str = null;
        private java.util.HashMap properties = new java.util.HashMap();
        private ProtocolStackType stackType = ProtocolStackType.values()[0];

        /**
         * Creates a new ProtocolConfiguration.
         *
         * @param config_str The configuration specification for the protocol, e.g.
         * <pre>VERIFY_SUSPECT(timeout=1500)</pre>
         *
         */
        public ProtocolConfiguration(Configurator enclosingInstance, String config_str) throws Exception
        {
            InitBlock(enclosingInstance);
            setContents(config_str);
        }

        public Protocol createLayer(ProtocolStack prot_stack) throws Exception
        {
            if (protocol_name == null)
            {
                return null;
            }

            

            Protocol protocol = null;
 
            if (protocol_name.equals("TCP"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.TCP();
            }
 
            else if (protocol_name.equals("TCPPING"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.TCPPING();
            }
 
            else if (protocol_name.equals("QUEUE"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.QUEUE();
            }
 
            else if (protocol_name.equals("TOTAL"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.TOTAL();
            }
 
            else if (protocol_name.equals("VIEW_ENFORCER"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.VIEW_ENFORCER();
            }
 
            else if (protocol_name.equals("pbcast.GMS"))
            {
                protocol = new com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS();
            }

 
            if (protocol != null)
            {
               
                prot_stack.getCacheLog().Info("Configurator.createLayer()", "Created Layer " + protocol.getClass().getName());
                protocol.setStack(prot_stack);
                if (properties != null)
                {
                    if (!protocol.setPropertiesInternal(properties))
                    {
                        return null;
                    }
                }
                protocol.init();
            }
            else
            {
                prot_stack.getCacheLog().Error("Configurator.createLayer()", "Couldn't create layer: " + protocol_name);
            }

            return protocol;
        }

        @Override
        public String toString()
        {
            StringBuilder retval = new StringBuilder();
            retval.append("Protocol: ");
            if (protocol_name == null)
            {
                retval.append("<unknown>");
            }
            else
            {
                retval.append(protocol_name);
            }
            if (properties != null)
            {
                retval.append("(" + Global.CollectionToString(properties) + ')');
            }
            return retval.toString();
        }
    }
}
