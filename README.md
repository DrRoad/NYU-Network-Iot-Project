##A Prototype of IoT Platform for Smart Cities

###About

This is a library for a IoT project - using AWS cloud as backend to control mobile devices with sensors, collecting data back to server and storing in database. The android app is modified from Aircasting   [aircasting.org](http://aircasting.org).

### Contribute

Mobile Team:     Joe Zuhusky,    Soumie Kumar  
Networking Team: Wesley Painter, Zal Bhathena  
Server Team:     Hongtao Cheng,  Wenliang Zhao  
Supervisors:     Prof. Lakshmi Subramanian, Fatima Zarinni, Shiva Radhakrishnan

#### Contact

You can contact the authors by email at [wz927@nyu.edu](mailto:info@wz927.nyu.edu).

### Usage
1. Package server folder as a jar (you can use IntelliJ)
2. Install the app in mobile folder into your android phone
3. Upload jar file onto AWS cloud, and run it. (make sure you have MySQL database on AWS and in java source code, make sure all passwords are right)
4. Open Aircasting app on you phone, the app will pin server immediately, and whenever server receieves the pin, it will send command to mobile to starting collecting data.
5. The result will be shown on google map and save as html file.
5. You can also setup an Apache web server if you want to check the results in real time.

### License
