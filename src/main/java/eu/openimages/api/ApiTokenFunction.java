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

import org.mmbase.bridge.Node;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.*;


/**
 * Nodefunction on mmbaseusers to generate an API token for a specific user,
 * uses {@link ApiToken}.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public final class ApiTokenFunction extends NodeFunction<String> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(ApiTokenFunction.class);
    
    private String key = "pindakaas";

    public ApiTokenFunction() {
        super("apitoken");
    }

    @Override
    public String getFunctionValue(final Node node, final Parameters parameters) {
        if (log.isDebugEnabled()) {
            log.debug("node #" + node.getNumber());
        }
        
        // get secret key
        String key = node.getNodeManager().getProperty("apitokenkey");
        if (key == null) {
            log.warn("No property 'apitokenkey' found, using default: " + key);
        }
        
        String result = "An error occurred: no apikey!";
        String username = node.getStringValue("username");
        String password = node.getStringValue("password");

        try {
            ApiToken apiToken = new ApiToken();
            //apiToken.setFormat("base64");
            result = apiToken.encrypt(username, password, key);
        } catch (IllegalArgumentException iae) {
            log.error("IllegalArgumentException " + iae);
        }

        return result;
    }
}
