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

package eu.openimages.processors;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.datatypes.processors.Processor;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Processor that generates creator name ("Vliet, Paul van" or "Filmmuseum") based upon a node's creator.
 * For the 'dc:creator' field, fills these per default with values from user. 
 * Except for already existing nodes when the new value is 'admin', then it tries to use the current 
 * value of publisher which can be more meaningful. See its equivalent {@link PublisherName}.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */

public class CreatorName {

    private static final Logger log = Logging.getLoggerInstance(CreatorName.class);
    private static final long serialVersionUID = 1L;

    public static class Getter implements Processor {
        
        private static final long serialVersionUID = 1L;
        
        public Object process(Node node, Field field, Object value) {
            if (node != null && (value == null || "".equals(value)) ) {
                if (log.isDebugEnabled()) {
                    log.debug("Value of field " + field + " is null, setting default");
                }
                
                return getCreatorName(node);
            }
            return value;
        }
    }
    
    public static class Setter implements Processor {
        
        private static final long serialVersionUID = 1L;
        
        public Object process(Node node, Field field, Object value) {
            if (node != null && (value == null || "".equals(value)) ) {
                if (log.isDebugEnabled()) {
                    log.debug("Value of field " + field + " is null, getting default");
                }

                String creator = getCreatorName(node);
                String publisher = (String) node.getValueWithoutProcess("publisher");
                if ("admin".equals(creator) && !"".equals(publisher)) {
                    creator = publisher;
                }
                
                return creator;
            }
            return value;
        }
    }

    @Override
    public String toString() {
        return "CreatorName";
    }

    /**
     * Generate default value for dc:creator and/or dc:publisher based on node creators 
     * firstname, suffix and lastname (f.e. Vliet, Paul van). 
     * @param   node to find creators name for 
     * @return 'real name' of this nodes creator or just current clouds user if none is known
     */
    protected static String getCreatorName(Node node) {
        StringBuilder sb = new StringBuilder();
        
        String username = node.getStringValue("creator");
        if (username == null || "".equals(username)) {
            username = node.getCloud().getUser().getIdentifier();
        }
        
        final NodeManager mmbaseusers = node.getCloud().getNodeManager("mmbaseusers");
        NodeQuery q = mmbaseusers.createQuery();
        Queries.addConstraint(q, Queries.createConstraint(q, "username", FieldCompareConstraint.EQUAL, username));
        NodeList users = mmbaseusers.getList(q);
        
        if (users.size() > 0) {
            Node user = users.get(0);
            
            if (user == null) {
                log.warn("user null?!");
            } else {
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
            }
            if (sb.length() == 0) { 
                sb.append(username);
            }
        } else {
            log.warn("no usernode found");
        }
        
        return sb.toString();
    }

}


