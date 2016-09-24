#!/bin/sh

DIR="$(dirname $(readlink -f $0))"
java -jar $DIR/laser-scanner-1.0.jar

