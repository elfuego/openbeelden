$(document).ready(function() {
    $('a.editme').click(function(ev){
        ev.preventDefault();
        var link = ev.target.href;
        
        var id = link.substring(link.indexOf("#"));
        var query = link.substring(link.indexOf("?") + 1, link.indexOf('#'));
        link = link.substring(0, link.indexOf("?"));
        var params = getParams(query);

	    $(id).load(link, params, function(){
                $('#form_' + params.nr + ' input.cancel').click(function(ev){
                    params['cancel'] = 'Cancel'; 
                    $(id).load(link, params);
                });
                $('#form_' + params.nr).ajaxForm({ 
                    target: id,
                    success: clearMsg('#form_' + params.nr)
                });
            });
    });

    initClearMsg();
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
