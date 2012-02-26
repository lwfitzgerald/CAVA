/*
 * MiraJe - Java port of Mirage
 * High Performance Music Similarity and Automatic Playlist Generator
 * 
 * Original Mirage Mono / C# Source code - http://hop.at/mirage
 * Copyright (C) 2007 Dominik Schnitzer <dominik@schnitzer.at>
 * 
 * Java port MiraJe
 * Copyright (C) 2010 Luke Fitzgerald <lf8975@bris.ac.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

#include <glib.h>
#include <jni.h>

typedef struct MiraJeAudio MiraJeAudio;

MiraJeAudio*
mirajeaudio_initialise(gint rate, gint seconds, gint winsize, gboolean debug);

float*
mirajeaudio_decode(MiraJeAudio *ma, const gchar *file, int *frames, int *size, int *ret);

void
mirajeaudio_destroy(MiraJeAudio *ma);

void
mirajeaudio_canceldecode(MiraJeAudio *ma);

#ifndef _Included_cava_miraje_AudioDecoder
#define _Included_cava_miraje_AudioDecoder
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     cava_miraje_AudioDecoder
 * Method:    mirajeaudio_initialise
 * Signature: (IIIZ)J
 */
JNIEXPORT jlong JNICALL Java_cava_miraje_AudioDecoder_mirajeaudio_1initialise
(JNIEnv *, jobject, jint, jint, jint, jboolean);

/*
 * Class:     cava_miraJe_AudioDecoder
 * Method:    mirajeaudio_decode
 * Signature: (JLjava/lang/String;[I[I[I)[F
 */
JNIEXPORT jfloatArray JNICALL Java_cava_miraje_AudioDecoder_mirajeaudio_1decode
(JNIEnv *, jobject, jlong, jstring, jintArray, jintArray, jintArray);

/*
 * Class:     cava_miraJe_AudioDecoder
 * Method:    mirajeaudio_destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_cava_miraje_AudioDecoder_mirajeaudio_1destroy
(JNIEnv *, jobject, jlong);

/*
 * Class:     cava_miraJe_AudioDecoder
 * Method:    mirajeaudio_canceldecode
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_cava_miraje_AudioDecoder_mirajeaudio_1canceldecode
(JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
