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

package eu.openimages.api;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

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
 * Downloads a media stream from an url for a media item (mediafragments node) into Open Images. 
 * It starts a thread and calls {@link Downloader} to do the actual work. The media file itself is
 * saved in a mediasources node and transcoded by the streams application when the download finishes.
 * Url and information about success or failure of the download are saved as properties 
 * on the mediafragments node.
 *
 * @author Michiel Meeuwissen
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public final class MediaDownload extends NodeFunction<String> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(MediaDownload.class);

    private static final Parameter<String> URL = new Parameter<String>("url", String.class);
    private final static Parameter[] PARAMETERS = { URL, Parameter.LOCALE };

    private final static String URL_KEY    = MediaDownload.class.getName() + ".url";
    private final static String STATUS_KEY = MediaDownload.class.getName() + ".status";

    private final Map<Integer, Future<?>> runningJobs = new ConcurrentHashMap<Integer, Future<?>>();

    public MediaDownload() {
        super("mediadownload", PARAMETERS);
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
    protected void setDownloadUrl(Node node, String link) {
        setProperty(node, URL_KEY, link);
    }
    protected void setDownloadStatus(Node node, String status) {
        log.info("Setting status of " + node.getNumber() + " to " + status);
        setProperty(node, STATUS_KEY, status);
    }
    protected String getDownloadUrl(Node node) {
        return getProperty(node, URL_KEY);
    }
    protected String getDownloadStatus(Node node) {
        return getProperty(node, STATUS_KEY);
    }


    private Node getMediaSource(Node mediafragment) {
        Node n = null;
        NodeList list = SearchUtil.findRelatedNodeList(mediafragment, "mediasources", "related");
        mediafragment.getCloud().setProperty(org.mmbase.streams.createcaches.Processor.NOT, "no implicit processing please");
        if (list.size() > 0) {
            if (list.size() > 1) {
                log.warn("more then one node found");
            }
            n = list.get(0);
            n.setNodeValue("mediafragment", mediafragment);
            if (log.isDebugEnabled()) {
                log.debug("Existing source " + n.getNodeManager().getName() + " " + n.getNumber());
            }
        } else {
            // create node
            n = mediafragment.getCloud().getNodeManager("streamsources").createNode();
            n.setNodeValue("mediafragment", mediafragment);
            if (log.isDebugEnabled()) {
                log.debug("Created source " + n.getNodeManager().getName() + " " + n.getNumber());
            }
        }
        return n;
    }

    
    protected Future<?> submit(final Node node, final Parameters parameters) {
        return ThreadPools.jobsExecutor.
            submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (log.isDebugEnabled()) {
                                log.debug("media : " + node);
                                log.debug("params: " + parameters);
                            }
                            URL url = new URL(parameters.get(URL));
                            
                            // create streamsource node 
                            Node source = getMediaSource(node);
                            
                            Downloader downloader = new Downloader();
                            downloader.setUrl(url);
                            downloader.setNode(source);
                            log.info("Now calling: " + downloader);
                            String result = downloader.download();
                            
                            // download is ready
                            MediaDownload.this.setDownloadUrl(node, parameters.get(URL));
                            MediaDownload.this.setDownloadStatus(node, "ok");
                            
                            node.getCloud().setProperty(org.mmbase.streams.createcaches.Processor.NOT, null);
                            
                            log.info("Saving: " + result);
                            source.setStringValue("url", result); 
                            source.commit();
                            
                        } catch (MalformedURLException ue) {
                            log.error(ue.getMessage(), ue);
                            MediaDownload.this.setDownloadStatus(node, "BADURL " + ue.getMessage());
                        } catch (IOException ioe) {
                            log.error(ioe.getMessage(), ioe);
                            MediaDownload.this.setDownloadStatus(node, "IOERROR " + ioe.getMessage());
                        } catch (Throwable t) {
                            log.error(t.getMessage(), t);
                            MediaDownload.this.setDownloadStatus(node, "UNEXPECTED " + t.getMessage());
                        } finally {
                            MediaDownload.this.runningJobs.remove(node.getNumber());
                            log.info("Running jobs: " + MediaDownload.this.runningJobs);
                        }
                    }
                });
    }

    @Override
    public String getFunctionValue(final Node node, final Parameters parameters) {
        String status = getDownloadStatus(node);
        if (status == null) {
            Action action = ActionRepository.getInstance().get("oip", "downloadmedia");
            if (action == null) {
                throw new IllegalStateException("Action could not be found");
            }
            if (node.getCloud().may(action, null)) {
                synchronized(runningJobs) {
                    Future<?> future = runningJobs.get(node.getNumber());
                    if (future == null) {
                        setDownloadStatus(node, "busy: " + System.currentTimeMillis());
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
