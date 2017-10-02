// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

#ifndef AUXILIARY_H
#define AUXILIARY_H

#ifdef __cplusplus
extern "C" {
#endif

#include "stdlib.h"

// allocate memory for an array argv
static inline char** make_argv(int argc) {
	return (char**)malloc(sizeof(char*) * argc);
}

// set argument in array argv
static inline void set_arg(char** argv, int i, char* str) {
	argv[i] = str;
}

// allocate memory for a char buffer
static inline char* make_buffer(int size) {
	return (char*)malloc(size * sizeof(char));
}

// free char buffer
static inline void free_buffer(char* buf) {
	free(buf);
}

// allocate memory for an array of chars
static inline char** make_array_char(int nitems, int nchars) {
	char** A = (char**)malloc(nitems * sizeof(char*));
	int i = 0;
	for (i = 0; i < nitems; i++) {
		A[i] = make_buffer(nchars);
	}
	return A;
}

// free array of chars memory
static inline void free_array_char(char** A, int nitems) {
	int i = 0;
	for (i = 0; i < nitems; i++) {
		free(A[i]);
	}
	free(A);
}

// return an item of array of chars
static inline char* array_item(char** A, int i) {
	return A[i];
}

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // AUXILIARY_H
