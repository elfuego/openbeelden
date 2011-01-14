package eu.openimages.mediawiki;

import com.google.gson.*;
import com.google.gson.stream.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.logging.java.Impl;
import org.mmbase.util.logging.*;
import org.mmbase.util.WriterOutputStream;
import org.mmbase.util.ChainedWriter;
import org.mmbase.util.StringBuilderWriter;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Michiel Meeuwissen
 * @version $Id: $
 */

public class Exporter {
    private Logger log = Logger.getLogger(Exporter.class.getName());
    private static final String PYTHON                =  "python";
    private static final String UPLOADER              = "fancy_uploader";
    private static final String DEFAULT_USERNAME      = "default_username";
    private static final String DEFAULT_PASSWORD      = "default_password";
    private static final String DEFAULT_BODY_TEMPLATE = "default_body_template";

    private Map<String, String> settings;

    private Map<String, String> getSettings() throws IOException {
        if (settings == null) {
            settings = new HashMap<String, String>();
            InputStream configStream = Exporter.class.getClassLoader().getResourceAsStream("exporter.json");
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
    private String bodyTemplate = "User:Elfuego2";
    private File   uploadFile;
    private final Map<String, String> metaData = new HashMap<String, String>();

    public void setUserName(String u) {
        if (u != null) {
            userName = u;
        }
    }
    private String getUserName() {
        return userName == null ? getSettings().get(DEFAULT_USERNAME) : userName;
    }
    public void setPassword(String w) {
        if (w != null) {
            password = w;
        }
    }
    private String getPassword() {
        return password == null ? getSettings().get(DEFAULT_PASSWORD) : password;
    }

    public void setBodyTemplate(String b) {
        bodyTemplate = b;
    }
    private String getBodyTemplate() {
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

    public String export() throws IOException, ProcessException, InterruptedException  {
        File tempFile = writeJson();
        log.info("" + tempFile + " " + metaData);
        tempFile.deleteOnExit();


        StringBuilderWriter result = new StringBuilderWriter(new StringBuilder());
        StringBuilderWriter errors = new StringBuilderWriter(new StringBuilder());
        long returnCode = CommandExecutor.execute(new ByteArrayInputStream(new byte[0]),
                                                  new WriterOutputStream(new ChainedWriter(result, new LoggerWriter(new Impl(log), Level.SERVICE)), "UTF-8"),
                                                  new WriterOutputStream(new ChainedWriter(errors, new LoggerWriter(new Impl(log), Level.ERROR)),   "UTF-8"),
                                                  new CommandExecutor.Method(),
                                                  new String[0],
                                                  settings.get(PYTHON),
                                                  settings.get(UPLOADER),
                                                  tempFile.toString(), uploadFile.toString());
        tempFile.delete();
        log.fine("" + result);
        log.info("returnCode " + returnCode);
        log.info("result " + result);
        log.info("errors " + errors);
        if (errors.getBuffer().length() > 0) {
            throw new IOException(errors.toString());
        }
        return result.toString();


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