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

import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.Identifier;
import org.mmbase.bridge.*;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.Url;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.mmsite.LocaleUtil;

/**
 * UrlConverter that can filter and create urls for the OIP.
 *
 * @author André van Toly
 * @version $Id$
 */
public class MediaUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(MediaUrlConverter.class);

    private static CharTransformer trans = new Identifier();
    private boolean useTitle = false;

    private final LocaleUtil  localeUtil = LocaleUtil.getInstance();

    public MediaUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/media/");
        addBlock(ComponentRepository.getInstance().getComponent("oip").getBlock("mediafragment"));
    }

    public void setUseTitle(boolean t) {
        useTitle = t;
    }

    @Override
    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 1000);
    }

    public static final Parameter<Node> MEDIA = new Parameter<Node>("media", Node.class);

    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, MEDIA, LocaleUtil.LOCALE};
    }

    /**
     * Generates a nice url for 'media'.
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
        if (block.getName().equals("mediafragment")) {
            Node n = frameworkParameters.get(MEDIA);
            if (n == null) throw new IllegalStateException("No media parameter used in " + frameworkParameters);
            b.append(n.getNumber());
            if (useTitle) {
                b.append("/").append(trans.transform(n.getStringValue("title")));
            }
            localeUtil.appendLanguage(b, frameworkParameters);

            if (log.isDebugEnabled()) {
                log.debug("b now: " + b.toString());
            }
        }
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String>  path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) log.debug("path pieces: " + path + ", path size: " + path.size());

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);

        StringBuilder result = new StringBuilder();
        if (path.size() == 0) {
            //result.append("/url.media/index.jspx");
            return Url.NOT; // handled by mmsite
        } else {
            result.append("/mediafragment.jspx?n=");

            String last = path.get(path.size() - 1); // last element can contain language
            last = localeUtil.setLanguage(last, request);
            path.set(path.size() - 1, last);    // put it back

            String nr;
            if (path.size() > 0) {
                nr = path.get(0);    // nodenumber is first element
            } else {
                if (log.isDebugEnabled()) log.debug("path not > 0");
                return Url.NOT;
            }
            Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            if (cloud.hasNode(nr)) {
                Node mediafragment = cloud.getNode(nr);
                
                String nmName = mediafragment.getNodeManager().getName();
                if (!nmName.equals("mediafragments") &&
                        !nmName.equals("videofragments") &&
                        !nmName.equals("imagefragments") &&
                        !nmName.equals("audiofragments")) {
                    throw new FrameworkException("not a mediafragment");
                }

                Date today = new Date();
                boolean show = mediafragment.getBooleanValue("show");
                Date online = mediafragment.getDateValue("online");
                Date offline = mediafragment.getDateValue("offline");
                if (!show || online.after(today) || offline.before(today)) {
                    if (log.isServiceEnabled()) {
                        log.service("Media item #" + nr + " offline - because show: " + show + ", online: " + online + " or offline: " + offline);
                    }
                    return Url.NOT;
                } else {
                    frameworkParameters.set(MEDIA, mediafragment);
                    result.append(nr);
                }
                
            } else {
                // node not found
                return Url.NOT;
            }

        }

        if (log.isDebugEnabled()) log.debug("returning: " + result.toString());
        return new BasicUrl(this, result.toString());
    }

}
