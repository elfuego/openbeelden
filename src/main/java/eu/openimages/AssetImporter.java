package eu.openimages;


import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.servlet.FileServlet;
import org.mmbase.applications.media.Format;
import org.mmbase.streams.transcoders.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.ErrorHandler;
import org.mmbase.util.logging.*;


/**
 * Imports  all mediafragments which are described with set of XML. Default this ready all XML from
 * the directory 'B&amp;G' in the files directory. This is the location where the files must be
 * present any way.
 *
 * This can be scheduled in crontab, which will autoamaticly add the new files to the system. (It
 * would _remove_ fragments though).
 *
 * @author Michiel Meeuwissen;
 * @version $Id$
 */
public class AssetImporter implements Runnable, LoggerAccepter {
    private static final Logger LOG = Logging.getLoggerInstance(AssetImporter.class);

    private ChainedLogger log = new ChainedLogger(LOG);
    private final Thread thread = Thread.currentThread();
    public static AssetImporter running;

    protected String[] dirNames = new String[] {"BG", "B&G"};



    public void addLogger(Logger l) {
        log.addLogger(l);
    }
    public boolean removeLogger(Logger l) {
        return log.removeLogger(l);
    }
    public boolean containsLogger(Logger l) {
        return log.containsLogger(l);
    }


    private static final DateFormat DATEFORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final Pattern STRINGS = Pattern.compile("title|description|date|format|identifier");

    protected class Handler extends DefaultHandler {
        private final Cloud cloud;
        private final File file;
        final StringBuilder buf = new StringBuilder();
        final List<String> subjects = new ArrayList<String>();
        final List<String> coverage = new ArrayList<String>();
        final List<String> source   = new ArrayList<String>();
        final Map<String, String> fields  = new HashMap<String, String>();

        Handler(Cloud cloud, File file) {
            this.cloud = cloud;
            this.cloud.setProperty(org.mmbase.streams.CreateCachesProcessor.NOT, "no");
            this.cloud.setProperty(org.mmbase.streams.DeleteCachesProcessor.NOT, "no");
            this.cloud.setProperty(org.mmbase.datatypes.processors.BinaryFile.DISABLE_DELETE, "disable");
            this.file = file;
        }


        @Override public void characters(char[] ch, int start, int length)  {
            buf.append(new String(ch, start, length));
        }

        String getUrl(String dirName, String fileName) {
            return dirName + "/" + fileName;
        }

        Node getMediaSource(String fileName) {
            Node n = null;
            for (String dirName : dirNames) {
                String url = getUrl(dirName, fileName);
                n = SearchUtil.findNode(cloud, "mediasources", "url", url);
                break;
            }
            if (n == null) {
                log.service("No node found for " + fileName);
            }
            return n;
        }

