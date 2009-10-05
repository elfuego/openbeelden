$(document).ready(function() {
    var progressUrl = $("head meta[name=ContextRoot]").attr("content") + "progress.jspx";
    $("form[enctype=multipart/form-data]").each(function() {
        $(this).submit(function() {
            var form = this;
            var result;
            var i = 0;
            var progress = function() {
                $(form).find(".progressInfo").each(function() {
                    $.ajax({
                        url: progressUrl,
                        async: false,
                        contentType: 'xml',
                        complete: function(data) {
                            result = data.responseText;
                            if (result.indexOf('100%') > -1 && i == 0) {
                                result = '<div class="PROGRESS"><p>Starting..</p></div>';
                            }
                            i++;
                        }
                    });
                });
                $(form).find(".progressInfo").html(result);
                setTimeout(progress, 1000);
            };
            progress();
        });
    });
});
