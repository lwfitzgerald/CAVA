#! /bin/sh
if ./autogen.sh
then
	if make
	then
		mv .libs/libmirajeaudio.so ./
		mv .libs/libmiraje.so ./
	fi
fi
