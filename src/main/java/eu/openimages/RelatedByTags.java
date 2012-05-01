/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2009 Netherlands Institute for Sound and Vision

The Open Images Platform is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Open Images Platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with The Open Images Platform.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.openimages;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.CloudThreadLocal;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.storage.search.Constraint;
import org.mmbase.storage.search.FieldValueConstraint;
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

    /* only nodes belonging to portal are returned */
    public Map<Integer,Integer> getHitsByPortal(Node node, Node portal, String type, String max) {
        Map<Integer, Integer> nodesMap = getNodes(node, type, null);
        Map<Integer, Integer> filteredMap = filterNodes(nodesMap, portal);
        if (max != null) {
            int imax = 0;
            try {
                imax = Integer.parseInt(max);
            } catch (NumberFormatException nfe) {
                log.error("Could not parse max value '" + max + "': " + nfe);
            }
            return hitsMap(filteredMap, imax);
        } else {
            return hitsMap(filteredMap, -1);
        }

    }

    /**
     * Most used tags, sorted by most popular if needed,
     * not included tags with no relations (unused tags).
     *
     * @param type  Optional nodemanager to use
     * @param max   Maximum number of tags to return, defaults to 99
     * @param sort  Sort tags: up or down
     * @return tags with count attached (node/count)
     */
    public static Map<Integer, Integer> getTagsByCount(String type, String max, String sort) {
        if (type == null || "".equals(type)) {
            type = "object";
        }
        if (!"up".equals(sort) && !"down".equals(sort)) {
            sort = "none";
        }
        int imax = 99;
        if (max != null) {
            try {
                imax = Integer.parseInt(max);
            } catch (NumberFormatException nfe) {
                log.error("Could not parse max value '" + max + "': " + nfe);
            }
        }

        Cloud cloud = CloudThreadLocal.currentCloud();
        Map<Integer,Integer> map = new HashMap<Integer,Integer>();
        NodeManager sourceNodeManager = cloud.getNodeManager(type);
        int c = 0;
        Iterator<Node> ni =  SearchUtil.findNodeList(cloud, "tags").iterator();
        while (ni.hasNext() && c < imax) {
            Node node = ni.next();
            //int count = 0;
            try {
                Query query = Queries.createRelatedNodesQuery(node, sourceNodeManager, "related", "source");

                if (sourceNodeManager.hasField("show")) {
                    Constraint extraConstraint = Queries.createConstraint(query, type + ".show", FieldValueConstraint.EQUAL, 1);
                    query.setConstraint(extraConstraint);
                }

                int count = Queries.count(query);
                if (count > 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("tag #" + node.getStringValue("name") + " : size " + count);
                    }
                    map.put(node.getNumber(), count);
                    c++;
                }

            } catch (Exception e) {
                log.error("Exception while building query: " + e);
            }

        }

        if ("none".equals(sort)) {
            return map;

        } else {
            Collection<Integer> keyMap = map.keySet();
            List<Integer>       valMap = new ArrayList<Integer>(map.values());

            Collections.sort(valMap);
            if ("up".equals(sort)) {
                Collections.reverse(valMap);
            }

            LinkedHashMap<Integer,Integer> sortedMap = new LinkedHashMap<Integer,Integer>();
            for (Integer val : valMap) {
                for (Integer key : keyMap) {
                    Integer comp1 = map.get(key);
                    if (comp1 == val) {
                        map.remove(key);
                        keyMap.remove(key);
                        sortedMap.put(key, val);
                        break;
                    }
                }
            }

            return sortedMap;
        }
    }


    /**
     * Finds tags related to a node with a maximum number of related tags.
     *
     * @param node  Content node to return related nodes of
     * @param max   Maximum nr of tags to return, defaults to 99
     * @return tags or null when no NodeList could be made with the specified parameters
     */
    private static NodeList relatedTags(Node node, int max) {
        //return node.getRelatedNodes(cloud.getNodeManager("tags"), "related", "destination");
        if (max == -1) max = 99;
        try {
            NodeManager tagsManager = node.getCloud().getNodeManager("tags");
            NodeQuery query = Queries.createRelatedNodesQuery(node, tagsManager, "related", "destination");
            query.setMaxNumber(max);
            return tagsManager.getList(query);
        } catch (Exception e) {
            log.error("Exception while building query: " + e);
            return node.getCloud().createNodeList();
        }

    }

    /**
     * Find other nodes with the same tags, sorted by most hits.
     *
     * @param node  Original content node that should be excluded from the results
     * @param tags  List with tags that are shared among them
     * @param type  Nodetype to look for, defaults to type of the source node
     * @param max   Maxiumum number of nodes to return
     * @return content nodes
     */
    private static NodeList nodesWithSameTags(Node node, NodeList tags, String type, String max) {
        Map<Integer,Integer> map = relatedContent(node, tags, type, max);

        Cloud cloud = node.getCloud();
        NodeList nl = cloud.createNodeList();
        for (Integer key : map.keySet()) {
            nl.add(cloud.getNode(key));
        }
        return nl;
    }

    /*
     * Filter map with nodes and remove the ones that do not belong to portal.
     * A portal consists of the nodes uploaded by the related portal manager, plus
     * the ones that have a tag, keyword or username in the related filters node.
     *
     * @param map       nodes to filter
     * @param portal    portal (pools) node
     */
    private static Map<Integer, Integer> filterNodes(Map<Integer, Integer> map, Node portal) {
        //Node portal = cloud.getNode("pool_oip");
        final Cloud cloud = portal.getCloud();
        final Map<Integer,Integer> filteredMap = new HashMap<Integer, Integer>();
        final Node filterNode = SearchUtil.findRelatedNode(portal, "filters", "portalrel");
        final Node ownerNode  = SearchUtil.findRelatedNode(portal, "mmbaseusers", "portalrel");

        Collection<String> tgs = Collections.emptyList();
        Iterable<String> kws   = Collections.emptyList();
        Collection<String> urs = new ArrayList<String>();
        if (filterNode != null) {
            String tags = filterNode.getStringValue("tags");
            String keywords = filterNode.getStringValue("keywords");
            String users = filterNode.getStringValue("users");

            kws = Arrays.asList(keywords.split(";"));
            tgs = Arrays.asList(tags.split(";"));
            urs.addAll(Arrays.asList(users.split(";")));
        }
        if (ownerNode != null) {
            urs.add(ownerNode.getStringValue("username"));
        }

        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            boolean hit = false;
            Integer nodenr = entry.getKey();
            Node n = cloud.getNode(nodenr);
            Integer hits = entry.getValue();    // hits for key (node)

            // TODO: check excluded media from portal (related with role excluded to portal, still unused though)

            // users
            String owner = n.getStringValue("owner");
            if (cloud.hasNode(owner)) {
                String n_userName = cloud.getNode(owner).getStringValue("username");
                log.debug("username " + n_userName + " : " + urs);
                if (urs.contains(n_userName)) {
                    filteredMap.put(nodenr, hits);
                    log.debug("added u #" + nodenr + " [" + hits + "]");
                    continue;
                }
            }

            // keywords
            String keywords = n.getStringValue("keywords");
            String[] n_kws = keywords.split(";");
            for (String it : kws){
                log.debug("it: " + it);
                for (String itt : n_kws) {
                    log.debug("itt: " + itt);
                    if (it.equals(itt)) {
                        log.debug("hitt: " + itt);
                        hit = true;
                        break;
                    }
                }
                if (hit) break;
            }
            if (hit) {
                filteredMap.put(nodenr, hits);
                log.debug("added kw #" + nodenr + " [" + hits + "]");
                continue;
            }

            // tags
            for (Node relatedTag : relatedTags(n, 99)) {
                String tagName = relatedTag.getStringValue("name");
                log.debug("tagname " + tagName + " : " + tgs);
                if (tgs.contains(tagName)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                filteredMap.put(nodenr, hits);
                log.debug("added t #" + nodenr + " [" + hits + "]");
            }

        }

        return filteredMap;
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
        Map<Integer, Integer> map = new HashMap<Integer,Integer>();
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

        for (Node tagNode : tags) {
            try {
                NodeQuery query = Queries.createRelatedNodesQuery(tagNode, targetNodeManager, "related", "source");

                if (targetNodeManager.hasField("show")) {
                    Constraint extraConstraint = Queries.createConstraint(query, type + ".show", FieldValueConstraint.EQUAL, 1);
                    query.setConstraint(extraConstraint);
                }
                if (imax > 0) query.setMaxNumber(imax);

                for (Node n : targetNodeManager.getList(query)) {
                    int nr = n.getNumber();
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
     * @param  max Maximum to return
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
        for(Map.Entry<Integer, Integer> entry : map.entrySet()) {
            Integer nodenr = entry.getKey();         // node number
            Integer hits   = entry.getValue();    // hits for key (node)
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
