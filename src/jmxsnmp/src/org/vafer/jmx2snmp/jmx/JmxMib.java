package org.vafer.jmx2snmp.jmx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The JmxMib represents a walk-able SNMP OID tree. The tree is loaded and constructed from a load(Reader). The expect format include a definition for the root node as "<oid>" and
 * all other nodes.
 *
 * 1.3.6.1.4.1.27305 = <oid> 1.3.6.1.4.1.27305.12 = bean1 1.3.6.1.4.1.27305.12.1 = AnotherInt 1.3.6.1.4.1.27305.12.2 = SomeColor 1.3.6.1.4.1.27305.12.3 = TheBoolean
 *
 * @threadsafe yes
 */
public final class JmxMib
{

    final private AtomicReference<Node> root = new AtomicReference<Node>(new Node(null, 0, null));
    private HashMap completeMapping = new HashMap();
    private HashMap serverMapping = new HashMap();
    private HashMap cacheMapping = new HashMap();
    private static int cacheId = 0;
    private static int serverId = 0;
    private String[][] oidParents =
    {
        {
            "",
            "",
            "",
            "",
            "",
            ""
        },
        {
            "<oid>",
            "ncache",
            "server",
            "cache",
            "serverName",
            "cacheName"
        },
    };

    public JmxMib()
    {
    }

    public static class Bean
    {

        public boolean leaf;
        public String relativePath;
        public String absolutePath;
    };

    private static class Node
    {

        public final Node parent;
        public final int idx;
        public String value;
        public final SortedMap<Integer, Node> childs = new TreeMap<Integer, Node>();

        public Node(Node parent, int idx, String value)
        {
            this.parent = parent;
            this.idx = idx;
            this.value = value;
        }

        public String getOid()
        {
            if (parent == null)
            {
                return null;
            }

            final String parentOid = parent.getOid();

            if (parentOid == null)
            {
                return "" + idx;
            }

            return parentOid + '.' + idx;
        }

        public String getPath()
        {
            if (parent == null)
            {
                return null;
            }

            final String parentPath = parent.getPath();

            if (parentPath == null)
            {
                return value;
            }

            return parentPath + '.' + value;
        }

        public Node getNext()
        {
            Node leaf = this;
            while (leaf.childs.size() > 0)
            {
                leaf = leaf.childs.get(leaf.childs.firstKey());
            }

            if (leaf != this)
            {
                return leaf;
            }

            Node n = leaf;
            while (n != null)
            {
                final Node p = n.parent;

                if (p == null)
                {
                    return null;
                }

                final Node sibling = p.childAfter(n.idx);

                if (sibling != null)
                {
                    if (sibling.childs.size() > 0)
                    {
                        return sibling.getNext();
                    }
                    return sibling;
                }

                n = p;
            }

            return null;
        }

        public Node childAfter(int index)
        {

            Iterator<Integer> iterator = childs.keySet().iterator();
            while (iterator.hasNext())
            {
                Integer key = iterator.next();
                if (key > index)
                {
                    return childs.get(key);
                }
            }

            return null;
        }

        public int getNodeCount()
        {
            int count = 1;
            for (Node node : childs.values())
            {
                count += node.getNodeCount();
            }
            return count;
        }
    }

    private Node createNode(Node root, String oid)
    {
        final String[] indexes = oid.split("\\.");
        Node node = root;
        for (int i = 0; i < indexes.length; i++)
        {

            final Integer idx = Integer.parseInt(indexes[i]);

            Node n = node.childs.get(idx);
            if (n == null)
            {
                n = new Node(node, idx, null);
                node.childs.put(idx, n);
            }

            node = n;
        }

        return node;
    }

    private Node lookupNode(Node root, String oid)
    {
        final String[] indexes = oid.split("\\.");
        Node node = root;
        for (int i = 0; i < indexes.length; i++)
        {

            final Integer idx = Integer.parseInt(indexes[i]);

            Node n = node.childs.get(idx);
            if (n == null)
            {
                return null;
            }

            node = n;
        }

        return node;
    }

