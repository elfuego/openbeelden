package eu.openimages.mediawiki;

import com.google.gson.*;
import com.google.gson.stream.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import java.io.*;
import java.util.*;
/*
import org.mmbase.util.logging.java.Impl;
import java.util.logging.Logger;
*/

/**
 * @author Michiel Meeuwissen
 * @version $Id: $
 */

public class Exporter {
    private Logger log = Logging.getLoggerInstance(Exporter.class);
    private static final String PYTHON                =  "python";
    private static final String UPLOADER              = "fancy_uploader";
    private static final String DEFAULT_USERNAME      = "default_username";
    private static final String DEFAULT_PASSWORD      = "default_password";
    private static final String DEFAULT_BODY_TEMPLATE = "default_body_template";

    private Map<String, String> settings;

    private Map<String, String> getSettings() throws IOException {
        if (settings == null) {
            settings = new HashMap<String, String>();
            InputStream configStream = ResourceLoader.getConfigurationRoot().getResourceAsStream("exporter.json");
            JsonReader reader = new JsonReader(new InputStreamReader(configStream, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                settings.put(reader.nextName(), reader.nextString());
            }
            reader.endObject();
        }
        return settings;
    }

    private String userName;
    private String password;
    private String bodyTemplate;
    private File   uploadFile;
    private final Map<String, String> metaData = new HashMap<String, String>();

    public void setUserName(String u) {
        if (u != null) {
            userName = u;
        }
    }
    private String getUserName() throws IOException {
        return userName == null ? getSettings().get(DEFAULT_USERNAME) : userName;
    }
    public void setPassword(String w) {
        if (w != null) {
            password = w;
        }
    }
    private String getPassword() throws IOException {
        return password == null ? getSettings().get(DEFAULT_PASSWORD) : password;
    }

    public void setBodyTemplate(String b) {
        bodyTemplate = b;
    }
    private String getBodyTemplate() throws IOException {
        return bodyTemplate == null ? getSettings().get(DEFAULT_BODY_TEMPLATE) : bodyTemplate;
    }
    public void setProperty(String name, String value) {
        metaData.put(name, value);

    }
    public void setProperty(String name, String value, Locale loc) {
        metaData.put(name + ":" + loc, value);
    }


    public void setFile(File f) {
        uploadFile = f;
    }

    public void setLogger(Logger l) {
        log = l;
    }

    public Map<String, String> getProperties() {
        return metaData;
    }

    protected File writeJson() throws IOException {
        File tempFile = File.createTempFile(Exporter.class.getName(), ".json");
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
        writer.beginObject();
        writer.name("username").value(getUserName());
        writer.name("password").value(getPassword());
        writer.name("body_template").value(getBodyTemplate());
        writer.name("metadata");
        writer.beginObject();
        for (Map.Entry<String, String> entry : metaData.entrySet()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
        writer.endObject();
        writer.close();
        return tempFile;
    }

    /*
{"result": "Success", "imageinfo": {"comment":
"{{subst:User:Elfuego2|owner:en=admin|intro:en=|dc_creator:en=admin|security_context:en=admin|publisher:en=admin|otype:en=68|subtitle:en=|show:en=true|offline:en=14
January 2111 00:00|contributor:en=|created:en=14 January 2011
13:11|title=basic|relation:en=|coverage:en=|id=525|language:en=en|body:en=|objecttype:en=videofragments|creator:en=admin|length:en=10560|lastmodified:en=14
January 2011 13:11|keywords:en=|source:en=|stop:en=|start:en=0|date:en=14 January 2011
13:11|lastmodifier:en=admin|extension=ogv|online:en=14 January 2011 00:00|project=Open
Images|title:en=basic|identifier:en=|number:en=525|subst=subst:}}", "sha1": "ba86601c405c66f63bb7663ddb73f4d183dfe6fe",
"bitdepth": 0, "url": "http://upload.wikimedia.org/wikipedia/test/9/9a/Basic_-_Open_Images_-_525.ogv", "timestamp":
"2011-01-14T23:48:18Z", "metadata": [{"name": "version", "value": 2}, {"name": "streams", "value": [{"name": 1605321751,
"value": [{"name": "serial", "value": 1605321751}, {"name": "group", "value": 0}, {"name": "type", "value": "Theora"},
{"name": "vendor", "value": "Xiph.Org libtheora 1.1 20090822 (Thusnelda)"}, {"name": "length", "value": 10.56}, {"name":
"size", "value": 543047}, {"name": "header", "value": [{"name": "VMAJ", "value": 3}, {"name": "VMIN", "value": 2},
{"name": "VREV", "value": 1}, {"name": "FMBW", "value": 32}, {"name": "FMBH", "value": 27}, {"name": "PICW", "value":
512}, {"name": "PICH", "value": 418}, {"name": "PICX", "value": 0}, {"name": "PICY", "value": 8}, {"name": "FRN",
"value": 25}, {"name": "FRD", "value": 1}, {"name": "PARN", "value": 1}, {"name": "PARD", "value": 1}, {"name": "CS",
"value": 0}, {"name": "NOMBR", "value": 0}, {"name": "QUAL", "value": 32}, {"name": "KFGSHIFT", "value": 6}, {"name":
"PF", "value": 0}, {"name": "NSBS", "value": 336}, {"name": "NBS", "value": 5184}, {"name": "NMBS", "value": 864}]},
{"name": "comments", "value": [{"name": "ENCODER", "value": "ffmpeg2theora-0.25"}, {"name": "SOURCE_OSHASH", "value":
"47c20419fabaaabd"}]}]}, {"name": 437718618, "value": [{"name": "serial", "value": 437718618}, {"name": "group",
"value": 0}, {"name": "type", "value": "Vorbis"}, {"name": "vendor", "value": "Xiph.Org libVorbis I 20090709"}, {"name":
"length", "value": 10.579591836735}, {"name": "size", "value": 89404}, {"name": "header", "value": [{"name":
"vorbis_version", "value": 0}, {"name": "audio_channels", "value": 2}, {"name": "audio_sample_rate", "value": 44100},
{"name": "bitrate_maximum", "value": 0}, {"name": "bitrate_nominal", "value": 80000}, {"name": "bitrate_minimum",
"value": 0}, {"name": "blocksize_0", "value": 8}, {"name": "blocksize_1", "value": 11}, {"name": "framing_flag",
"value": 0}]}, {"name": "comments", "value": [{"name": "ENCODER", "value": "ffmpeg2theora-0.25"}, {"name":
"SOURCE_OSHASH", "value": "47c20419fabaaabd"}]}]}]}, {"name": "length", "value": 10.579591836735}], "height": 418,
"width": 512, "mime": "application/ogg", "user": "Mihxil", "descriptionurl":
"http://test.wikipedia.org/wiki/File:Basic_-_Open_Images_-_525.ogv", "size": 632760}, "filename":
"Basic_-_Open_Images_-_525.ogv"}
    */

    protected String getUrl(String result) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(result));
        reader.beginObject();
        try {
            while (reader.hasNext()) {
                String key   = reader.nextName();
                if ("result".equals(key)) {
                    String value = reader.nextString();
                    if (! "Success".equals(value)) {
                        throw new RuntimeException(result);
                    }
                } else if ("imageinfo".equals(key)) {
                    reader.beginObject();
                    while(reader.hasNext()) {
                        String imageInfoKey = reader.nextName();
                        if ("descriptionurl".equals(imageInfoKey)) {
                            return reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
        throw new RuntimeException("No url found in " + result);
    }

    public String export() throws IOException, ProcessException, InterruptedException  {
        File tempFile = writeJson();
        log.info("" + tempFile + " " + metaData);
        tempFile.deleteOnExit();


        StringBuilderWriter result = new StringBuilderWriter(new StringBuilder());
        StringBuilderWriter errors = new StringBuilderWriter(new StringBuilder());
        long returnCode = CommandExecutor.execute(new ByteArrayInputStream(new byte[0]),
                                                  new WriterOutputStream(new ChainedWriter(result, new LoggerWriter(log, Level.DEBUG)), "UTF-8"),
                                                  new WriterOutputStream(new ChainedWriter(errors, new LoggerWriter(log, Level.ERROR)),   "UTF-8"),
                                                  new CommandExecutor.Method(),
                                                  new String[0],
                                                  settings.get(PYTHON),
                                                  settings.get(UPLOADER),
                                                  tempFile.toString(), uploadFile.toString());
        tempFile.delete();
        if (log.isDebugEnabled()) {
            log.debug("returnCode " + returnCode);
            log.debug("result " + result);
            log.debug("errors " + errors);
        }
        if (errors.getBuffer().length() > 0) {
            throw new IOException(errors.toString());
        }
        return getUrl(result.toString());


    }

    public static void main(String[] arg) throws Exception {
        File file = new File(arg[0]);
        Exporter exporter = new Exporter();
        //System.out.println("settings: " + exporter.getSettings());

        exporter.setFile(file);
        exporter.setProperty("title", "hoi");
        exporter.setProperty("id", "1234");
        exporter.setProperty("extension", "png");
        exporter.setProperty("project", "Open Beelden");
        exporter.export();
    }
}