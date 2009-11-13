// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Javascript to init and control the player in player.js.
  This should mainly be integrated in player.js but has configuration that relies on correct urls.
  
  @author: André van Toly
  @version: 0.3
  @changes: moved methods to player.js, classes for id's
*/

function inititPlayer() {
    $('div.oiplayer').oiplayer({
        /* msie (or windows java) has issues with just a dir */
        'server' : '<mm:url page="/" absolute="true" />',
        /* the files (jar, flash) need to be in this directory */
        'jar' : '${mm:link('/player/cortado-ovt-stripped-wm_r38710.jar')}',
        'flash' : '${mm:link('/player/flowplayer-3.1.1.swf')}'
    });
}

function initPlayer(id) {
    var config = { 
        /* msie (or windows java) has issues with just a dir */
        'server' : '<mm:url page="/" absolute="true" />',
        /* the files (jar, flash) need to be in this directory */
        'jar' : '/player/cortado-ovt-stripped-wm_r38710.jar',
        'flash' : '/player/flowplayer-3.1.1.swf'
    };
    
    var mediaEl = createPlayer(id, config);
    if (mediaEl != undefined) {
        var img = $('<img src="' + player.poster + '" class="preview" alt="" />');
        if (player.type == 'audio') {
            img = $('<img src="' + $('#' + id + ' img').attr('src') + '" class="preview" alt="audio" />');
        }
        $('#' + id + ' div.player').empty();
        
        $(img).attr("width", player.width);
        $(img).attr("height", player.height);
        $('#' + id + ' div.player').append(img);
        
        /* click preview: play */
        $('#' + id + ' img.preview').click(function(ev) {
            ev.preventDefault();
            if (player.type == 'video') {
                $('#' + id + ' img.preview').remove();
            }
            $('#' + id + ' div.player').append(mediaEl);
            
            player.play();
            followProgress();
            $('#' + id + ' ul.controls li.play').addClass('pause');
        });
        
        /* click play/pause button */
        $('#' + id + ' ul.controls li.play').click(function(ev) {
            ev.preventDefault();
            if (player.state == 'pause') {
                player.play();
                followProgress();
                if ($('#' + id + ' ul.controls li.pause').length == 0) {
                    $('#' + id + ' ul.controls li.play').addClass('pause');
                }
            } else if (player.state == 'play') {
                player.pause();
                $('#' + id + ' ul.controls li.play').removeClass('pause');
            } else {
                if (player.type == 'video') {
                    $('#' + id + ' img.preview').remove();
                }
                $('#' + id + ' div.player').append(mediaEl);
                player.play();
                followProgress();
                if ($('#' + id + ' ul.controls li.pause').length == 0) {
                    $('#' + id + ' ul.controls li.play').addClass('pause');
                }
            }
            //console.log("player state: " + player.state);
        });
        
    } else {
        $('#' + id + ' ul.controls').hide();
    }
}

$(document).ready(function() {
    //initPlayer('oiplayer');
    inititPlayer();
});

</mm:content>
