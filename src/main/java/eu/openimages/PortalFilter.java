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
import java.util.regex.Pattern;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mmbase.bridge.util.CloudThreadLocal;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.servlet.*;
import org.mmbase.module.core.*;
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
public class PortalFilter implements Filter, MMBaseStarter {

    private static Logger log = Logging.getLoggerInstance(PortalFilter.class);
    
    /*
     * serverName -> pools node
     */
    private static final Map<String, Node> portals = new HashMap<String, Node>();
    
    /*
     * The context this servlet lives in
     */
    protected ServletContext ctx = null;
    
    /*
     * MMBase needs to be started first to be able to load config
     */
    private MMBase mmbase;
	private Thread initThread;

    /*
     * Methods that need to be overriden form MMBaseStarter
     */
    public MMBase getMMBase() {
        return mmbase;
    }

    public void setMMBase(MMBase mm) {
        mmbase = mm;
        // logging is not completely initialized, replace logger instance too
        log = Logging.getLoggerInstance(PortalFilter.class);
    }

    public void setInitException(ServletException se) {
        // never mind, simply ignore
    }
    
    /**
     * Initializes filter
     */
    public void init(javax.servlet.FilterConfig config) throws ServletException {
        log.info("Starting PortalFilter with " + config);
	    ctx = config.getServletContext();

		/* initialize MMBase if its not started yet */
        if (! MMBaseContext.isInitialized()) {
            MMBaseContext.init(ctx);
            MMBaseContext.initHtmlRoot();
        }

        initThread = new MMBaseStartThread(this);
        initThread.start();

        log.info("PortalFilter initialized");
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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (mmbase == null) {
            if (log.isDebugEnabled()) {
                log.debug("Still waiting for MMBase (not initialized)");
            }
            chain.doFilter(request, response);
            return;
        }
        
        if (request instanceof HttpServletRequest) {
            
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            String serverName = req.getServerName();
            
            if (portals.containsKey(serverName)) {
                request.setAttribute("portal", portals.get(serverName));
            
            } else {
                if (decorateRequest(req, res)) {
                    if (log.isDebugEnabled()) {
                        log.debug("portal: " + request.getAttribute("portal"));
                    }
                } else {
                    request.setAttribute("portal", null);
                }
            }
            
            chain.doFilter(request, response);

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Request not an instance of HttpServletRequest.");
            }
            chain.doFilter(request, response);
        }
     
    }


    public static boolean decorateRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String serverName = req.getServerName();
        String scheme = req.getScheme();
        
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(serverName);
        
        final Cloud cloud = getCloud(req);
        NodeList nl = cloud.getNodeManager("pools").getList(null, null, null);
        NodeIterator ni = nl.nodeIterator();
        
        boolean found = false;
        while (ni.hasNext()) {
            Node pool = ni.nextNode();
            try {
                Query query = Queries.createRelatedNodesQuery(pool, cloud.getNodeManager("urls"), "portalrel", "destination");
                
                Constraint constraint = Queries.createConstraint(query, "urls.url", FieldValueConstraint.LIKE, sb.toString() + "%");
                query.setConstraint(constraint);
                
                NodeList urls = cloud.getList(query);
                if (log.isDebugEnabled()) { 
                    log.debug("query: " + query);
                }
                if (urls.size() > 0) {
                    if (log.isDebugEnabled()) { 
                        log.debug("Found: " + urls.get(0));
                    }
                    req.setAttribute("portal", pool);
                    //res.setHeader("X-OpenImages-Portal", pool.getStringValue("name"));
                    
                    // cache
                    log.service("Adding portal '" + serverName + "'.");
                    portals.put(serverName, pool);
                    found = true;
                    
                    return true;
                }
                
            } catch (Exception ex) {
                log.error("Exception while building query: " + ex);
                return false;
            }
        }
        
        if (!found) {
            log.service("Assuming portal '" + serverName +  "' is default.");
            portals.put(serverName, null);
        }
        
        return false;
    }
    

    private static Cloud getCloud(HttpServletRequest req) {
        HttpSession session = req.getSession(false); // false: do not create a session, only use it
        if (session == null) {
            return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
        } else {
            if (log.isDebugEnabled()) { 
                log.debug("from session");
            }
            Object c = session.getAttribute("cloud_mmbase");
            if (c != null) {
                if (c instanceof Cloud) {
                    Cloud cloud = (Cloud) c;
                    if (cloud.getUser() != null && cloud.getUser().isValid()) {
                        return cloud;
                    } else {
                        log.warn("" + c + " is not a valid Cloud");
                        return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
                    }
                } else {
                    log.warn("" + c + " is not a Cloud, but a " + c.getClass());
                    return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
                }
            } else {
                return ContextProvider.getDefaultCloudContext().getCloud("mmbase");
            }
        }
    }

    public void destroy() {
        // empty
    }


}
