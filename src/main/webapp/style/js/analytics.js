/*
Track downloads with Google Analytics
https://developers.google.com/analytics/devguides/collection/gajs/methods/gaJSApiEventTracking

category    Downloads
action      nodenumber media item plus title tag of download link, f.e. '786 hd video/mp4'
opt_label   (a.k.a. Event Label) url of downloaded file
opt_value   Integer, event value (empty)
*/
$(document).ready(function(){
    if ($('#download').length) {
        $('#download').find('a').click(function(ev){
            var url = ev.target.href;
            var page = window.location.pathname;
            var result = page.match(/\/(\d+)\//);
            if (result != null) { 
                var media = result[1]; 
            } else { var media = page; }
            var title = $(this).attr('title');
            var label = media + " " +  title;
            //console.log('url: ' + url + ' label: ' + label);
            _gaq.push(['_trackEvent', 'Downloads', url, label]);
        });
    }
});
