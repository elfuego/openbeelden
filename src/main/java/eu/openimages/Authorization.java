package eu.openimages;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.security.Operation;
import org.mmbase.security.Rank;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class Authorization extends Verify {

    private static final Logger log = Logging.getLoggerInstance(Authorization.class);

    @Override
    public ContextProvider getContextProvider() {
        return new BasicContextProvider("mmbaseusers", "mmbasecontexts") {

            @Override
            public String getContextNameField(String table) {
                if ("mmbaseusers".equals(table)) {
                    return "number";
                } else {
                    return super.getContextNameField(table);
                }
            }
            @Override
            public boolean mayDoOnContext(MMObjectNode userNode, MMObjectNode contextNode,
                                          Operation operation, boolean checkOwnRights) {
                if (userNode.getNumber() == contextNode.getNumber()) {
                    if (log.isDebugEnabled()) log.debug("The context is the user");
                    switch(operation.getInt()) {
                        case Operation.READ_INT:   return true;
                        case Operation.WRITE_INT:  return true;
                        case Operation.DELETE_INT: return true;
                    }
                }
                
                /* if current userNode has a rank > contextNode's owner */
                MMObjectNode node = getNode(contextNode.getNumber(), false);
                MMObjectBuilder users = Authenticate.getInstance().getUserProvider().getUserBuilder();
                
                if (node.getBuilder() == users) {
                    if (Authenticate.getInstance().getUserProvider().getRank(userNode).getInt() > 
                            Authenticate.getInstance().getUserProvider().getRank(node).getInt()) {
                        if (log.isDebugEnabled()) log.debug("Higher rank so may read, write, delete");
                        switch(operation.getInt()) {
                            case Operation.READ_INT:   return true;
                            case Operation.WRITE_INT:  return true;
                            case Operation.DELETE_INT: return true;
                        }
                    }
                }
               
                return super.mayDoOnContext(userNode, contextNode, operation, checkOwnRights);
            }

        };
    }


}
