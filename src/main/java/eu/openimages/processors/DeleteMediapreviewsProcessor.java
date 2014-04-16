/*

This file is part of the MMBase MMSite application, 
which is part of MMBase - an open source content management system.
    Copyright (C) 2011 André van Toly

MMBase MMSite is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MMBase MMSite is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MMBase. If not, see <http://www.gnu.org/licenses/>.

*/

package eu.openimages.processors;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.datatypes.processors.*;
import org.mmbase.util.logging.*;


/**
 * This commit-processor is used on related images of mediafragments nodes. 
 * While deleting the original node it also deletes the related thumbs.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */

public class DeleteMediapreviewsProcessor implements CommitProcessor {
    private static final long serialVersionUID = 0L;

    public static String NOT = DeleteMediapreviewsProcessor.class.getName() + ".DONOT";

    private static final Logger LOG = Logging.getLoggerInstance(DeleteMediapreviewsProcessor.class);
    
    
    public void commit(final Node node, final Field field) {

        if (node.getCloud().getProperty(NOT) != null) {
            LOG.service("Not doing because of property");
            return;
        }
        if (node.getNumber() > 0) {
            NodeList images = SearchUtil.findRelatedNodeList(node, "images", "related");
            LOG.info("Deleting " + images.size() + " images of #" + node.getNumber());
            for (Node img : images) {
                if (img.mayDelete()) {
                    img.delete(true);
                } else {
                    LOG.warn("May not delete #" + img);
                }
            }            
        }
    }

}
