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
});



/* TODO: make this a jquery plugin */
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

/* loads form to edit node */
function editMe(ev) {
    var tag = ev.target;
    var link = ev.target.href;
    
    var id = link.substring(link.indexOf("#"));
    var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
    link = link.substring(0, link.indexOf("?"));
    var params = getParams(query);
    
    var formId = (params.type != null ? '#form_' + params.type : '#form_' + params.nr);
    params['editme'] = 'true';  /* inform form about being editme ajax editor */
    $(id).load(link, params, function(){
        $(formId + ' .cancel').click(function(ev){
            ev.preventDefault();
            params['cancel'] = 'Cancel'; 
            $(id).load(link, params, function(){ 
                clearMsg(); 
                $(this).find('a.editme').click(function(ev){ 
                    ev.preventDefault();
                    editMe(ev);
                });
            });
            $(tag).show();
        });
        
        // ajax form options
        var options = {
            target: id + ' div.log',
            success: afterSubmit,
            data: { editme: 'true' }
        };
        $(formId).ajaxForm(options);
    });
    
    $(tag).hide();
}

/* ajaxFrom success after submit */
function afterSubmit(response, status, xhr) {
    //$(this).css('border', '1px solid blue');    // div.log
    var parent = $(this).parent();
    //$(parent).css('border', '1px solid green'); // li#add_mediaetc

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
    setTimeout(function(){
        if (el != undefined) {
            $(el).find('p.msg:not(.stay)').slideUp(1000);
        } else {
            $('p.msg:not(.stay)').slideUp(1000);
        }
    }, 5000);
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
