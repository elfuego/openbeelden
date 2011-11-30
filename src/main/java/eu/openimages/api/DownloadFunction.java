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

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import java.net.UnknownHostException;


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
    private final static Parameter[] PARAMETERS = { URL, EMAIL, Parameter.LOCALE };

    public DownloadFunction() {
        super("downloadmedia", PARAMETERS);
    }

    private Node getMediaSource(Node mediafragment) {
        Node src = null;
        NodeList list = SearchUtil.findRelatedNodeList(mediafragment, "mediasources", "related");
        if (list.size() > 0) {
            if (list.size() > 1) {
                log.warn("More then one streamsources found for #" + mediafragment.getNumber());
            }
            src = list.get(0);
            if (log.isDebugEnabled()) {
                log.debug("Found streamsources #" + src.getNumber());
            }
        }

        return src;
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
        String result = "An error occurred";

        parameters.set("email", getUserMail(node));
        result = (String) node.getFunctionValue("download", parameters).get();

        log.info("Got result: " + result);

        Node source = getMediaSource(node);
        if (node.getStringValue("language") != null) {
            if (log.isDebugEnabled()) {
                log.debug("Setting language of source to: " + node.getStringValue("language"));
            }
            source.setValueWithoutProcess("language", node.getStringValue("language"));
            source.commit();
        } else {
            log.warn("language not set");
        }

        result = "From OIP : " + result;

        return result;
    }
}
