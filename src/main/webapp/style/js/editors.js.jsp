/*<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><jsp:directive.page session="false" />
*///<mm:content type="text/javascript" expires="3600" postprocessor="none"><os:cache time="0"><mm:escape escape="none">
<fmt:setBundle basename="eu.openimages.messages" scope="request" />
<mm:import id="any_lang"><fmt:message key="search.any_language" /></mm:import>
<mm:import id="textarea_classes">textarea.mm_f_intro, textarea.mm_f_body, textarea.mm_nm_pages, textarea.mm_nm_pages_translations, textarea.mm_nm_pools_translations, textarea.mm_nm_licenses, textarea.mm_nm_licenses_translations</mm:import>

/*
  Functions for new (portal) editors in OIP 
  @author: Andre van Toly
  @version:  '$Id$
  @changes: initial version
*/

$(document).ready(function() {
    initEditme('body');
    clearMsg();
    initPortalSwitch();
    initSortable();
    initSearchme();
    initMMBasevalidatorForTiny();

    /* field descriptions */
    if ($('form fieldset p.info').length) {
        $('form fieldset label').hover(function(ev) {
            $(this).parent('div').find('p.info').show();
        }, function(ev) {
            $(this).parent('div').find('p.info').hide();
        });
    }
});

var tinyMceConfig = {
    theme: "advanced",
    mode : "specific_textareas",
    //editor_selector : /(mm_f_intro|mm_f_body)/,
    plugins : "fullscreen,xhtmlxtras",
    content_css : "${mm:link('/style/css/tiny_mce.css')}",
    entity_encoding : "raw",
    <c:if test="${!empty requestScope['javax.servlet.jsp.jstl.fmt.locale.request']}">
      language : "${requestScope['javax.servlet.jsp.jstl.fmt.locale.request']}",
    </c:if>
    
    theme_advanced_toolbar_align : "left",
    theme_advanced_blockformats : "p,h3,h4,h5,blockquote",
    theme_advanced_path_location : "bottom",
    theme_advanced_toolbar_location : "top",
  
    theme_advanced_buttons1 : "formatselect,bold,italic,|,link,unlink,|,removeformat,fullscreen",
    theme_advanced_buttons2 : "",
    theme_advanced_buttons3 : "",
    theme_advanced_resizing : true
}

function initSearchme() {
    $('.searchme').submit(function(ev) {
        ev.preventDefault();
        searchMe(this, ev);
    });
}

function pageMe(target, ev) {
    var link = ev.target.href;
    $(target).load(link, function() {
        var list = $(this).find('ul.sortable');
        initSortable(list);
        $(this).find('a.cancel').click(function(ev){
            ev.preventDefault();
            $(target).empty();
        });
        $(this).find('li.pager a').click(function(ev) {
            ev.preventDefault();
            pageMe(target, ev);
        });
    });
}

function searchMe(self, ev) {
    ev.preventDefault();
    
    var link = $(self).attr('action');
    var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
    var params = getParams(query);
    params['q'] = $(self).find('input[name=q]').val();
    
    var results_target = $(self).next('div.searchresults');
    $(results_target).load(link, params, function() {
        var list = $(this).find('ul.sortable');
        initSortable(list);
        $(this).find('a.cancel').click(function(ev){
            ev.preventDefault();
            $(list).empty().addClass('empty').append('<li class="empty">Drop here...</li>');
        });
        $(this).find('li.pager a').click(function(ev) {
            ev.preventDefault();
            pageMe(results_target, ev);
        });
    });
}

/* TODO: make this a jquery plugin? */
function initEditme(el) {
    $(el).find('a.editme').click(function(ev){
        var link = ev.target.href.substring(0, ev.target.href.indexOf("?"));
        if (link.indexOf('editors/editor.jspx') < 0) {
            ev.preventDefault();
            editMe(ev);
        }
    });
}

jQuery.fn.editme = function(settings) {
    var config = {};
    if (settings) $.extend(config, settings);
    
    this.each(function(){
        $(this).find('a.editme').click(function(ev){});
    });
}

