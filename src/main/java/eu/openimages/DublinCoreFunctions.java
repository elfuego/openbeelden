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

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.bridge.util.Queries;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Functions for default field values in Dublin Core fields. 
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public final class DublinCoreFunctions {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(DublinCoreFunctions.class);
    

    private Node node;

    public void setNode(Node n) {
        node = n;
    }
    

    /**
     * Generate default value for dc:creator and/or dc:publisher based on firstname, suffix 
     * and lastname (f.e. Vliet, Paul van) or just username if none of these is known.
     */
    public String getPublisher() {

        // presuming this is username
        String username = node.getCloud().getUser().getIdentifier();
        
        final NodeManager mmbaseusers = node.getCloud().getNodeManager("mmbaseusers");
        NodeQuery q = mmbaseusers.createQuery();
        Queries.addConstraint(q, Queries.createConstraint(q, "username", FieldCompareConstraint.EQUAL, username));
        Node user = mmbaseusers.getList(q).get(0);
        
        StringBuilder sb = new StringBuilder();
        
        if (user != null) {
            String firstname = user.getStringValue("firstname");
            String suffix    = user.getStringValue("suffix");
            String lastname  = user.getStringValue("lastname");
            boolean organisation = user.getBooleanValue("organisation");
            
            if (organisation) {     // Netherlands Institute for Sound and Vision
                sb.append(firstname);
                if (sb.length() > 0) sb.append(" ");
                sb.append(suffix);
                if (sb.length() > 0) sb.append(" ");
                sb.append(lastname);
            } else {                // Vliet, Paul van
                sb.append(lastname);
                if (sb.length() > 0) sb.append(", ");
                sb.append(firstname);
                if (sb.length() > 0) sb.append(" ");
                sb.append(suffix);
            }
        } else {
            log.warn("user null");
        }
        if (sb.length() == 0) { 
            sb.append(node.getCloud().getUser().getIdentifier());
        }
        
        log.debug("returning field value: " + sb.toString());
        
        return sb.toString();
    }
    

}
