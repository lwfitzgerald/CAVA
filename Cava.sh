#!/bin/bash

#Check to see if CAVAHOME has already been set. If it hasn't use
#(my) default location
if [ -z "$CAVAHOME" ]; then
    CAVAHOME="/home/ben/Documents/CAVA/trunk"
fi

#If no argument passed, assume mirage is available
#Otherwise, assume it is not
if [  -z "$1" ]; then
    MIRAGEAVAILBLE=true
else
    MIRAGEAVAILBLE=false
fi

#Later we'll define paths to fonts, images and track database relative to
#CAVAHOME. We'll also set it up so mirageavailable is passed in
#AAAND, we'll set the classpath here rather than relying on the system :)
#if [ "$MIRAGEAVAILBLE" = true ]; then

#fi

LIBPATH=""$CAVAHOME"/lib/database/derby.jar:"$CAVAHOME"/lib/database/jaudiotagger-2.0.2.jar:"$CAVAHOME"/lib/last.fm/last.fm-bindings.jar:"$CAVAHOME"/lib/jotify/jcraft.jar:"$CAVAHOME"/lib/jotify/jotify.jar:"$CAVAHOME"/lib/jspot/jspot.jar:"$CAVAHOME"/lib/xugglerjava/commons-cli.jar:"$CAVAHOME"/lib/xugglerjava/logback-classic.jar:"$CAVAHOME"/lib/xugglerjava/logback-core.jar:"$CAVAHOME"/lib/xugglerjava/slf4j-api.jar:"$CAVAHOME"/lib/xugglerjava/xuggle-xuggler.jar"

RUNCOMMAND="java -splash:"$CAVAHOME"/src/cava/images/splash.png -Djava.library.path="$CAVAHOME"/src/cava/miraje/libmirajeaudio -cp $CLASSPATH:"$CAVAHOME"/src/:"$LIBPATH" cava.Cava"

$RUNCOMMAND
