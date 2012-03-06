/*<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><jsp:directive.page session="false" />
*///<mm:content type="text/javascript" expires="3600" postprocessor="none"><os:cache time="0"><mm:escape escape="none">
<fmt:setBundle basename="eu.openimages.messages" scope="request" />
<mm:import id="any_lang"><fmt:message key="search.any_language" /></mm:import>
<mm:import id="textarea_classes">textarea.mm_f_body, textarea.mm_f_intro, textarea.mm_nm_mmbaseusers, textarea.mm_nm_users_translations, textarea.mm_nm_pages, textarea.mm_nm_pages_translations, textarea.mm_nm_pools, textarea.mm_nm_pools_translations, textarea.mm_nm_licenses, textarea.mm_nm_licenses_translations</mm:import>

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
    gecko_spellcheck : true,
    //editor_selector : /(mm_f_intro|mm_f_body)/,
    plugins : "fullscreen,xhtmlxtras",
    content_css : "${mm:link('/style/css/tiny_mce.css')}",
    entity_encoding : "raw",
    <c:if test="${!empty requestScope['javax.servlet.jsp.jstl.fmt.locale.request']}">
      language : "${requestScope['javax.servlet.jsp.jstl.fmt.locale.request']}",
    </c:if>
    setup : function(ed) { 
        
        var followTiny = null;
        var saveTiny = function(ed) {
            ed.save();
            $("#" + ed.id).trigger("paste"); // triggers mmbase
        }
        
        ed.onInit.add(function(ed) {  
            if ($.browser.msie) {   // MSIE only
                saveTiny(ed);
            }
        });
        /* onChange: Fires when a new undo level is added to the editor */
        //ed.onChange.add(function(ed) {    
        /* onActivate: when textarea gets focus (and at init of tinyMCE) */
        ed.onActivate.add(function(ed) {
            clearInterval(followTiny);
            var count = 0;
            followTiny = setInterval(function(){
                if (ed.isDirty()) { // || ed.getContent() == 0
                    saveTiny(ed);
                } else {    /* editors get activated at init then deactivated, make them stop following */
                    saveTiny(ed);
                    clearInterval(followTiny);
                }
                if (count > 999) {  /* seems reasonable to stop after some time to avoid some browsers from crashing */
                     clearInterval(followTiny);
                }
                count++;
            }, 1500);   /* 1.5 sec interval needed (?) to give other threads (mmbase validator) time to check */
        });
        ed.onDeactivate.add(function(ed) {  // check if we need to validate
            if (ed.isDirty() || ed.getContent() == 0) {
                saveTiny(ed);
            }
        });
        ed.onRemove.add(function(ed) {
            if (followTiny != null) clearInterval(followTiny);
        });
    },
    
    theme_advanced_toolbar_align : "left",
    theme_advanced_blockformats : "p,h3,h4,h5,blockquote",
    theme_advanced_path_location : "bottom",
    theme_advanced_toolbar_location : "top",
  
    theme_advanced_buttons1 : "formatselect,bold,italic,|,bullist,numlist,|,link,unlink,|,removeformat,code,fullscreen",
    theme_advanced_buttons2 : "",
    theme_advanced_buttons3 : "",
    theme_advanced_resizing : true
}

/*
 * Inits tinyMCE html editor on dynamically loaded forms
 */
function initTiny(el) {
    $(el).find("${textarea_classes}").each(function() {
        $(this).tinymce(tinyMceConfig);
    });
    initMMBasevalidatorForTiny(el);
}

/* Trigger events on original textareas to have tinyMCE and MMBaseValidator cooperate */
function initMMBasevalidatorForTiny(el) {
    $("body").mousedown(function(ev) {
        for (edId in tinyMCE.editors) {
            var ed = tinyMCE.editors[edId];
            if (ed.isDirty() && ed.id == tinyMCE.activeEditor.id) {
                ed.save();
                $("#" + edId).trigger("paste");
            }
        }
    });
    $(el).find('input').focus(function(ev){
        for (edId in tinyMCE.editors) {
            var ed = tinyMCE.editors[edId];
            if (ed.isDirty() && ed.id == tinyMCE.activeEditor.id) {
                ed.save();
                $("#" + edId).trigger("paste");
            }
        }
    });
}

