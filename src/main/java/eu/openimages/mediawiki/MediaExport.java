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

package eu.openimages.mediawiki;

import java.util.*;
import java.io.*;

import org.mmbase.bridge.*;
import org.mmbase.applications.media.urlcomposers.*;
import org.mmbase.security.ActionRepository;

import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.LocalizedString;
import org.mmbase.bridge.util.NodeMap;
import org.mmbase.bridge.util.MapNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Exports a media item (mediafragments) to Mediawiki.
 *
 * @author Andr&eacute; van Toly
 * @version $Id: NodeTranslation.java 39860 2009-11-23 19:50:59Z andre $
 */
public final class MediaExport extends NodeFunction<String> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(MediaExport.class);

    public final static Parameter[] PARAMETERS = {
        new Parameter("url", java.lang.String.class),
        new Parameter("username", java.lang.String.class),
        new Parameter("password", java.lang.String.class)
    };

    public MediaExport() {
        super("mediaexport", PARAMETERS);
    }


    @Override
    public String getFunctionValue(Node node, Parameters parameters) {

        Cloud cloud = node.getCloud();
        NodeManager nm = node.getNodeManager();

        if (log.isDebugEnabled()) {
            log.debug("media : " + node);
            log.debug("params: " + parameters);
        }

        if (node.getCloud().may(ActionRepository.getInstance().get("oip", "exportmedia"), null)) {

            String url = (String) parameters.get("url");
            String username = (String) parameters.get("username");
            String password = (String) parameters.get("password");

            List args = new ArrayList();
            List sources = (List) node.getFunctionValue("filteredurls", args).get();

            log.debug("source: " + sources);

            URLComposer cu = (URLComposer) sources.get(0);
            //String link = cu.getUrl();

            Node source = cloud.getNode(cu.getSource().getNumber());
            File file = new File(org.mmbase.servlet.FileServlet.getDirectory(), source.getStringValue("url"));
            String fileName = file.toString();
            int i = fileName.lastIndexOf(".");
            String extension = fileName.substring(i + 1, fileName.length());
            //(File) source.getFunctionValue("file", null);

            // translations
            String translations_builder = nm.getProperty("translations.builder");
            if (translations_builder == null) {
                translations_builder = nm.getName() + "_translations";
            }
            NodeManager translationsNM = cloud.getNodeManager(translations_builder);
            // TODO: check if there are multiple for just one lang (f.e. > 1 for en)
            NodeList translations = SearchUtil.findRelatedNodeList(node, translationsNM.getName(), "langrel");


            try {
                Exporter exporter = new Exporter();
                exporter.setUserName(username);
                exporter.setPassword(password);
                exporter.setFile(file);
                //exporter.setProperty("title", node.getStringValue("title"));


                for (Node t : translations) {

                    String lang = t.getStringValue("language");
                    Locale loc = new Locale(lang);
                    
                    for (Field tf : translationsNM.getFields()) {
                        exporter.setProperty(tf.getName(), t.getStringValue(tf.getName()), loc);
                    }

                }

                String la = node.getStringValue("language");
                Locale lo = new Locale(la);
                for (Field nf : nm.getFields()) {
                    exporter.setProperty(nf.getName(), node.getStringValue(nf.getName()), lo);
                }

                
                exporter.setProperty("id", "" + node.getNumber());
                exporter.setProperty("extension", extension);
                exporter.setProperty("project", "Open Beelden");
                long result = exporter.export();
                return result == 0 ? "Succeeded!" : "Failed: " + result;
            } catch (Exception e) {
                log.error(e);
                return e.getMessage();
            }

        } else {
            return "Not allowed";
        }

    }

}
