package eu.openimages;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.security.Operation;
import org.mmbase.security.Rank;
import org.mmbase.module.core.MMObjectNode;
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
    public  ContextProvider getContextProvider() {
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
                    log.debug("The context is the user");
                    switch(operation.getInt()) {
                    case Operation.READ_INT:   return true;
                    case Operation.WRITE_INT:  return true;
                    case Operation.DELETE_INT: return true;
                    }
                }
                
                /* if contextNode's owner is een usernode met rank < project manager */
                /*
                Cloud cloud = org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
                Node cuNode = cloud.getNode(contextNode.getNumber());
                
                if (cuNode.getNodeManager().getName().equals("mmbaseusers")) {
                    Rank rank1 = UserProvider.getRank(userNode);
                    Rank rank2 = UserProvider.getRank(cuNode);
                    
                    if (rank2.getInt() < rank1.getInt()) {
                        log.debug("Project manager ?");
                        switch(operation.getInt()) {
                            case Operation.READ_INT:   return true;
                            case Operation.WRITE_INT:  return true;
                            case Operation.DELETE_INT: return true;
                        }
                    }
                }
                */
               
                return super.mayDoOnContext(userNode, contextNode, operation, checkOwnRights);
            }

        };
    }




}
