/*
  Inits and controls the video player based on the html5 videotag. Depends on jquery. It enables
  the use of three players in a generic way: video-tag, java player cortado (for ogg) and flash.
  Sifts through the sources provided by the video-tag to find a suitable player.
  This script borrows heavily from the rather brilliant one used at Steal This Footage which enables
  a multitude of players: http://footage.stealthisfilm.com/

  @author: André van Toly
  @version: 0.1
  @params:
    id - id of the element that contains the video-tag
    config - configuration parameters
        'dir' : directory the plugins live
        'jar' : JAR file of Cortado
        'flash' : location of flowplayer.swf

  @changes: initial version
*/

var player;

function createPlayer(id, config) {
    var videotag = $('#' + id + ' video:first');
    var sources = $('#' + id).find('source');
    var types = $.map(sources, function(i) {
        return $(i).attr('type');
    });
    var urls = $.map(sources, function(i) {
        return $(i).attr('src');
    });

    if (urls.length == 0) {
        //console.log("No source elements found");
        urls[0] = $(videotag).attr('src');
        types[0] = "unknown";
    }

    if (videotag != undefined) {
        var selectedPlayer = selectPlayer(types, urls);
        if (selectedPlayer.type == 'video') {
            player = new VideoPlayer();
        } else if (selectedPlayer.type == 'cortado') {
            player = new CortadoPlayer();
        } else if (selectedPlayer.type == 'flash') {
            player = new FlowPlayer();
        } else {
            player = new Player();
        }
        //console.log("type/url: " + selectedPlayer.type + " / " + selectedPlayer.url);
        //player.info = "playing: " + selectedPlayer.url + " / " + selectedPlayer.type;
        return player.init(id, selectedPlayer.url, config);
    }
}

function Player() {
    this.myname = "super";
}

Player.prototype._init = function(id, url, config) {
    this.player = $('#' + id + ' video')[0];        // the first video tag it finds
    this.url = url;
    /* if (this.urls.length == 0) this.urls[0] = $(this.player).attr('src'); */
    this.poster = $(this.player).attr('poster');
    if ($(this.player).attr('autoplay') == undefined) { // html5 can just have <video autoplay />
        this.autoplay = false;
    } else {
        this.autoplay = $(this.player).attr('autoplay');
    }
    this.width = $(this.player).attr('width');
    this.height = $(this.player).attr('height');
    this.state = 'init';
    return this.player;
}

Player.prototype.init = function(id, url, config) {
    return this._init(id, config);
}
Player.prototype.play = function() {
    this.state = 'play';
}
Player.prototype.pause = function() {
    this.state = 'pause';
}
Player.prototype.position = function() { }
Player.prototype.info = function() { return "Playing: " + this.url; }

function VideoPlayer() {
    this.myname = "videoplayer";
}
VideoPlayer.prototype = new Player();
VideoPlayer.prototype.init = function(id, url, config) {
    this._init(id, config); // just init and pass it along
    this.url = url;
    //console.log("video!" + url + ", img: " + this.poster);
    this.player.controls = false;
    return this.player;
}
VideoPlayer.prototype.play = function() {
    this.player.autoplay = true;
    this.player.play();
    this.state = 'play';
}

VideoPlayer.prototype.pause = function() {
    this.player.pause();
    this.state = 'pause';
}

VideoPlayer.prototype.position = function() {
    try {
        return this.player.currentTime;
    } catch(err) {
        //console.log("Error: " + err);
    }
    return -1;
}
VideoPlayer.prototype.info = function() {
    //return "Playing: " + this.url;
}

