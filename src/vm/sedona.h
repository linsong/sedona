//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Mar 07  Brian Frank  Creation
//

#ifndef __SEDONA_H
#define __SEDONA_H

//////////////////////////////////////////////////////////////////////////
// Overview
//////////////////////////////////////////////////////////////////////////

//
// This header file defines the key types and macros used by the Sedona
// VM for different platforms.  For each target platform the following
// constructs must be defined by this header file:
//
// Integer types as defined by the C 99 standard in "stdint.h":
//   bool       TRUE or FALSE
//   int8_t     signed 8 bit (1 byte)
//   uint8_t    unsigned 8 bit (1 byte)
//   int16_t    signed 16 bit (2 byte)
//   uint16_t   unsigned 16 bit (2 byte)
//   int32_t    signed 32 bit (4 byte)
//   uint32_t   unsigned 32 bit (4 byte)
//   int64_t    signed 64 bit (8 byte)
//   uint64_t   unsigned 64 bit (8 byte)
//
// Other misc types and macros:
//   size_t     size to use for memory block lengths
//   NULL       null pointer
//   TRUE       1
//   FALSE      0
//
// Endian
//   Must define either IS_LITTLE_ENDIAN or IS_BIG_ENDIAN.
//
// Sedona Code Block Size
//   Must define SCODE_BLOCK_SIZE as a number of bytes each Sedona
//   block occupies.  The total scode address space is defined
//   by (2^16 * blockSize).  Use a larger block size for bigger
//   images, or a small size like 1 byte to optimize for tight
//   code space.
//
// Sedona Code Block Size <-> Address
//   Must define the following macro which takes a code base addr
//   and Sedona block index and and converts it into a memory pointer:
//     uint8_t* block2addr(uint8_t* cb, uint16_t block)
//     addr = cb + block*blockSize
//
// Memory Management
//   The following standard library functions must be available
//   for memory management:
//     void* malloc(size_t)
//     void free(void*)
//     void memmove(void*, const void*, size_t)
//     void memset(void*, int, size_t)
//
// Main
//   If you wish to use the standard command line main that loads
//   from a file, then define the USE_STANDARD_MAIN macro.
//
// Debug
//   Define SCODE_DEBUG to turn on all scode debug code (takes
//   up a lot of space and relies on printf for output).
//
// Computed Goto
//   The main loop of the VM can be toggled to use either a switch
//   statement or computed gotos.  Computed gotos are not ANSI C and
//   only available for GCC.  The default code compiles as switch.  But
//   if using GCC then define COMPUTED_GOTO to compile with gotos.
//   Computed gotos will perform better because switch generates
//   extra machine instructions for the range check.  Tests on Jennic
//   showed compute gotos increased performance by 8%.
//

////////////////////////////////////////////////////////////////
// Windows
////////////////////////////////////////////////////////////////

#if defined( _WIN32 )

// turn off annoying Windows deprecation notices (must be before stdio.h)
#define _CRT_SECURE_NO_DEPRECATE

// includes
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

// stdint.h C99 Exact-width integer types
typedef unsigned __int8   bool;
typedef __int8            int8_t;
typedef __int16           int16_t;
typedef __int32           int32_t;
typedef __int64           int64_t;
typedef unsigned __int8   uint8_t;
typedef unsigned __int16  uint16_t;
typedef unsigned __int32  uint32_t;
typedef unsigned __int64  uint64_t;
typedef short             int_least16_t;

// macros
#define USE_STANDARD_MAIN
#define IS_LITTLE_ENDIAN
#define SCODE_BLOCK_SIZE 4
#define block2addr(cb, block) ((cb) + (block<<2))
#define SCODE_DEBUG
#define TRUE 1
#define FALSE 0

#define ISNANF(f) (_isnan((double)(f)))
#define ISNAN(d)  (_isnan(d))


////////////////////////////////////////////////////////////////
// QNX
////////////////////////////////////////////////////////////////

#elif defined( __QNX__ )

// includes
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>
#include <math.h>

// macros
#define USE_STANDARD_MAIN
#define SCODE_BLOCK_SIZE 4
#define block2addr(cb, block) ((cb) + (block<<2))
#define SCODE_DEBUG
#ifndef TRUE
  #define TRUE 1
#endif
#ifndef FALSE
  #define FALSE 0
