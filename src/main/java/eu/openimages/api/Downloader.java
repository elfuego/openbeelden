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

import java.io.*;
import java.net.*;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.servlet.FileServlet;
import org.mmbase.util.transformers.*;

/**
 * @author Andr&eacute; van Toly
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public class Downloader {
    private Logger log = Logging.getLoggerInstance(Downloader.class);
    private URL url;
    private Node node;
    
    public void setLogger(Logger l) {
        log = l;
    }
    public void setUrl(URL u) {
        url = u;
    }
    private URL getUrl() {
        return url;
    }
    public void setNode(Node n) {
        node = n;
    }
    private Node getNode() {
        return node;
    }

    static File directory = null;
    private static File getDirectory() {
        if (directory != null) return directory;
        File servletDir = FileServlet.getDirectory();
        if (servletDir == null) throw new IllegalArgumentException("No FileServlet directory found (FileServlet not (yet) active)?");
        return servletDir;
    }

    private static File getFile(final Node node, final Field field, String fileName) {
        return new File(getDirectory(), getFileName(node, field, fileName).replace("/", File.separator));
    }

    private static String getFileName(final Node node, final Field field, String fileName) {
        StringBuilder buf = new StringBuilder();
        org.mmbase.storage.implementation.database.DatabaseStorageManager.appendDirectory(buf, node.getNumber(), "/");
        buf.append("/").append(node.getNumber()).append(".");
        buf.append(fileName);
        return  buf.toString();
    }
    
    private Asciifier fileNameTransformer = new Asciifier();
    {
        fileNameTransformer.setReplacer("_");
        fileNameTransformer.setMoreDisallowed("[\\s!?:/,]");
    }
    private String contenttypeField = "mimetype";
    
    public String download() throws MalformedURLException, SocketException, IOException {

        HttpURLConnection huc = getURLConnection(url);
        int length = huc.getContentLength();
        
        BufferedInputStream in = new BufferedInputStream(huc.getInputStream());
        log.info("content length " + length);
        //node.setInputStreamValue(String fieldName, InputStream value, long size);
        //node.setInputStreamValue("url", in, length);
        //org.mmbase.datatypes.processors.BinaryFile.Setter.process(node, node.getNodeManager().getField("url"), in);
        
        //SerializableInputStream is = Casting.toSerializableInputStream(value);
        String urlStr = url.toString();
        String name = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
        Field field = node.getNodeManager().getField("url");
        File dir = getDirectory();
        log.debug("name " + name + ", dir: " + dir);
        
        String existing = (String) node.getValue(field.getName());
        if (existing != null && ! "".equals(existing)) {
            File ef = new File(dir, existing);
            if (ef.exists() && ef.isFile()) {
                log.service("Removing existing file " + ef);
                ef.delete();
            } else {
                log.warn("Could not find " + ef + " so could not delete it");
            }
        }
        
        File f = getFile(node, field, fileNameTransformer.transform(name));
        log.debug("f: " + f.toString());
        
        Map<String, String> meta = FileServlet.getInstance().getMetaHeaders(f);
        meta.put("Content-Disposition", "attachment; " + FileServlet.getMetaValue("filename", name));
        FileServlet.getInstance().setMetaHeaders(f, meta);
        
        //in.moveTo(f);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        byte[] buf = new byte[1024];
        int b = 0;
        while ((b = in.read(buf)) != -1) {
            out.write(buf, 0, b);
        }
        out.flush();
        in.close();
        out.close();
        
        String urlValue = f.toString().substring(dir.toString().length() + 1);
        node.setValueWithoutProcess("url", urlValue);
        
        if (log.isDebugEnabled()) {
            log.debug("Saved in url field: " + urlValue);
            log.debug("Set a file " + f.getName());
        }
        
        if (node.getNodeManager().hasField(contenttypeField)) {
            if (! node.isChanged(contenttypeField) || node.isNull(contenttypeField)) {
                node.setStringValue(contenttypeField, huc.getContentType());
                log.info("Found " + huc.getContentType());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Field " + contenttypeField + " is already changed " + node.getChanged() + " not setting to " + huc.getContentType());
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No field " + contenttypeField);
            }
        }

        return getUrl().toString();
    }

    /**
     * Opens and tests an URLConnection.
     *
     * @param  url
     * @return a connection or null in case of a bad response (f.e. not a 200)
     */
    private HttpURLConnection getURLConnection(URL url) throws SocketException, IOException {
        URLConnection uc = url.openConnection();
        //HttpURLConnection huc
        if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
            HttpURLConnection huc = (HttpURLConnection)uc;
            int res = huc.getResponseCode();
            if (res == -1) {
                log.error("Server error, bad HTTP response: " + res);
                return null;
            } else if (res < 200 || res >= 400) {
                log.warn(res + " - " + huc.getResponseMessage() + " : " + url.toString());
                return null;
            } else {
                return huc;
            }
        /*   
        } else if (url.getProtocol().equals("file")) {
            InputStream is = uc.getInputStream();
            is.close();
            // If that didn't throw an exception, the file is probably OK
            return uc;
        */
        } else {
            // return "(non-HTTP)";
            return null;
        }
    }
 
    /*
    public static void main(String[] arg) throws Exception {
        Downloader downloader = new Downloader();
        String link = arg[0];
        downloader.setUrl(new URL(link));
        downloader.download();
    } 
    */
}
