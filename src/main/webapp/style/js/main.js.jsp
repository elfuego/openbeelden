// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content type="text/javascript" expires="300">

/*
  Main javascript file for the Open Images Platform
  @author: André van Toly
  @version: 0.1
  @changes: initial version
*/

function initLangSwitch() {
    $("select[id='mm_org.mmbase.mmsite.language']").change(function() {
        var form = $(this).parents('form');
        var action = form.attr("action").split('/');
        var last = action[action.length - 1].split('.');
        if ($(this).val() == '') {
            action[action.length - 1] = last[0];
        } else {
            action[action.length - 1] = last[0] + "." + $(this).val();
        }
        var newUrl = action.join("/");
        document.location = newUrl;
        //form.submit();
    });
}

function initClearMsg() {
    setTimeout("clearMsg();", 10000);
}

function clearMsg() {
    $('p.msg').slideUp(1000);
}

function initToolbar() {
    $('li.tag a').click(function(ev) {
        ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.license a').click(function(ev) {
        ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.share a').click(function(ev) {
        ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.download a').click(function(ev) {
        ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.embed a').click(function(ev) {
        ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
}

function initTagsuggest() {
    $('input.tagsuggest').mmTagsuggest({
        url: '${mm:link('/action/tags.jspx')}',
        resultId: '#tagsuggestions'
    });
}

function initClose() {
    $('a.close').click(function() {
        $(this).parents('div.popup').hide('normal');
    });
}

$(document).ready(function() {
    initLangSwitch();
    initClearMsg();
    initToolbar();
    initTagsuggest();
    initClose();
    if ($("#tabs").length) {
        var $tabs = $("#tabs").tabs();   // the jquery-ui.js for this has to be included in the page
        var loc = document.location.href;
        var anchorIndex = loc.indexOf('#');
        if (anchorIndex > 0) {
            var anchor = loc.substring(anchorIndex);
            if (anchor.indexOf("#t_") == 0) {
                $("#tabs").tabs('select', '#' + anchor.substring(3));
            } else {
                $("#tabs").tabs('select', '#t_' + anchor.substring(1));
            }
        }
        
        $('#tabs').bind('tabsshow', function(event, ui) {
                var a = ui.tab.href;
                a = a.substring(a.indexOf('#'));
                if (a.indexOf('#t_') == 0) {
                    document.location = '#' + a.substring(3);
                } else {
                    document.location = "#t_" + a.substring(1);
                }
            });
        
        $(".thumbsonly a").click(function(ev) {
                var a = ev.target.href;
                a = a.substring(a.indexOf('#'));
                if (a.indexOf('#t_') == 0) {
                    document.location = '#' + a.substring(3);
                } else {
                    document.location = "#t_" + a.substring(1);
                }
                $tabs.tabs('select', 3); // switch to fourth
                return false;
            });
        
    }
});

</mm:content>
