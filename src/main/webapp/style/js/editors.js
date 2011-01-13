/*
  Functions for new (portal) editors in OIP 
  @author: André van Toly
  @version:  '$Id: main.js.jsp 43901 2010-12-09 16:12:33Z andre $
  @changes: initial version
*/
$(document).ready(function() {
    initEditme('body');
    clearMsg();
    initPortalSwitch();
    initSortable();
});


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
function initSortable() {
    if ($('.sortable').length > 0) {
        $(".sortable").sortable({
            update: function(ev, ui) { 
                sortSortable(this);
            },
            connectWith: ".connected",  /* not supported (yet) */
            receive: function(ev, ui) { 
                sortSortable(this);     /* not supported (yet) */
            }
        }).disableSelection();
    }
}

function sortSortable(list) {
    var items = $(list).sortable("toArray");
    for (i = 0; i < items.length; i++) {
        var result = items[i].match(/\d+/);
        if (result != null) {
            if (order == undefined) {
                var order = result;
            } else {
                order = order + "," + result;
            }
        }
    }
    
    var params = new Object();
    params['order'] = order;
    params['query_id'] = list.id;
    $.ajax({
        url: 'order.jspx',
        data: params,
        dataType: "xml",
        complete: function(data) {
            $('#' + list.id + ' li.log').html(data.responseText);
            clearMsg('#' + list.id + ' li.log');
        }
    });    
}

/*
 * Inits tinyMCE html editor on dynamically loaded forms
 */
function initTiny( ) {
    tinyMCE.init({
        theme: "advanced",
        mode : "specific_textareas",
        editor_selector : /(mm_f_intro|mm_f_body|mm_f_description)/,
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
