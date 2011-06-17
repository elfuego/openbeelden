/*<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><jsp:directive.page session="false" />
*///<mm:content type="text/javascript" expires="3600" postprocessor="none" language="${param.locale}"><os:cache time="3600"><mm:escape escape="javascript-compress">
<fmt:setBundle basename="eu.openimages.messages" scope="request" />
<fmt:message key="search.any_language" var="any_lang" />
<fmt:message key="media.list"       var="media_list" />
<fmt:message key="media.thumbnails" var="media_thumbs" />

/*
  Main javascript file for the Open Images Platform
  @author: Andre van Toly
  @version  '$Id$'
*/

/* Fills language pulldown with 'Any language' in appropiate language in some forms. */
function initMultiLang() {
    // change empty language options
    $("select[id='mm_searchlang'] option[value='']").text("${any_lang}");
    
    if ($('form#search').length) {  // change selected language in adv. search
        var la = $("#hiddensearchlang").val();
        $("select[id='mm_searchlang'] option[value='" + la + "']").attr("selected", "selected");
    }
    
    if ($('form#upload_form').length) {
        var loc = location.href;
        var langs = loc.split('.');
        var lang = langs[langs.length - 1];
        if (lang.length < 3) {
            $("select[id='mm_upload_form_language'] option[value='" + lang + "']").attr("selected", "selected");
        }
    }

}

/* Portal pulldown */
function initPortalSwitch() {
    $("select[id='choose_portal']").change(function() {
        document.location = $(this).val();
    });
}

/* Remove messages after a couple of seconds. Use '.msg.stay' if you want a msg to stay. */
function clearMsg(el) {
    setTimeout(function(){
        if (el != undefined) {
            $(el).find('p.msg:not(.stay)').slideUp(1000);
        } else {
            $('p.msg:not(.stay)').slideUp(1000);
        }
    }, 5000);
}

/* Toolbar on media item page */
function initToolbar() {
    if ($("div.popup").length) {
        var loc = document.location.href;
        if (loc.indexOf('#license') < 0) {
            $(loc.substring(loc.indexOf('#'))).toggle();
        }
    }
    $('li.license a, li.download a, li.share a').click(function(ev) {
        var link = ev.target.href;
        $(link.substring(link.indexOf("#"))).slideToggle('fast');
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

/* Close toolbar 'pop-ups'. */
function initClose() {
    $('div.popup a.close').click(function(ev) {
        ev.preventDefault();
        $(this).closest('div.popup').slideUp(400);
    });
}

/* Selects input type contents, makes it easier to copy */
function initCopyInput() {
    $('input.copyvalue, textarea.copyvalue').click(function(ev) {
        $(this).focus();
        $(this).select();
    });
}

/* Remove user favorite */
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

/* Add and remove tags with ajax etc. */
function initTags() {
    if ($("div#tag").length) {
        $('div#tag').toggle();
    }

    $('ul#tags li.add a').click(function(ev) {
        ev.preventDefault();
        $('div#tag').slideToggle('fast');
    });

    $('div#tag a.close').click(function(ev) {
        ev.preventDefault();
        $('div#tag').slideUp('fast');
    });
    
    $('ul#tags a.delete').hover(
        function(ev){$(ev.target).prev('a').css("color", "#c00");},
        function(ev){$(ev.target).prev('a').css("color", "#333");}
    );

    $('ul#tags a.delete').click(function(ev){
        ev.preventDefault();
        var link = ev.target.href;
        $(ev.target).parent().toggleClass('deleted').hide();
        $.ajax({
            url: link,
            dataType: 'html',
            success: function(xml) {
                $('#tagfeedback').html(xml);
                clearMsg('#tagfeedback');
            }
        });
    });
    
    $('input#tagsuggest').mmTagsuggest({
        url: '${mm:link("/action/tags.jspx")}',
        resultId: '#tagsuggestions'
    });

    // ajax form options
    var options = {
        target: '#tagfeedback',
        success: addedTag
    };
    $('#tagform').ajaxForm(options);
}

function addedTag() {
    var newTag = $(this).find('span.result');   // new tag in results
    var newItem = $('#addtag').clone().empty(); // clone template and make it empty
    newItem.removeAttr('id');
    newItem.removeAttr('class');
    newItem.append(newTag);
    $('#tags').append(newItem); // add list item with added tag
    clearMsg('#tagfeedback');
}

/* Tabs video, audio etc. */
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
                
                if (a.indexOf('thumbs') < 0) {
                    $(".thumbsonly a").each(function() {
                        if ($(this).hasClass('thumbsactive')) {
                            $(this).removeClass('thumbsactive').text("${media_thumbs}");
                            $(this).parent('.thumbsonly').removeClass("active");
                        }
                    });
                }
            });

        $(".thumbsonly a").click(function(ev) {
                $(this).toggleClass('thumbsactive');
                
                if ($(this).hasClass('thumbsactive')) {
                    $(this).text("${media_list}");
                    $(this).parent('.thumbsonly').addClass("active");
                    $tabs.tabs('select', 3); // switch to fourth
                } else {
                    $(this).text("${media_thumbs}");
                    $(this).parent('.thumbsonly').removeClass("active");
                    $tabs.tabs('select', 1); // switch to fourth
                }

                var a = ev.target.href;
                a = a.substring(a.indexOf('#'));
                if (a.indexOf('#t_') == 0) {
                    document.location = '#' + a.substring(3);
                } else {
                    document.location = "#t_" + a.substring(1);
                }
                
                return false;
            });
    }
}