function CortadoPlayer() {
    this.myname = "cortadoplayer";
}
CortadoPlayer.prototype = new Player();
CortadoPlayer.prototype.init = function(id, url, config) {
    this._init(id, config);
    this.url = url;
    //console.log("cortado! " + url + ", img: " + this.poster);
    var jar = config.dir + "/" + config.jar;
    this.player = document.createElement('object'); // create new element!
    $(this.player).attr('classid', 'java:com.fluendo.player.Cortado.class');
    $(this.player).attr('style', 'display:block;width:' + this.width + 'px;height:' + this.height + 'px;');
    this.player.type = 'application/x-java-applet';
    $(this.player).attr('archive', jar);
    $(this.player).attr('width', this.width);
    $(this.player).attr('height', this.height);

    var params = {
        'code' : 'com.fluendo.player.Cortado.class',
        'archive' : jar,
        'url': url,
         // 'local': 'false',
        'keepAspect': 'true',
        'showStatus' : 'hide',
        'video': 'true',
        'audio': 'true',
        'seekable': 'auto',
        'showStatus': 'hide',
        'autoPlay': this.autoplay,
        'bufferSize': '4096',
        'bufferHigh': '25',
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
    this.player.doPlay();
    this.state = 'play';
}

CortadoPlayer.prototype.pause = function() {
    this.player.doPause();
    this.state = 'pause';
//     try {
//         this.player.doStop();
//     } catch(err) { }
}
CortadoPlayer.prototype.position = function() {
    return this.player.getPlayPosition()
}
CortadoPlayer.prototype.info = function() {
    //return "Playing: " + this.url";
}

function FlowPlayer() {
    this.myname = "flowplayer";
}
FlowPlayer.prototype = new Player();
FlowPlayer.prototype.init = function(id, url, config) {
    this._init(id, config);
    //console.log("flash! " + url);
    this.url = url;
    var flwplayer = config.dir + "/" + config.flash;
    this.player = $f(id, { src : flwplayer }, {
        clip: {
            url: url,
            autoPlay: true,
            autoBuffering: true
        },
        plugins: { controls: null }
    });

    return this.player;
}

FlowPlayer.prototype.play = function() {
    $f(0).play();
    this.state = 'play';
}

FlowPlayer.prototype.pause = function() {
    $f(0).pause();
    this.state = 'pause';
}

FlowPlayer.prototype.position = function() {
    return this.player.getTime();
}

FlowPlayer.prototype.info = function() {
    //return "Playing: " + this.url;
}

/* Selects which player to use. Adapt this to change the prefered order, in this video, cortado, flash. */
function selectPlayer(types, urls) {
    var proposal = new Object();
    var probably = canPlayVideo(types, urls);
    if (probably != undefined) {
        proposal.type = "video";
        proposal.url = probably;
    }

    if (proposal.type == undefined) {
        probably = canPlayCortado(types, urls);
        if (probably != undefined && (supportMimetype('application/x-java-applet') || navigator.javaEnabled())) {
            proposal.type = "cortado";
            proposal.url = probably;
        } else {
            proposal.type = "flash";
            var flash_url;
            for (var i = 0; i < types.length; i++) {
                //console.log("testing 4 f: " + types[i]);
                if (types[i].indexOf("video/mp4") > -1 || types[i].indexOf("video/flv") > -1 || types[i].indexOf("video/mpeg") > -1) {
                    flash_url = urls[i];
                    //console.log("4f: " + flash_url);
                }
            }

            proposal.url = flash_url;

        }
    }
    //console.log("prop: " + proposal.type + ":" + proposal.url);
    return proposal;
}

/*
 * Returns url it expects to be able to play
*/
function canPlayCortado(types, urls) {
    var url;
    for (var i = 0; i < types.length; i++) {
        //console.log("testing: " + types[i]);
        if (types[i].indexOf("video/ogg") > -1 ||
            types[i].indexOf("application/x-ogg") > -1 ||
            types[i].indexOf("audio/ogg") > -1) {
            url = urls[i];
            break;
        }
    }
    //console.log("canPlayCortado: " + url);
    return url;
}

/*
 * Returns url it expects to be able to play
*/
function canPlayVideo(types, urls) {
    var probably;
    var el = document.createElement("video");
    if (el.canPlayType) {
        for (var i = 0; i < types.length; i++) {
            //console.log("testing v: " + types[i] + ": " + el.canPlayType(types[i])+ ":" + urls[i]);
            /*
             http://www.whatwg.org/specs/web-apps/current-work/multipage/video.html#dom-navigator-canplaytype
             Firefox 3.5 is very strict about this and does not return 'probably', but does on 'maybe'.
            */
            //console.log(el.canPlayType( types[i] ));
            if (el.canPlayType( types[i] ) == "probably") {
                return urls[i]; // this is the best we can do

                //console.log("found one: " + probably);
            }
            if (el.canPlayType( types[i] ) == "maybe") {
                probably = urls[i]; // if we find nothing better
            }
        }
        if (probably != undefined) {
            return probably;
        }

        //console.log("No source elements found");

        // last fall back, the 'src' attribute itself.
        if ($('video').length) {
            var url = $('video').attr('src');
            if (url != undefined &&
                (url.lastIndexOf('.mp4') > -1 || url.lastIndexOf('.h264') > -1
                 )) {
                //probably = "video/mp4";
                probably = url;
            }
        }

    }
    
    return probably;
}

function supportMimetype(mt) {
	var support = false;
    if (navigator.mimeTypes && navigator.mimeTypes.length > 0) {
		for (var i = 0; i < navigator.mimeTypes.length; i++) {
			if (navigator.mimeTypes[i].type.indexOf(mt) > -1) {
				support = true;
    		}
		}
    }
	return support;
}
