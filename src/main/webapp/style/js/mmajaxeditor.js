/* mmajaxeditor: uses jquery and it's Form Plug-In */

/*
 * MMAjaxeditor attaches an event to a link to display a form. 
 * When clicked the form is displayed using the links' parameters. The link contains all the 
 * parameters needed for the form to function correctly f.e. the nodetype to create: 
 * 'form-create.jsp?type=news#create_id'.
 * The fragment (#) part should contain the id of the html element in which to display everything.
 * If the id contains 'create' it is presumed we are creating: a new node will be added with
 * the same tag as this id (f.e. a 'li#node_345' below 'li#create_news'). If not the contents of the
 * element with that id (f.e. the element with '#node_345') are replaced with the new content (the 
 * edited node).
 * 
 * @param anchor    link with paramaters for the editor and an id (f.e. #node_345) of a target element
 *
 */
function MMAjaxeditor(anchor) {
	var link = anchor.href;
	this.id = link.substring(link.indexOf("#") + 1);
	link = link.substring(0, link.indexOf("#"));
	
	this.url = link.substring(0, link.indexOf("?"));
    var query = link.substring(link.indexOf("?") + 1);
	this.params = this.getParams(query);
	// indication to the form 
	if (this.params.mmajaxeditor == undefined) {
    	this.params.mmajaxeditor = "yes";   
    }
	this.el = document.getElementById(this.id);
	//console.log("url: " + this.url + ", query: " + query + ", id: " + this.id);

	if ($(this.el).find('form').length > 0) {
	    $(this.el).find('form').remove();
	} else {
	    this.addForm(this.el);
	}
}

/*
 * Adds the form and passes it on.
 */
MMAjaxeditor.prototype.addForm = function(el) {
	var form = this.loadElement(this.url, this.params);
	if ($(el).find('form').length > 0) $(el).find('form').remove();
    $(el).append(form);
	$(el).find('form').addClass('mmajaxeditor');
    this.bindFormEvents();
}

/*
 * Simply loads an element (a form you would expect) with ajax
 */
MMAjaxeditor.prototype.loadElement = function(url, params) {
    var result;
    $.ajax({async: false, url: url, type: "GET", dataType: "xml", data: params,
        complete: function(response, status) {
            if (status == "success" || status == "notmodified") {
                result = response.responseText;
            } else {
                var div = $('<p class="err">Error: ' + response.status + ' - ' + response.statusText + '</p>');
                result = div;
            }
        }
       });

    return result;
}

/*
 * (re)binds events to the resulting form so that it uses jquery Form and MMBaseValidator
 */
MMAjaxeditor.prototype.bindFormEvents = function() {
	var self = this;
	var result;
	var options = { 
	    dataType: "html", 
	    complete: function(response, status) {
            if (status == "success" || status == "notmodified") {
                result = response.responseText;
            } else {
                result = $('<p class="err">Error: ' + response.status + ' - ' + response.statusText + '</p>');
            }
	        //console.log(result);
	        if (self.id.indexOf('create') > -1) {
        	    $(self.el).find('form').remove();
                var tag = document.createElement(self.el.tagName);    // create a new tag like this one
                var newid = result.substring(result.indexOf('#node_') + 1, result.indexOf('"', result.indexOf('#node_')));
                tag.id = newid;
                $(tag).html(result);
                $(self.el).after(tag);      // append it after this one (create)
                self.bindMMAjaxeditorEvents('#' + newid);
	        } else {
    	        $(self.el).html(result);    // replace everything (edit/delete)
	        }
	        
            if ($(self.el).find('form input').length == 0) {
                //console.log("No inputs left, we're finished editing.");
                self.bindMMAjaxeditorEvents();
            } else {
	            $(self.el).find("form").ajaxForm(options);
                if (typeof MMBaseValidator == "function" && self.validator == undefined) {
                    self.validator = new MMBaseValidator(self.el);
                }
                self.bindMMAjaxeditorEvents( $(self.el).find("form") );
            }
	    }
	};
    if (result != undefined) {
        alert("when is this? i hope never...");
        $(this.el).html(result);   // replace everything (edit/delete)
    }
    $(this.el).find("form").ajaxForm(options);
	if (typeof MMBaseValidator == "function" && this.validator == undefined) {
        this.validator = new MMBaseValidator(this.el);
    }
    this.bindMMAjaxeditorEvents( $(this.el).find("form") );
}

/*
 * Binds links with class 'mmajaxeditor' etc.
 */
MMAjaxeditor.prototype.bindMMAjaxeditorEvents = function(el) {
    if (el == undefined) el = this.el;
    
    $(el).find('a.mmajaxeditor').click(function(ev) {
        ev.preventDefault();
        var anchor = ev.target;
		new MMAjaxeditor(anchor);
    });
    
    /*
    $("a.mmajaxeditor_close").click(function(ev) {
        ev.preventDefault();
    	var link = ev.target.href;
        link = link.substring(0, link.indexOf("#"));
        //var query = link.substring(link.indexOf("?") + 1);
        //var params = editor.getParams(query);
        var html = editor.loadElement(link, null);
        $(editor.el).removeClass("mmajaxeditor");
        $(editor.el).html(html);

        $(editor.el).find("a.mmajaxeditor").click(function(ev) {
            ev.preventDefault();
            var anchor = ev.target;
            new MMAjaxeditor(anchor);
        });
    });
    */
}

/*
function showRequest(formData, jqForm, options) { 
    var queryString = $.param(formData); 
    console.log('About to submit:\n' + queryString); 
    return true; 
}
*/


/*
 * Returns the parameters from a query string in an object. 
 */
MMAjaxeditor.prototype.getParams = function(query) {
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

function prepMMAjaxeditor() {
    $("a.mmajaxeditor").click(function(ev) {
        ev.preventDefault();
        var anchor = ev.target;
		new MMAjaxeditor(anchor);
    });
}

function initHideMsg() {
    setTimeout("hideMsg();", 5000);
}

function hideMsg() {
    $('p.msg').slideUp(800);
}

$(document).ready(function() {
    prepMMAjaxeditor();
    //initHideMsg();
});