#endif

#ifdef __LITTLEENDIAN__
  #define IS_LITTLE_ENDIAN
#elif defined(__BIGENDIAN__)
  #define IS_BIG_ENDIAN
#endif

#define _chdir  chdir


#define ISNANF(f) (isnan(f))
#define ISNAN(d)  (isnan(d))



////////////////////////////////////////////////////////////////
// UNIX
////////////////////////////////////////////////////////////////

#elif defined( __UNIX__ )

// includes
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <endian.h>
#include <sys/stat.h>
#include <math.h>

// debug
#define SCODE_DEBUG

// determine endianness
#ifdef __BYTE_ORDER
#  if __BYTE_ORDER == __LITTLE_ENDIAN
#    define IS_LITTLE_ENDIAN
#  else
#    define IS_BIG_ENDIAN
#  endif
#elif defined (BYTE_ORDER) && defined(LITTLE_ENDIAN) && defined(BIG_ENDIAN)  
#  if BYTE_ORDER == LITTLE_ENDIAN
#    define IS_LITTLE_ENDIAN
#  else
#    define IS_BIG_ENDIAN
#  endif
#endif

// macros
#define USE_STANDARD_MAIN
#define SCODE_BLOCK_SIZE 4
#define block2addr(cb, block) ((cb) + (block<<2))
#define _chdir chdir

#ifndef TRUE
#  define TRUE 1
#endif

#ifndef FALSE
#  define FALSE 0
#endif


#define ISNANF(f) (isnanf(f))
#define ISNAN(d)  (isnan(d))


#endif   

//////// end of definitions for standard platforms ( _WIN32, __QNX__, __UNIX__ ) ////////


////////////////////////////////////////////////////////////////
//
// If none of the above platforms, put definitions into a file
// named sedona-local.h and define SEDONA_LOCAL_H on the compiler
// command line.
//
////////////////////////////////////////////////////////////////

#ifdef SEDONA_LOCAL_H

 #include <sedona-local.h>

#endif



////////////////////////////////////////////////////////////////
// Error Checking
////////////////////////////////////////////////////////////////

// endian
#ifdef IS_BIG_ENDIAN
#elif defined(IS_LITTLE_ENDIAN)
#else
#error "Must define IS_BIG_ENDIAN or IS_LITTLE_ENDIAN"
#endif

// sedona block size
#ifndef SCODE_BLOCK_SIZE
#error "Must define SCODE_BLOCK_SIZE"
#endif

// sedona word size
#ifndef block2addr
#error "Must define block2addr(cb, block)"
#endif

// isNan macro
#if !defined( ISNAN ) || !defined( ISNANF )
#error "Must define ISNAN() and ISNANF() macros"
#endif

//////////////////////////////////////////////////////////////////////////
// Standard Definitions
//////////////////////////////////////////////////////////////////////////