        @Override
        public void  startElement(String uri, String localName, String qName, Attributes attributes) {


        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName)  {
            if (localName.equals("dc")) {
                try {
                    final String identifier = fields.get("identifier");

                    File directory = file.getParentFile();
                    log.info("Looking in " + directory + " for " + identifier);
                    FilenameFilter filter = new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                File f = new File(dir, name);
                                return ! file.equals(f) && ResourceLoader.getName(name).equals(identifier);
                            }
                        };
                    Node mediaFragment = null;
                    for (File subFile : directory.listFiles(filter)) {
                        log.info("Importing  " + subFile.getName());

                        Node mediaSource = getMediaSource(subFile.getName());
                        if (mediaSource != null && recreate) {
                            log.info("Deleting " + mediaSource.getNumber());
                            mediaSource.delete(true);
                            mediaSource = null;
                        }
                        if (mediaSource == null) {
                            mediaSource   = cloud.getNodeManager("videostreamsources").createNode();
                            mediaSource.setValueWithoutProcess("url", getUrl(dirNames[0], subFile.getName()));
                            mediaSource.commit();
                            log.info("Created " + mediaSource.getNodeManager().getName() + " " + mediaSource.getNumber());
                        } else {
                            if (mediaFragment == null) {
                                mediaFragment = mediaSource.getNodeValue("mediafragment");
                                if (mediaFragment != null) {
                                    log.info("Found mediafragment " + mediaFragment.getNodeManager().getName() + " " + mediaFragment.getNumber());
                                }
                            }
                        }
                        if (mediaFragment == null) {
                            mediaFragment = cloud.getNodeManager("videofragments").createNode();
                            mediaFragment.commit();
                            log.info("Created mediafragment " + mediaFragment.getNumber());
                        }

                        mediaSource.setNodeValue("mediafragment", mediaFragment);
                        log.info("Found " + mediaSource);
                        if (subFile.getName().endsWith(".ogg")) {
                            // They mean theora (.ogv)
                            mediaSource.setIntValue("format", Format.OGV.getValue());
                        }
                        Node mediaProvider = cloud.getNode("default.provider");
                        log.info("mppp " + mediaProvider);
                        mediaSource.setNodeValue("mediaprovider", mediaProvider);
                        mediaSource.setLongValue("filesize", subFile.length());
                        mediaSource.commit();

                        if (mediaSource.isNull("width")) {// || ! file.isDirectory()) {
                            try {
                                Recognizer recognizer = new FFMpegRecognizer().clone();
                                Analyzer a = new FFMpegAnalyzer();
                                ChainedLogger chain = new ChainedLogger(log);
                                chain.addLogger(new AnalyzerLogger(a, mediaSource, null));
                                recognizer.analyze(subFile.toURI(), chain);
                                a.ready(mediaSource, null);
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }

                    }
                    if (mediaFragment != null) {
                        mediaFragment.setStringValue("keywords", Casting.toString(subjects));
                        mediaFragment.setStringValue("coverage", Casting.toString(coverage));
                        if (mediaFragment.getNodeManager().hasField("publisher")) {
                            mediaFragment.setStringValue("publisher", AssetImporter.this.publisher);
                        }

                        // Should we not use the dc:source field?
                        //mediaFragment.setStringValue("source",   Casting.toString(source));

                        // according to http://dublincore.org/documents/dcmi-terms/#terms-source
                        // The described resource may be derived from the related resource in whole or in part. Recommended best practice is to identify the related resource by means of a string conforming to a formal identification system.
                        mediaFragment.setStringValue("source", file.getName());

                        String title = fields.get("title");
                        Pattern pattern = Pattern.compile("(.*?):\\s*(Weeknummer.*)");
                        Matcher matcher = pattern.matcher(title);
                        if (matcher.matches()) {
                            title = matcher.group(1).trim();
                            mediaFragment.setStringValue("subtitle", matcher.group(2).trim());
                        } else {
                            title = title.trim();
                        }
                        title = title.charAt(0) + title.substring(1).toLowerCase();
                        mediaFragment.setStringValue("title", title);

                        String text = fields.get("description");
                        String sentence = "Bioscoopjournaals waarin Nederlandse onderwerpen van een bepaalde week worden gepresenteerd.";
                        String pattern2 = ".*" + sentence + ".*";
                        if (text.matches(pattern2)) {
                            text = text.replaceAll(sentence, "");
                            mediaFragment.setStringValue("intro", sentence);
                        }

                        mediaFragment.setStringValue("body", text);
                        mediaFragment.setStringValue("language", "nl");

                        {
                            String[] entries = fields.get("format").split(":");
                            long length = 1000 * ( Integer.parseInt(entries[2]) + 60 * ( Integer.parseInt(entries[1]) + 60 * Integer.parseInt(entries[0])));
                            mediaFragment.setLongValue("length", length);
                        }
                        try {
                            //mediaFragment.setValueWithoutProcess("created", DATEFORMAT.parse(fields.get("date")));
                            mediaFragment.setValueWithoutProcess("date", DATEFORMAT.parse(fields.get("date")));
                        } catch (ParseException pe) {
                            log.error(pe.getMessage());
                        }
                        mediaFragment.commit();
                        log.info("Matched mediafragment " + mediaFragment.getNumber() + " " + mediaFragment.getStringValue("title"));
                    } else {
                        log.warn("No files found, ignoring this");
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
                subjects.clear();
                coverage.clear();
                fields.clear();
            } else if (STRINGS.matcher(localName).matches()) {
                fields.put(localName, buf.toString());
            } else if (localName.equals("subject")) {
                subjects.add(buf.toString());
            } else if (localName.equals("coverage")) {
                coverage.add(buf.toString());
            } else if (localName.equals("source")) {
                source.add(buf.toString());
            } else {
                log.warn("Unrecognized tag " + localName);
            }
            buf.setLength(0);
        }

    }


