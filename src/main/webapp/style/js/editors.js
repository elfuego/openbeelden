/*
  Functions for new (portal) editors in OIP 
  @author: André van Toly
  @version:  '$Id: main.js.jsp 43901 2010-12-09 16:12:33Z andre $
  @changes: initial version
*/
$(document).ready(function() {
    initEditme('body');
    initClearMsg();
    initPortalSwitch();
});

/* TODO: make this a jquery plugin */
function initEditme(el) {
    $(el + ' a.editme').click(function(ev){
        ev.preventDefault();
        editMe(ev);
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
    // inform form about being editme ajax editor
    params['editme'] = 'true';
    
    //console.log('link ' + link + ' id ' + id);

    $(id).load(link, params, function(){
        $(formId + ' .cancel').click(function(ev){
            ev.preventDefault();
            params['cancel'] = 'Cancel'; 
            $(id).load(link, params, function(){ 
                initClearMsg(); 
                $(this).find('a.editme').click(function(ev){ 
                    ev.preventDefault();
                    editMe(ev);
                });
            });
            $(tag).show();
        });
        $(formId + ' a.close').click(function(ev){
            ev.preventDefault();
            var hash = ev.target.href;
            params['cancel'] = 'Close'; 
            $(id).load(link, params, function(){ initClearMsg(); });
            $(tag).show();
        });
        
        // ajax form options
        var options = {
            target: id + ' div.log',
            success: afterSubmit,
            data: { editme: 'true' }
        };
        $(formId).ajaxForm(options);
        /* $(formId).submit(function() { 
            $(this).ajaxSubmit(options);
            return false;
        }); */
    });
    // hide tag clicked
    $(tag).hide();
}

/* ajaxFrom success after submit */
function afterSubmit(response, status, xhr) {
    //console.log('status: ' + status + ' response: ' + response);
    //console.log(xhr);

    var form = xhr[0];
    //console.log('form v ' + form.delete.value);
    
    //$(this).css('border', '1px solid blue');
    var parentDiv = $(this).parent('div');
    //$(parentDiv).css('border', '1px solid red');
    
    /* after saving or cancelling new node, form is kept around for some reason */
    $(parentDiv).find('form').remove();

    if (response.indexOf('node_deleted') > -1) { 
        /* node is deleted, remove editme links */
        $(parentDiv).find('a.editme').remove();
    }

    
    /* bind editme to new node */
    $(this).find('a.editnew').click(function(ev){
        ev.preventDefault();
        editMe(ev);
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

function initClearMsg() {
    setTimeout("clearMsg();", 10000);
}

function clearMsg(el) {
    if (el != undefined) {
        $(el).find('p.msg:not(.stay)').slideUp(1000);
    } else {
        $('p.msg:not(.stay)').slideUp(1000);
    }
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
