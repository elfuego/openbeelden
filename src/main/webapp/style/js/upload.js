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
                                result = '<div class="PROGRESS">Uploading...</div>';
                            }
                            i++;
                        }
                    });
                });
                $(form).find(".progressInfo").html('<div class="PROGRESS"><p>Uploading...</p></div>');
                $(form).block( { message: $(result), 
                                 css: { 
                                     width: '96%', 
                                     textAlign: 'left',
                                     'white-space': 'nowrap',
                                     color: '#555',
                                     'font-weight': 'bold',
                                     padding: '2px 2px 2px 4px',
                                     border: '2px solid #ccc' 
                                 }, 
                                 fadeIn: 0, 
                                 fadeOut: 0 
                            });
                setTimeout(progress, 500);
            };
            progress();
        });
    });
});
