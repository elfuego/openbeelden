/*
  Functions for new (portal) editors in OIP 
  @author: André van Toly
  @version:  '$Id$
  @changes: initial version
*/
$(document).ready(function() {
    initEditme('body');
    clearMsg();
    initPortalSwitch();
    initSortable();
    
    initSearchme();
});

function initSearchme() {
    $('.searchform').submit(function(ev) {
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
    //console.log('link ' + link);

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
        ev.preventDefault();
        editMe(ev);
    });
}

jQuery.fn.editme = function(settings) {
    var config = {};
    if (settings) $.extend(config, settings);
    
    this.each(function(){
        $(this).find('a.editme').click(function(ev){});
    });
}

/* forms to add, edit and delete nodes */
function editMe(ev) {
    var tag = ev.target;
    var link = ev.target.href;
    
    var id = link.substring(link.indexOf("#"));
    var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
    link = link.substring(0, link.indexOf("?"));
    var params = getParams(query);
    
    var formId = (params.type != null ? '#form_' + params.type : '#form_' + params.nr);
    params['editme'] = 'true';  /* inform form about being editme ajax editor */
    $(id).load(link, params, function() {
		   
		   // what to do while cancelling
		   $(formId + ' .cancel').click(function(ev) {
               ev.preventDefault();
               params['cancel'] = 'Cancel'; 
               $(id).load(link, params, function() { 
                   $(this).find('a.editme').click(function(ev){ 
                       ev.preventDefault();
                       editMe(ev);
                   });
                   clearMsg(id);
               });
               $(tag).show();
           });

		   // fields validator
		   var validator = new MMBaseValidator();
		   validator.prefetchNodeManager(params.type);  // XXX: params.type not always present
		   //console.log('id ' + id);
           validator.addValidationForElements($(id + " .mm_validate"));
		   validator.validateHook = function(valid, entry) {
		       var button = $(id + " input[type=submit][class=submit]");
		       button[0].disabled = validator.invalidElements != 0;
		   };
		   validator.validateHook();
		   
		   // ajax form options
		   var options = {
		       target: id + ' div.log',
		       success: afterSubmit,
		       data: { editme: 'true' }
		   };
		   $(formId).ajaxForm(options);
		   
		   // init tinyMCE html editor
		   initTiny();
	});
    $(tag).hide();
}

/* ajaxFrom success after submit */
function afterSubmit(response, status, xhr) {
    
    var parent = $(this).parent();

    if (response.indexOf('node_created') > -1) {   /* indicates we've made a new node */
        $(parent).next().find('a.editme').show();  /* make sure tag 'new' is shown again */

        var newContent = $(parent).find('div.node_created');
        var classes = $(newContent).attr('class').split(' ');
        var newItem = $(parent).clone().empty();
        var newId = 'newId';
        for (var i = 0; i < classes.length; i++) {
            if (classes[i].indexOf('edit_') > -1) {
                var newId = classes[i];
            }
        }
        newItem.attr('id', newId);
        newItem.removeClass('notsortable');
        newItem.html(newContent);
        newItem.insertAfter(parent);
        $(newItem).find('a.new').click(function(ev){
            ev.preventDefault();
            editMe(ev);
        });
        
        /* after saving new node, form is kept around for some reason */
        $(parent).find('form').remove();
    }

    if (response.indexOf('node_deleted') > -1) { /* node is deleted */
        $(parent).find('form').remove();
        $(this).find('a.cancel').click(function(ev){
            ev.preventDefault();
            $(parent).remove();
        });
    }
    
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
            connectWith: ".connected",
            cancel: ".notsortable",
            remove: function(ev, ui) {
                var edit_id = $(ui.item).attr('id');
                var nodenr = edit_id.match(/\d+/);
                var listId = $(this).attr('id');
                if (listId.indexOf('related_') > -1) {
                    var params = { 
                        id: listId, 
                        related: '',
                        unrelated: '' + nodenr
                    };
                    $.ajax({
                            url: "/mmbase/searchrelate/relate.jspx",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                                clearMsg('#' + listEl.id + ' li.log');
                                //console.log('removed ' + nodenr + ' from ' + listId);
                            }
                        });
                }
            },
            receive: function(ev, ui) { 
                var edit_id = $(ui.item).attr('id');
                var nodenr = edit_id.match(/\d+/);
                var listId = $(this).attr('id');
                var senderId = $(ui.sender).attr('id');
                if (senderId.indexOf('found_') > -1) {
                    var params = { 
                        id: listId, 
                        related: '' + nodenr, 
                        unrelated: ''
                        //deleted: deletedRelations
                    };
                    $.ajax({
                            url: "/mmbase/searchrelate/relate.jspx",
                            type: "GET",
                            datatype: "xml",
                            data: params,
                            complete: function(data) {
                                $('#' + listEl.id + ' li.log').html(data.responseText);
                                clearMsg('#' + listEl.id + ' li.log');
                                //console.log('received ' + nodenr + ' from ' + senderId);
                            }
                        });
                }

            },
            update: function(ev, ui) { 
                sortSortable(this);
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
            url: 'order.jspx',                  /* TODO: make this ${mm:link('/editors/order.jspx')} */
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
 * Inits tinyMCE html editor on dynamically loaded forms
 */
/*
example:
    // This triggers MMBaseValidator
    $("#" + tinyMCE.activeEditor.editorId).trigger("paste");
    tinyMCE.execCommand('mceRemoveControl', false, textAreaId );
|mm_f_description
*/

function initTiny( ) {
    tinyMCE.init({
        theme: "advanced",
        mode : "specific_textareas",
        editor_selector : /(mm_f_intro|mm_f_body||mm_nm_pools|mm_nm_pools_translations|mm_nm_licenses|mm_nm_licenses_translations|mm_nm_mmbaseusers)/,
        plugins : "fullscreen,xhtmlxtras",
        //content_css : "${mm:link('/style/css/tiny_mce.css')}",
        content_css : "/style/css/tiny_mce.css",
        entity_encoding : "raw",
        // language : "${requestScope['javax.servlet.jsp.jstl.fmt.locale.request']}",
        
        theme_advanced_toolbar_align : "left",
        theme_advanced_blockformats : "p,h3,h4,h5,blockquote",
        theme_advanced_path_location : "bottom",
        theme_advanced_toolbar_location : "top",
      
        theme_advanced_buttons1 : "formatselect,bold,italic,|,link,unlink,|,removeformat,fullscreen",
        theme_advanced_buttons2 : "",
        theme_advanced_buttons3 : "",
        theme_advanced_resizing : true    
    });
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
