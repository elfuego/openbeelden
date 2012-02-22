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
import java.util.concurrent.*;
import java.io.*;

import org.mmbase.bridge.*;
import org.mmbase.applications.media.urlcomposers.*;
import org.mmbase.applications.media.Format;
import org.mmbase.security.ActionRepository;
import org.mmbase.security.Action;

import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.util.functions.*;
import org.mmbase.util.ThreadPools;
import org.mmbase.util.logging.*;


/**
 * Exports a media item (mediafragments) to Mediawiki.
 *
 * @author Andr&eacute; van Toly
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public final class MediaExport extends NodeFunction<String> {
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = Logging.getLoggerInstance(MediaExport.class);

    private  static final Parameter<String> USER     = new Parameter<String>("username",   String.class);
    private  static final Parameter<String> PASSWORD = new Parameter<String>("password",   String.class);
    private  static final Parameter<Node> PORTAL     = new Parameter<Node>("portal",       Node.class);

    private final static Parameter[] PARAMETERS = { USER, PASSWORD, PORTAL, Parameter.LOCALE};

    private final static String URL_KEY    = MediaExport.class.getName() + ".url";
    private final static String STATUS_KEY = MediaExport.class.getName() + ".status";

    private final Map<Integer, Future<?>> runningJobs = new ConcurrentHashMap<Integer, Future<?>>();

    public MediaExport() {
        super("mediaexport", PARAMETERS);
    }


    protected URLComposer getUploadedSource(Node node) {
        Function filteredUrlFunction = node.getFunction("filteredurls");
        Parameters args = filteredUrlFunction.createParameters();
        args.setAutoCasting(true);
        // only OGV
        args.set("format", Format.OGV);

        // And prefer the one which is labeled with 'hi'.
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(org.mmbase.applications.media.filters.ClientLabelSorter.ATT, "hi");
        args.set("attributes", attributes);

        List sources = (List) filteredUrlFunction.getFunctionValue(args);

        if (LOG.isDebugEnabled()) {
            LOG.debug("source: " + sources);
        }

        URLComposer uc = (URLComposer) sources.get(0);
        return uc;
    }
    protected void setProperty(Node node, String key, String value) {
        NodeManager properties = node.getCloud().getNodeManager("properties");
        Function set = properties.getFunction("set");
        Parameters params = set.createParameters();
        params.set("node", node);
        params.set("key", key);
        if (value.length() > 255) {
            value = value.substring(0, 255);
        }
        params.set("value", value);
        set.getFunctionValue(params);
    }

    protected String getProperty(Node node, String key) {
        NodeManager properties = node.getCloud().getNodeManager("properties");
        Function get = properties.getFunction("get");
        Parameters params = get.createParameters();
        params.set("node", node);
        params.set("key", key);
        return (String) get.getFunctionValue(params);
    }
    protected void setWikiUrl(Node node, String result) {
        setProperty(node, URL_KEY, result);
    }
    protected void setWikiStatus(Node node, String status) {
        LOG.info("Status status of " + node.getNumber() + " to " + status);
        setProperty(node, STATUS_KEY, status);
    }
    protected String getWikiUrl(Node node) {
        return getProperty(node, URL_KEY);
    }
    protected String getWikiStatus(Node node) {
        return getProperty(node, STATUS_KEY);
    }

    protected void setMetaData(Exporter exporter, Node node) {
        NodeManager nm = node.getNodeManager();
        Cloud cloud = node.getCloud();
        // translations
        String translations_builder = nm.getProperty("translations.builder");
        if (translations_builder == null) {
            translations_builder = nm.getName() + "_translations";
        }
        NodeManager translationsNM = cloud.getNodeManager(translations_builder);
        // TODO: check if there are multiple for just one lang (e.g. > 1 for en)
        // TODO: Is there not function somewhere to calculate the transactions?
        NodeList translations = SearchUtil.findRelatedNodeList(node, translationsNM.getName(), "langrel");
        exporter.setProperty("title", node.getStringValue("title"));

        for (Node t : translations) {
            Locale loc = new Locale(t.getStringValue("language"));
            for (Field tf : translationsNM.getFields()) {
                exporter.setProperty(tf.getName(), t.getStringValue(tf.getName()), loc);
            }
        }
        {
            Locale loc = new Locale(node.getStringValue("language"));
            for (Field nf : nm.getFields()) {
                exporter.setProperty(nf.getName(), node.getStringValue(nf.getName()), loc);
                exporter.setProperty(nf.getName(), node.getStringValue(nf.getName()));
            }
        }
        exporter.setProperty("id", "" + node.getNumber());
    }
    protected void setFile(Exporter exporter, Node node) {
        final URLComposer uc = getUploadedSource(node);
        Cloud cloud = node.getCloud();
        Node source = cloud.getNode(uc.getSource().getNumber());
        File file = (File) source.getFunctionValue("file", null).get();
        exporter.setFile(file);
        exporter.setProperty("extension", uc.getFormat().toString().toLowerCase());
        exporter.setProperty("url", uc.getURL());

    }

    protected Future<?> submit(final Node node, final Parameters parameters) {
        return ThreadPools.jobsExecutor.
            submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("media : " + node);
                                LOG.debug("params: " + parameters);
                            }
                            Exporter exporter = new Exporter();
                            //exporter.setLogger(log);
                            exporter.setUserName(parameters.get(USER));
                            exporter.setPassword(parameters.get(PASSWORD));
                            MediaExport.this.setMetaData(exporter, node);
                            MediaExport.this.setFile(exporter, node);
                            Node portal = parameters.get(PORTAL);
                            exporter.setProperty("project", portal != null ? portal.getStringValue("title") : "Open Images");
                            LOG.debug("Now calling " + exporter);
                            String url =  exporter.export();
                            // upload is ready
                            MediaExport.this.setWikiUrl(node, url);
                            MediaExport.this.setWikiStatus(node, "ok");
                        } catch (IOException ioe) {
                            LOG.error(ioe.getMessage(), ioe);
                            MediaExport.this.setWikiStatus(node, "IOERROR " + ioe.getMessage());
                        } catch (org.mmbase.util.externalprocess.ProcessException pe) {
                            LOG.error(pe.getMessage(), pe);
                            MediaExport.this.setWikiStatus(node, "PERROR " + pe.getMessage());
                        } catch (InterruptedException ie) {
                            LOG.error(ie.getMessage(), ie);
                            MediaExport.this.setWikiStatus(node, "INTERRUPTED " + ie.getMessage());
                        } catch (Throwable t) {
                            LOG.error(t.getMessage(), t);
                            MediaExport.this.setWikiStatus(node, "UNEXPECTED " + t.getMessage());
                        } finally {
                            MediaExport.this.runningJobs.remove(node.getNumber());
                            LOG.info("Running jobs " + MediaExport.this.runningJobs);
                        }
                    }
                });
    }

    @Override
    public String getFunctionValue(final Node node, final Parameters parameters) {
        String status = getWikiStatus(node);
        if (status == null) {
            Action action = ActionRepository.getInstance().get("oip", "exportmedia");
            if (action == null) {
                throw new IllegalStateException("Action could not be found");
            }
            if (node.getCloud().may(action, null)) {
                synchronized(runningJobs) {
                    Future<?> future = runningJobs.get(node.getNumber());
                    if (future == null) {
                        setWikiStatus(node, "busy:" + System.currentTimeMillis());
                        future = submit(node, parameters);
                    }
                }
            } else {
                throw new org.mmbase.security.SecurityException("Not allowed");
            }
        }
        return status;
    }
}
