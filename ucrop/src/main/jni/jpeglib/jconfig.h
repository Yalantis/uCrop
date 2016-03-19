/* jconfig.h.  Generated from jconfig.h.in by configure.  */
/* Version ID for the JPEG library.
 * Might be useful for tests like "#if JPEG_LIB_VERSION >= 60".
 */
#define JPEG_LIB_VERSION 62

/* Support arithmetic encoding */
#define C_ARITH_CODING_SUPPORTED 1

/* Support arithmetic decoding */
#define D_ARITH_CODING_SUPPORTED 1

/* Define if your compiler supports prototypes */
#define HAVE_PROTOTYPES 1

/* Define to 1 if you have the <stddef.h> header file. */
#define HAVE_STDDEF_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if the system has the type `unsigned char'. */
#define HAVE_UNSIGNED_CHAR 1

/* Define to 1 if the system has the type `unsigned short'. */
#define HAVE_UNSIGNED_SHORT 1

/* Define if you want use complete types */
/* #undef INCOMPLETE_TYPES_BROKEN */

/* Define if you have BSD-like bzero and bcopy */
/* #undef NEED_BSD_STRINGS */

/* Define if you need short function names */
/* #undef NEED_SHORT_EXTERNAL_NAMES */

/* Define if you have sys/types.h */
#define NEED_SYS_TYPES_H 1

/* Define if shift is unsigned */
/* #undef RIGHT_SHIFT_IS_UNSIGNED */

/* Use accelerated SIMD routines. */
#define WITH_SIMD 1

/* Define to 1 if type `char' is unsigned and you are not using gcc.  */
#ifndef __CHAR_UNSIGNED__
/* # undef __CHAR_UNSIGNED__ */
#endif

/* Define to empty if `const' does not conform to ANSI C. */
/* #undef const */

/* Define to `__inline__' or `__inline' if that's what the C compiler
   calls it, or to nothing if 'inline' is not supported under any name.  */
#ifndef __cplusplus
/* #undef inline */
#endif

#define INLINE inline

/* Define to `unsigned int' if <sys/types.h> does not define. */
/* #undef size_t */
