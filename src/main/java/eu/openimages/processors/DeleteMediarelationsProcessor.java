/*

This file is part of the MMBase MMSite application, 
which is part of MMBase - an open source content management system.
    Copyright (C) 2012 André van Toly

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

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.ContextProvider;
import org.mmbase.bridge.Field;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.RelationList;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.datatypes.processors.CommitProcessor;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * This commit-processor is used on deleting relations a media node can have, for example (and in this case) with tags created
 * by other users. You do not want to delete the tags, but relations with it, which can be of another security context.
 * It uses a cloud from class security to do so.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 */

public class DeleteMediarelationsProcessor implements CommitProcessor {
    private static final long serialVersionUID = 0L;

    public static String NOT = DeleteMediarelationsProcessor.class.getName() + ".DONOT";
    private static final Logger LOG = Logging.getLoggerInstance(DeleteMediarelationsProcessor.class);
    
    public void commit(final Node node, final Field field) {

        if (node.getCloud().getProperty(NOT) != null) {
            LOG.service("Not doing because of property");
            return;
        }

        if (node.getNumber() > 0
                && !"mediafragments_translations".equals(node.getNodeManager().getName())) {

            Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null); // using class security
            Node media = cloud.getNode(node.getNumber());
            RelationList rels = SearchUtil.findRelations(media, "tags", "related", "number", "DOWN", "destination");

            LOG.info("Deleting " + rels.size() + " relations with tags of #" + node.getNumber());
            for (Node r : rels) {
                if (r.mayDelete()) {
                    r.delete(true);
                } else {
                    LOG.warn("May not delete relation #" + r);
                }
            }


        }
    }

}