/* Show/hide form field information  */
function initFieldInfos() {
    if ($('form fieldset p.info').length) {
        $('form fieldset label').hover(function(ev) {
            $(this).parent('div').find('p.info').show();
        }, function(ev) {
            $(this).parent('div').find('p.info').hide();
        });
    }
}

/* Hides username input value when it matches user0 in registration form */
function hideUser0() {
    if ($('#mm_username').length) {
        var username = $('#mm_username').val();
        var user = /^user\d+/;   // user0
        if (username.match(user)) {
            //console.log('hiding user0 :' + username);
            $('#mm_username').val("");
        }
    }
}

/* Open link in new window or tab (uses '._blank') */
function initBlank() {
    $('._blank').click(function(ev){ 
        ev.preventDefault();
        if (ev.target.tagName.toLowerCase() == 'a') {
            window.open(ev.target); 
        } else {
            window.open($(ev.target).parent().attr('href')); 
        }
    });
}

/* Media play statistics: registers start. */
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

/* Show/hide fieldset.plus */
function initPlusfields() {
    $('fieldset.plus').hide();
    $('input#add_more_info').click(function(ev) {
        $('fieldset.plus').slideToggle('slow');
    });
    initDeleteform();
}

/* Delete media form is hidden below edit media form */
function initDeleteform() {
    $('body.user-media div.b_user-mediadelete').hide();
    $('input#deletemedia, body.user-media input#cancel').click(function(ev) {
        ev.preventDefault();
        $('div.b_user-mediadelete').slideToggle('fast');
    });
}

/* Lightbox for images */
function initLightBox() {
    var settings = jQuery.extend({
        imageLoading:   '${mm:link('/style/css/images/loading.gif')}',
        imageBtnPrev:   '${mm:link('/style/css/images/lightbox-prev.png')}',
        imageBtnNext:   '${mm:link('/style/css/images/lightbox-next.png')}',
        imageBtnClose:  '${mm:link('/style/css/images/lightbox-close.png')}',
        imageBlank:     '${mm:link('/style/css/images/lightbox-blank.png')}'
    }, settings);
    $('a.lightbox').lightBox(settings);
}

$(document).ready(function() {
    initMultiLang();
    initPortalSwitch();
    clearMsg();
    initToolbar();
    initRemoveFav();
    initCopyInput();
    if ($('#tags').length) initTags();
    initClose();
    initTabs('tabs');
    initTabs('usertabs');
    initBlank();
    initPlusfields();
    initFieldInfos();
    hideUser0();
    if ($('a.lightbox').length) initLightBox();
    
    /* Init oiplayer only when there is video or audio tag */
    if ($('video').length || $('audio').length) {
        $('.main-column').oiplayer({
            'server' : '<mm:url page="/" absolute="true" />',
            'jar' : '/oiplayer/plugins/cortado-ovt-stripped-wm_r38710.jar',
            'flash' : '/oiplayer/plugins/flowplayer-3.1.5.swf',
            'controls' : 'top'
        });
        initPlayStats();
    }
});
//</mm:escape></os:cache></mm:content>

