// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Javascript to init the player
  @author: André van Toly
  @version: 0.3
  @changes: moved methods to player.js, classes for id's
*/

function initPlayer(id) {
    var config = { 
        /* msie (or windows java) has issues with just a dir */
        'server' : '<mm:url page="/" absolute="true" />',
        /* the files (jar, flash) need to be in this directory */
        'dir' : '${mm:link('/player')}',
        'jar' : 'cortado-ovt-stripped-wm_r38710.jar',
        'flash' : 'flowplayer-3.1.1.swf'
    };
    
    var mediaEl = createPlayer(id, config);
    if (mediaEl != undefined) {
        $('#' + id + ' div.player').empty();
        var img = $('<img src="' + player.poster + '" class="preview" alt="" />');
        $(img).attr("width", player.width);
        $(img).attr("height", player.height);
        $('#' + id + ' div.player').append(img);
        
        /* click preview: play */
        $('#' + id + ' img.preview').click(function(ev) {
            ev.preventDefault();
            $('#' + id + ' img.preview').remove();
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
                $('#' + id + ' img.preview').remove();
                $('#' + id + ' div.player').append(mediaEl);
                player.play();
                followProgress();
                if ($('#' + id + ' ul.controls li.pause').length == 0) {
                    $('#' + id + ' ul.controls li.play').addClass('pause');
                }
            }
        });
        
    } else {
        $('#' + id + ' ul.controls').hide();
    }
}

$(document).ready(function() {
    initPlayer('oiplayer');
});

</mm:content>
