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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mmbase.security.implementation.cloudcontext.Authenticate;
import org.mmbase.security.implementation.cloudcontext.ContextProvider;
import org.mmbase.security.implementation.cloudcontext.User;
import org.mmbase.security.implementation.cloudcontext.UserProvider;
import org.mmbase.security.implementation.cloudcontext.Verify;
import org.mmbase.security.implementation.cloudcontext.workflow.WorkFlowContextProvider;
import org.mmbase.security.Operation;
import org.mmbase.security.Rank;
import org.mmbase.module.core.MMObjectNode;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Authorizes actions by Open Images users. In short: each user has access to their own created nodes,
 * users with rank 'portal manager' or 'project manager' have access to all nodes of users with a
 * lower rank, including user account nodes (mmbaseusers).
 * A portal manager can edit its own nodes and those of site users, but not from other portal
 * managers and thus not from other portals.
 * A project manager prevails over portal manager, can edit portal manager nodes and nodes of other project
 * managers - to share the project management burden. A project manager can not edit user account nodes
 * of other project managers, only its own.
 *
 * @author Michiel Meeuwissen
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public class Authorization extends Verify {

    private static final Logger log = Logging.getLoggerInstance(Authorization.class);

    /** int value for portal manager rank */
    public final static int PORTALMANAGER_INT  = 400;
    /** int value for project manager rank */
    public final static int PROJECTMANAGER_INT = 500;

    @Override
    public ContextProvider getContextProvider() {
        return new WorkFlowContextProvider("mmbaseusers", "mmbasecontexts") {

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
                                log.debug("Higher rank so may do on user account #" + node.getNumber());
                            }
                            switch(operation.getInt()) {
                                case Operation.READ_INT:   return true;
                                case Operation.WRITE_INT:  return true;
                                case Operation.DELETE_INT: return true;
                            }
                        }

                    } else {    // not user builder but some other node

                        String owner = node.getContext(user);
                        if (owner == null || "".equals(owner)) return super.mayDo(user, node, operation);

                        Pattern p = Pattern.compile("[0-9]*");
                        Matcher m = p.matcher(owner);
                        if (m.matches()) {
                            int nodeInt;
                            try {
                                nodeInt = Integer.parseInt(owner);
                            } catch (NumberFormatException nfe) {
                                log.warn("still a nfe: " + nfe);
                                // escape
                                return super.mayDo(user, node, operation);
                            }

                            MMObjectNode user_node = getNode(nodeInt, false);
                            String username = user_node.getStringValue("username");
                            if (log.isDebugEnabled()) {
                                log.debug("owner node #" + owner + " has username: " + username);
                            }

                            /* if user rank > portal man (higher then portal manager)
                               && user rank >= node owner's rank (node rank higher or equal then own rank)
                               && proj. man rank >= node owner's rank (thus not admin's nodes) */
                            if (user.getRank().getInt() > PORTALMANAGER_INT &&
                                    user.getRank().getInt() >= up.getRank(up.getUser(username)).getInt() &&
                                    PROJECTMANAGER_INT >= up.getRank(up.getUser(username)).getInt() ) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Higher or equal rank (then owner) so may do on node #" + node.getNumber() + " (node owner rank " + up.getRank(node).getInt() + ")");
                                }
                                switch(operation.getInt()) {
                                    case Operation.READ_INT:   return true;
                                    case Operation.WRITE_INT:  return true;
                                    case Operation.DELETE_INT: return true;
                                }
                            }

                        } else {
                            return super.mayDo(user, node, operation);
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
                            log.debug("Higher rank (then owner) so may do on node #" + node.getNumber());
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
