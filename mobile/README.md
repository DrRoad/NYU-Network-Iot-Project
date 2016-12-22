##AirCasting - Share your Air!

###About

This is the android app for the AirCasting project - the project aims to build a platform for gathering, visualization and sharing of environmental data. To learn more about the platform visit [aircasting.org](http://aircasting.org).

### Additional Notes

This Version of the App was developed for a project at NYU for graduate Networks & Mobile Systems course. To get started with the app, follow all of the instructions listed below. I.e. download the entire codebase, make sure maven is installed on your machine, and use the Maven SDK deployer to put the Android SDK into your maven repo for builds.

-Some Issues we ran into when we first got started with the app: (1) Make sure you have all of the packages installed. The necessary packages can be seen in XML format in the pom.xml file. (2) We ran into some compilation issues, where every test failed. A package was needed, and the dependency that fixed the issue has been added to the pom.xml file. (3) RECOMMENDATION: Do not try to use android studio for development. We had issues and could never get the app to compile when trying to migrate to gradle. I recommend using an Eclipse IDE, we used a "neon" version for android dev, you can find one of these on the web. Import your project to eclipse as a maven project and it should work out fine. From there, only code in Eclipse, use a command line to compiler code. "mvn install" from command line in Main directory, and then use adb to install to a device via USB. (4) Do some reading on EventBus from Google Guava, the app uses it extensively, as does our extension code. Basically its a singleton accessable anywhere in the app. Its useful for developing event-driven code, but most of the time it's impossible to follow flows of execution. Our extension code tries to use meaningful event names to aid with code readability. (5) When originally working on the app, we had no idea where to start reading code. Important classes to read and understand, it will help you get up and running faster by understanding these classes -> "ButtonsAcitivity.java" "Intents.java" "SessionManager.java" "SessionRepository.java" "MeasurementRepository.java" "AudioReader.java" "SyncDriver.java" "SessionDriver.java" 

### Contribute

If you'd like to contribute just use the usual github process - fork, make changes, issue a pull request.

Grab maven (http://maven.apache.org) and Android SDK (http://developer.android.com/sdk/index.html), then use Maven SDK Deployer (https://github.com/mosabua/maven-android-sdk-deployer/) to put Android SDK into your local maven repository.

#### Contact

You can contact the authors by email at [info@habitatmap.org](mailto:info@habitatmap.org).

### Thanks
AirCasting uses The YourKit Java Profiler for Performance Tuning

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products: [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).

### License

The project is licensed under the GPLv3. For more information see COPYING and visit [http://www.gnu.org/licenses/gpl-3.0.html](http://www.gnu.org/licenses/gpl-3.0.html).
