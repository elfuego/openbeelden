$(document).ready(function() {
    var progressUrl = $("head meta[name=ContextRoot]").attr("content") + "action/progress.jspx";
    $("form[enctype=multipart/form-data]").each(function() {
        var pInfo = $(this).find(".progressInfo").first();
        $(this).submit(function() {
            var form = this;
            var result = "<div>Uploading...</div>";
            var i = 0;
            var progress = null;
            window.clearInterval(progress);
            progress = window.setInterval(function() {
                $.ajax({
                    url: progressUrl,
                    async: false,
                    cache: false,
                    contentType: 'xml',
                    error: function(xhr, status, err) {
                        result = '<div>Error: ' + status + " : " + err + '</div>';
                    },
                    complete: function(data) {
                        result = data.responseText;
                        if (result.indexOf('100%') > -1 && i == 0) {
                            $(pInfo).html("<div>Uploading...</div>");
                        } else {
                            $(pInfo).html(result);
                        }
                        i++;
                    },
                    success: function(data) {
                        result = data.responseText;
                        $(pInfo).html(result);
                        window.clearInterval(progress);
                    }
                });
            }, 500);
        });
    });
});
