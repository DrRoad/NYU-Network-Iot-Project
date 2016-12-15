#! /usr/bin/env python

import mysql.connector
import time
#import gmplot

def writeMap(center_lon, center_lat, zoom, filename, coords):
    f = open(filename, 'w')
    f.write('<html>\n')
    f.write('<head>\n')
    f.write(
        '<meta name="viewport" content="initial-scale=1.0, user-scalable=no" />\n')
    f.write(
        '<meta http-equiv="content-type" content="text/html; charset=UTF-8"/>\n')
    f.write('<title>Google Maps - pygmaps </title>\n')
    
    ### legend color
    f.write('<style type="text/css">\n')
    f.write('  #legend {\n')
    f.write('  background: #FFF;\n')
    f.write('  padding: 10px;\n')
    f.write('  margin: 5px;\n')
    f.write('  font-size: 12px;\n')
    f.write('  font-family: Arial, sans-serif;\n')
    f.write('}\n\n')
    
    f.write('.color {\n')
    f.write('  border: 1px solid;\n')
    f.write('  height: 12px;\n')
    f.write('  width: 12px;\n')
    f.write('  margin-right: 3px;\n')
    f.write('  float: left;\n')
    f.write('}\n\n')
    
    c = 1
    for k, v in sorted(colors.items()):
        f.write('.c'+str(c)+' {\n')
        f.write('  background: #'+v+'\n')
        f.write('}\n\n')
        c += 1
    
    f.write('</style>')
    
    f.write('<script type="text/javascript" src="https://maps.googleapis.com/maps/api/js?libraries=visualization&sensor=true_or_false"></script>\n')
    f.write('<script type="text/javascript">\n')
    f.write('\tfunction initialize() {\n')
    
    f.write('\t\tvar centerlatlng = new google.maps.LatLng(%f, %f);\n' % (center_lat, center_lon))
    f.write('\t\tvar myOptions = {\n')
    f.write('\t\t\tzoom: %d,\n' % (zoom))
    f.write('\t\t\tcenter: centerlatlng,\n')
    f.write('\t\t\tmapTypeId: google.maps.MapTypeId.ROADMAP\n')
    f.write('\t\t};\n')
    f.write(
        '\t\tvar map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);\n')
    f.write('\n')
    
    ### tp[0]: lat, tp[1]: lon, tp[2]: color, tp[3]: title
    for tp in coords:
        f.write('var myLatLng = {lat: %f, lng: %f};\n' % (tp[0], tp[1]))
        f.write('var pinImage = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + tp[3] + '");\n')
        f.write('var marker = new google.maps.Marker({\n')
        f.write('\tposition: myLatLng,\n')
        f.write('\tmap: map,\n')
        f.write('title: "' + tp[2] + '",\n')
        f.write('icon: pinImage\n')
        f.write('});\n')
        f.write('marker.setMap(map);\n\n')
        
    ### plot legend
    f.write('var legend = document.createElement("div");\n')
    f.write('legend.id = "legend";\n')
    f.write('var content = [];\n')
    f.write('content.push("<h3>Legend</h3>");\n')
    c = 1
    for k, v in sorted(colors.items()):
        f.write("content.push('<p><div class=" + '"color c' + str(c) + '"></div>' + str(k) + ' dB' + "</p>');\n")
        c += 1
    f.write("legend.innerHTML = content.join('');\n")
    f.write('legend.index = 1;\n')
    f.write('map.controls[google.maps.ControlPosition.RIGHT_BOTTOM].push(legend);\n')
        
    f.write('\t}\n')
    f.write('</script>\n')
    f.write('</head>\n')
    f.write(
        '<body style="margin:0px; padding:0px;" onload="initialize()">\n')
    f.write(
        '\t<div id="map_canvas" style="width: 100%; height: 100%;"></div>\n')
    f.write('</body>\n')
    f.write('</html>\n')
    f.close()

ms = int(round(time.time() * 1000))
#ms = 124459

colors = {120: 'FF3933', 110: 'FF6E33', 100: 'FF8333', 90: 'FFAC33', 80: 'FFDA33',
           70: 'FFFC33', 60: 'C7FF33', 50: '71FF33', 40: '33FFA5', 30: '33FFF0',
		   20: '33D4FF', 10: '3393FF', 0: '334FFF'}
db = mysql.connector.connect(host="localhost", port=3306, user="root", passwd="5587900", db="IoT")
cursor = db.cursor()

query = cursor.execute("SELECT n.imei, n.time, n.lat, n.lon, n.noise FROM noise n INNER JOIN ( \
                        SELECT imei, max(time) as MaxTime from noise \
                        GROUP BY imei) n1 ON n.imei = n1.imei AND n.time = n1.MaxTime")
db.close()
data = cursor.fetchall()

lats = [[] for i in range(13)]
lons = [[] for i in range(13)]
noises = [[] for i in range(13)]
ids = [[] for i in range(13)]
left_x = 180.
right_x = -180.
up_y = 0.
down_y = 90.
for row in data:
    tmp_ms = row[1]
    if ms - tmp_ms > 60000:
        continue

    tmp_color = int(row[4] / 10)
    if tmp_color > 12:
        tmp_color = 120
    elif tmp_color < 0:
        tmp_color = 0
    lats[tmp_color].append(row[3])
    lons[tmp_color].append(row[2])
    noises[tmp_color].append(row[4])
    ids[tmp_color].append(row[0])    

    if row[2] < left_x:
        left_x = row[2]
    if row[2] > right_x:
        right_x = row[2]
    if row[3] < down_y:
        down_y = row[3]
    if row[3] > up_y:
        up_y = row[3]

center_x = (left_x + right_x) / 2
center_y = (up_y + down_y) / 2
zoom = 14

"""
gmap = gmplot.GoogleMapPlotter(center_y, center_x, zoom)
for i in range(13):
    if len(lats[i]) > 0:
        gmap.scatter(lats[i], lons[i], color=colors[i*10], size = 80, marker=False)
gmap.draw("mymap.html")
"""

coords = []
for i in range(13):
    if len(lats[i]) > 0:
        coords += map(lambda tp: (tp[0], tp[1], tp[2] + ": %.2f" % tp[3], colors[i*10]), zip(lats[i], lons[i], ids[i], noises[i]) )

writeMap(center_x, center_y, zoom, '~/mymap.html', coords)