// C++
#ifdef __cplusplus
extern "C" {
#endif

//////////////////////////////////////////////////////////////////////////
// Macros
//////////////////////////////////////////////////////////////////////////

#define NULLBOOL   2
#define NULLFLOAT  0x7fc00000
#ifdef _WIN32
  #define NULLDOUBLE 0x7ff8000000000000i64
#else
  #define NULLDOUBLE 0x7ff8000000000000ll
#endif

//////////////////////////////////////////////////////////////////////////
// Cell Definitions
//////////////////////////////////////////////////////////////////////////

// Cell is a single stack unit capable of holding
// a 32-bit int, 32-bit float, or a memory pointer
typedef union
{
  int32_t ival;    // 32-bit signed int
  float   fval;    // 32-bit float
  void*   aval;    // address pointer
}
Cell;

// Cell constants
extern Cell zeroCell;       // Cell constant for false, 0, 0.0f, and NULL
extern Cell oneCell;        // Cell constant for true, 1
extern Cell negOneCell;     // Cell constant for -1
#define nullCell  zeroCell  // Cell constant for NULL
#define falseCell zeroCell  // Cell constant for false
#define trueCell  oneCell   // Cell constant for true

//////////////////////////////////////////////////////////////////////////
// Type Definitions
//////////////////////////////////////////////////////////////////////////

// Primitive Type Ids
#define VoidTypeId    0
#define BoolTypeId    1
#define ByteTypeId    2
#define ShortTypeId   3
#define IntTypeId     4
#define LongTypeId    5
#define FloatTypeId   6
#define DoubleTypeId  7
#define BufTypeId     8

//////////////////////////////////////////////////////////////////////////
// SedonaVM Definitions
//////////////////////////////////////////////////////////////////////////

struct SedonaVM_s;


// NativeMethod is a function pointer which implements
// a native method.  All native methods must return a Cell
// and take a pointer to the parameter Cells.
// NOTE: native methods which return long/double don't
//  return a Cell, instead they return an int64_t - they
//  are handled by a special CallNativeWide opcode.
typedef Cell (*NativeMethod)(struct SedonaVM_s* vm, Cell* params);
typedef int64_t (*NativeMethodWide)(struct SedonaVM_s* vm, Cell* params);


// SedonaVM
typedef struct SedonaVM_s
{
  // memory segments
  const uint8_t* codeBaseAddr;  // pointer to base of code image
  size_t    codeSize;           // num bytes in code image
  uint8_t*  stackBaseAddr;      // pointer to base of stack segment
  size_t    stackMaxSize;       // num bytes available for stack
  Cell*     sp;                 // current stack pointer

  // main method arguments
  const char** args;            // list of C string arguments
  int32_t argsLen;              // number of arguments

  // callbacks (or NULL to disable)
  void (*onAssertFailure)(const char* location, uint16_t linenum);

  // results
  unsigned assertSuccesses;  // num of times assert(true) was called
  unsigned assertFailures;   // num of times assert(false) was called

  // native method table - pointer to the 2D array of NativeMethod pointers
  NativeMethod** nativeTable;

  // function to call a native method
  int (*call)(struct SedonaVM_s* vm, uint16_t method, Cell* args, int argc);

  // private fields
  uint8_t*  dataBaseAddr;     // base for static field data
}
SedonaVM;

// Virtual Machine Control
extern int vmRun(SedonaVM* vm);
extern int vmResume(SedonaVM* vm);
extern int vmCall(SedonaVM* vm, uint16_t method, Cell* args, int argc);

// Virtual Machine Debug
#ifdef SCODE_DEBUG
extern const char* qnameType(SedonaVM* vm, uint16_t block);
extern const char* qnameSlot(SedonaVM* vm, uint16_t block);
extern const char* curMethod(SedonaVM* vm, Cell* fp);
extern const char* opcodeToName(int opcode);
extern void dumpStack(SedonaVM* vm, Cell* sp);
#endif

//////////////////////////////////////////////////////////////////////////
// Getters
//////////////////////////////////////////////////////////////////////////

// getters
extern void*    getConst(SedonaVM* vm, void* self, int offset);
extern uint8_t  getByte(void* self, int offset);
extern uint16_t getShort(void* self, int offset);
extern int32_t  getInt(void* self, int offset);
extern float    getFloat(void* self, int offset);
extern int64_t  getWide(void* self, int offset);
extern void*    getRef(void* self, int offset);
extern void*    getInline(void* self, int offset);

// setters
extern void setByte(void* self, int offset, uint8_t val);
extern void setShort(void* self, int offset, uint16_t val);
extern void setInt(void* self, int offset, int32_t val);
extern void setFloat(void* self, int offset, float val);
extern void setWide(void* self, int offset, int64_t val);
extern void setRef(void* self, int offset, void* val);

// sys::Component
#define getCompType(vm, self)   getConst(vm, self, 2)

// sys::Kit
#define getKitName(vm, self)    getConst(vm,  self, 2)

// sys::Type
#define getTypeId(vm, self)     getByte(self, 0)
#define getTypeName(vm, self)   getConst(vm, self, 2)
#define getTypeKit(vm, self)    getConst(vm, self, 4)
#define getTypeBase(vm, self)   getConst(vm, self, 6)
#define getTypeSizeof(self)     getShort(self, 8)
#define getTypeInit(self)       getShort(self, 10)

// sys::Slot
#define getSlotName(vm, self)   getConst(vm, self, 2)
#define getSlotType(vm, self)   getConst(vm, self, 4)
#define getSlotHandle(vm, self) getShort(self, 6)

// end C++
#ifdef __cplusplus
}
#endif

// end __SEDONA_H
#endif
