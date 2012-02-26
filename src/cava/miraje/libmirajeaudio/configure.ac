dnl Process this file with autoconf to produce a configure script.
AC_PREREQ([2.54])
AC_INIT([MiraJe], [0.5.1])
AM_INIT_AUTOMAKE([foreign])
AC_CONFIG_MACRO_DIR([m4])

dnl pkg-config
AC_PATH_PROG(PKG_CONFIG, pkg-config, no)
if test "x$PKG_CONFIG" = "xno"; then
        AC_MSG_ERROR([You need to install pkg-config])
fi

AC_PROG_INSTALL

AC_PROG_LIBTOOL

IT_PROG_INTLTOOL([0.35.0])

expanded_libdir=`( case $prefix in NONE) prefix=$ac_default_prefix ;; *) ;; esac
                   case $exec_prefix in NONE) exec_prefix=$prefix ;; *) ;; esac
                   eval echo $libdir )`

AC_SUBST(expanded_libdir)

AM_CFLAGS="-O3"
CFLAGS="-O3"

dnl libmirageaudio
PKG_CHECK_MODULES(FFTW3F, fftw3f)
PKG_CHECK_MODULES(LIBSAMPLERATE, samplerate)
GSTREAMER_REQUIRED_VERSION=0.10.15
AC_SUBST(GSTREAMER_REQUIRED_VERSION)
PKG_CHECK_MODULES(GST,
    gstreamer-0.10 >= $GSTREAMER_REQUIRED_VERSION
    gstreamer-base-0.10 >= $GSTREAMER_REQUIRED_VERSION
    gstreamer-plugins-base-0.10 >= $GSTREAMER_REQUIRED_VERSION)
AC_SUBST(GST_CFLAGS)
AC_SUBST(GST_LIBS)
PKG_CHECK_MODULES(GLIB, glib-2.0)

AC_CHECK_PROG(JAVAH, javah, yes, no)
if test "x$JAVAH" = "xno"; then
	AC_MSG_ERROR([You need a Java VM and JDK installed])
fi

AC_ISC_POSIX
AC_PROG_CC
AC_HEADER_STDC
AM_PATH_GLIB_2_0

AC_OUTPUT(Makefile)
