// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Javascript to init and control the player in player.js.
  This should mainly be integrated in player.js but has configuration that relies on correct urls.
  
  @author: André van Toly
  @version  '$Id$'
  @changes: moved methods to player.js, classes for id's
*/

function initPlayer() {
    $('.main-column, .b_user-mediapreview').oiplayer({
        'server' : '<mm:url page="/" absolute="true" />', /* msie (or windows java) has issues with just a dir */
        'jar' : '${mm:link('/oiplayer/plugins/cortado-ovt-stripped-wm_r38710.jar')}',
        'flash' : '${mm:link('/oiplayer/plugins/flowplayer-3.1.1.swf')}',
        'controls' : false
    });
}

$(document).ready(function() {
    initPlayer();
});

</mm:content>
