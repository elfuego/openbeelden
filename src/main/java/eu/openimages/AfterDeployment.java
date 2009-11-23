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

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.security.implementation.cloudcontext.ContextBuilderFunctions;
import org.mmbase.security.Operation;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Class to be run after the OIP application with data has finished loading when the application is
 * first installed on an empty database.
 * It creates and sets needed security configuration, groups, ranks and accounts.
 * 
 * @author Michiel Meeuwissen;
 * @version $Id$
 */
public class AfterDeployment implements Runnable {
    private static final Logger log = Logging.getLoggerInstance(AfterDeployment.class);


    private final String[] SITEUSER_OBJECTS = new String[] {
        "videofragments", "videostreamsources", "videostreamsourcescaches",
        "audiofragments", "audiostreamsources", "audiostreamsourcescaches",
        "streamsources",  "streamsourcescaches",
        "imagefragments", "imagesources", "images",
        "tags",
        "mediafragments", "mediafragments_translations",
        "langrel", "ratingrel", "insrel", "posrel"
    };

    public void run() {
        Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
        CloudThreadLocal.bind(cloud);
        log.info("Running " + this + " with " + cloud);
        
        /* admin */
        Node adminNode    = SearchUtil.findNode(cloud, "mmbaseusers", "username", "admin");
        Node adminGroup   = SearchUtil.findNode(cloud, "mmbasegroups", "name", "Administrators");
        Node adminContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "admin");
        log.info("Changing default context and password of admin user");
        adminNode.setNodeValue("defaultcontext", adminContext);
        adminNode.setStringValue("password", "openimages2009");
        adminNode.commit();

        /* site users */
        log.info("Revoking some rights of 'Users' on the 'default' context.");
        Node defaultContext   = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "default");
        Node systemContext    = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "system");
        Node siteusersContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "siteusers");
        Node usersGroup       = SearchUtil.findNode(cloud, "mmbasegroups",   "name", "Users");
        Node siteusersRank    = SearchUtil.findNode(cloud, "mmbaseranks", "name", "site user");

        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.WRITE,  cloud.getUser());
        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.DELETE, cloud.getUser());
        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.revoke(systemContext,   usersGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.grant(siteusersContext, usersGroup, Operation.CREATE, cloud.getUser());

        for (String nm : SITEUSER_OBJECTS) {
            cloud.getNodeManager(nm).setContext("siteusers");
        }

        /* b+g */
        Node begNode    = SearchUtil.findNode(cloud, "mmbaseusers", "username", "beeldengeluid");
        Node begContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "beeldengeluid");
        begNode.setNodeValue("rank", siteusersRank);
        begNode.commit();
        
        /* project managers */
        Node pmGroup = SearchUtil.findNode(cloud, "mmbasegroups", "name", "Project managers");
        log.info("Granting some rights to group 'Project managers' #" + pmGroup.getNumber());
        
        Node pmUser  = SearchUtil.findNode(cloud, "mmbaseusers", "username", "foofoo");
        Node pmRank  = SearchUtil.findNode(cloud, "mmbaseranks", "name", "project manager");
        pmUser.setNodeValue("rank", pmRank);
        pmUser.commit();
        
        ContextBuilderFunctions.grant(defaultContext, pmGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.grant(defaultContext, pmGroup, Operation.WRITE, cloud.getUser());
        ContextBuilderFunctions.grant(defaultContext, pmGroup, Operation.DELETE, cloud.getUser());
        ContextBuilderFunctions.grant(defaultContext, pmGroup, Operation.CHANGE_CONTEXT, cloud.getUser());
        ContextBuilderFunctions.grant(defaultContext, pmGroup, Operation.CHANGE_RELATION, cloud.getUser());
        
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.READ, cloud.getUser());
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.WRITE, cloud.getUser());
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.DELETE, cloud.getUser());
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.CHANGE_CONTEXT, cloud.getUser());
        ContextBuilderFunctions.grant(begContext, pmGroup, Operation.CHANGE_RELATION, cloud.getUser());
        
        ContextBuilderFunctions.grant(systemContext,  pmGroup, Operation.CREATE, cloud.getUser());

        RelationManager rm = cloud.getRelationManager(cloud.getNodeManager("mmbasegroups"), cloud.getNodeManager("mmbasegroups"), "contains"); 
        Node everybody     = SearchUtil.findNode(cloud, "mmbasegroups", "name", "Everybody");
        everybody.createRelation(pmGroup, rm).commit();     // get rights from everybody
        usersGroup.createRelation(pmGroup, rm).commit();    // get rights from users
        pmGroup.createRelation(adminGroup, rm).commit();    // give rights to admins

        CloudThreadLocal.unbind();

    }
}