    protected void read(Cloud cloud, InputSource source) throws IOException, SAXException, java.net.URISyntaxException {
        log.service("Reading " + source.getSystemId());
        XMLReader parser = XMLReaderFactory.createXMLReader();
        Handler handler = new Handler(cloud, new File(new java.net.URI(source.getSystemId()).toURL().getFile()));
        parser.setContentHandler(handler);
        parser.setErrorHandler(new ErrorHandler(true, ErrorHandler.FATAL_ERROR));
        parser.parse(source);
    }



    public static void read(Cloud cloud, Logger logger) throws IOException, SAXException, InterruptedException , java.net.URISyntaxException {
        if (running != null) {
            running.addLogger(logger);
            logger.service("Already running, Joining " + running);
            synchronized(AssetImporter.class) {
                while (running != null) {
                    AssetImporter.class.wait();
                }
            }
        } else {
            synchronized(AssetImporter.class) {
                try {
                    running = new AssetImporter();
                    running.addLogger(logger);
                    running.read(cloud);
                } finally {
                    running.log.info("Ready, notifying");
                    running = null;
                    AssetImporter.class.notifyAll();
                }
            }
        }
    }


    private String fileName = null;
    public void setFile(String f) {
        fileName = f;
    }

    private boolean recreate = false;

    public void setRecreate(boolean r) {
        recreate = r;
    }

    private String publisher = null;

    public void setPublisher(String p) {
        publisher = p;
    }

    Pattern XML_PATTERN = Pattern.compile(".*\\.xml$");
    public void read(Cloud cloud) throws IOException, SAXException,java.net.URISyntaxException {
        File file = fileName == null ? new File(FileServlet.getDirectory(), dirNames[0]) : new File(fileName);
        if (file.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        File f = new File(dir, name);
                        return XML_PATTERN.matcher(f.toString()).matches();
                    }
                };
            File [] files = file.listFiles(filter);
            for (File xml : files) {
                InputSource source = new InputSource(new FileInputStream(xml));
                source.setSystemId(xml.toURI().toString());
                read(cloud, source);
            }
        } else {
            InputSource source = new InputSource(new FileInputStream(file));
            source.setSystemId(file.toURI().toString());
            read(cloud, source);
        }
    }



    public void run() {
        try {

            Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
            read(cloud);
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        } catch (SAXException se) {
            log.error(se.getMessage(), se);
        } catch (java.net.URISyntaxException u) {
            log.error(u.getMessage(), u);
        }

    }

    protected void deleteImport(Cloud cloud, String dir) {
        NodeManager mediasources = cloud.getNodeManager("mediasources");
        NodeQuery q = mediasources.createQuery();
        Queries.addConstraint(q, Queries.createConstraint(q, "url", Queries.getOperator("LIKE"), dir + "%"));
        for (Node source : mediasources.getList(q)) {
            Node mediaFragment =  source.getNodeValue("mediafragment");
            if (mediaFragment != null) {
                mediaFragment.delete(true);
            }
            source.delete(true);
        }


    }

    /**
     * Uses RMMCI to test this class on a running instance. Normally the run() method is scheduled
     * using crontab.
     */
    public static void main(String[] argv) throws IOException, SAXException, java.net.URISyntaxException {
        LOG.setLevel(Level.SERVICE);
        String localhost =  java.net.InetAddress.getLocalHost().getHostName();
        Cloud cloud = ContextProvider.getCloudContext("rmi://" + localhost + ":1111/remotecontext").getCloud("mmbase", "class", null);
        AssetImporter assets = new AssetImporter();
        if (argv.length > 0) {
            assets.setFile(argv[0]);
            if (! new File(argv[0]).isDirectory()) {
                assets.setRecreate(true);
            }
            assets.setPublisher("Nederlands Instituut voor Beeld en Geluid / NOS");
        }

        assets.read(cloud);
        //assets.deleteImport(cloud, "B&G");
    }

}
