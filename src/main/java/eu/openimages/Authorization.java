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
 * @author André van Toly
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
            public boolean mayDo(User user, MMObjectNode node, Operation operation) {
                UserProvider up = Authenticate.getInstance().getUserProvider();
                if (up.isOwnNode(user, node)) {
                    // own user node, let super handle it
                    return super.mayDo(user, node, operation);
                } else {
                    if (node.getBuilder() == up.getUserBuilder()) {
                        if (user.getRank().getInt() >= Rank.BASICUSER_INT && 
                                user.getRank().getInt() > up.getRank(node).getInt()) {
                            
                            if (log.isDebugEnabled()) {
                                log.debug("Higher rank so may read, write or delete user account #" + node.getNumber());
                            }
                            switch(operation.getInt()) {
                                case Operation.READ_INT:   return true;
                                case Operation.WRITE_INT:  return true;
                                case Operation.DELETE_INT: return true;
                            }
                            
                        }
                    }
                    
                }
                return super.mayDo(user, node, operation);
            }
            
            @Override
            public boolean mayDoOnContext(MMObjectNode userNode, MMObjectNode contextNode,
                                          Operation operation, boolean checkOwnRights) {
                if (userNode.getNumber() == contextNode.getNumber()) {
                    if (log.isDebugEnabled()) {
                        log.debug("The context is the user");
                    }
                    switch(operation.getInt()) {
                        case Operation.READ_INT:   return true;
                        case Operation.WRITE_INT:  return true;
                        case Operation.DELETE_INT: return true;
                    }
                }
                
                /* if current userNode has a rank > contextNode's owner */
                UserProvider up = Authenticate.getInstance().getUserProvider();
                MMObjectNode node = getNode(contextNode.getNumber(), false);
                
                if (node.getBuilder() == up.getUserBuilder()) {
                    if (up.getRank(userNode).getInt() >= Rank.BASICUSER_INT && 
                            up.getRank(userNode).getInt() > up.getRank(node).getInt()) {
                        
                        if (log.isDebugEnabled()) {
                            log.debug("Higher rank so may read, write or delete other user's node #" + node.getNumber());
                        }
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