/* Forms to add, edit and delete nodes */
function editMe(ev) {
    var tag = ev.target;
    var link = ev.target.href;
    
    var id = link.substring(link.indexOf("#") + 1);
    var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
    link = link.substring(0, link.indexOf("?"));
    var params = getParams(query);
    
    //var formId = (params.type != null ? 'form_' + params.type : 'form_' + params.nr);
    params['editme'] = 'true';  /* inform form about being editme ajax editor */
    params['target'] = id;
    $('#' + id).load(link, params, function() {
           $('#' + id).addClass('editmeform');
		   
		   // what to do while cancelling
		   $('#' + id + ' .cancel').click(function(ev) {
               ev.preventDefault();
               params['cancel'] = 'Cancel';
               params['target'] = id;
               $('#' + id).load(link, params, function() {
                   $('#' + id).removeClass('editmeform');
                   initEditme(this);
                   clearMsg('#' + id);
               });
               $('#' + id).find("${textarea_classes}").each(function() {
                   //console.log('removed tiny from ' + $(this).attr('id'));
                   $(this).tinymce().remove();
               });

               $(tag).show();
           });
           
           var formId = $('#' + id + ' form').attr('id');

		   // fields validator
		   var validator = new MMBaseValidator();
		   validator.prefetchNodeManager(params.type);  // XXX: params.type not always present
           validator.addValidationForElements($('#' + formId + " .mm_validate"));
		   validator.validateHook = function(valid, entry) {
		       var button = $('#' + formId + " input[type=submit][class=submit]");
		       if (button.length) {
    		       button[0].disabled = validator.invalidElements != 0;
    		   }
		   };
		   validator.validateHook();
		   
		   // ajax form options
		   var options = {
		       target: '#' + id,
		       beforeSubmit: beforeSubmit,
		       success: afterSubmit,
		       data: { editme: 'true', target: id }
		   };
		   $('#' + formId).ajaxForm(options);
		   
		   initTiny('#' + id);

           /* field descriptions */
           if ($('#' + formId + ' fieldset p.info').length) {
               $('#' + formId + ' fieldset label').hover(function(ev) {
                   $(this).parent('div').find('p.info').show();
               }, function(ev) {
                   $(this).parent('div').find('p.info').hide();
               });
           }
		   
	}).hide().fadeIn("fast");
    $(tag).hide();
}

/* Trigger events on original textareas to have tinyMCE and MMBaseValidator cooperate */
function initMMBasevalidatorForTiny() {
    $("body").mousedown(function(ev) {
        var ed = tinyMCE.activeEditor;
        if (ed != null && ed.isDirty()) {
            //console.log("dirty: " + ed.editorId);
            tinyMCE.triggerSave();  // ?! does tiny paste here?
            // event on original textarea triggers MMBaseValidator
            $("#" + ed.editorId).trigger("paste");
        }
    });
}

/*
 * Integrates tinyMCE with jquery.ajaxForm. 
 * Puts tinyMCE's content in the array submitted by ajaxForm
 * and removes tinyMCE which otherwise would still be bound.
 */
function beforeSubmit(arr, $form, options) {
    $($form).find("${textarea_classes}").each(function() {
        var edId = $(this).attr("id"); 
        var edName = $(this).attr("name");
        for (var i = 0; i < arr.length; i++) {
            var some = arr[i];
            if (some['name'] == edName) {
                var content = $('#' + edId).tinymce().getContent();
                //console.log('doing: ' + some['name']);
                some['name'] = edName;
                some['value'] = content;
            }
        }
        $(this).tinymce().remove();
    });
	
	return true;    // true = submit
}

/*
 * Inits tinyMCE html editor on dynamically loaded forms
 */
function initTiny(el) {
    $(el).find("${textarea_classes}").each(function() {
        $(this).tinymce(tinyMceConfig);
    });
    initMMBasevalidatorForTiny();
}

