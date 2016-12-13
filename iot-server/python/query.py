#! /usr/bin/env python

import MySQLdb

db = MySQLdb.connect(host="localhost", port=3306, user="root", passwd="1234", db="IoT")
cursor = db.cursor()

query = cursor.execute("SELECT * FROM noise")
data = cursor.fetchall()
for row in data:
    print row

db.close()
