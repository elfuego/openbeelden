/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2009 Netherlands Institute for Sound and Vision

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

import javax.servlet.http.HttpServletRequest;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.security.implementation.cloudcontext.Authenticate;
import org.mmbase.security.implementation.cloudcontext.BasicUserProvider;
import org.mmbase.security.implementation.cloudcontext.UserProvider;
import org.mmbase.security.implementation.cloudcontext.User;
import org.mmbase.security.implementation.cloudcontext.UserStatus;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Authentication of users in Open Images, only users with an activated
 * status {@link org.mmbase.security.implementation.cloudcontext.UserStatus.INUSE} have access.
 * The user node (own account) is the default context of nodes (see owner field) this user creates.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class Authentication extends Authenticate {

    private static final Logger log = Logging.getLoggerInstance(Authentication.class);
    private boolean allowEncodedPassword = true;    /* apikey makes use of it */

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
    public org.mmbase.security.UserContext login(String application, java.util.Map<String, ?> loginInfo, Object[] parameters) throws SecurityException {
        UserProvider users = getUserProvider();
        if (users == null) {    
            throw new SecurityException("builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported");
        }

        allowEncodedPassword = org.mmbase.util.Casting.toBoolean(users.getUserBuilder().getInitParameter("allowencodedpassword"));
        if (application.equals("apikey")) {
            log.debug("application " + application);
            log.debug("users " + users + ", allowEncodedPassword " + allowEncodedPassword);
            
            String apikey = (String) loginInfo.get("apikey");
            
            if (apikey == null || "".equals(apikey)) {
                HttpServletRequest req = (HttpServletRequest) loginInfo.get(Parameter.REQUEST.getName());
                apikey = (String) req.getAttribute("apikey");
            }
            
            log.debug("API key is " + apikey);
            
            /* call method for API key: get username and password from API key */
            String username = "foofoo";
            String password = "5426824942db4253";
            
            // login with provided credentials with "name/encodedpassword"
            // below copied from name/encodedpassword in super 
            if (username == null || password == null) {
                log.debug("SecurityException 1");
                throw new SecurityException("Expected the property 'username' and 'password'. But received " + loginInfo);
            }
            MMObjectNode node = users.getUser(username, password, true);
            if (node != null && ! users.isValid(node)) {
                log.debug("SecurityException 2");
                throw new SecurityException("Logged in an invalid user");
            }
            log.debug("user " + node);
            if (node == null) return null;
            return new User(node, getKey(), application);
            
            // TODO: put creditials from apikey in logInfo map
            //loginInfo.put("username", username);
            //loginInfo.put("encodedpassword", password);
            
            // switch application to name/encodedpassword
            //application = "name/encodedpassword";
        }
        return super.login(application, loginInfo, parameters);
    }


    @Override
    public Parameters createParameters(String application) {
        application = application.toLowerCase();
        if ("apikey".equals(application)) {
            return new Parameters(Parameter.REQUEST, new Parameter("apikey", String.class));
        } else {
            return super.createParameters(application);
        }
    }

    @Override
    public String[] getTypes(int method) {
        if (allowEncodedPassword) {
            if (method == METHOD_ASIS) {
                return new String[] {"anonymous", "name/password", "name/encodedpassword", "class", "apikey"};
            } else {
                return new String[] {"name/password", "name/encodedpassword", "class", "apikey"};
            }
        } else {
            if (method == METHOD_ASIS) {
                return new String[] {"anonymous", "name/password", "class", "apikey"};
            } else {
                return new String[] {"name/password", "class", "apikey"};
            }
        }

    }
}
