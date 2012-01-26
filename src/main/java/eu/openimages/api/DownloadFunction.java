/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2011 Netherlands Institute for Sound and Vision

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

package eu.openimages.api;

import java.io.File;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.servlet.FileServlet;
import org.mmbase.streams.CreateSourcesWithoutProcessFunction;
import org.mmbase.util.functions.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import eu.openimages.PortalFilter;

/**
 * Nodefunction on mmbaseusers to generate an API token for a specific user,
 * uses {@link eu.openimages.api.ApiToken}.
 *
 * @author Andr&eacute; van Toly
 * @version $Id: DownloadFunction.java 46039 2011-11-22 09:44:50Z andre $
 */
public final class DownloadFunction extends NodeFunction<String> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(DownloadFunction.class);

    private static final Parameter<String> URL = new Parameter<String>("url", String.class);
    private static final Parameter<String> EMAIL = new Parameter<String>("email", String.class);
    private static final Parameter<Integer> TIMEOUT = new Parameter<Integer>("timeout", Integer.class);
    /* paramemeters reflect those of org.mmbase.streams.download.DownloadFunction */
    private final static Parameter[] PARAMETERS = { URL, EMAIL, TIMEOUT, Parameter.LOCALE, Parameter.REQUEST };

    public DownloadFunction() {
        super("downloadmedia", PARAMETERS);
    }

    private String getUserMail(Node mediafragment) {
        String email = "";
        Cloud cloud = mediafragment.getCloud();
        String owner = mediafragment.getStringValue("owner");
        if (cloud.hasNode(owner)) {
            Node user = cloud.getNode(owner);
            email = user.getStringValue("email");
        }

        return email;
    }

    @Override
    public String getFunctionValue(final Node node, final Parameters parameters) {
        if (log.isDebugEnabled()) {
            log.debug("node #" + node.getNumber());
            log.debug("params: " + parameters);
        }

        StringBuilder result = new StringBuilder("An error occurred");
        String email = getUserMail(node);

        parameters.set("email", email);
        result = new StringBuilder( (String) node.getFunctionValue("download", parameters).get() );
        log.info("Download result: " + result.toString());

        Node source = CreateSourcesWithoutProcessFunction.getMediaSource(node);

        if (source != null) {
            if (source.getNodeManager().hasField("language") && node.getStringValue("language") != null ) {
                if (log.isDebugEnabled()) {
                    log.debug("Setting language of source to: " + node.getStringValue("language"));
                }
                source.setValueWithoutProcess("language", node.getStringValue("language"));
                source.commit();
            } else {
                log.warn("language not set, source: " + source);
            }

            String portalurl = PortalFilter.getPortalUrl(node.getCloud());
            String filesdir = FileServlet.getBasePath("files");

            String filename  = source.getStringValue("url");
            if (log.isDebugEnabled()) {
                log.debug("portalurl: " + portalurl + ", filesdir: " + filesdir + ", fileName " + filename);
            }

            result = new StringBuilder(filename);

            if (filesdir != null && !"".equals(filesdir)) {
                result.insert(0, filesdir);
            }

            if (portalurl != null && !"".equals(portalurl)) {
                result.insert(0, portalurl);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("returning: " + result.toString());
        }
        return result.toString();
    }
}
