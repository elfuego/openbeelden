/*
 * OpenStreetMap with OpenLayers (and jQuery).
 * Display, place and remove pins to show, create and delete locations related to media.
 * @author: André van Toly
 * @version: '$Id:  $'
 */
$(document).ready(function(){
    if ($('#openstreetmap').length) {
        var editSize = true;
        if ($('#openstreetmap').is('.small')) editSize = false;;
        var context = "/";
        if ($("meta[name='ContextRoot']").length) context = $("meta[name='ContextRoot']").attr("content");
		map = new OpenLayers.Map("openstreetmap");
        var markerArray = [];
		var markers, marker, pos, newMarkerPos;
        var count = 0;  // number of markers
                
        var lat = 51.507743;  // 28.609315, -17.923079
        var lon = -0.127931;
        var zoom = 10;

        // sizes for icons
        var size = new OpenLayers.Size(32,32);
        var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);

        // adapt default click behaviour to enable placing of pins
        if (editSize) {
            OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {                
                defaultHandlerOptions: {
                    'single': true,
                    'double': false,
                    'pixelTolerance': 4,
                    'stopSingle': false,
                    'stopDouble': false
                },
  
                initialize: function(options) {
                    this.handlerOptions = OpenLayers.Util.extend({}, this.defaultHandlerOptions);
                    OpenLayers.Control.prototype.initialize.apply(this, arguments); 
                    this.handler = new OpenLayers.Handler.Click(this, {'click': this.trigger }, this.handlerOptions);
                }, 
  
                trigger: function(ev) {
                    var lonlat = map.getLonLatFromPixel(ev.xy);
                    //console.log("New marker near " + lonlat.lat + " N, " + lonlat.lon + " E - " + ev.xy.x + "/" + ev.xy.y );
                    marker = blueMarker(lonlat);
                }  
            });
        }
        
        map.addLayer(new OpenLayers.Layer.OSM());
        markers = new OpenLayers.Layer.Markers("Markers");
        map.addLayer(markers);

        var blueIcon = new OpenLayers.Icon(context + 'style/images/loc-blue-32.png', size, offset);
        function blueMarker(p) {
			//console.log('new blue: ' + p);
			newMarkerPos = p;
            
			m = new OpenLayers.Marker(p, blueIcon);
            markers.addMarker(m);
			var np = p.transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));
            $('#geolongitude').val(np.lon.toFixed(6));
            $('#geolatitude').val(np.lat.toFixed(6));
            return m;
        } 
        
        function redMarker(p) {
			pos = p; // for just one location on map
			//console.log('new red: ' + p);
            
            var icon = new OpenLayers.Icon(context + 'style/images/loc-red-32.png', size, offset);
            var mark = new OpenLayers.Marker(p, icon);
            var myID = lonlatToId(p);
			mark.myId = myID;
			mark.myName = $('#' + myID).find('span.name').text();
            markers.addMarker(mark);
			markerArray.push(mark);

            mark.events.register('mousedown', mark, function(ev) {
                //console.log('You clicked : ' + this.myId);
				$('#geolist li span.name').css('font-weight', 'normal');
				$('#' + this.myId + ' span.name').css('font-weight', 'bold');
				if (editSize) {
                    this.popup = new OpenLayers.Popup.FramedCloud("Popup",
                        this.lonlat,
                        new OpenLayers.Size(80,80),
                        '<div>' + this.myName + '<br>' + this.lonlat.lat + ' N,<br />' + this.lonlat.lon + ' E</div>',
                        null, false);
                    map.addPopup(this.popup);
                }
            }, true);
            mark.events.register('mouseout', mark, function(ev) { 
                if (this.popup) { 
                    var p = this.popup;
                    this.popup = null;
                    setTimeout( function(){ p.destroy(); }, 2000);
                }
            });

			return mark;
        }
        
        // get location(s) from page
        $('.geolonlat').each(function(){
            count++;
			var lo = $(this).find('span.lon').text();
            var la = $(this).find('span.lat').text();

            var p = new OpenLayers.LonLat(lo, la).transform(
                new OpenLayers.Projection("EPSG:4326"),  // from WGS 1984
                map.getProjectionObject()  // Spherical Mercator
            );
            var myId = lonlatToId(p);
			$(this).attr('id', myId);
            redMarker(p);
            
            // cool marker bounce effect when clicked
            $(this).find('span').click(function(ev){
                ev.preventDefault();
                var id = $(ev.target).closest('li').attr('id');
                var mrk = getMyMarker(id);
                $(mrk.icon.imageDiv).effect("bounce", { times: 3 }, 400);
            });
            /* pinred = new OpenLayers.Icon('/style/images/loc-red-32.png', size, offset);
            marker = new OpenLayers.Marker(pos, pinred);
			marker.myId = myId;
			marker.myName = $(this).find('span.name').text();
            markers.addMarker(marker);
			markerArray.push(marker); */
        });
        
        // not all functions in small map
        if (editSize) {
            var click = new OpenLayers.Control.Click();
            map.addControl(click);
            click.activate();

            // shows coordinates while mousing over map
            map.addControl(new OpenLayers.Control.MousePosition(
                { displayProjection: new OpenLayers.Projection("EPSG:4326") }
            ));
        }
		
		function getMyMarker(id){
		    for (var i in markerArray){
		        if (markerArray[i].myId == id){
		            return markerArray[i];
		        }       
		    }
		}
		
		function removeMyMarker(id){
		    for (var i in markerArray){
		        if (markerArray[i].myId == id){
		            markers.removeMarker(markerArray[i]);	// Remove from layer
		            markerArray.splice(i, 1);	// Remove from array
		            return;
		        }       
		    }
		}
		
		// replace blue marker when zooming
		map.events.register("zoomend", map, function(ev){
			if (newMarkerPos != undefined) {
	            var np = new OpenLayers.LonLat(newMarkerPos.lon, newMarkerPos.lat).transform(
	                new OpenLayers.Projection("EPSG:4326"),  // from WGS 1984
	                map.getProjectionObject()  // Spherical Mercator
	            );
				marker = blueMarker(np);
			}
		});
		
        if (count == 0) {
            map.zoomToMaxExtent();
        } else if (count == 1) {
            // allways last?
            map.setCenter(pos, zoom);
        } else {
            var bounds = markers.getDataExtent();
            map.zoomToExtent(bounds, false);
        }
        
        // form stuff to save location starts here
        function addedLocation(data) {
				//console.log('added new location ' + newMarkerPos);
				if (newMarkerPos != undefined) {
                    var np = new OpenLayers.LonLat(newMarkerPos.lon, newMarkerPos.lat).transform(
                        new OpenLayers.Projection("EPSG:4326"),  // from WGS 1984
                        map.getProjectionObject()  // Spherical Mercator
                    );
                    redMarker(np);
                    newMarkerPos = undefined;
                }

				markers.clearMarkers();
                if (data != null) { 
                    $.grep(data, function(i) {
                        $('#geolist').append( $('.result').find('li') );
                    });
                }
			    for (var i in markerArray) {
		            //console.log('add: ' + i + ' : ' + markerArray[i]);
		            //console.log(markerArray[i]);
					markers.addMarker(markerArray[i]);
				}				
		}
        var options = {
            target: '#geofeedback',
            success: addedLocation
        };
        $('#geoform').ajaxForm(options);
		
		// delete marker
		/* $('.geolonlat a.delete').click(function(ev) {
			var markerId = $(this).parent('li').attr('id');
			var nodeId = $(ev.target).attr('href');
			
			var res = nodeId.match(/loc(\d+)/);
			var nr = res[1];
			console.log('nr: ' + nr);
            var params = new Object();
			params.delete = nr;
			$.ajax({
				async: false, 
				url: '/action/locsubmit.jspx', 
				data: params,
				success: function(data) {
					removeMyMarker(markerId);
					$('#' + markerId).fadeOut(400,function(){$('#' + markerId).remove();});
					$('#geofeedback').html('<p class="msg">Location (node #' + nr + ') deleted.</p>');
				},
				error: function(data) {
					$('#geofeedback').html('<p class="err">Error : ' + data + '</p>');
				}
			})
		}); */
    }
});

