#include "stdlib.h"
#include "jni.h"
#include "decQuad.h"

decQuad*
miraje_getdiagonalmatrix(int rows, int cols, decContext* set) {
	int i, j;

	decQuad* matrix = malloc((rows+1)*(cols+1) *sizeof(decQuad));

	for(i=0; i <= rows; i++) {
		for(j=0; j <= cols; j++) {
			if(i == j && i != 0) {
				decQuadFromInt32(&matrix[i * (cols+1) + j], (int32_t) 1);
			} else {
				decQuadZero(&matrix[i * (cols+1) + j]);
			}
		}
	}

	return matrix;
}

decQuad*
miraje_getdecquadmatrix(float *floatmatrix, int rows, int cols, decContext* set) {
	int i, j;
	decQuad* decquadmatrix = malloc((rows+1)*(cols+1) * sizeof(decQuad));

	// float->string buffer
	char string[DECQUAD_String];

	for(i=0; i <= rows; i++) {
		for(j=0; j <= cols; j++) {
			if(i == 0 || j == 0) {
				decQuadZero(&decquadmatrix[i * (cols+1) + j]);
			} else {
				snprintf(string, DECQUAD_String, "%.30f", floatmatrix[(i-1) * cols + (j-1)]);
				decQuadFromString(&decquadmatrix[i * (cols+1) + j], string, set);
			}
		}
	}

	return decquadmatrix;
}

void
miraje_updatefloatmatrix(float *floatmatrix, decQuad *decquadmatrix, int rows, int cols, decContext* set) {
	int i, j;

	char string[DECQUAD_String];

	for(i=1; i <= rows; i++) {
		for(j=1; j <= cols; j++) {
			decQuadToString(&decquadmatrix[i * (cols+1) + j], string);
			floatmatrix[(i-1) * cols + (j-1)] = atof(string);
		}
	}

	free(decquadmatrix);
}

int
miraje_gaussjordan(decQuad *a, decQuad *b, int rows, int cols, decContext* set) {
	int indxc[rows+1];
	int indxr[rows+1];
	int ipiv[rows+1];
	int i, icol = 0, irow = 0, j, k, l, ll;
	int32_t comparisonint;
	decQuad big, dum, pivinv, temp, temp2;

	for(j=1; j <= rows; j++) {
		ipiv[j] = 0;
	}

	for(i=1; i <= rows; i++) {
		decQuadZero(&big);
		for(j=1; j <= rows; j++) {
			if(ipiv[j] != 1){
				for(k=1; k <= rows; k++) {
					if(ipiv[k] == 0) {
						// Calculate abs(a[j * cols + k] and store in temp
						decQuadAbs(&temp, &a[j * (cols+1) + k], set);

						// Compare temp and big and store in temp2
						decQuadCompare(&temp2, &temp, &big, set);

						// Convert resulting decQuad int to a real int
						comparisonint = decQuadToInt32(&temp2, set, DEC_ROUND_HALF_UP);
						if(comparisonint != -1) {
							big = temp;
							irow = j;
							icol = k;
						}
					} else if(ipiv[k] > 1) {
						return 1;
					}
				}
			}
		}

		ipiv[icol]++;
		if(irow != icol) {
			for(l=1; l <= rows; l++) {
				temp = a[irow * (cols+1) + l];
				a[irow * (cols+1) + l] = a[icol * (cols+1) + l];
				a[icol * (cols+1) + l] = temp;
			}
			for(l=1; l <= rows; l++) {
				temp = b[irow * (cols+1) + l];
				b[irow * (cols+1) + l] = b[icol * (cols+1) + l];
				b[icol * (cols+1) + l] = temp;
			}
		}

		indxr[i] = irow;
		indxc[i] = icol;

		if(decQuadIsZero(&a[icol * (cols+1) + icol]) == 1) {
			return 2;
		}

		// Set up the constant 1
		decQuadFromInt32(&temp, (int32_t) 1);

		// Do the division
		decQuadDivide(&pivinv, &temp, &a[icol * (cols+1) + icol], set);

		a[icol * (cols+1) + icol] = temp;

		for(l=1; l <= rows; l++)
			decQuadMultiply(&a[icol * (cols+1) + l], &a[icol * (cols+1) + l], &pivinv, set);
		for(l=1; l <= rows; l++)
			decQuadMultiply(&b[icol * (cols+1) + l], &b[icol * (cols+1) + l], &pivinv, set);

		for(ll=1; ll <= rows; ll++) {
			if(ll != icol) {
				dum = a[ll * (cols+1) + icol];
				decQuadZero(&a[ll * (cols+1) + icol]);
				for(l=1; l <= rows; l++) {
					// Calculate a[icol * cols + l]*dum and store in temp
					decQuadMultiply(&temp, &a[icol * (cols+1) + l], &dum, set);

					// Do subtraction
					decQuadSubtract(&a[ll * (cols+1) + l], &a[ll * (cols+1) + l], &temp, set);
				}
				for(l=1; l <= rows; l++) {
					// Calculate b[icol * cols + l]*dum
					decQuadMultiply(&temp, &b[icol * (cols+1) + l], &dum, set);

					// Do subtraction
					decQuadSubtract(&b[ll * (cols+1) + l], &b[ll * (cols+1) + l], &temp, set);
				}
			}
		}
	}

	for(l=rows; l >= 1; l--) {
		if(indxr[l] != indxc[l]) {
			for(k=1; k <= rows; k++) {
				temp = a[k * (cols+1) + indxr[l]];
				a[k * (cols+1) + indxr[l]] = a[k * (cols+1) + indxc[l]];
				a[k * (cols+1) + indxc[l]] = temp;
			}
		}
	}

	return 0;
}

