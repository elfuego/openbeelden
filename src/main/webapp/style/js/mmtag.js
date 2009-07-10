/*
  jQuery plugin for tagsuggestions, depends on typeWatch jQuery plugin
  @author: André van Toly
  @version: 0.1
  @params:
    url - the JSON tagsuggestions
    resultId - id of an html element in which to display the tag suggestions
  @changes: initial version
*/
$.fn.mmTagsuggest = function(o) {
    return this.each(function() {
        var self = this;
        var options = jQuery.extend({
			url : undefined,
			resultId : '#mmtagsuggestions'
		}, o);
        //console.log("options: " + options.url + ", " + options.resultId); 
        var resultEl = $(options.resultId);

        $(this).attr('autocomplete', 'off');  // FF etc. autocomplete off
        
        $(this).typeWatch({
            callback: suggestTags,
            wait: 250,
            highlight: true,
            captureLength: 1
            });
        
        function suggestTags() {
            var text = $(this.el).val();
            var inputbox = this.el;
            var url = options.url;
            var matches;
            $(options.resultId).empty();
            //console.log("text: " + text + " url: " + url);
            
            var params = new Object();
            params.tag = text;
            $.ajax({async: false, url: url, type: "GET", dataType: "json", data: params,
                    success: function(data) { 
                        $.grep(data, function(i) {
                            return $(resultEl).append( $(tagLink(i)).addClass('add') );
                        });
                        $(resultEl).click(function(ev) {   // bind click on one of the tags in this div
                            $(inputbox).val( $(ev.target).text() );
                        });
                    }
            });
        }
        
        function tagLink(text) {
           return '<a href="#" class="tag">' + text + '</a>';
        }
        
    });
}