/* ajaxFrom success after submit */
function afterSubmit(response, status, xhr) {
    var parent = $(this).parent();
    
    var thisId = $(this).attr('id');
    if (response.indexOf('node_created') > -1) {        /* node created */
        /* make sure tag 'new' is shown again */
        $(this).next().find('a.editme').show();
        
        var newContent = $(this).find('div.node_created');
        var classes = $(newContent).attr('class').split(' ');
        var newItem = $(this).clone().empty();
        var newId = 'newId';
        for (var i = 0; i < classes.length; i++) {
            if (classes[i].indexOf('edit_') > -1) {
                var newId = classes[i];
            }
        }
        newItem.html(newContent);
        newItem.attr('id', newId);
        newItem.removeClass('notsortable');
        
        /* after saving new node, form is kept around for some reason */
        if ($(parent).find('form').length) {
            //console.log('still found a form ' + $(parent).find('form').length);
            $(parent).find('form').remove();
        }
        
        /* if this (div) contains .targetme : append new content to it */
        if ($(this).hasClass('targetme')) {
            //console.log('putting ' + newId + ' in target: ' + $(this).attr('id') );
            $(this).html(newItem);
            initEditme('#' + newId);
        } else {
            //console.log('inserting ' + newId + ' before: ' + $(this).attr('id') );
            newItem.insertBefore(this);
            initEditme('#' + newId);
        }
        
    } else if (response.indexOf('node_deleted') > -1) { /* node deleted */
        //console.log('deleted init editme on ' + thisId + ' initEditme on this');
        initEditme(this);
        
        var self = this;
        $(this).find('a.close').click(function(ev){
            ev.preventDefault();
            $(self).remove();
        });
    
    } else {                                            /* node edited (or action cancelled) */
        initEditme(this);
        
    }
    //console.log('we are at ' + thisId);
    clearMsg(this);
}

/* 
 * ul.sortable has to have same id as NodeQuery, which is written to session.
 * All li's must have a prefix and node number as an id, f.e. 'edit_234'
 * TODO (?): make transfers to and from 'connected' lists work
 */
function initSortable(listEl) {
    if (listEl == undefined) {
        listEl = ".sortable";
    }

    if ($(listEl).length > 0) {
        $(listEl).sortable({
            distance: 30,
            connectWith: ".connected",
            start: function(ev, ui) {    /* check for tinyMCE (sigh..) */
               var listId = $(this).attr('id');
               $('#' + listId).find("${textarea_classes}").each(function() {
                   $(this).tinymce().remove();
               });                
            },
            stop: function(ev, ui) {    /* check for tinyMCE (sigh..) */
               var listId = $(this).attr('id');
               $('#' + listId).find("${textarea_classes}").each(function() {
                   $(this).tinymce(tinyMceConfig);
               });                
            },
            cancel: ".notsortable",
            remove: function(ev, ui) {
                var edit_id = $(ui.item).attr('id');
                var nodenr = edit_id.match(/\d+/);
                var listId = $(this).attr('id');
                
                
                var senderId = $(ui.sender).attr('id');

                if (listId.indexOf('found_') < 0 
                        && listId.indexOf('_footer') < 0 && listId.indexOf('_header') < 0) {

                    var editclasses = $("#" + edit_id).attr("class");
                    if (editclasses.indexOf('relation_') > -1) {
                        var relnr = editclasses.match(/\d+/);
                        //console.log("relnr '" + relnr + "'");
                    }
    
                    if (relnr != undefined) {
                        var params = { 
                            id: listId, 
                            related: '',
                            unrelated: '',
                            deleted: '' + relnr
                        };
                    } else {
                        var params = { 
                            id: listId, 
                            related: '',
                            unrelated: '' + nodenr
                            //deleted: deletedRelations
                        };
                    }
                    $.ajax({
                            url: "${mm:link('/mmbase/searchrelate/relate.jspx')}",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                                clearMsg('#' + listEl.id + ' li.log');
                            },
                            error: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                            }
                            
                        });
                }
            },
            receive: function(ev, ui) { 
                var edit_id = $(ui.item).attr('id');
                var nodenr = edit_id.match(/\d+/);
                
                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id');
                
                // add to list (and remove from?)
                if (listId.indexOf('found_') < 0
                        && listId.indexOf('_footer') < 0 && listId.indexOf('_header') < 0) {
                    //console.log('receive - listId ' + listId + ', senderId ' + senderId + ', nr ' + nodenr);
                    if ( $('#' + listId).hasClass("sortcancel") && senderId.indexOf('found_') >= 0) {
                        //console.log("receive - cancel sorting? " + listId);
                        //$( "#" + listId ).sortable("cancel");
                    }
                    var params = { 
                        id: listId, 
                        related: '' + nodenr, 
                        unrelated: ''
                    };
                    $.ajax({
                            url: "${mm:link('/mmbase/searchrelate/relate.jspx')}",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                                clearMsg('#' + listEl.id + ' li.log');
                                //console.log('received ' + nodenr + ' from ' + senderId);
                            },
                            error: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                            }

                        });
                }

            },
            update: function(ev, ui) { 
                var edit_id = $(ui.item).attr('id');
                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id');
                
                // are we updating related?
                // and its the sender
                if (listId.indexOf('related_') > -1 && senderId != undefined && senderId.indexOf('related_') > -1) {
                    
                    if ( $('#' + listId).hasClass("sortcancel") ) {
                        $('#' + listId).sortable("cancel");
                    } else {
                        sortSortable(this);
                    }
                } else {
                    
                    if ( $('#' + listId).hasClass("sortcancel") ) {
                        //console.log('not sorting because of class');
                        //$('#' + listId).sortable("cancel");
                    } else {
                        sortSortable(this);
                    }

                }

            }
            
        }).disableSelection();
    }
}

