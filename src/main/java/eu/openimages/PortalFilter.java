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

    protected boolean up = false;

    @Override
    public void notify(SystemEvent se) {
        if (se instanceof org.mmbase.module.tools.ApplicationsInstalledEvent) {
            up = true;
            LOG.service("Applications are installed, we can not decorate the request");
        }
    }
    @Override
    public int getWeight() {
        return 0;
    }

    /**
     * Initializes filter
     */
    @Override
    public void init(javax.servlet.FilterConfig config) throws ServletException {
        LOG.info("Starting PortalFilter with " + config);
        ctx = config.getServletContext();
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

        if (! up) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Still waiting for MMBase (not initialized)");
            }
            chain.doFilter(request, response);
            return;
        }

        if (request instanceof HttpServletRequest) {

            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            decorateRequest(req, res);
            chain.doFilter(request, response);

        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request not an instance of HttpServletRequest.");
            }
            chain.doFilter(request, response);
        }

    }


    public static boolean decorateRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String serverName = req.getServerName();

        Map<String, Object> attributes = CACHE.get(serverName);

        if (attributes == null) {
            attributes = new HashMap<String, Object>();
            String scheme = req.getScheme();

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(serverName);

            final Cloud cloud = getCloud(req);
            NodeList nl = cloud.getNodeManager("pools").getList(null, null, null);
            NodeIterator ni = nl.nodeIterator();

            Node portal = null;
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
                    LOG.service("Assuming portal '" + serverName +  "' is default.");
                    portal = cloud.getNode("pool_oip");
                } else {
                    LOG.warn("There is no default pool with alias 'pool_oip'");
                }
            }

            attributes.put("portal", portal == null ? null : new org.mmbase.bridge.util.NodeMap(portal));
            attributes.put("isdefaultportal", portal != null && portal.getAliases().contains("pool_oip"));
            CACHE.put(serverName, attributes);
        }
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            req.setAttribute(e.getKey(), e.getValue());
        }

        return true;

    }


    private static Cloud getCloud(HttpServletRequest req) {
        return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
    }
    @Override
    public void destroy() {
        // empty
    }


}
