// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Javascript to init the player
  @author: André van Toly
  @version: 0.1
  @changes: initial version
*/

function initPlayer() {
    var id = "vplayer";
    /* these files (jar, flash) need to be in that directory */
    var config = { 
        'dir' : '${mm:link('/player')}',
        'jar' : 'cortado-ovt-stripped-wm_r38710.jar',
        'flash' : 'flowplayer-3.1.1.swf'
    };
    
    var el = createPlayer(id, config);
    if (el != undefined ) {
        $('#' + id).empty();
        
        var img = $('<img src="' + player.poster + '" class="preview" alt="" />');
        $(img).attr("width", player.width);
        $(img).attr("height", player.height);
        $('#' + id).append(img);
        
        /* click preview: play */
        $('#' + id + ' img.preview').click(function(ev) {
            ev.preventDefault();
            $('#' + id + ' img.preview').hide();
            $('#' + id).append(el);
            player.play();
            showInfo(id);
            followProgress();
            
            $('#playercontrols li#play').addClass('pause');
            //console.log("state: " + player.state);
            //console.log("Player play...");
        });
        
        $('#playercontrols li#play').click(function(ev) {
            ev.preventDefault();
            //console.log("state: " + player.state);
            if (player.state == 'pause') {
                player.play();
                followProgress();
                if (! $('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').addClass('pause');
                //console.log("Player play...");
            } else if (player.state == 'play') {
                player.pause();
                if ($('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').removeClass('pause');
                //console.log("Player pause...");
            } else {
                $('#' + id + ' img.preview').hide();
                $('#' + id).append(el);
                player.play();
                showInfo(id);
                followProgress();

                if (! $('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').addClass('pause');
                //console.log("Player play...");
            }
        });
        
        $('#playercontrols li.playerinfo').hide();
        
        
    } else {
        $('#playercontrols').hide();
    }
}

function showInfo(id) {
    var text = "" + player.url;
    $('#playercontrols li.playerinfo').show();
    $('#playercontrols li.playerinfo').text(text);
}

function followProgress() {
    var oldpos;
    var pos;
    var text = "00:00";
    $('li#position').text(text);
    var progress = function() {
        oldpos = pos;
        pos = player.position();
        if (pos > 0) {
            var min = Math.round(pos / 60);
            var sec = Math.round(pos % 60);
            text = (min < 10 ? '0' + min : min) + ":" + (sec < 10 ? '0' + sec : sec);
            $('li#position').text(text);
        }
        if (player.state == "play" && pos != oldpos) {
            setTimeout(progress, 300);
        }
    };
    progress();
}

$(document).ready(function() {
    initPlayer();
});

</mm:content>