function sortSortable(list) {
    var items = $(list).sortable("toArray");
    var total = 0;
    for (i = 0; i < items.length; i++) {
        var result = items[i].match(/\d+/);
        if (result != null) {
            if (order == undefined) {
                var order = result;
            } else {
                order = order + "," + result;
            }
            total += 1;
        }
    }
    if (total > 1) {
        var params = new Object();
        params['id'] = list.id;
        params['order'] = order;
        $.ajax({
            url: "${mm:link('/editors/order.jspx')}",
            data: params,
            dataType: "xml",
            complete: function(data) {
                $('#' + list.id + ' li.log').html(data.responseText);
                clearMsg('#' + list.id + ' li.log');
            }
        });
    }
    if (total == 0) {
        $(list).addClass('empty');
        $(list).append('<li class="notsortable empty">Drop here...</li>');
    } else {
        $(list).find('li.empty').remove();
        if ($(list).is('.empty')) {
            $(list).removeClass('empty');
        }
    }
    
}

/*
 * Returns parameters from a query string in an object. 
 */
function getParams(query) {
	var params = new Object();
	var pairs = query.split('&');
	for (var i = 0; i< pairs.length; i++) {
		var pos = pairs[i].indexOf('=');
		if (pos == -1) continue;
		var name = pairs[i].substring(0, pos);
		var value = pairs[i].substring(pos + 1);
		value = decodeURIComponent(value);	// Decode it, if needed
		params[name] = value;
	}
	return params;
}

function clearMsg(el) {
    var remove = null;
    remove = setInterval(function() {
        if (el != undefined) {
            $(el).find('p.msg:not(.stay)').slideUp(1000);
            clearInterval(remove);
        } else {
            $('p.msg:not(.stay)').slideUp(1000);
            clearInterval(remove);
        }
    }, 7500);
}

function initPortalSwitch() {
    $("select[id='edit_portal']").change(function() {
        var form = $(this).parents('form');
        var action = form.attr("action").split('/');
        var last = action[action.length - 1].split('.');
        if ($(this).val() == '') {
            action[action.length - 1] = last[0];
        } else {
            action[action.length - 1] = last[0] + "?p=" + $(this).val();
        }
        var newUrl = action.join("/");
        document.location = newUrl;
    });
}
//</mm:escape></os:cache></mm:content>
