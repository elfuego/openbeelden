/*
  Inits and controls the video player based on the html5 videotag. Depends on jquery. It enables
  the use of three players in a generic way: video-tag, java player cortado (for ogg) and flash.
  Sifts through the sources provided by the video-tag to find a suitable player.
  This script borrows heavily from the rather brilliant one used at Steal This Footage which enables
  a multitude of players (but defies MSIE ;-) http://footage.stealthisfilm.com/

  @author: André van Toly
  @version: 0.3
  @params:
    id - id of the element that contains the video-tag
    config - configuration parameters
        'dir' : directory the plugins live in
        'jar' : JAR file of Cortado
        'flash' : location of flowplayer.swf

  @changes: trying to support the audio tag some more (with thanks to Cannoball Adderley)
*/

var player;

function createPlayer(id, config) {
    var mediatag = findMediatag(id);
    var sources = $('#' + id).find('source');
    var types = $.map(sources, function(i) {
        return $(i).attr('type');
    });
    var urls = $.map(sources, function(i) {
        return $(i).attr('src');
    });

    if (urls.length == 0) {
        urls[0] = $(mediatag).attr('src');
        types[0] = "unknown";
    }

    if (mediatag != undefined) {
        var selectedPlayer = selectPlayer(mediatag, types, urls);
        if (selectedPlayer.type == 'media') {
            player = new MediaPlayer();
        } else if (selectedPlayer.type == 'cortado') {
            player = new CortadoPlayer();
        } else if (selectedPlayer.type == 'msie_cortado') {
            player = new MSCortadoPlayer();
        } else if (selectedPlayer.type == 'flash') {
            player = new FlowPlayer();
        } else {
            player = new Player();
        }
        player.info = selectedPlayer.type + ": " + selectedPlayer.url;
        //console.log(player.info);
        return player.init(id, selectedPlayer.url, config);
    }
}

function Player() {
    this.myname = "super";
}

Player.prototype._init = function(id, url, config) {
    var mediatag = findMediatag(id);
    this.player = mediatag.element;
    this.type = mediatag.type;
    this.url = url;
    this.id = id;
    /* if (this.urls.length == 0) this.urls[0] = $(this.player).attr('src'); */
    this.poster = $(this.player).attr('poster');
    if ($(this.player).get(0).getAttribute('autoplay') == undefined) { // html5 can just have <video autoplay />
        this.autoplay = false;
    } else {
        this.autoplay = $(this.player).get(0).getAttribute('autoplay');
    }
    if ($(this.player).get(0).getAttribute('autobuffer') == undefined) {
        this.autobuffer = false;
    } else {
        this.autobuffer = $(this.player).get(0).getAttribute('autobuffer');
    }
    if ($(this.player).get(0).getAttribute('controls') == undefined) {
        this.controls = false;
    } else {
        this.controls = $(this.player).get(0).getAttribute('controls');
    }
    this.width  = $(this.player).attr('width');
    this.height = $(this.player).attr('height');
    if (this.width  == undefined) this.width  = $("head meta[name=media-width]").attr("content");
    if (this.height == undefined) this.height = $("head meta[name=media-height]").attr("content");
    this.duration = $("head meta[name=media-duration]").attr("content"); // not a mediatag attr.
    this.state = 'init';
    this.pos = 0;
    return this.player;
}

Player.prototype.init = function(id, url, config) { }
Player.prototype.play = function() { }
Player.prototype.pause = function() { }
Player.prototype.position = function() { }
Player.prototype.info = function() { }

function MediaPlayer() {
    this.myname = "mediaplayer";
}
MediaPlayer.prototype = new Player();
MediaPlayer.prototype.init = function(id, url, config) {
    this._init(id, url, config); // just init and pass it along
    this.url = url;
    var self = this;
    this.player.addEventListener("playing", 
                                  function(ev) {
                                      self.state = 'play';
                                      followProgress();
                                  },
                                  false);
    return this.player;
}
MediaPlayer.prototype.play = function() {
    //this.player.autoplay = true;
    this.player.play();
    this.state = 'play';
}