int
miraje_inversematrix(float *floatmatrix, int rows, int cols) {
	// Create working context
	decContext set;
	decContextDefault(&set, DEC_INIT_DECQUAD);

	// Generate Diagonal matrix
	decQuad *diagonalmatrix = miraje_getdiagonalmatrix(rows, cols, &set);

	// Convert float matrix to decQuad matrix
	decQuad *decquadmatrix = miraje_getdecquadmatrix(floatmatrix, rows, cols, &set);

	if(miraje_gaussjordan(decquadmatrix, diagonalmatrix, rows, cols, &set) != 0) {
		return 1;
	}

	miraje_updatefloatmatrix(floatmatrix, decquadmatrix, rows, cols, &set);

	return 0;
}

float*
miraje_multiplymatrix(float *matrix1, float *matrix2, int rows1, int cols1, int cols2) {
	float *result = calloc(rows1 * cols2, sizeof(float));
	int i, j, k;

	for(i=0; i < rows1; i++) {
		for(j=0; j < cols2; j++) {
			int idx = i*cols2 + j;
			int icols1 = i*cols1;
			for(k=0; k < cols1; k++) {
				result[idx] += matrix1[icols1 + k] * matrix2[k*cols2 + j];
			}
		}
	}

	return result;
}

JNIEXPORT jint JNICALL Java_cava_miraje_Matrix_miraje_1inversematrix
  (JNIEnv *env, jobject jobj, jfloatArray jfloatmatrix, jint rows, jint cols) {
	jfloat *floatmatrix = (*env)->GetFloatArrayElements(env, jfloatmatrix, 0);

	if(miraje_inversematrix(floatmatrix, rows, cols) != 0) {
		return 1;
	}

	(*env)->ReleaseFloatArrayElements(env, jfloatmatrix, floatmatrix, 0);

	return 0;
}

JNIEXPORT jfloatArray JNICALL Java_cava_miraje_Matrix_miraje_1multiplymatrix
  (JNIEnv *env, jobject jobj, jfloatArray jmatrix1, jfloatArray jmatrix2, jint rows1, jint cols1, jint cols2) {
	jfloat *matrix1 = (*env)->GetFloatArrayElements(env, jmatrix1, 0);
	jfloat *matrix2 = (*env)->GetFloatArrayElements(env, jmatrix2, 0);

	// Multiply matrices
	float *result = miraje_multiplymatrix(matrix1, matrix2, rows1, cols1, cols2);

	// Release old arrays
	(*env)->ReleaseFloatArrayElements(env, jmatrix1, matrix1, 0);
	(*env)->ReleaseFloatArrayElements(env, jmatrix2, matrix2, 0);

	// Create array to return result
	jfloatArray returnarray = (*env)->NewFloatArray(env, rows1 * cols2);
	(*env)->SetFloatArrayRegion(env, returnarray, 0, rows1 * cols2, result);

	free(result);

	return returnarray;
}
