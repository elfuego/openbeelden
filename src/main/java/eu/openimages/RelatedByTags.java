package eu.openimages;

import java.util.*;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeIterator;
import org.mmbase.bridge.Query;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.NotFoundException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Finds nodes that share the same tags as a given node and returns them sorted by 
 * number of hits. The map with hits is supposed to be sorted in descending order: nodes
 * with most hits first. The map with nodes is not sorted.
 * 
 * @author  André van Toly
 * @version $Id$
 */
public class RelatedByTags {

    private static final Logger log = Logging.getLoggerInstance(RelatedByTags.class);
    
    // nodes:hits
    public Map<Integer, Integer> getNodes(Node node, String type, String max) {
        NodeList l = relatedTags(node, -1);
        return relatedContent(node, l, type, max);
    }
    public Map<Integer, Integer> getNodes(Node node, String type) {
        return getNodes(node, type, null);
    }
    public Map<Integer, Integer> getNodes(Node node) {
        return getNodes(node, null, null);
    }
    
    // hits:nodes
    public Map<Integer, Integer> getHits(Node node, String type, String max) {
        Map<Integer, Integer> nodesMap = getNodes(node, type, null);
        if (max != null) {
            int imax = 0;
            try {
                imax = Integer.parseInt(max);
            } catch (NumberFormatException nfe) {
                log.error("Could not parse max value '" + max + "': " + nfe);
            }
            return hitsMap(nodesMap, imax);
        } else {
            return hitsMap(nodesMap, -1);
        }
    }
    public Map<Integer, Integer> getHits(Node node, String type) {
        Map<Integer, Integer> nodesMap = getNodes(node, type, null);
        return hitsMap(nodesMap, -1);
    }
    public Map<Integer, Integer> getHits(Node node) {
        Map<Integer, Integer> nodesMap = getNodes(node, null, null);
        return hitsMap(nodesMap, -1);
    }
    
    /**
     * Finds the tags related to a node with a maximum number of related tags.
     *
     * @param node  Content node to return related nodes of
     * @param max   Maximum nr of tags to return, defaults to 99
     * @return tags or null when no NodeList could be made with the specified parameters
     */
    public static NodeList relatedTags(Node node, int max) {
        //return node.getRelatedNodes(cloud.getNodeManager("tags"), "related", "destination");
        if (max == -1) max = 99;
        Cloud cloud = node.getCloud();
        NodeList tags = cloud.createNodeList();
        try {
            Query query = Queries.createRelatedNodesQuery(node, cloud.getNodeManager("tags"), "related", "destination");
            query.setMaxNumber(max);
            
            NodeList nl = cloud.getList(query);
            NodeIterator ni = nl.nodeIterator();
            while (ni.hasNext()) {
                Node n = ni.next(); //clusternode
                Node tag = cloud.getNode(n.getIntValue("tags.number"));
                if (log.isDebugEnabled()) log.debug("Found node: " + tag);
                tags.add(tag);
            }

        } catch (Exception e) {
            log.error("Exception while building query: " + e);
        }

        return tags;
    }
    
