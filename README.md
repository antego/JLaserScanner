# Java-Laser-Scanner
Program for 3D-scanning written in java

## Building and packaging with maven
1. Add two opencv libraries to your local maven repository.
jar library:
```shell
mvn install:install-file \
  -Dfile=<path to opencv jar> \
  -DgroupId=org.opencv \
  -DartifactId=opencv_jar \
  -Dversion=<version of opencv lib> \
  -Dpackaging=jar \
  -DgeneratePom=true 
```
native library:
    mvn install:install-file \
      -Dfile=<path to opencv native lib (.so or .dll)> \
      -DgroupId=org.opencv \
      -DartifactId=<"libopencv_native" for linux and "opencv_native" for windows> \
      -Dversion=<version of opencv lib> \
      -Dpackaging=<.so for linux and .dll for windows> \
      -DgeneratePom=true 
2. Check opencv dependencies versions in `pom.xml`. 
3. Now you can build and test project with maven, just go into project directory and execute corresponding maven goals.

##Running app
Maven will package application in zip file. Packaged archive will be in target folder. Extract it and run LaserScanner.sh or LaserScanner.bat.

