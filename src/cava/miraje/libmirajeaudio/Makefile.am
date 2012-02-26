INCLUDES = \
        -I$(top_srcdir) \
        -Wall \
        -D_FORTIFY_SOURCE=2 \
        $(GLIB_CFLAGS) \
        $(GST_CFLAGS) \
        $(LIBSAMPLERATE_CFLAGS) \
        $(FFTW3F_CFLAGS) \
        -I /usr/lib/jvm/java-6-sun/include/ \
        -I /usr/lib/jvm/java-6-sun/include/linux/ \
        -I libdecnumber/

mirajeaudiolibdir = $(expanded_libdir)
mirajeaudiolib_LTLIBRARIES = libmirajeaudio.la
libmirajedir = $(expanded_libdir)
libmiraje_LTLIBRARIES = libmiraje.la

libmirajeaudio_la_LDFLAGS = -avoid-version -module
libmirajeaudio_la_SOURCES = \
        gst-mirajeaudio.c \
        gst-mirajeaudio.h

libmirajeaudio_la_LIBADD = \
        $(GLIB_LIBS) \
        $(LIBSAMPLERATE_LIBS) \
        $(FFTW3F_LIBS) \
        $(GST_LIBS)

libmiraje_la_LDFLAGS = -avoid-version -module
libmiraje_la_SOURCES = \
	libmiraje.c \
	libdecnumber/decQuad.c \
	libdecnumber/decContext.c

MAINTAINERCLEANFILES = Makefile.in
EXTRA_DIST = $(libmirajeaudio_la_SOURCES) \
	     $(libmiraje_la_SOURCES)
