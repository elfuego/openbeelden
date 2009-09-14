<%@ page language="java" contentType="text/html" session="false" 
	import="java.util.regex.*"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" 
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<mm:content expires="0" type="text/html" escaper="none">
<mm:cloud>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nl">
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8" />
  <title>Test regex</title>
</head>
<body>
<mm:import externid="text" jspvar="text">Input #0, image2, from 'grumpies.png':</mm:import>

<form>
<fieldset>
  <textarea id="text" name="text" rows="4" cols="80">${text}</textarea><br />
  <input name="action" type="submit" value="Submit" />
</fieldset>
</form>
<hr />
<pre>
PHP: if (!preg_match('/Stream #(?:[0-9\.]+)(?:.*)\: Video: (?P<videocodec>.*) (?P<width>[0-9]*)x(?P<height>[0-9]*)/',implode('\n',$output),$matches))
$pattern = "/Audio: (.*), ([0-9]*) Hz, (stereo|mono|([0-9]+) channels)/";

C#:
// these are connected:
// 0) this is base for getting stream info
"Stream #(?<number>\d+?\.\d+?)(\((?<language>\w+)\))?: (?<type>.+): (?<data>.+)"
// 1) if the type is audio:
"(?<codec>\w+), (?<frequency>[\d]+) (?<frequencyUnit>[MK]?Hz), (?<chanel>\w+), (?<format>\w+)(, (?<bitrate>\d+) (?<bitrateUnit>[\w/]+))?"
// 2) if the type is video:
"(?<codec>\w+), (?<format>\w+), (?<width>\d+)x(?<height>\d+), (?<bitrate>\d+(\.\d+)?) (?<bitrateUnit>[\w\(\)]+)"

Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'presto.mp4':
  Duration: 00:00:10.56, start: 0.000000, bitrate: 389 kb/s
    Stream #0.0(eng): Audio: aac, 44100 Hz, 2 channels, s16
    Stream #0.1(eng): Video: mpeg4, yuv420p, 352x288 [PAR 1:1 DAR 11:9], 30 tbr, 600 tbn, 1k tbc
    
<mm:present referid="text">
<%
String input = text;
out.println("input: " + input);
//Pattern IMAGE    = Pattern.compile(".*?\\sVideo: .*?, .*?, ([0-9]+)x([0-9]+).*");

String inputRegex = "^Input #\\d+?, (.+?), from '([^']+)?.*";
Pattern INPUT = Pattern.compile(inputRegex);

String imageRegex = "^Input #\\d+?, (image\\d*), from .*?";
Pattern IMAGE = Pattern.compile(imageRegex);

// Stream #0.1(eng): Video: mpeg4, yuv420p, 352x288 [PAR 1:1 DAR 11:9], 30 tbr, 600 tbn, 1k tbc
String videoRegex = ".*?\\sVideo: (.*?), (.*?), ([0-9]+)x([0-9]+).*";
Pattern VIDEO = Pattern.compile(videoRegex);

// Stream #0.1[0x1c0]: Audio: mp2, 44100 Hz, 2 channels, s16, 192 kb/s
String audioRegex = ".*?\\sAudio: (.*?), (.*?) Hz, (stereo|mono|([0-9]+) channels), .*?";
Pattern AUDIO = Pattern.compile(audioRegex);

/* 
browserevent.ram: Unknown format 
[NULL @ 0x1804800]Unsupported video codec
*/
Pattern PATTERN_UNKNOWN     = Pattern.compile("\\s*(.*): Unknown format.*?");
Pattern PATTERN_UNSUPPORTED = Pattern.compile("\\s*(.*)Unsupported video codec.*?");

Matcher m = PATTERN_UNKNOWN.matcher(input);
if (m.matches()) {
    out.println("UNKNOWN !!");
    out.println("file: " + m.group(1));
}
m = PATTERN_UNSUPPORTED.matcher(input);
if (m.matches()) {
    out.println("UNSUPPORTED !!");
    out.println("error?: " + m.group(1));
}

Pattern DURATION = Pattern.compile("\\s*Duration: (.*?),.* bitrate:.*?");
Pattern BITRATE  = Pattern.compile("\\s*Duration: .* bitrate: (.*?) kb/s.*?");
Pattern START    = Pattern.compile("\\s*Duration: .* start: (.*?), bitrate:.*?");
Matcher durationM = DURATION.matcher(input);
if (durationM.matches()) {
    out.println("DURATION !!");
    out.println("duration: " + durationM.group(1));
    
    Matcher bitrateM = BITRATE.matcher(input);
    Matcher startM = START.matcher(input);
    if (bitrateM.matches()) {
        out.println("BITRATE !!");
        out.println("bitrate: " + bitrateM.group(1));
    }
    if (startM.matches()) {
        out.println("START !!");
        out.println("start: " + startM.group(1));
    }
}

Matcher inputM = INPUT.matcher(input);
if (inputM.matches()) {
    out.println("INPUT !!");
    String types = inputM.group(1);
    out.println("match 1:  " + types);
    out.println("   file:  " + inputM.group(2));
    
    if (types.startsWith("image")) out.println("This may be an image.");
}