/* Inits an inline editor editMe targeted at div#elementwithid
   to add, edit and delete nodes (include afterSubmit).
   The link f.e. '/editor.jspx?nr=123#htmlelement' is the editor to use, 
   which will be loaded in f.e. 'div#htmlelement' in the page.
   - add: region .targetme receives new node, else node is is insterted before it 
   - edit:
   - delete (response.indexOf('node_deleted') > -1): item is removed from id
*/
function initEditme(el) {
    $(el).find('a.editme').click(function(ev){
        var link = ev.target.href.substring(0, ev.target.href.indexOf("?"));
        if (link.indexOf('editors/editor.jspx') < 0) {
            ev.preventDefault();
            //editMe(ev);
            
            var link = ev.target.href;
            var id = link.substring(link.indexOf("#") + 1);
            var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
            link = link.substring(0, link.indexOf("?"));
            var params = getParams(query);
            params['editme'] = 'true';  /* inform form about being editme ajax editor */
            $('#' + id).load(link, params, function(){ bindMe(id, link, params); }).hide().fadeIn("fast");
        }
    });
}

/* TODO: finish this and make editme into a jquery plugin? */
jQuery.fn.editme = function(settings) {
    var config = {};
    if (settings) $.extend(config, settings);
    
    this.each(function(){
        $(this).find('a.editme').click(function(ev){});
    });
}

/* Search functions for editme (search, paging) */
function initSearchme() {
    $('form.searchme').submit(function(ev) {
        ev.preventDefault();
        searchMe(this, ev);
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
        initEditme(results_target);
        initSortable(list);
        $(this).find('a.cancel').click(function(ev){
            ev.preventDefault();
            $(results_target).find('strong').remove();
            $(results_target).find('a.cancel').remove();
            $(list).empty().addClass('empty');
            //$(list).empty().addClass('empty').append('<li class="empty notsortable">Drop here...</li>');
        });
        $(this).find('li.pager a').click(function(ev) {
            ev.preventDefault();
            pageMe(results_target, ev);
        });
    });
}