MediaPlayer.prototype.pause = function() {
    this.player.pause();
    this.state = 'pause';
}

MediaPlayer.prototype.position = function() {
    try {
        this.pos = this.player.currentTime;
        return this.pos;
    } catch(err) {
        //console.log("Error: " + err);
    }
    return -1;
}
MediaPlayer.prototype.info = function() {
    /*  duration able in webkit, 
        unable in mozilla without: https://developer.mozilla.org/en/Configuring_servers_for_Ogg_media
    */
    //return "Duration: " + this.player.duration + " readyState: " + this.player.readyState;
}


function CortadoPlayer() {
    this.myname = "cortadoplayer";
}
CortadoPlayer.prototype = new Player();
CortadoPlayer.prototype.init = function(id, url, config) {
    this._init(id, url, config);
    this.url = url;
    var jar = config.dir + "/" + config.jar;
    var usevideo = true;
    var useheight = this.height;
    if (this.type == 'audio') {
        usevideo = false;
        useheight = 12;
    }
    
    this.player = document.createElement('object'); // create new element!
    $(this.player).attr('classid', 'java:com.fluendo.player.Cortado.class');
    $(this.player).attr('style', 'display:block;width:' + this.width + 'px;height:' + useheight + 'px;');
    $(this.player).attr('type', 'application/x-java-applet');
    $(this.player).attr('archive', jar);
    if (this.width)  $(this.player).attr('width', this.width);
    if (this.height) $(this.player).attr('height', this.height);
    var params = {
        'code' : 'com.fluendo.player.Cortado.class',
        'archive' : jar,
        'url': url,
         // 'local': 'false',
        'duration': Math.round(this.duration),
        'keepAspect': 'true',
        'showStatus' : this.controls,
        'video': usevideo,
        'audio': 'true',
        'seekable': 'auto',
        'autoPlay': this.autoplay,
        'bufferSize': '256',
        'bufferHigh': '50',
        'bufferLow': '5'
    }
    for (name in params) {
        var p = document.createElement('param');
        p.name = name;
        p.value = params[name];
        this.player.appendChild(p);
    }
    return this.player;
}

CortadoPlayer.prototype.play = function() {
    if (this.state == 'pause') {
        // impossible when duration is unknown (and not really smooth in cortado)
        // console.log("pos: " + this.pos + " pos as double: " + this.duration / this.pos);
        // this.player.doSeek(this.duration / this.pos);
        this.player.doPlay();
    } else {
        this.player.doPlay();
    }
    this.state = 'play';
}

CortadoPlayer.prototype.pause = function() {
    this.pos = this.player.getPlayPosition();
    this.player.doPause();
    this.state = 'pause';
//     try {
//         this.player.doStop();
//     } catch(err) { }
}
CortadoPlayer.prototype.position = function() {
    this.pos = this.player.getPlayPosition();
    return this.pos;
}
CortadoPlayer.prototype.info = function() {
    //return "Playing: " + this.url";
}

