package eu.openimages;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.security.Operation;
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
                return super.mayDoOnContext(userNode, contextNode, operation, checkOwnRights);
            }

        };
    }




}
