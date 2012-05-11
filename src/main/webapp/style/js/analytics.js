/*
Track downloads with Google Analytics
https://developers.google.com/analytics/devguides/collection/gajs/methods/gaJSApiEventTracking

category    Downloads
action      title tag of download link, f.e. 'hd 756x554 video/mp4'
opt_label   (a.k.a. Event Label) url of downloaded file
opt_value   Integer, event value (empty)
*/
$(document).ready(function(){
    if ($('#download').length) {
        $('#download').find('a').click(function(ev){
            //ev.preventDefault();
            var url = ev.target.href;
            var title = $(this).attr('title');
            //console.log('t: ' + title + ' url: ' + url);
            _gaq.push(['_trackEvent', 'Downloads', title, url]);
        });
    }
});