function MSCortadoPlayer() {
    this.myname = "msie_cortadoplayer";
}
MSCortadoPlayer.prototype = new CortadoPlayer();
MSCortadoPlayer.prototype.init = function(id, url, config) {
    this._init(id, url, config);
    /* msie (or windows java) can only load an applet from the root of a site, not a directory or context */
    var jar = config.server + config.dir + "/" + config.jar; 
    var usevideo = true;
    var useheight = this.height;
    if (this.type == 'audio') { 
        usevideo = false;
        useheight = 12;
    }
    var element = document.createElement('div');
    var obj_html = '' +
    '<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" '+
    '  codebase="http://java.sun.com/update/1.5.0/jinstall-1_5_0-windows-i586.cab" '+
    '  id="msie_cortadoplayer_' + id + '" '+
    '  allowscriptaccess="always" width="' + this.width + '" height="' + useheight + '">'+
    ' <param name="code" value="com.fluendo.player.Cortado" />'+
    ' <param name="archive" value="' + jar + '" />'+
    ' <param name="url" value="' + this.url + '" /> '+
    ' <param name="duration" value="' + Math.round(this.duration) + '" /> '+
    ' <param name="local" value="true" /> ' +
    ' <param name="keepAspect" value="false" /> ' +
    ' <param name="video" value="' + usevideo + '" /> ' +
    ' <param name="audio" value="true" /> ' +
    ' <param name="seekable" value="auto" /> '+
    ' <param name="showStatus" value="' + this.controls + '" /> '+
    ' <param name="bufferSize" value="256" /> '+
    ' <param name="bufferHigh" value="50" /> '+
    ' <param name="bufferLow" value="5" /> '+
    ' <param name="autoPlay" value="' + this.autoplay + '" /> '+
    ' <strong>Your browser does not have a Java Plug-in. <a href="http://java.com/download">Get the latest Java Plug-in here</a>.</strong>' +
    '</object>';
    $(element).html(obj_html);
    this.player = element.firstChild;
    return this.player;
}

function FlowPlayer() {
    this.myname = "flowplayer";
}
FlowPlayer.prototype = new Player();
FlowPlayer.prototype.init = function(id, url, config) {
    this._init(id, url, config);
    this.url = url;
    var flwplayer = config.dir + "/" + config.flash;
    var flwcontrols = 'flowplayer.controls-3.1.3.swf';

    /* flowplayer replaces everything in the geven element */
    var el = $('#' + id).find('div.player')[0];
    this.player = $f(el, { src : flwplayer, width : this.width, height : this.height }, {
        clip: {
            url: url,
            autoPlay: this.autoplay,
            duration: this.duration,
            scaling: 'fit',
            autoBuffering: this.autobuffer,
            bufferLength: 5
        },
        plugins: { controls: { height: 24, hideDelay: 2000, fullscreen: false } }
    });

    return this.player;
}

FlowPlayer.prototype.play = function() {
    if (this.player.getState() == 4) {
        this.player.resume();
    } else if (this.player.getState() != 3) {
        this.player.play();
    }
    this.state = 'play';
}

FlowPlayer.prototype.pause = function() {
    if (this.player.getState() == 3) this.player.pause();
    this.state = 'pause';
}

FlowPlayer.prototype.position = function() {
    this.pos = this.player.getTime();
    return this.pos;
}

FlowPlayer.prototype.info = function() {
    //return "Playing: " + this.url;
}

/* 
   Selects which player to use and returns a proposal.type and proposal.url. 
   Adapt this to change the prefered order, here the order is: video, cortado, msie_cortado flash.
*/
function selectPlayer(tag, types, urls) {
    var proposal = new Object();
    var probably = canPlayMedia(types, urls);
    if (probably != undefined) {
        proposal.type = "media";
        proposal.url = probably;
        return proposal;    // optimization
    }
    
    if (proposal.type == undefined) {
        probably = canPlayCortado(types, urls);
        if (probably != undefined && (supportMimetype('application/x-java-applet') || navigator.javaEnabled())) {
            if ($.browser.msie) {   // Argh! A browser check!
                /* IE always reports true on navigator.javaEnabled(),
                   that's why we need to check for the java plugin IE style. 
                   It needs an element with id 'clientcaps' somewhere in the page. 
                */
                var javaVersionIE = clientcaps.getComponentVersion("{08B0E5C0-4FCB-11CF-AAA5-00401C608500}", "ComponentID");
                if (javaVersionIE) {
                    proposal.type = "msie_cortado";
                    proposal.url = probably;
                }
                if (tag.type == 'audio') {      // always use cortado on msie
                    proposal.type = "msie_cortado";
                    proposal.url = probably;
                }
            } else {
                proposal.type = "cortado";
                proposal.url = probably;
            }
        }
    }
    if (proposal.type == undefined) {
        var flash_url;
        for (var i = 0; i < types.length; i++) {
            if (types[i].indexOf("video/mp4") > -1 || types[i].indexOf("video/flv") > -1
                /* || types[i].indexOf("video/mpeg") > -1 */ ) {
                proposal.url = urls[i];
                proposal.type = "flash";
            }
        }
    }
    return proposal;
}

