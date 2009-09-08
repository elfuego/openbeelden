// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Javascript to init the player
  @author: André van Toly
  @version: 0.2
  @changes: support for msie
*/

function initPlayer() {
    var id = "vplayer";
    var config = { 
        'server' : '<mm:url page="/" absolute="true" />',   /* msie (or windows java) has issues with just a dir */
        'dir' : '${mm:link('/player')}',    /* the files (jar, flash) need to be in this directory */
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
            followProgress();
            
            $('#playercontrols li#play').addClass('pause');
        });
        
        $('#playercontrols li#play').click(function(ev) {
            ev.preventDefault();
            if (player.state == 'pause') {
                player.play();
                followProgress();
                if (! $('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').addClass('pause');
            } else if (player.state == 'play') {
                player.pause();
                if ($('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').removeClass('pause');
            } else {
                $('#' + id + ' img.preview').hide();
                $('#' + id).append(el);
                player.play();
                followProgress();

                if (! $('#playercontrols #play').is('.pause')) 
                    $('#playercontrols #play').addClass('pause');
            }
        });
        
        $('#playercontrols li.playerinfo').hide();
        
    } else {
        $('#playercontrols').hide();
    }
}

$(document).ready(function() {
    initPlayer();
});

</mm:content>
