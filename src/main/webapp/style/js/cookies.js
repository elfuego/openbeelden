/*
  Javascript detecting cookie warning etc., depends on jQuery.
  @author:  Andre van Toly
  @version: '$Id$'
*/

$(document).ready(function() {
    
    if ($('div#cookie-bar').length) {
        var cookieName = "eu.openimages.cookieallow";
        var koekje = getCookie(cookieName);
        //console.log("cookie: " + koekje);
        if (koekje == undefined && cookiesEnabled()) {
            //console.log("no cookie");
        } else {
            $('div#cookie-bar').hide();
            $('div#cookie-bar').remove();
        }
        
        $('button.allow-cookies').click(function(){
            setCookie(cookieName, "ALLOW_COOKIES", 365 * 10);
            $('div#cookie-bar').remove();
            location.reload(true);
        });
        $('button.disallow-cookies').click(function(){
            setCookie(cookieName, "DISALLOW_COOKIES", 365 * 10);
            $('div#cookie-bar').remove();
        });
    } else {
        //console.log('nothing to do');
    }
    
});

function cookiesEnabled() {
	var enabled = (navigator.cookieEnabled) ? true : false;
	if (typeof navigator.cookieEnabled == "undefined" && !cookieEnabled) { 
		document.cookie="testcookie";
		enabled = (document.cookie.indexOf("testcookie") != -1) ? true : false;
	}
	return enabled;
}

function setCookie(name, value, exdays) {
    var exdate = new Date();
    exdate.setDate(exdate.getDate() + exdays);
    value = escape(value) + ((exdays==null) ? "" : "; expires="+ exdate.toUTCString());
    //console.log(name + "=" + value);
    document.cookie = name + "=" + value;
}

function getCookie(name) {
    var allcookies = document.cookie;
    var pos = allcookies.indexOf(name);
    if (pos != -1) {
        var start = pos + name.length + 1;
        var end = allcookies.indexOf(";", start);
        if (end == -1) end = allcookies.length;
        var value = allcookies.substring(start, end);
        value = decodeURIComponent(value);
        return value;
    } else {
        //console.log("no cookie found");
    }
}
