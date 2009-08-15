$(document).ready(function() {
    var progressUrl = $("head meta[name=ContextRoot]").attr("content") + "progress.jspx";
    $("form[enctype=multipart/form-data]").each(function() {
        $(this).submit(function() {
            var form = this;
            var result;
            var progress = function() {
                $(form).find(".progressInfo").each(function() {
                    $.ajax({
                        url: progressUrl,
                        async: false,
                        contentType: 'xml',
                        complete: function(data) {
                            result = data.responseText;
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
