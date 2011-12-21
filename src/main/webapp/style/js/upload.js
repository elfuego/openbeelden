$(document).ready(
    function() {
	var lang = $("html").attr("xml:lang");
	var progressUrl = $("head meta[name=ContextRoot]").attr("content") + "action/progress.jspx?lang=" + lang;
	$("form.mm_form").each(
	    function() {
		var pInfo = $(this).find(".progressInfo").first();
		$(this).submit(
		    function() {
			var form = this;
			var i = 0;
			var progress = null;
			clearInterval(progress);
			var progressFunction =
			    function() {
				$.ajax(
				    { url: progressUrl,
				      async: true,
				      error: function(xhr, status, err) {
					  var result = '<div>Error: ' + status + " : " + err + '</div>';
					  $(pInfo).html(result);
					  
				      },
				      success: function(data) {
					  var result = data.responseText;
					  $(pInfo).html(result);
					  alert(result);
					  //console.log('success');
				      }
				    });
				//console.log('uploading: ' + i);
			    };
			progressFunction();
			setInterval(progressFunction, 1000);
		    });
	    });

	// Just some stuff to guess a nicer title
	$("input[type=file]").change(
	    function(ev) {
		var file = ev.target.value;
		var title = $(ev.target.form).find(".mm_validate.mm_f_title")[0];
		if (title.originalValue == $(title).val()) {
		    var li = file.lastIndexOf('\\');
		    if (li > 0) {
			file = file.substring(li + 1);
		    }
		    li = file.lastIndexOf('.');
		    if (li > 0) {
			file = file.substring(0, li);
		    }
		    if (file.length >= 3) {
			$(title).val(file);
		    }
		}
	    });
    });