Matcher imageM = IMAGE.matcher(input);
if (imageM.matches()) {
    out.println("IMAGE !!");
    out.println("match 1:  " + imageM.group(1));
}

Matcher videoM = VIDEO.matcher(input);
if (videoM.matches()) {
    out.println("VIDEO !!");
    out.println("  codec: " + videoM.group(1));
    out.println(" format: " + videoM.group(2));
    out.println("  width: " + videoM.group(3));
    out.println(" height: " + videoM.group(4));
}

Matcher audioM = AUDIO.matcher(input);
if (audioM.matches()) {
    out.println("AUDIO !!");
    out.println("   codec: " + audioM.group(1));
    out.println("   freq.: " + audioM.group(2));
    out.println("channels: " + audioM.group(3));
    
    // check again for bitrate
    Pattern AUDIO_BITRATE = Pattern.compile(".*?\\sAudio: .* (.*?) kb\\/s.*?");
    Matcher audiobitrateM = AUDIO_BITRATE.matcher(input);
    if (audiobitrateM.matches()) {
        out.println(" bitrate: " + audiobitrateM.group(1));
    }
}



%>
inputRegex used: <%= inputRegex %>
imageRegex used: <%= imageRegex %>
videoRegex used: <%= videoRegex %>
audioRegex used: <%= audioRegex %>
</pre>
</mm:present>

<h3>Sample</h3>
<pre>
ffmpeg -i filename.ext

Input #0, mpeg, from '3777.3747.mpg_beng_org.mpg':
  Duration: 00:02:26.26, start: 31.444267, bitrate: 1374 kb/s
    Stream #0.0[0x1e0]: Video: mpeg1video, yuv420p, 352x288 [PAR 178:163 DAR 1958:1467], 1152 kb/s, 25 tbr, 90k tbn, 25 tbc
    Stream #0.1[0x1c0]: Audio: mp2, 44100 Hz, 2 channels, s16, 192 kb/s

Input #0, ogg, from 'matrix.ogg':
  Duration: 00:00:46.65, start: 0.000000, bitrate: 766 kb/s
    Stream #0.0: Invalid Codec type -1
    Stream #0.1: Video: theora, yuv420p, 640x360, 23.98 tbr, 23.98 tbn, 23.98 tbc
    Stream #0.2: Audio: vorbis, 44100 Hz, 2 channels, s16, 80 kb/s
At least one output file must be specified

Input #0, flv, from 'basic.flv':
  Duration: 00:00:14.48, start: 0.000000, bitrate: N/A
    Stream #0.0: Video: flv, yuv420p, 480x360, 25 tbr, 1k tbn, 1k tbc
    Stream #0.1: Audio: aac, 44100 Hz, 2 channels, s16

Input #0, rm, from 'Real_Media.rm':
  Duration: 00:00:35.29, start: 0.000000, bitrate: 156 kb/s
    Stream #0.0: Audio: cook, 44100 Hz, 2 channels, s16, 32 kb/s
    Stream #0.1: Video: rv40, yuv420p, 320x240, 117 kb/s, 12 tbr, 1k tbn, 12 tbc

Input #0, mpeg, from 'BG_9383.mpg':
  Duration: 00:02:39.21, start: 66.370867, bitrate: 1374 kb/s
    Stream #0.0[0x1e0]: Video: mpeg1video, yuv420p, 352x288 [PAR 178:163 DAR 1958:1467], 1152 kb/s, 25 tbr, 90k tbn, 25 tbc
    Stream #0.1[0x1c0]: Audio: mp2, 44100 Hz, 2 channels, s16, 192 kb/s

Input #0, asf, from 'youcanviewvideo.wmv':
  Duration: 00:00:13.76, start: 5.000000, bitrate: 114 kb/s
    Stream #0.0(eng): Audio: wmav2, 16000 Hz, 1 channels, s16, 16 kb/s
    Stream #0.1(eng): Video: wmv3, yuv420p, 320x240, 136 kb/s, 15 tbr, 1k tbn, 1k tbc

Input #0, ogg, from 'zztop.ogg':
  Duration: 00:00:03.47, start: 0.000000, bitrate: 215 kb/s
    Stream #0.0: Audio: vorbis, 44100 Hz, 2 channels, s16, 192 kb/s

Input #0, wav, from 'some.wav':
  Duration: 00:00:02.94, bitrate: 384 kb/s
    Stream #0.0: Audio: pcm_s24le, 8000 Hz, 2 channels, s16, 384 kb/s

Input #0, mp3, from 'zztop.mp3':
  Duration: 00:00:03.46, start: 0.000000, bitrate: 191 kb/s
    Stream #0.0: Audio: mp3, 44100 Hz, 2 channels, s16, 192 kb/s

Input #0, image2, from 'grumpies.png':
  Duration: 00:00:00.04, start: 0.000000, bitrate: N/A
    Stream #0.0: Video: png, rgb24, 322x243, 25 tbr, 25 tbn, 25 tbc

Input #0, image2, from 'bodem.gif':
  Duration: 00:00:00.04, start: 0.000000, bitrate: N/A
    Stream #0.0: Video: gif, pal8, 464x8, 25 tbr, 25 tbn, 25 tbc    
</pre>
</body>
</html>
</mm:cloud></mm:content>
