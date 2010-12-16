$(document).ready(function() {
    $('a.editme').click(function(ev){
        ev.preventDefault();
        var tag = ev.target;
        var link = ev.target.href;
        
        var id = link.substring(link.indexOf("#"));
        var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
        link = link.substring(0, link.indexOf("?"));
        var params = getParams(query);
        
        var formId = (params.type != null ? '#form_' + params.type : '#form_' + params.nr);
        // inform form about being editme ajax editor
        params['editme'] = 'true';

	    $(id).load(link, params, function(){
            $(formId + ' .cancel, ' + formId + ' a.closeme').click(function(ev){
                ev.preventDefault();
                params['cancel'] = 'Cancel'; 
                $(id).load(link, params, function(){ initClearMsg(); });
                $(tag).show();
            });
            
            // ajax form options
            var options = {
                target: id + ' div.log',
                data: { editme: 'true' },
                success: initClearMsg
            };
            $(formId).submit(function() { 
                $(this).ajaxSubmit(options);
                return false;
            });
        });
        // hide tag clicked
        $(tag).hide();
    });

    initClearMsg();
    initPortalSwitch();
});


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
