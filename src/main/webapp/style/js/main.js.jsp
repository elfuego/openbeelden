// -*- mode: javascript; -*-
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content encoding="UTF-8" type="text/javascript" expires="300">
<fmt:setBundle basename="eu.openimages.messages" scope="request" />
<mm:import id="any_lang"><fmt:message key="search.any_language" /></mm:import>

/*
  Main javascript file for the Open Images Platform
  @author: Andre van Toly
  @version  '$Id$'
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
    });
    // change the empty language options
    var choose_lang = $("#menu label[for='mm_org.mmbase.mmsite.language']").text();
    $("select[id='mm_org.mmbase.mmsite.language'] option[value='']").text(choose_lang);
    $("select[id='mm_searchlang'] option[value='']").text("${any_lang}");
    // change selected language in adv. search
    var la = $("#hiddensearchlang").val()
    $("select[id='mm_searchlang'] option[value='" + la + "']").attr("selected", "selected");
}

function initClearMsg() {
    setTimeout("clearMsg();", 10000);
}

function clearMsg() {
    $('p.msg:not(.stay)').slideUp(1000);
}

function initToolbar() {
    if ($("div.popup").length) {
        var loc = document.location.href;
        $(loc.substring(loc.indexOf('#'))).toggle();
    }
    $('li.tag a').click(function(ev) {
        //ev.preventDefault();
        var link = ev.target.href;  // id of el. to show is in fragment
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.license a').click(function(ev) {
        //ev.preventDefault();
        var link = ev.target.href;
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.share a').click(function(ev) {
        //ev.preventDefault();
        var link = ev.target.href;
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.download a').click(function(ev) {
        //ev.preventDefault();
        var link = ev.target.href;
        var id = link.substring(link.indexOf("#") + 1);
        $('#' + id).toggle('fast');
    });
    $('li.embed a').click(function(ev) {
        //ev.preventDefault();
        var link = ev.target.href;
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

/* Makes input type contents easier to copy */
function initCopyInput() {
    $('input.copyvalue').click(function(ev) {
        $(this).focus();
        $(this).select();
    });
}

function initTabs(id) {
    if ($('#' + id).length) {
        var $tabs = $('#' + id).tabs();   // the jquery-ui.js for this has to be included in the page
        var loc = document.location.href;
        var anchorIndex = loc.indexOf('#');
        if (anchorIndex > 0) {
            var anchor = loc.substring(anchorIndex);
            if (anchor.indexOf("#t_") == 0) {
                $('#' + id).tabs('select', '#' + anchor.substring(3));
            } else {
                $('#' + id).tabs('select', '#t_' + anchor.substring(1));
            }
        }
        
        $('#' + id).bind('tabsshow', function(event, ui) {
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
}

/* Shows and hides information about form fields */
function initFieldInfos() {
    if ($('form fieldset p.info').length) {
        $('form fieldset label').hover(function(ev) {
            $(this).next('p.info').show();
        }, function(ev) {
            $(this).next('p.info').hide();
        });
    }
}
/* Open link in new window or tab */
function initBlank() {
    $('._blank').click(function(ev) { ev.preventDefault(); window.open(ev.target); });
}

$(document).ready(function() {
    initLangSwitch();
    initClearMsg();
    initToolbar();
    initCopyInput();
    if ($("input.tagsuggest").length) initTagsuggest();
    initClose();
    initTabs('tabs');
    initTabs('usertabs');
    initFieldInfos();
    initBlank();

    $('.main-column, .b_user-mediapreview').oiplayer({
        'server' : '<mm:url page="/" absolute="true" />',
        'jar' : '${mm:link('/oiplayer/plugins/cortado-ovt-stripped-wm_r38710.jar')}',
        'flash' : '${mm:link('/oiplayer/plugins/flowplayer-3.1.5.swf')}',
        'controls' : 'dark top'
    });
});

</mm:content>
