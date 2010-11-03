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
 * Processor that generates publisher name ("Vliet, Paul van" or "Filmmuseum") based upon a node's creator.
 * Similar to {@link CreatorName} for the 'dc:publisher' field, fills these per default with values from user.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */

public class PublisherName {

    private static final Logger log = Logging.getLoggerInstance(PublisherName.class);
    
    private static final long serialVersionUID = 1L;

    public static class Getter implements Processor {
        private static final long serialVersionUID = 1L;
        
        public Object process(Node node, Field field, Object value) {
            if (node != null && (value == null || "".equals(value)) ) {
                if (log.isDebugEnabled()) {
                    log.debug("Value of " + field + " is null, getting default");
                }
                
                String creator = CreatorName.getCreatorName(node);
                if (node.mayWrite()) {
                    node.setValueWithoutProcess(field.getName(), creator);
                }
                
                return creator;
            }
            return value;
        }
    }
    
    public static class Setter implements Processor {
        private static final long serialVersionUID = 1L;
        public Object process(Node node, Field field, Object value) {
            if (node != null && (value == null || "".equals(value)) ) {
                if (log.isDebugEnabled()) {
                    log.debug("Value of " + field + " is null, setting default");
                }
                return CreatorName.getCreatorName(node);
            }
            return value;
        }
    }

    @Override
    public String toString() {
        return "PublisherName";
    }

}


