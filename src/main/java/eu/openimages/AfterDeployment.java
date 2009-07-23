package eu.openimages;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.security.implementation.cloudcontext.ContextBuilderFunctions;
import org.mmbase.security.Operation;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author Michiel Meeuwissen;
 * @version $Id$
 */
public class AfterDeployment implements Runnable {
    private static final Logger log = Logging.getLoggerInstance(AfterDeployment.class);


    private final String[] SITEUSER_OBJECTS = new String[] {
        "videostreamsources", "videostreamsourcescaches",
        "audiostreamsources", "audiostreamsourcescaches",
        "streamsources", "streamsourcescaches",
        "imagesources", "images",
        "tags",
        "mediafragments", "mediafragments_translations",
        "langrel", "ratingrel", "insrel", "posrel"
    };

    public void run() {
        Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
        CloudThreadLocal.bind(cloud);
        log.info("Running " + this + " with " + cloud);
        Node adminNode = SearchUtil.findNode(cloud, "mmbaseusers", "username", "admin");
        Node adminContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "admin");

        log.info("Changing default context and password of admin user");
        adminNode.setNodeValue("defaultcontext", adminContext);
        adminNode.setStringValue("password", "openimages2009");
        adminNode.commit();


        log.info("Revoking some rights of 'Users' on the 'default' context.");
        Node defaultContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name", "default");
        Node systemContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name",  "system");
        Node siteusersContext = SearchUtil.findNode(cloud, "mmbasecontexts", "name",  "siteusers");
        Node usersGroup     = SearchUtil.findNode(cloud, "mmbasegroups",   "name", "Users");

        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.WRITE,  cloud.getUser());
        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.DELETE, cloud.getUser());
        ContextBuilderFunctions.revoke(defaultContext,  usersGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.revoke(systemContext,   usersGroup, Operation.CREATE, cloud.getUser());
        ContextBuilderFunctions.grant(siteusersContext, usersGroup, Operation.CREATE, cloud.getUser());

        for (String nm : SITEUSER_OBJECTS) {
            cloud.getNodeManager(nm).setContext("siteusers");
        }
        CloudThreadLocal.unbind();

    }
}
