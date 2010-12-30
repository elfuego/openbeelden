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

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.Url;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.Identifier;
import org.mmbase.mmsite.LocaleUtil;

/**
 * UrlConverter that can filter and create urls for OIP.
 *
 * @author André van Toly
 * @version $Id$
 */
public class UsersUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(UsersUrlConverter.class);
    private static final CharTransformer trans = new Identifier();

    /* piece of path that leads to edit environment of user account, e.g. /user/[username]/edit */
    protected String editpath = "edit";

    private static final LocaleUtil  localeUtil = new LocaleUtil();

    public UsersUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/users/");
        Component oip = ComponentRepository.getInstance().getComponent("oip");
        if (oip == null) throw new IllegalStateException("No such component oip");
        addBlock(oip.getBlock("user"));
        addBlock(oip.getBlock("user-edit"));
        addBlock(oip.getBlock("user-picture"));
        addBlock(oip.getBlock("user-media"));
        addBlock(oip.getBlock("user-mediaupload"));
        addBlock(oip.getBlock("user-mediapreview"));
        addBlock(oip.getBlock("user-streams"));
        addBlock(oip.getBlock("user-delete"));
    }


    public static final Parameter<Node> USER = new Parameter<Node>("user", Node.class);


    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, USER, LocaleUtil.LOCALE};
    }


    @Override
    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 1000);
    }

    public void setLocales(String s) {
        localeUtil.setLocales(s);
    }

    public void setEditpath(String e) {
        editpath = e;
    }

    @Override
    public boolean isFilteredMode(Parameters fwparams) throws FrameworkException {
        boolean res = super.isFilteredMode(fwparams);
        /*if (log.isDebugEnabled()) {
            //log.debug("Checking wether filtered mode for " + fwparams + " -> " + res, new Exception());
            log.debug("Checking filtered mode: " + fwparams + " -> " + res);
        }*/
        return  res;
    }

    /**
     * Builds nice link for a user. For example:
     *
     * &lt;mm:link referids="media"&gt;
     *   &lt;mm:frameworkparam name="component"&gt;oip&lt;/mm:frameworkparam&gt;
     *   &lt;mm:frameworkparam name="block"&gt;user-media&lt;/mm:frameworkparam&gt;
     *   &lt;p&gt;&lt;a href="${_}"&gt;${_}&lt;/a&gt;&lt;/p&gt;
     * &lt;/mm:link&gt;
     *
     * Can generate a path to a media item page: /users/admin/dashboard/media/1248/ZK_2
     *
     */
    @Override
    protected void getNiceDirectoryUrl(StringBuilder b,
                                                 Block block,
                                                 Parameters parameters,
                                                 Parameters frameworkParameters,  boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("Found oip block " + block);
            log.debug("" + parameters + frameworkParameters);
        }

        String blockName = block.getName();
        if (blockName.indexOf("user") > -1) {

            Node n = frameworkParameters.get(USER);
            if (n == null) throw new IllegalStateException("No node found in " + frameworkParameters + " (pars: " + parameters + ")");
            Cloud cloud = n.getCloud();
            String username = n.getStringValue("username");
            b.append(username);

            if (blockName.equals("user-edit")) {
                b.append("/").append(editpath);

            } else if (blockName.equals("user-mediaupload")) {
                    b.append("/upload");

            } else if (blockName.equals("user-picture")) {
                b.append("/").append(editpath).append("/picture");
                String edit = (String) parameters.get("edit");
                parameters.set("edit", null);

                log.debug("edit param found: " + edit);
                if (edit != null && !"".equals(edit)) {
                    b.append("/edit");
                }

            } else if (blockName.equals("user-media")) {
                String media = (String) parameters.get("media");
                parameters.set("media", null);

                if (cloud.hasNode(media)) {
                    Node mediaNode = cloud.getNode(media);
                    b.append("/").append(editpath).append("/media/").append(media);
                    b.append("/").append(trans.transform(mediaNode.getStringValue("title")));
                }

            } else if (blockName.equals("user-mediapreview")) {
                String media = (String) parameters.get("media");
                parameters.set("media", null);

                if (cloud.hasNode(media)) {
                    Node mediaNode = cloud.getNode(media);
                    b.append("/").append(editpath).append("/media/").append(media);
                    b.append("/").append(trans.transform(mediaNode.getStringValue("title")));
                    b.append("/preview");

                    String edit = (String) parameters.get("edit");
                    parameters.set("edit", null);
                    if (edit != null && !"".equals(edit)) {
                        b.append("/edit");
                    }
                }
            } else if (blockName.equals("user-streams")) {
                String media = (String) parameters.get("media");
                parameters.set("media", null);

                if (cloud.hasNode(media)) {
                    Node mediaNode = cloud.getNode(media);
                    b.append("/").append(editpath).append("/media/").append(media);
                    b.append("/").append(trans.transform(mediaNode.getStringValue("title")));
                    b.append("/streams");

                    String trigger = (String) parameters.get("trigger");
                    String all     = (String) parameters.get("all");
                    String stream  = (String) parameters.get("stream");
                    if (log.isDebugEnabled()) {
                        log.debug("trigger: " + trigger);
                        log.debug("all:     " + all);
                        log.debug("stream:  " + stream);
                    }
                    parameters.set("trigger", null);
                    if (trigger != null && !"".equals(trigger)) {
                        b.append("/trigger");
                        if (all != null && all.equals("true")) {
                            b.append("/all");
                            parameters.set("all", null);
                        }
                        if (stream != null && !"".equals(stream)) {
                            b.append("/").append(stream);
                            parameters.set("stream", null);
                        }
                    }
                    String interrupt = (String) parameters.get("interrupt");
                    if (log.isDebugEnabled()) log.debug("interrupt: " + interrupt);
                    parameters.set("interrupt", null);
                    if (interrupt != null && !"".equals(interrupt)) {
                        b.append("/interrupt");
                    }
                }
            } else if (blockName.equals("user-delete")) {
                b.append("/").append(editpath).append("/delete");
            } else if (blockName.equals("user")) {

            } else if (blockName.equals("user-medialist")) {


            } else {
                throw new IllegalStateException("Unrecognized block name '" + blockName + "'");
            }
            localeUtil.appendLanguage(b, frameworkParameters);
        }

        if (log.isDebugEnabled()) log.debug("b now: " + b.toString());
    }


    /**
     * Translates the result of {@link #getNiceDirectoryUrl} back to an actual JSP that
     * can render the block.
     * Blocks: users (overview users), user (user profile), user-media, user-streams etc.
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String>  path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) log.debug("path pieces: " + path + ", path size: " + path.size());

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);

        StringBuilder result = new StringBuilder();
        if (path.size() == 0) {
            //result.append("/url.user/index.jspx");
            return Url.NOT; // handled by mmsite
        } else {
            result.append("/user.jspx?n=");

            String last = path.get(path.size() - 1); // last element can contain language
            last = localeUtil.setLanguage(last, request);
            path.set(path.size() - 1, last);    // put it back

            if (path.size() > 0) {
                final String username = path.get(0);    // username is first element
                if (log.isDebugEnabled()) {
                    log.debug("username: " + username);
                }

                final Cloud cloud = frameworkParameters.get(Parameter.CLOUD);

                final Node node = SearchUtil.findNode(cloud, "mmbaseusers", "username", username);
                if (node == null) {
                    log.debug("No user with name" + username);
                    return Url.NOT;
                }

                // checks dates and stuff
                Date today = new Date();
                int status = node.getIntValue("status");
                Date validfrom = node.getDateValue("validfrom");
                Date validto = node.getDateValue("validto");
                if (status < 1 || validfrom.after(today) || validto.before(today)) {
                    if (log.isServiceEnabled()) {
                        log.service("user offline because status: " + status + ", validfrom: " + validfrom + ", validto: " + validto);
                    }
                    return Url.NOT;
                }

                frameworkParameters.set(USER, node);
                String nr = "" + node.getNumber();
                result.append(nr);

                if (path.size() > 1) {
                    String editing = path.get(1);

                    /* users/[username]/dasboard */
                    if (editing.equals(editpath) && path.size() == 2) {
                        result.append("&block=user-edit&cacheable=false");

                    /* users/[username]/upload */
                    } else if (editing.equals("upload") && path.size() == 2) {
                        result.append("&block=user-mediaupload&cacheable=false");

                    /* users/[username]/dasboard/picture */
                    } else if (editing.equals(editpath) && path.size() > 2) {
                        String type = path.get(2);  // f.e. media

                        if (type.equals("picture")) {
                            if (log.isDebugEnabled()) log.debug("editing picture of " + username);
                            result.append("&block=user-picture&cacheable=false");
                            if (path.size() > 3) {
                                String edit = path.get(3);
                                log.debug("edit: " + edit);
                                result.append("&edit=").append(edit);
                            }
                        } else if (type.equals("delete")) {
                            if (log.isDebugEnabled()) log.debug("Deleting of " + username);
                            result.append("&block=user-delete&cacheable=false");

                        /* /users/[username]/dashboard/media/[234]/my_title */
                        } else if (type.equals("media") && path.size() > 3) {
                            String nodenr = path.get(3);
                            String title = path.get(4);
                            if (log.isDebugEnabled()) {
                                log.debug("type: " + type);
                                log.debug("nodenr: " + nodenr);
                            }
                            result.append("&media=").append(nodenr);

                            /* /user/[username]/dashboard/media/[234]/my_title/preview */
                            if (path.size() > 5 && path.get(5).equals("preview")) {
                                result.append("&block=user-mediapreview");
                                if (path.size() > 6) {
                                    String edit = path.get(6);
                                    if (log.isDebugEnabled()) log.debug("edit: " + edit);
                                    result.append("&edit=").append(edit);
                                }
                            /* /users/[username]/dashboard/media/[234]/my_title/streams */
                            } else if (path.size() > 5 && path.get(5).equals("streams")) {
                                result.append("&block=user-streams");
                                if (path.size() > 6) {
                                    String action = path.get(6);
                                    /* f.e. /users/[username]/dashboard/media/[234]/my_title/streams/trigger/all/[123] */
                                    if (action.equals("trigger")) {
                                        if (log.isDebugEnabled()) log.debug("trigger: " + action);
                                        result.append("&trigger=").append(nodenr);  // number mediafragment
                                        if (path.size() > 7) {
                                            String path8 = path.get(7);
                                            if (log.isDebugEnabled()) log.debug("all or node: " + path8);
                                            if (path8.equals("all")) {
                                                result.append("&all=true");
                                                if (path.size() > 8) {
                                                    result.append("&stream=").append(path.get(8));
                                                }
                                            } else {
                                                result.append("&stream=").append(path8);    // node number
                                            }


                                        }
                                    } else if (action.equals("interrupt")) {
                                        if (log.isDebugEnabled()) log.debug("interrupt: " + action);
                                        result.append("&interrupt=").append(nodenr);
                                    }
                                }

                            } else {
                                result.append("&block=user-media");
                            }
                        }

                    } else {
                        return Url.NOT;
                    }
                } else {
                    result.append("&block=user");
                }

            } else {
                return Url.NOT;
            }
        }

        if (log.isDebugEnabled()) log.debug("returning: " + result.toString());
        return new BasicUrl(this, result.toString());
    }

}
