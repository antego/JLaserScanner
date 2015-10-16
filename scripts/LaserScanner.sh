#!/bin/sh

DIR="$(dirname $(readlink -f $0))"
java  -Djava.library.path="$DIR/lib" -jar $DIR/Laser_Scanner-1.0-SNAPSHOT.jar

