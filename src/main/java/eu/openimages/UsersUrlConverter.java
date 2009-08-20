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
 * UrlConverter that can filter and create urls for the OIP.
 *
 * @author André van Toly
 * @version $Id$
 */
public class UsersUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(UsersUrlConverter.class);
    private static CharTransformer trans = new Identifier();

    /* piece of path that leads to edit environment of user account, f.e. /user/[username]/edit */
    protected static String editpath = "edit";

    private final LocaleUtil  localeUtil = new LocaleUtil();

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
    }


    public static final Parameter<Node> USER = new Parameter<Node>("user", Node.class);


    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, USER, LocaleUtil.LOCALE};
    }


    @Override public int getDefaultWeight() {
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
        if (log.isDebugEnabled()) {
            //log.debug("Checking wether filtered mode for " + fwparams + " -> " + res, new Exception());
            log.debug("Checking wether filtered mode for " + fwparams + " -> " + res);
        }
        return  res;
    }

    /**
     * Generates a nice url.
     *
     */
    @Override protected void getNiceDirectoryUrl(StringBuilder b,
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
                    log.debug("trigger: " + trigger);
                    parameters.set("trigger", null);
                    if (trigger != null && !"".equals(trigger)) {
                        b.append("/trigger");
                    }
                    String interrupt = (String) parameters.get("interrupt");
                    log.debug("interrupt: " + interrupt);
                    parameters.set("interrupt", null);
                    if (interrupt != null && !"".equals(interrupt)) {
                        b.append("/interrupt");
                    }
                }
            }
            localeUtil.appendLanguage(b, frameworkParameters);
        }

        if (log.isDebugEnabled()) log.debug("b now: " + b.toString());
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block.
     * Blocks: users (overview users), user (user profile), ..
     */
    @Override public Url getFilteredInternalDirectoryUrl(List<String>  path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
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

                    /* user/[username]/dasboard */
                    if (editing.equals(editpath) && path.size() == 2) {
                        result.append("&block=user-edit&cacheable=false");

                    /* user/[username]/upload */
                    } else if (editing.equals("upload") && path.size() == 2) {
                        result.append("&block=user-mediaupload&cacheable=false");

                    /* user/[username]/dasboard/picture */
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

                        /* /user/[username]/dashboard/media/[234]/my_title */
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
                                    log.debug("edit: " + edit);
                                    result.append("&edit=").append(edit);
                                }
                            } else if (path.size() > 5 && path.get(5).equals("streams")) {
                                result.append("&block=user-streams");
                                if (path.size() > 6) {
                                    String action = path.get(6);
                                    if (action.equals("trigger")) {
                                        log.debug("trigger: " + action);
                                        result.append("&trigger=").append(nodenr);
                                    } else if (action.equals("interrupt")) {
                                        log.debug("interrupt: " + action);
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
