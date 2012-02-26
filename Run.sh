#!/bin/bash

if [ -z "$CAVAHOME" ]; then
    echo "No CAVAHOME set. Exiting..."
    exit;
fi

LIBPATH=""$CAVAHOME"/lib/database/derby.jar:"$CAVAHOME"/lib/database/jaudiotagger-2.0.2.jar:"$CAVAHOME"/lib/last.fm/last.fm-bindings.jar:"$CAVAHOME"/lib/jotify/jcraft.jar:"$CAVAHOME"/lib/jotify/jotify.jar:"$CAVAHOME"/lib/jspot/jspot.jar:"$CAVAHOME"/lib/xugglerjava/commons-cli.jar:"$CAVAHOME"/lib/xugglerjava/logback-classic.jar:"$CAVAHOME"/lib/xugglerjava/logback-core.jar:"$CAVAHOME"/lib/xugglerjava/slf4j-api.jar:"$CAVAHOME"/lib/xugglerjava/xuggle-xuggler.jar"

RUNCOMMAND="java -Djava.library.path="$CAVAHOME"/src/cava/miraje/libmirajeaudio -cp $CLASSPATH:"$CAVAHOME"/src/:"$LIBPATH" $1 $2"

$RUNCOMMAND