function pageMe(target, ev) {
    var link = ev.target.href;
    $(target).load(link, function() {
        var list = $(this).find('ul.sortable');
        initEditme(target);
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

function bindMe(id, link, params) {   
   // first what to do while cancelling
   $('#' + id + ' .cancel').click(function(ev) {
       ev.preventDefault();
       params['cancel'] = 'Cancel';
       $('#' + id).find("${textarea_classes}").each(function() {
           $(this).tinymce().remove();
       });
       //console.log('cancelling: ' + link + ' show form ' + params['showform']);
       $('#' + id).load(link, params, function() {
           //$('#' + id).removeClass('editmeform');
           initEditme(this);
           clearMsg('#' + id);
       });
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
       error: function(res, st){ 
          $('#' + id).prepend('<p class="err">' + st + ' ' + res.status + ' : ' + res.statusText + '</p>'); 
       },
       data: { editme: 'true', target: id } // TODO: these still needed here? 
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
   
}


/*
 * Integrates tinyMCE with jquery.ajaxForm. 
 * Puts tinyMCE's content in the array submitted by ajaxForm
 * and removes tinyMCE editor which otherwise would still be bound.
 */
function beforeSubmit(arr, $form, options) {
    $($form).find("${textarea_classes}").each(function() {
        var edId = $(this).attr("id"); 
        var edName = $(this).attr("name");
        for (var i = 0; i < arr.length; i++) {
            var some = arr[i];
            if (some['name'] == edName) {
                var content = $('#' + edId).tinymce().getContent();
                some['name'] = edName;
                some['value'] = content;
            }
        }
        $(this).tinymce().remove();
    });
	
	return true;    // true = submit
}

/* ajaxFrom success after submit */
function afterSubmit(response, status, xhr, $form) {
    var parent = $(this).parent();
    var thisId = $(this).attr('id');
    
    if (response.indexOf('node_created') > -1) {        /* node created */
        var link = xhr.responseXML.URL;
        var query = link.substring(link.indexOf("?") + 1, link.length);
        var params = getParams(query);
        /* make sure tag 'new' is shown again */
        $(this).next().find('a.editme').show();
        
        var newContent = $(this).find('div.node_created');
        var classes = $(newContent).attr('class').split(' ');
        var newItem = $(this).clone().empty();
        var newId = 'newId';
        for (var i = 0; i < classes.length; i++) {
            if (classes[i].indexOf('relation_') > -1) {
                var newId = classes[i];
            } else if (classes[i].indexOf('node_') > -1) {
                var newId = classes[i];
            }
        }
        newItem.html(newContent);
        newItem.attr('id', newId);
        newItem.removeClass('notsortable');
        
        /* after saving new node, form is kept around for some reason ?! */
        if ($(this).find('form').length) {
            console.log('still found a form ' + $(this).find('form').length);
            //$(this).find('form').remove();
        }
        
        /* if this (div) contains .targetme : append new content to it */
        console.log('show : ' + params['showform'] )
        if ($(this).hasClass('targetme')) {
            $(this).html(newItem);
            initEditme('#' + newId);
        } else {
            newItem.insertBefore(this);
            if (params['showform'] == 'true') {
                bindMe(thisId, link, params); 
            } else {
                initEditme('#' + newId);
            }
        }
        
    } else if (response.indexOf('node_deleted') > -1) { /* node deleted */
        initEditme(this);
        
        var self = this;
        var listLength = $(this).parent('ul').find('li:not(.notsortable)').length;
        var uList = $(this).parent('ul');
        //console.log('thisId ' + thisId);
        $(this).find('a.close').click(function(ev){     // TODO: change this to something automatic
            ev.preventDefault();
            $(self).remove();
            if (listLength == 1) {  // was 1, now empty
                //console.log('list le ' + listLength + ' p ' + $(this).parent('ul').attr('id'));
                $(uList).prepend('<li id="iamempty" class="notsortable empty">Drop here...</li>');
                var params = new Object();
                params['type'] = 'mediafragments';  // TODO: find and use nodetype
                $('#iamempty').load("${mm:link('/editors/show-node.empty.jspx')}", params); // hackery!
            }
        });
    
    } else {                                            /* node edited (or action cancelled) */
        var link = xhr.responseXML.URL;
        var query = link.substring(link.indexOf("?") + 1, link.length);
        var params = getParams(query);
        //console.log("show form " + params['showform']);
        if (params['showform'] == 'true') {
            bindMe(thisId, link, params);   // bind form again
        } else {
            if ($(this).find('form').length) $(this).find('form').remove(); // clean up
            initEditme(this);
        }
    }
    clearMsg(this);
}

/* 
 * ul.sortable has to have same id as NodeQuery, which is written to session.
 * All li's must have a prefix and node number as an id, f.e. 'node_234'
 * TODO (?): make transfers to and from 'connected' lists work
 */
function initSortable(listEl) {
    if (listEl == undefined) {
        listEl = ".sortable";
    }

    if ($(listEl).length > 0) {
        
        /* relate function by clicking a.relate link */
        $('a.relate').click(function(ev){   /* link to click to add li elements to list */
            ev.preventDefault();
            var link = ev.target.href;
            var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
            var params = getParams(query);
            //console.log(params);
            
            var listItem = $(this).parents('li');
            var thisListId = $(listItem).parent('ul').attr('id');
            //var destListId = "related" + thisListId.substr(thisListId.indexOf('_'), thisListId.length);
            var destListId = "related_" + $('#' + thisListId).closest('div.searchme').find("input:hidden[name=destinationlist]").val();
            var nodenr = params['nr'];
            
            $("#" + destListId + " > li.new").before(listItem);
            $(listItem).attr('id', 'node_' + nodenr);   // give it a new id
            
            var relParams = { 
                id: destListId, 
                related: '' + nodenr, 
                unrelated: ''
            };
            $.ajax({
                url: "${mm:link('/editors/relate.jspx')}",
                type: "GET",
                datatype: "xml",
                data: relParams,
                complete: function(data) {
                    var response = data.responseText;
                    $('#' + destListId + '_log').html(response);
                    
                    if (response.indexOf('number') > -1) {
                        var result = response.match(/\s+number='(\d+)'/);
                        console.log('found: ' + result);
                        
                        var newrel = result[1];
                        $(listItem).attr('id', "relation_" + newrel);   // give it new id
                    }
                    
                    console.log('newrel: ' + newrel);

                    params['relation'] = newrel;
                    $(listItem).find('div.actions').load("${mm:link('/editors/actions.div.params.jspx')}", 
                        params, 
                        function(){ 
                            initEditme(this); 
                            $('#' + destListId).sortable("refresh");
                            //console.log('refresh ' + destListId);
                            $('#' + destListId).find('li.empty').remove();
                        }
                    );

                    clearMsg('#' + destListId + '_log');
                },
                error: function(data) {
                    $('#' + destListId + '_log').html(data.responseText);
                }
            }); 
        }); /* end relate function by clicking a.relate */
        
        /* jquery.sortable functions */
        $(listEl).sortable({
            distance: 30,
            connectWith: ".connected",
            cancel: ".notsortable, form",
            placeholder: "ui-state-highlight",
            start: function(ev, ui) {   /* check for tinyMCE and remove it */
               var listId = $(this).attr('id');
               $(this).addClass('activated');
               $('#' + listId).find("${textarea_classes}").each(function() {
                   $(this).tinymce().remove();
               });                
            },
            stop: function(ev, ui) {
               var listId = $(this).attr('id');
               $(this).removeClass('activated');
               $('#' + listId).find("${textarea_classes}").each(function() {
                   $(this).tinymce(tinyMceConfig);
               });                
            },
            remove: function(ev, ui) {
                var editId = $(ui.item).attr('id');    // list item id
                //var relnr = editId.match(/relation_\d+/);
                var relation_result = editId.match(/relation_(\d+)/);
                if (relation_result != null) {
                    var relnr = relation_result[1];
                } else {
                    var nodenr = editId.match(/\d+/);
                }
                
                if (nodenr == null) {
                    var editclasses = $("#" + editId).attr("class");
                    var n_result = editclasses.match(/node_(\d+)/);
                    if (n_result != null) {
                        var nodenr = n_result[1];
                        //console.log('n ' + nodenr);
                    }
                }

                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id'); //  (can be undefined)
                //console.log('r ' + relnr + ' n ' + nodenr + ' editid ' + editId + ' senderId ' + senderId);
                if (listId.indexOf('found_') < 0) {
                    
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
                            url: "${mm:link('/editors/relate.jspx')}",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {
                                $('#' + listId + '_log').html(data.responseText);
                                clearMsg('#' + listId + '_log');
                            },
                            error: function(data) {
                                $('#' + listId + '_log').html(data.responseText);
                            }
                            
                        });
                }
            },
            receive: function(ev, ui) { 
                var editId = $(ui.item).attr('id');
                var nodenr = editId.match(/\d+/);
                
                var editclasses = $("#" + editId).attr("class");
                /* if (editclasses.indexOf('relation_') > -1) {    // relation_posrel_126
                    var result = editclasses.match(/relation_\w+_(\d+)/);
                    var relnr = result[1]; 
                } */
                if (editclasses.indexOf('node_') > -1) {    // relation_posrel_126
                    var result = editclasses.match(/node_(\d+)/);
                    var nodenr = result[1];
                }
                
                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id');
                
                // add to list (and remove from?)
                if (listId.indexOf('found_') < 0) {
                    //console.log('receive - listId ' + listId + ', senderId ' + senderId + ', nr ' + nodenr);
                    /* if ( $('#' + listId).hasClass("sortcancel") && senderId.indexOf('found_') >= 0) {
                        $( "#" + listId ).sortable("cancel");
                    } */
                    var params = { 
                        id: listId, 
                        related: '' + nodenr, 
                        unrelated: ''
                    };
                    $.ajax({
                            url: "${mm:link('/editors/relate.jspx')}",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {

                                var response = data.responseText;
                                //$('#' + destListId + '_log').html(response);
                                
                                if (response.indexOf('number') > -1) {
                                    var result = response.match(/\s+number='(\d+)'/);
                                    var newrel = result[1];
                                    $(ui.item).attr('id', "relation_" + newrel);   // give it new id
                                    
                                    // update action links
                                    // nr, parent, role, relation
                                    var listclasses = $('#' + listId).attr('class');
                                    var newparams = new Object();
                                    newparams['nr'] = nodenr;
                                    newparams['relation'] = newrel;
                                    
                                    var p_result = listclasses.match(/parent_(\d+)/);
                                    if (p_result != null) newparams['parent'] = p_result[1];
                                    
                                    var r_result = listclasses.match(/role_(\w+)/);
                                    if (r_result != null) newparams['role'] = r_result[1];

                                    newparams['maydelete'] = $('#' + listId).hasClass("maydelete") ? "true" : "false";
                                    newparams['unpublish'] = $('#' + listId).hasClass("unpublish") ? "true" : "false";

                                    $(ui.item).find('div.actions').load("${mm:link('/editors/actions.div.params.jspx')}", 
                                        newparams, 
                                        function(){ 
                                            initEditme(this); 
                                        }
                                    );


                                }


                                $('#' + listId + '_log').html(data.responseText);
                                clearMsg('#' + listId + '_log');
                            },
                            error: function(data) {
                                $('#' + listId + '_log').html(data.responseText);
                            }

                        });
                }

            },
            update: function(ev, ui) { 
                var editId = $(ui.item).attr('id');
                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id');
                //console.log('edit: ' + editId + ' list: ' + listId + ' sender: ' + senderId)
                // are we updating related?
                // and its the sender
                /* if (listId.indexOf('related_') > -1 
                    && senderId != undefined && senderId.indexOf('related_') > -1) { */
                if (senderId != undefined) {
                    if ( $('#' + listId).hasClass("sortcancel") ) {
                        $('#' + listId).sortable("cancel");
                    } else {
                        sortSortable(this, true);
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
            
        });
        
        /* undocumented(?) jquery feature: https://groups.google.com/forum/?fromgroups#!topic/jquery-ui/B2h_Ea4jo5I 
           disables text selection within surrounding element */
        $(listEl).parent().disableSelection(); 
    }
}

/* 
 * Sorts the sortable list based on ajax query checking mmbase.
 * @param list      list element to sort
 * @param updated   if something has been removed from or added to list, to wait a sec. while db commits are made
 */
function sortSortable(list, updated) {
    var items = $(list).sortable("toArray");
    var total = 0;
    for (i = 0; i < items.length; i++) {
        var liId = items[i];
        if (liId != null) {
            var editclasses = $("#" + liId).attr("class");
            if (editclasses != null && editclasses.indexOf('node_') > -1) {    // relation_posrel_126
                var results = editclasses.match(/node_(\d+)/);
                var result = results[1];
                if (result != null) {
                    if (order == undefined) {
                        var order = result;
                    } else {
                        order = order + "," + result;
                    }
                    total += 1;
                }
            }
        }
    }
    
    if (total > 1) {
        var ms = 10;
        if (updated) ms = 1250; 
        var t = null;
        t = setTimeout(function() {
            var params = new Object();
            params['id'] = list.id;
            params['order'] = order;
            $.ajax({
                url: "${mm:link('/editors/order.jspx')}",
                data: params,
                dataType: "xml",
                complete: function(data) {
                    $('#' + list.id + '_log').html(data.responseText);
                    clearMsg('#' + list.id + '_log');
                }
            });
        }, ms);
    }
    if (total == 0) {
        $(list).addClass('empty');
        //console.log('empty');
        $(list).prepend('<li id="iamempty" class="notsortable empty">Drop here...</li>');
        var params = new Object();
        params['type'] = 'mediafragments';  // TODO: find and use nodetype
        $('#iamempty').load("${mm:link('/editors/show-node.empty.jspx')}", params);
        
    } else {
        $(list).find('li.empty').remove();
        //console.log('no longer empty, remove empty li !');
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
