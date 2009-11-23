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

function initPlayer() {
    $('.main-column, .right-column').oiplayer({
        /* msie (or windows java) has issues with just a dir */
        'server' : '<mm:url page="/" absolute="true" />',
        'jar' : '${mm:link('/player/cortado-ovt-stripped-wm_r38710.jar')}',
        'flash' : '${mm:link('/player/flowplayer-3.1.1.swf')}',
        'controls' : 'false'
    });
}

$(document).ready(function() {
    initPlayer();
});

</mm:content>
