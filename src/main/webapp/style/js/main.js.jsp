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
    $('li.license a, li.download a, li.embed a, li.share a, li.tag a').click(function(ev) {
        var link = ev.target.href;
        $( link.substring(link.indexOf("#")) ).slideToggle('fast');
    });
    $('li.favorite a').click(function(ev) {
        ev.preventDefault();
        var url = ev.target.href;
        $(ev.target).toggleClass('selected');
        $.ajax({ 
            url: url, 
            dataType: 'html', 
            success: function(xml) { 
                //console.log('ok: ' + xml); 
            }
        });
    });
}

function initRemoveFav() {
    $('div.b_user-favorites a.favorite').click(function(ev) {
        ev.preventDefault();
        var url = ev.target.href;
        var msg_spot = $(ev.target).closest('div.b_user-favorites').find('div.msgspot');
        var fav = $(ev.target).closest('dt');
        $.ajax({ 
            url: url, 
            dataType: 'html', 
            success: function(xml) { 
                $(fav).fadeOut('slow');
                $(msg_spot).html(xml);
            }
        });
    });
}

function initTagsuggest() {
    $('input.tagsuggest').mmTagsuggest({
        url: '${mm:link('/action/tags.jspx')}',
        resultId: '#tagsuggestions'
    });
}

function initClose() {
    $('a.close').click(function(ev) {
        ev.preventDefault();
        $(this).closest('div.popup').slideUp(400);
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
        var $tabs = $('#' + id).tabs();   /* jquery-ui.js must be included */
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

/* show/hide information about form fields */
function initFieldInfos() {
    if ($('form fieldset p.info').length) {
        $('form fieldset label').hover(function(ev) {
            $(this).next('p.info').show();
        }, function(ev) {
            $(this).next('p.info').hide();
        });
    }
}

/* put and show/hide labels behind inputs and textareas */
function initLabelsInInput() {
    $('fieldset.labelininput label').each(function(index) {
        var label = this;
        var inputId = $(this).attr('for');
        if ($('textarea#' +inputId).length > 0 || $('input#' +inputId).length > 0) {
            $(label).addClass('ininput');
            var input = $('#' + inputId);
            if (input.val().length > 0) {
                $(label).find('span').addClass('transparent');
            }
            input.focusin(function() {
                if (input.val().length == 0) {
                    $(label).find('span').animate({ 'opacity': 0.70 }, 'fast');
                }
            });
            input.live('keydown', function() {
                $(label).find('span').animate({ 'opacity': 0 }, 'fast');
            });
            input.live('paste', function() {
                $(label).find('span').animate({ 'opacity': 0 }, 'fast');
            });
            input.focusout(function() {
                if (input.val().length == 0) {
                    $(label).find('span').removeClass('transparent'); 
                    $(label).find('span').animate({ 'opacity': 1 });
                }
            });
        }
    });
}


/* Open link in new window or tab */
function initBlank() {
    $('._blank').click(function(ev) { ev.preventDefault(); window.open(ev.target); });
}

/* yeah well, in some ways we're still living in the previous millennium */
function staticFooter() {
    if (navigator.userAgent.match(/iPhone|iPod|iPad/i) != null) {
        $('#footer div').addClass('static');
    }
}

function initPlayStats() {
    $('div.oiplayer').bind("oiplayerplay", function(ev, pl) {
        var url = "${mm:link('/action/stats.jspx')}?id=" + pl.id;
        $.ajax({ 
            url: url, 
            dataType: 'html', 
            success: function(xml) { /* $('p.nr_of_views').html(xml); */ }
        });
    });
}

/* show/hide fieldset.plus */
function initPlusfields() {
    $('fieldset.plus').hide();

    $('h4.plusfields').find('a').click(function(ev) {
        var link = ev.target.href;
        $( link.substring(link.indexOf("#")) ).slideToggle('slow');
        $(this).closest('h4').toggleClass('open');
    });
}

$(document).ready(function() {
    initLangSwitch();
    initClearMsg();
    initToolbar();
    initRemoveFav();
    initCopyInput();
    if ($('input.tagsuggest').length) initTagsuggest();
    initClose();
    initTabs('tabs');
    initTabs('usertabs');
    initBlank();
    staticFooter();
    
    initPlusfields();
    if ($('fieldset.labelininput').length) initLabelsInInput();
    initFieldInfos();
    
    $('.main-column, .b_user-mediapreview').oiplayer({
        'server' : '<mm:url page="/" absolute="true" />',
        'jar' : '/oiplayer/plugins/cortado-ovt-stripped-wm_r38710.jar',
        'flash' : '/oiplayer/plugins/flowplayer-3.1.5.swf',
        'controls' : 'top'
    });
    initPlayStats();
});

</mm:content>
