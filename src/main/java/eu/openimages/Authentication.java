package eu.openimages;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class Authentication extends Authenticate {

    private static final Logger log = Logging.getLoggerInstance(Authentication.class);

    @Override
    public  UserProvider getUserProvider() {
        return new BasicUserProvider("mmbaseusers") {
            @Override
             protected String getDefaultContextField() {
                return "number";
            }
            @Override
            public String getDefaultContext(MMObjectNode node)  {
                return node.getStringValue(getDefaultContextField());
            }

            @Override
            protected boolean isStatusValid(MMObjectNode node) {
                boolean result =  node.getIntValue(getStatusField()) == UserStatus.INUSE.getValue();
                //log.debug("Checking whether " + node  + " is valid -> " + result);
                return result;
            }

            @Override
            protected boolean getUserNameCaseSensitive() {
                return false;
            }
        };
    }

    @Override
    public int getDefaultMethod(String protocol) {
        return super.getDefaultMethod(protocol);
        //return METHOD_SESSIONDELEGATE; // redirect to /login
    }
    @Override
    public User login(String application, java.util.Map<String, ?> loginInfo, Object[] parameters) throws SecurityException {
        return super.login(application, loginInfo, parameters);
    }



}
