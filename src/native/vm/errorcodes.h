//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   6 Mar 07  Brian Frank  Creation
//

#ifndef __ERROR_CODES_H
#define __ERROR_CODES_H

//
// Errors codes are assigned as follows:
//
//   Non-recoverable:
//     [C code: 1-39]
//     [Sedona: 40-99]
//     - don't attempt to auto-restart application because 
//       the vm, scode, or app itself is invalid
//
//   Recoverable: 
//     [C code: 100-139]
//     [Sedona: 140-249]
//     - something went wrong at runtime, but if we auto-restart 
//       the app things will probably start working again (at 
//       least for a little while)
//
//   Special: 
//     [250-255]
//     - special codes shared b/w C and Sedona code
//                     


// Convenience defns for detecting error type
#define ERR_MIN_UNRECOVERABLE_SCODE 1
#define ERR_MAX_UNRECOVERABLE_SCODE 39
#define ERR_MIN_UNRECOVERABLE_APP 40
#define ERR_MAX_UNRECOVERABLE_APP 99


// non-recoverable: bootstrap
#define ERR_MALLOC_IMAGE            1
#define ERR_MALLOC_STACK            2
#define ERR_MALLOC_STATIC_DATA      3
#define ERR_INPUT_FILE_NOT_FOUND    4
#define ERR_CANNOT_READ_INPUT_FILE  5

// non-recoverable: bad image
#define ERR_BAD_IMAGE_MAGIC         6
#define ERR_BAD_IMAGE_VERSION       7
#define ERR_BAD_IMAGE_BLOCK_SIZE    8
#define ERR_BAD_IMAGE_REF_SIZE      9
#define ERR_BAD_IMAGE_CODE_SIZE     10

// non-recoverable: runtime errors
#define ERR_UNKNOWN_OPCODE          11
#define ERR_MISSING_NATIVE          12

// recoverable: runtime errors
#define ERR_NULL_POINTER            100
#define ERR_STACK_OVERFLOW          101
#define ERR_INVALID_METHOD_PARAMS   102

// special codes (shared with Sedona)
#define ERR_YIELD                   253
#define ERR_RESTART                 254
#define ERR_HIBERNATE               255

#endif
