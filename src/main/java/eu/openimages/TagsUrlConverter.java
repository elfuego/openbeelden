/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2011 Netherlands Institute for Sound and Vision

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
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.mmsite.LocaleUtil;

/**
 * UrlConverter that can filter and create urls for the OIP.
 *
 * @author André van Toly
 * @version $Id$
 */
public class TagsUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(TagsUrlConverter.class);

    private static CharTransformer trans = new Identifier();
    private boolean useTitle = false;

    private final LocaleUtil  localeUtil = new LocaleUtil();

    public TagsUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/tags/");
        addBlock(ComponentRepository.getInstance().getComponent("oip").getBlock("tag"));
    }

    public void setLocales(String s) {
        localeUtil.setLocales(s);
    }

    @Override
    public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 1000);
    }

    public static final Parameter<Node> TAG = new Parameter<Node>("tag", Node.class);

    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, TAG, LocaleUtil.LOCALE};
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
        if (block.getName().equals("tag")) {
            Node n = frameworkParameters.get(TAG);
            if (n == null) throw new IllegalStateException("No tag parameter used in " + frameworkParameters);
            
            //b.append("/").append(trans.transform(n.getStringValue("name")));
            b.append("/").append(n.getStringValue("name"));
            
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
            return Url.NOT; // handled by mmsite
        } else {
            result.append("/tag.jspx?n=");

            String last = path.get(path.size() - 1); // last element can contain language
            last = localeUtil.setLanguage(last, request);
            path.set(path.size() - 1, last);    // put it back

            if (path.size() > 0) {
                final String tagname = path.get(0);    // tagName is first element
                if (log.isDebugEnabled()) {
                    log.debug("tagname: " + tagname);
                }
                
                final Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
                final Node node = SearchUtil.findNode(cloud, "tags", "name", tagname);
                if (node == null) {
                    log.debug("No tag with name" + tagname);
                    return Url.NOT;
                }
                frameworkParameters.set(TAG, node);
                String nr = "" + node.getNumber();
                result.append(nr);

 
            } else {
                if (log.isDebugEnabled()) { 
                    log.debug("path not > 0");
                }
                return Url.NOT;
            }

        }

        if (log.isDebugEnabled()) log.debug("returning: " + result.toString());
        return new BasicUrl(this, result.toString());
    }

}
