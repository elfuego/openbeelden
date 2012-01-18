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

package eu.openimages.api;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.Url;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * UrlConverter that can filter and create urls for the API in OIP.
 * It works somewhat as a RESTfull application. For example:
 * /api/media POST calls block create with type parameter media(fragments),
 * /api/media GET calls block list listing mediafragments,
 * /api/media/123 GET calls block get showing that specific media item,
 * /api/media/123 POST calls block update and updates that specific item.
 *
 * @author André van Toly
 * @version $Id: MediaUrlConverter.java 45828 2011-07-05 11:07:14Z andre $
 */
public class ApiUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(ApiUrlConverter.class);

    public ApiUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/api/");
        Component oipapi = ComponentRepository.getInstance().getComponent("oipapi");
        if (oipapi == null) throw new IllegalStateException("No such component oipapi");
        addBlock(oipapi.getBlock("create"));
        addBlock(oipapi.getBlock("list"));
        addBlock(oipapi.getBlock("get"));
        addBlock(oipapi.getBlock("update"));
    }

    @Override
    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 1000);
    }

    public static final Parameter<String> TYPE = new Parameter<String>("type", String.class);

    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] { Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, TYPE };
    }

    /**
     * Generates nice urls for 'api', like f.e. '/api/media' listing media.
     */
    @Override
    protected void getNiceDirectoryUrl(StringBuilder b,
                                                 Block block,
                                                 Parameters parameters,
                                                 Parameters frameworkParameters,  boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("" + parameters + frameworkParameters);
            log.debug("Found oip block " + block);
        }
        // url: /api/media
        if (block.getName().equals("list") || block.getName().equals("create")) {
            String type = frameworkParameters.get(TYPE);
            if (type == null) throw new IllegalStateException("No type parameter used in " + frameworkParameters);
            if (type.equals("mediafragments")) {    // shorten url to something more sensible
                type = "media";
            } else if (type.equals("videofragments")) {
                type = "video";
            } else if (type.equals("audiofragments")) {
                type = "audio";
            }

            b.append(type);

            if (log.isDebugEnabled()) {
                log.debug("b now: " + b.toString());
            }
        // url: /api/media/123
        } else if (block.getName().equals("get") || block.getName().equals("update") ) {
            Node node = frameworkParameters.get(Parameter.NODE);
            if (node == null) throw new IllegalStateException("No type parameter used in " + frameworkParameters);
            String type = node.getNodeManager().getName();
            if (type.equals("mediafragments") || type.equals("videofragments")) {
                type = "media";
            }

            b.append(type).append("/").append(node.getNumber());

            if (log.isDebugEnabled()) {
                log.debug("b now: " + b.toString());
            }
        }
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block.
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String> pa, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("path pieces: " + pa + ", path size: " + pa.size());
            log.debug("params " + params);
        }

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);
        String reqMethod = request.getMethod().toUpperCase();

        StringBuilder result = new StringBuilder();
        if (pa.size() == 0) {
            return Url.NOT; // handled by mmsite
        } else {
            String type = pa.get(0);
            if (log.isDebugEnabled()) {
                log.debug("type: " + type + ", reqMethod: " + reqMethod);
            }

            if ("media".equals(type)) { // reverse shortening of url
                type = "mediafragments";
            } else if ("video".equals(type)) {
                type= "videofragments";
            } else if ("audio".equals(type)) {
                type = "audiofragments";
            }
            frameworkParameters.set(TYPE, type);

            final Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            if (!cloud.hasNodeManager(type)) {
                log.warn("Nodemanager not found: " + type);
                return Url.NOT;
            }
            result.append("/api/block.xml.jspx?type=" + type);

            if (pa.size() == 1) {
                if (reqMethod.equals("POST")) {
                    if (type.equals("mediafragments")
                            || type.equals("videofragments")
                            || type.equals("audiofragments")
                            || type.equals("imagefragments")) {
                        log.debug("url: " + params.get("url"));
                        result.append("&block=media-create");
                    } else {
                        result.append("&block=create");
                    }
                } else {
                    result.append("&block=list");
                }

            } else if (pa.size() == 2) {
                String nr = pa.get(1);
                if (!cloud.hasNode(nr)) {
                    log.warn("Node not found: " + nr);
                    return Url.NOT;
                }
                /*  TODO: check if node belongs to type, should include parent nodemanagers (nm.getParent())
                } else if (!cloud.getNode(nr).getNodeManager().equals(cloud.getNodeManager(type))) {
                    log.warn("Node #" + nr + " not of type: " + type);
                    return Url.NOT; } */


                if (reqMethod.equals("POST")) {
                    result.append("&block=update");
                } else {
                    result.append("&block=get");
                }

                result.append("&n=").append(nr);
            }
        }


        if (log.isDebugEnabled()) {
            log.debug("returning: " + result.toString());
        }
        return new BasicUrl(this, result.toString());
    }

}
