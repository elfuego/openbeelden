/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2010 Netherlands Institute for Sound and Vision

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

import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.core.event.*;
import org.mmbase.storage.search.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Portal stuff likely needed during most requests. Looks for portal (pools) node that is related
 * with an urls node that is similar to server name, puts pools node on request with parameter 'portal'.
 * Default portal is a pools node with alias 'pool_oip'.
 *
 * @author Andre van Toly
 * @since OIP-1.1
 * @version $Id$
 */
public class PortalFilter implements Filter, SystemEventListener {

    private static final Logger LOG = Logging.getLoggerInstance(PortalFilter.class);

    /*
     * serverName -> pools node
     */
    private static final Map<String, Map<String, Object>> CACHE = new ConcurrentHashMap<String, Map<String, Object>>();

    /*
     * The context this servlet lives in
     */
    protected ServletContext ctx = null;
    private Pattern excludePattern = null;

    protected boolean appUp = false;
    protected boolean luceneUp = false;
    protected boolean up = false;

    public void notify(SystemEvent se) {
        if (se instanceof org.mmbase.module.tools.ApplicationsInstalledEvent) {
            appUp = true;
            up = appUp && luceneUp;
        }
        if (se instanceof org.mmbase.module.lucene.Lucene.ConfigurationRead) {
            luceneUp = true;
            up = appUp && luceneUp;
        }
    }

    public int getWeight() {
        return 0;
    }

    /**
     * Initializes filter
     */
    public void init(javax.servlet.FilterConfig config) throws ServletException {
        LOG.info("Starting PortalFilter with " + config);
        ctx = config.getServletContext();
        String excludes = config.getInitParameter("excludes");
        if (excludes != null && excludes.length() > 0) {
            excludePattern = Pattern.compile(excludes);
        }

        EventManager.getInstance().addEventListener(this);
    }



    /**
     * Filters a request and seeks pools node for portal
     * Waits for MMBase to be up.
     *
     * @param request   incoming request
     * @param response  outgoing response
     * @param chain     a chain object, provided for by the servlet container
     * @throws ServletException thrown when an exception occurs
     * @throws IOException thrown when an exception occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String servlet = req.getServletPath();
        if (excludePattern != null && excludePattern.matcher(servlet).find()) {
            chain.doFilter(request, response);
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("" + servlet + " does not match " + excludePattern);
        }

        if (! up && ! "/version.jspx".equals(servlet)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Still waiting for MMBase (not initialized)");
            }
            if (! res.isCommitted()) {
                res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MMBase not yet, or not successfully initialized (check mmbase log)");
            }
            return;
        }


        decorateRequest(req, res, up);
        chain.doFilter(request, response);

    }


    protected boolean decorateRequest(HttpServletRequest req, HttpServletResponse res, boolean cache)  {

        String serverName = req.getServerName();
        if (LOG.isDebugEnabled()) {
            LOG.debug("serverName " + serverName);
        }

        Map<String, Object> attributes = CACHE.get(serverName);

        if (attributes == null) {
            attributes = new HashMap<String, Object>();
            String scheme = req.getScheme();

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(serverName);
            Node portal = null;

            if (cache) {
                final Cloud cloud = getCloud(req);
                NodeList nl = cloud.getNodeManager("pools").getList(null, null, null);
                NodeIterator ni = nl.nodeIterator();


                while (ni.hasNext()) {
                    Node pool = ni.nextNode();
                    try {
                        Query query = Queries.createRelatedNodesQuery(pool, cloud.getNodeManager("urls"), "portalrel", "destination");

                        Constraint constraint = Queries.createConstraint(query, "urls.url", FieldValueConstraint.LIKE, sb.toString() + "%");
                        query.setConstraint(constraint);

                        NodeList urls = cloud.getList(query);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("query: " + query);
                        }
                        if (urls.size() > 0) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found: " + urls.get(0));
                            }
                            portal = pool;
                            break;
                        }

                    } catch (Exception ex) {
                        LOG.error("Exception while building query: " + ex);
                        return false;
                    }
                }
                if (portal == null) {
                    if (cloud.hasNode("pool_oip")) {
                        LOG.service("serverName '" + serverName + "' has no portal, handling as default.");
                        portal = cloud.getNode("pool_oip");
                    } else {
                        LOG.warn("There is no default pool with alias 'pool_oip'");
                    }
                }
            }

            attributes.put("portal", portal == null ? null : new org.mmbase.bridge.util.NodeMap(portal));
            attributes.put("isdefaultportal", portal != null && portal.getAliases().contains("pool_oip"));
            if (cache) {
                CACHE.put(serverName, attributes);
            }
        }
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            req.setAttribute(e.getKey(), e.getValue());
        }

        return true;

    }

    /**
     * Default portal url when that is defined, e.g. via portalrel related urls node.
     * Default portal is a pools node with alias 'pool_oip'.
     *
     * @param cloud MMBase cloud
     * @return link defined for default portal with alias 'pool_oip'
     */
    public static String getPortalUrl(Cloud cloud) {
        String url = null;
        if (cloud.hasNode("pool_oip")) {

            Node portal = cloud.getNode("pool_oip");
            Node portalurlNode = SearchUtil.findRelatedNode(portal, "urls", "portalrel");
            if (portalurlNode != null) {
                url = portalurlNode.getStringValue("url");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("portal url " + url);
                }
            }

        } else {
            LOG.warn("There is no default pool with alias 'pool_oip'");
        }

        return url;
    }


    private static Cloud getCloud(HttpServletRequest req) {
        return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
    }
    @Override
    public void destroy() {
        // empty
    }


}
