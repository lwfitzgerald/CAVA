#!/bin/bash

DEFAULTINSTALLLOCATION="/usr/local/CAVA"

if [ -z "$CAVAHOME" ]; then
    echo -e "Where do you want to install to (press enter for: "$DEFAULTINSTALLLOCATION")?: "
    read CAVAHOME
    echo "CAVAHOME:"$CAVAHOME
    if [ ! -n "$CAVAHOME" ]; then
        CAVAHOME=$DEFAULTINSTALLLOCATION
    fi
fi

if [ !  -d "$CAVAHOME" ]; then
    echo "Attempting to create CAVAHOME directory"
    mkdir $CAVAHOME
    if [ $? -ne 0 ]; then
        echo "Unable to make CAVAHOME directory. Do you need to run as root to install here?"
        exit
    fi
fi
LIBPATH=""$CAVAHOME"/lib/database/derby.jar:"$CAVAHOME"/lib/database/jaudiotagger-2.0.2.jar:"$CAVAHOME"/src/cava/Xuggler/:"$CAVAHOME"/lib/last.fm/last.fm-bindings.jar:"$CAVAHOME"/lib/jotify/jcraft.jar:"$CAVAHOME"/lib/jotify/jotify.jar:"/$CAVAHOME"lib/jotify/tritonus_jorbis-0.3.6.jar:"$CAVAHOME"/lib/jotify/tritonus_share-0.3.6.jar"

COMPILECOMMAND="javac -cp $CLASSPATH:"$CAVAHOME"/src/:"$LIBPATH" cava/*.java"

echo Copying files...
cp -r lib src $CAVAHOME
echo Compiling Java...
cd $CAVAHOME"/src"
$COMPILECOMMAND
if [ $? -eq 0 ]; then
    echo "Compilation complete"
else
    echo "Compilation failed"
    exit
fi

echo "Please set these environment variables before use:"
echo "export CAVAHOME="$CAVAHOME
echo "export XUGGLE_HOME="$CAVAHOME"/lib/xuggler"
echo "export PATH="$CAVAHOME"/lib/xuggler/bin:$PATH"
echo "export LD_LIBRARY_PATH="$CAVAHOME"/lib/xuggler/lib:$LD_LIBRARY_PATH"
echo "To run, navigate to "$CAVAHOME"/src and run Cava.sh"
exit