    public synchronized void dynamicUpdate(String nodeName, boolean isServer)
    {
        final Node newRoot = root.get();
        HashMap tempMap;
        Iterator iT;
        String key, modifiedKey;
        int nodeId;

        if (isServer)
        {
            key = oidParents[0][4];
            tempMap = serverMapping;
            nodeId = JmxMib.serverId++;
        }
        else
        {
            key = oidParents[0][5];
            tempMap = cacheMapping;
            nodeId = JmxMib.cacheId++;
        }

        modifiedKey = key.substring(0, key.length() - 1).concat("" + nodeId);
        tempMap.put(key, nodeName);

        iT = tempMap.entrySet().iterator();

        for (; iT.hasNext();)
        {
            Map.Entry current = (Map.Entry) iT.next();
            final Node node = createNode(newRoot, ((String) current.getKey()).replace(key, modifiedKey));
            node.value = (String) current.getValue();
        }

        root.set(newRoot);
    }

    /**
     *
     * Loads Object Ids from a string array
     * @param Oids
     */
    public synchronized void load(String[] Oids)
    {
        final Node newRoot = new Node(null, 0, null);
        for (String oid : Oids)
        {
            final String[] tokens = oid.split("=");
            final String key = tokens[0].trim();
            final String value = tokens[1].trim();
            final Node node = createNode(newRoot, key);
            node.value = value;
        }
        root.set(newRoot);
    }


    /**
     *
     * Load Object Ids from a oid file
     *
     * Usage: load(new FileReader("Path to file"));
     * 
     * @param pConfigReader
     * @throws IOException
     */
    public synchronized void load(Reader pConfigReader) throws IOException
    {
        final BufferedReader br = new BufferedReader(pConfigReader);

        final Node newRoot = new Node(null, 0, null);

        while (true)
        {
            final String line = br.readLine();

            if (line == null)
            {
                break;
            }

            final String[] tokens = line.split("=");
            if (tokens.length == 1 || tokens[0].trim().charAt(0) == '#')
            {
                continue;
            }
            final String key = tokens[0].trim();
            final String value = tokens[1].trim();
            final Node node = createNode(newRoot, key);
            node.value = value;
        }
        br.close();
        root.set(newRoot);
    }

    public synchronized void dynamicLoad(Reader pConfigReader) throws IOException, Exception
    {
        int loadedOidParents = 0;
        final BufferedReader br = new BufferedReader(pConfigReader);

        final Node newRoot = new Node(null, 0, null);

        while (true)
        {
            final String line = br.readLine();

            if (line == null)
            {
                break;
            }

            final String[] tokens = line.split("=");
            if (tokens.length == 1 || tokens[0].trim().charAt(0) == '#')
            {
                continue;
            }
            final String key = tokens[0].trim();
            final String value = tokens[1].trim();
            //We are only storing parent-oids for the moment, the rest is loaded at runtime in dynamicUpdate()
            if (loadedOidParents < 6)
            {
                if (!value.equalsIgnoreCase(oidParents[1][loadedOidParents]))
                {
                    throw new Exception("Object ids must be in order <oid>.ncache.server/cache.serverName/CacheName");
                }
                oidParents[0][loadedOidParents++] = key;
                final Node node = createNode(newRoot, key);
                node.value = value;
            }
            //server counters
            if (key.startsWith(oidParents[0][4]) && !oidParents[0][4].isEmpty())
            {
                serverMapping.put(key, value);
            }
            //cache counters
            else if (key.startsWith(oidParents[0][5]) && !oidParents[0][5].isEmpty())
            {
                cacheMapping.put(key, value);
            }
            completeMapping.put(key, value);
        }

        br.close();
        root.set(newRoot);
    }

    public String getPathFromOid(String oid)
    {
        final Node node = lookupNode(root.get(), oid);

        if (node == null)
        {
            return null;
        }

        return node.getPath();
    }

    public String getNextOidFromOid(String oid)
    {
        final Node node = lookupNode(root.get(), oid);

        if (node == null)
        {
            return null;
        }

        final Node nextNode = node.getNext();

        if (nextNode == null)
        {
            return null;
        }

        return nextNode.getOid();
    }

    public int getNodeCount()
    {
        return root.get().getNodeCount() - 1;
    }

    private void fillMapping(Node node, TreeMap<String, Bean> map)
    {

        if (node.value != null)
        {
            Bean bean = new Bean();
            bean.leaf = node.childs.size() == 0;
            bean.relativePath = node.value;
            bean.absolutePath = node.getPath();
            map.put(node.getOid(), bean);
        }

        for (Node child : node.childs.values())
        {
            fillMapping(child, map);
        }
    }

    public TreeMap<String, Bean> getMapping()
    {

        final TreeMap<String, Bean> map = new TreeMap<String, Bean>();

        final Node node = root.get();

        fillMapping(node, map);

        return map;
    }
}