/*
 * Returns Ogg url it expects to be able to play
*/
function canPlayCortado(types, urls) {
    var url;
    for (var i = 0; i < types.length; i++) {
        if (types[i].indexOf("video/ogg") > -1 ||
            types[i].indexOf("audio/ogg") > -1 ||
            types[i].indexOf("application/ogg") > -1 ||
            types[i].indexOf("application/x-ogg") > -1) {
            url = urls[i];
            break;
        }
    }
    return url;
}

/*
 * Returns url it expects to be able to play
*/
function canPlayMedia(types, urls) {
    //var probably;
    var vEl = document.createElement("video");
    var aEl = document.createElement("audio");
    if (vEl.canPlayType || aEl.canPlayType) {
        for (var i = 0; i < types.length; i++) {
            /*
             http://www.whatwg.org/specs/web-apps/current-work/multipage/video.html#dom-navigator-canplaytype
             Firefox 3.5 is very strict about this and does not return 'probably', but does on 'maybe'.
            */
            if (vEl.canPlayType( types[i] ) == "probably" || aEl.canPlayType( types[i] ) == "probably") {
                return urls[i]; // this is the best we can do
            }
            if (vEl.canPlayType( types[i] ) == "maybe" || aEl.canPlayType( types[i] ) == "maybe") {
                return urls[i]; // if we find nothing better
            }
        }
    }
}

function findMediatag(id) {
    var mediatag = new Object();
    mediatag.type = "video";
    mediatag.element = $('#' + id).find('video')[0];
    if (mediatag.element == undefined) {
        mediatag.type = "audio";
        mediatag.element = $('#' + id).find('audio')[0];
    }
    return mediatag;
}

function supportMimetype(mt) {
	var support = false;    /* navigator.mimeTypes is unsupported by MSIE ! */
    if (navigator.mimeTypes && navigator.mimeTypes.length > 0) {
		for (var i = 0; i < navigator.mimeTypes.length; i++) {
			if (navigator.mimeTypes[i].type.indexOf(mt) > -1) {
				support = true;
    		}
		}
    }
	return support;
}

function showInfo() {
    var text = player.info;
    var id = player.id;
	if ($('#' + id).find('div.playerinfo').length > 0) $('#' + id).find('div.playerinfo').remove();
    $('#' + id).append('<div class="playerinfo">' + text + '</div>');
}

function followProgress() {
    var pos;
    var oldpos = -1;
    var text = "00:00";
    var id = player.id;
    var progress = function() {
        pos = player.position();
        //console.log("oldpos: " + oldpos +  ", pos: " + pos + " state: " + player.state)
        if (!isNaN(pos) && pos > 0) {
            var min = Math.floor(pos / 60);
            var sec = Math.floor(pos - (min * 60));
            text = (min < 10 ? '0' + min : min) + ":" + (sec < 10 ? '0' + sec : sec);
            $('#' + id + ' ul.controls li.position').text(text);

            if (oldpos == pos) {
                player.state = 'pause';
                $('#' + id + ' ul.controls li.play').removeClass('pause');
            }
        }
        
        if (player.state == "play") {
            setTimeout(progress, 200);
            if (!isNaN(pos) && pos > 0) oldpos = pos;
            if ($('#' + id + ' ul.controls li.pause').length == 0) {
                $('#' + id + ' ul.controls li.play').addClass('pause');
            }
        }
        
    };
    progress();
    //showInfo();
}
