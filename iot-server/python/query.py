#! /usr/bin/env python

import mysql.connector
import gmplot, time

ms = int(round(time.time() * 1000))
ms = 124459

colors = {120: '#FF3933', 110: '#FF6E33', 100: '#FF8333', 90: '#FFAC33', 80: '#FFDA33',
           70: '#FFFC33', 60: '#C7FF33', 50: '#71FF33', 40: '#33FFA5', 30: '#33FFF0',
		   20: '#33D4FF', 10: '#3393FF', 0: '#334FFF'}
db = mysql.connector.connect(host="localhost", port=3306, user="root", passwd="5587900", db="IoT")
cursor = db.cursor()

query = cursor.execute("SELECT n.imei, n.time, n.lat, n.lon, n.noise FROM noise n INNER JOIN ( \
                        SELECT imei, max(time) as MaxTime from noise \
                        GROUP BY imei) n1 ON n.imei = n1.imei AND n.time = n1.MaxTime")
db.close()
data = cursor.fetchall()

lats = [[] for i in range(13)]
lons = [[] for i in range(13)]
#noises = [[] for i in range(13)]
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
    
    if row[2] < left_x:
        left_x = row[2]
    if row[2] > right_x:
        right_x = row[2]
    if row[3] < down_y:
        down_y = row[3]
    if row[3] > up_y:
        up_y = row[3]

print lats 
print lons
center_x = (left_x + right_x) / 2
center_y = (up_y + down_y) / 2
zoom = 14
gmap = gmplot.GoogleMapPlotter(center_y, center_x, zoom)
for i in range(13):
    if len(lats[i]) > 0:
        gmap.scatter(lats[i], lons[i], color=colors[i*10], size = 80, marker=False)
gmap.draw("mymap.html")
