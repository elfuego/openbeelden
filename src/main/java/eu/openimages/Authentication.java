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

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.module.core.MMObjectNode;
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
        return super.login(application, loginInfo, parameters);
    }



}