    /**
     * Finds the nodes that share the same tags in the specified nodelist. For every tag shared
     * a hit is registered as its value. 
     *
     * @param node  Original content node that should be excluded from the results
     * @param tags  List with tags
     * @param type  Nodetype to look for, defaults to type of the source node
     * @param max   Maxiumum number of nodes to return
     * @return content nodes
     */
    private static Map<Integer,Integer> relatedContent(Node node, NodeList tags, String type, String max) {
        Map<Integer,Integer> map = new HashMap<Integer,Integer>();
        NodeManager targetNodeManager = null;
        Cloud cloud = node.getCloud();
        int origNr = node.getNumber();

        if (type == null || "".equals(type)) {
            type = node.getNodeManager().getName();
            targetNodeManager = node.getNodeManager();
        } else {
            try {
                targetNodeManager = cloud.getNodeManager(type);
            } catch (NotFoundException nfe) {
                log.error("Target nodemanager '"+ type +"' was not found, falling back to source type: " + nfe);
                type = node.getNodeManager().getName();
                targetNodeManager = node.getNodeManager();
            }
        }

        int imax = 0;
        if (max != null) {
            try {
                imax = Integer.parseInt(max);
            } catch (NumberFormatException nfe) {
                log.error("Could not parse max value '" + max + "': " + nfe);
            }
        }        
        
        for (Iterator<Node> it = tags.iterator(); it.hasNext();) {
            Node tagNode = it.next();
            
            try {
                Query query = Queries.createRelatedNodesQuery(tagNode, targetNodeManager, "related", "source");
                if (imax > 0) query.setMaxNumber(imax);
                
                NodeList nl = cloud.getList(query);
                NodeIterator ni = nl.nodeIterator();
                while (ni.hasNext()) {
                    Node n = ni.next(); //clusternode
                    int nr = cloud.getNode(n.getIntValue(type + ".number")).getNumber();
                    if (nr == origNr) continue; // skip the original node
                    if (log.isDebugEnabled()) log.debug("Found node: " + nr);
                    
                    int hits = 1;
                    if (map.containsKey(nr)) {
                        hits = map.get(nr);
                        hits++;
                    }
                    map.put(nr, hits);
                }
                
            } catch (Exception e) {
                log.error("Exception while building query: " + e);
            }
            
        }
        
        return map;
    }
    
    /**
     * The number of hits sorted in (reverse) order: most hits first. The hits are the keys
     * so they are a bit arbitrary sorted by multiplying them with 1000 and adding their 
     * occurence order.
     *
     * @param  map Nodes that share one or more of the same tags
     * @return The number of hits of a node multiplied by 1000 plus 1, 2 or .. to make 
     * them unique to enable the use of hits as keys
     */
    private static SortedMap<Integer, Integer> hitsMap(Map<Integer, Integer> map, int max) {
        SortedMap<Integer,Integer> checkMap = new TreeMap<Integer,Integer>();
        SortedMap<Integer,Integer> hitsMap = new TreeMap<Integer,Integer>(
            new Comparator<Integer>() { 
                // Comparator makes sure keys are in descending order (highest first)
                public int compare(Integer first, Integer last) { return last - first; } 
            }
        );
        SortedMap<Integer,Integer> maxhitsMap = new TreeMap<Integer,Integer>(
            new Comparator<Integer>() { 
                public int compare(Integer first, Integer last) { return last - first; } 
            }
        );
        
        // swap the key/value and save to a checkMap to check how many nodes have same nr of hits
        Set<Integer> keySet = map.keySet();
        Iterator<Integer> i = keySet.iterator();
        while (i.hasNext()) {
            Integer nodenr = i.next();         // node number
            Integer hits = map.get(nodenr);    // hits for key (node)
            int timeshit = 0;               // nr of items that has this nr of hits
            
            if (checkMap.containsKey(hits)) {
                timeshit = checkMap.get(hits);
            }
            timeshit++;                     // nr of items that has this number of hits
            checkMap.put(hits, timeshit);
            
            int key = (hits * 1000) + timeshit; // 1001, 1002, 2001, 2002, 2003 etc.
            if (log.isDebugEnabled()) log.debug("adding to hitsMap " + key + " : " + nodenr);
            hitsMap.put(key, nodenr);
        }
        
        // now iterate over hitsMap to return nodes with most hits first within the max
        if (max > 0) {
            for (int j = 0; j < max; j++) {
                if (!hitsMap.isEmpty()) {
                    Integer key = hitsMap.firstKey();
                    Integer value = hitsMap.get(key);
                    if (log.isDebugEnabled()) log.debug("adding to maxhitsMap " + key + " : " + value);
                    maxhitsMap.put(key, value);
                    hitsMap.remove(key);
                }
            }
            return maxhitsMap;
        } else {
            return hitsMap;
        }
    }
    
}