/* Creates an id from longitude and latitude usable in html and css */
function lonlatToId(lonlat) {	// 28.609315, -17.923079
	var lon = ""+lonlat.lon;
	var lat = ""+lonlat.lat;
	
	var newlon, newlat;
	var regex = /^(?:\+|-)?([0-9]+)\.([0-9]+)/;
	newlon = (lon.indexOf('-') > -1 ? 'S' : 'N'); 
	newlon = newlon + lon.replace(regex, "$1d$2");
	newlat = (lat.indexOf('-') > -1 ? 'W' : 'E'); 
	newlat = newlat + lat.replace(regex, "$1d$2");
	//console.log("id: " + newlon + newlat);
	return newlon+newlat;
}

function idToLonlat(id) {  // W72d363554S16d035956
    var lat, lon;
    var regex = /^[we]+([0-9]+)d([0-9]+)[ns]([0-9]+)d([0-9]+)$/i;
    var result = id.match(regex);
    if (result != null) {
        lat = result[1] + "." + result[2];
        lon = result[3] + "." + result[4];
    }
    if (id.toLowerCase().indexOf('w') > -1) lon = "-" + lon;
    if (id.toLowerCase().indexOf('s') > -1) lat = "-" + lat;
	//console.log("lon/lat: " + lon + "/" + lat);
	return { 'lon': lon, 'lat': lat };
}

