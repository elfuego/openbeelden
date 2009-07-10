// -*- mode: javascript; -*-
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" 
%><mm:content type="text/javascript" language="${param.locale}">
<mm:import externid="editor" /> // editor: ${editor}
/* some used to customize searchrelate etc. */

function bindAllthese() {
    
    /* actions when relating nodes */
    $("div.mm_related").bind("mmsrRelate", function (e, tr, relater) { 
        if (e) {
            if (relater.needsCommit()) {
                relater.commit(e);
            }
            $(tr).find('img.editrelation').show();  // show editrelation button
        }
    });
    
    /* actions when unrelating nodes */
    $("div.mm_related").bind("mmsrUnrelate", function (e, tr, relater) { 
        if (e) {
            if (relater.needsCommit()) {
                //$(relater.div).find('div.submitbutton').show();
                $(relater.div).find('div.implicit').show();     // shows results of removal
                $(relater.div).find('div.mm_relate_repository img.editrelation').hide(); // hide editrelation button
                relater.commit(e);
            }
        }
    });
    
    /* show messages when (auto)committing */
    $("div.mm_related").bind("mmsrCommitted", function (e, submitter, status, relater, related, unrelated) { 
        $(relater.div).find("div.submitbutton").hide();
    
        if (status == "success") {
            if ($(relater.div).find('div.mm_relate_repository > div.succeeded').length == 0) {
                $(relater.div).find('div.mm_relate_repository').prepend('<div class="succeeded"></div>');
            } else {
                $(relater.div).find('div.mm_relate_repository > div.succeeded').show();
            }
            
            var msg = "";
            if (unrelated) msg += " Removed relation(s): " + unrelated;
            if (related) msg += " Saved new relation(s): " + related;
            $(relater.div).find('div.mm_relate_repository > div.succeeded').text(msg).fadeOut(2400);
            
            /* bind editrelation buttons */
            $(relater.div).find('img.editrelation').unbind('click');
            $(relater.div).find('img.editrelation').click(function(ev) {
                ev.stopPropagation();
                var img = ev.target;
                var nr = $(img).parents('tr').find("td.node.number").text();
                $('div.mm_relate_current').find("tr.node_" + nr).toggle();
            });
            
        }
        if (status == "failed") {
            $(relater.div).find('div.mm_relate_repository').prepend('<div class="error">Some error!</div>');
        }
    });
}

/* events on edit relation and edit node buttons */
function bindButtons(relater) {
    if (relater.canEditrelations) {
        /* show and hide tr with relation info */
        $(relater.div).find('img.editrelation').click(function(ev) {
            ev.stopPropagation();
            var img = ev.target;

            var rel = $(img).parent('a').attr("href");
            var id = rel.substring(rel.indexOf("#") + 1);
            $(relater.div).find('div.mm_relate_current tr.' + id).toggle();
        });
    }
    
    /* edit node */
    <mm:link page="$editor">var editor = "${_}";</mm:link>
    $(relater.div).find('img.editnode').click(function(ev) {
        ev.stopPropagation();
        var img = ev.target;
        var href = $(img).parent('a').attr("href");
        var node = href.substring(href.lastIndexOf("_") + 1);   // edit.jsp#node_348
        window.location = editor + "?nr=" + node;
    });
}

/* when ready */
$("div.mm_related").bind("mmsrRelaterReady", function (e, relater) { 
    bindButtons(relater);
});

$("div.mm_related").bind("mmsrPaged", function (e, status, relater) {
    bindButtons(relater);
});

$(document).ready(function() {
    $('tr.relation').hide();
    //$('div.mm_relate_repository div.submitbutton').hide();
    bindAllthese();
    //console.log("Editor: ${editor}");
});
</mm:content>
