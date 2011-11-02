//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 May 09  Brian Frank  Move methods from sys_File_std.c
//

#include "sedona.h"


// Define this to implement "scheme" convention for specifying kit/manifest DB locations
#define IMPL_SCHEME_CONVENTION

// =========================================================================== //
#ifdef IMPL_SCHEME_CONVENTION

#include "stdlib.h"
#include "string.h"


char fullpath[1024];   // buf to hold complete path
char  schemestr[32];   // buf to hold scheme string

#define SCHEME_DELIM (':')


#define MAX_NUM_SCHEMES 3
const char* schemes[MAX_NUM_SCHEMES]     = { "m", "k", "sedona" };
const char* schemePaths[MAX_NUM_SCHEMES] = { "/manifests/", "/kits/", "/", };


// --------------------------------------------------------------------------- //
// expandFilePath
//     - captures scheme name, if any, in ord & constructs full local path
// --------------------------------------------------------------------------- //
char* expandFilePath( const char* ord )
{
  char *delim, *rem, *pathptr, *sedHome, *dash;
  int s;

  if (ord==NULL) return NULL;

  delim = strchr(ord, SCHEME_DELIM);
  rem = delim+1;

  // If no scheme, assume whole ord is file path
  if (delim==NULL) return (char*)ord;

  // Scheme found!

  // Copy scheme string into separate buf - should check for string length overrun eventually
  strncpy(schemestr, ord, delim-ord);
  schemestr[delim-ord] = '\0';    // make sure it gets null term.

  //
  // If scheme is in map then expand full path and copy into buf;
  //   otherwise just return remainder of ord after scheme delimiter 
  //
  for (s=0; s<MAX_NUM_SCHEMES; s++)
  {
    if (strcmp(schemes[s], schemestr)==0)
      break;
  }

  // Point to beginning of path buf
  pathptr = fullpath;

  // If we found our scheme...
  if (s<MAX_NUM_SCHEMES)
  {
    // Begin with sedona_home env var
    sedHome = getenv("sedona_home");
    if (sedHome!=NULL)
    {
      strcpy(pathptr, sedHome);          // strcpy copies null term. too
      pathptr += strlen(sedHome);        // ptr moves to char after path ends
    }

    // Copy the path for this scheme
    strcpy(pathptr, schemePaths[s]);     // strcpy copies null term. too
    pathptr += strlen(schemePaths[s]);   // ptr moves to char after path ends

    // Next add the kit name (i.e. filename up to dash)
    dash = strchr(rem, '-');
    if (dash!=NULL)
    {
      strncpy(pathptr, rem, dash-rem);
      pathptr += dash-rem;
      *pathptr++ = '/';
    }
  }


  // Finally, copy filename itself
  strcpy(pathptr, rem);   

  // DIAG
  //printf("\t** Serving file: %s\n", fullpath);
  // DIAG

  return fullpath;
}

#endif
// =========================================================================== //


#ifdef _WIN32
#include <windows.h>
#endif

// int FileStore.doSize(Str name)
Cell sys_FileStore_doSize(SedonaVM* vm, Cell* params)
{
  // The FileStore API indicates that this native call should not
  // cause the file to be opened. We'll do our best.
 #ifdef IMPL_SCHEME_CONVENTION
  const char* name = expandFilePath(params[0].aval);
 #else
  const char* name = params[0].aval;
 #endif

  Cell result;
#ifdef _WIN32
  BOOL fOk;
  WIN32_FILE_ATTRIBUTE_DATA fileInfo;

  if (name == NULL) return negOneCell;
  fOk = GetFileAttributesEx(name, GetFileExInfoStandard, (void*)&fileInfo);
  if (!fOk) return negOneCell;
  result.ival = fileInfo.nFileSizeLow;
#elif defined(_POSIX_SOURCE)
  struct stat statInfo;
  if (name == NULL || stat(name, &statInfo)) return negOneCell;
  result.ival = statInfo.st_size;
#else
  // Our best was not good enough. Open the file...
  FILE* fp;
  fp = fopen(name, "rb");
  if (fp == NULL) return negOneCell;

  // seek to end to get file size
  fseek(fp, 0, SEEK_END);
  result.ival = ftell(fp);
  fclose(fp);
#endif

  return result;
}

// Obj FileStore.doOpen(Str name, Str mode)
Cell sys_FileStore_doOpen(SedonaVM* vm, Cell* params)
{
 #ifdef IMPL_SCHEME_CONVENTION
  const char* name = expandFilePath(params[0].aval);
 #else
  const char* name = params[0].aval;
 #endif

  const char* mode = params[1].aval;
  const char* fopenMode;
  Cell result;
  FILE* fp;

  // sanity check arguments
  if (name == NULL || mode == NULL) return nullCell;

  // sanity check mode
  if (mode[1] != '\0') return nullCell;
  switch (mode[0])
  {
    case 'm':
      // create file in case it doesn't exist yet
      fp = fopen(name, "a+b");
      if (fp == NULL) return nullCell;
      fclose(fp);
      fopenMode = "r+b";
      break;
    case 'r':
      fopenMode = "rb";
      break;
    case 'w':
      fopenMode = "wb";
      break;
    default:  return nullCell;
  }

  result.aval = fopen(name, fopenMode);
  return result;
}

// int FileStore.doRead(Obj)
Cell sys_FileStore_doRead(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  Cell result;

  // sanity check arguments. FileStore requires -1 return value on error/eof.
  if ((fp == NULL) || ((result.ival = fgetc(fp)) == EOF))
	  return negOneCell;
  else
    return result;
}

// int FileStore.doReadBytes(Obj, byte[], int, int)
Cell sys_FileStore_doReadBytes(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  uint8_t* buf = (uint8_t*)params[1].aval;
  int32_t  off = params[2].ival;
  int32_t  len = params[3].ival;
  Cell result;

  // sanity check arguments. FileStream requires -1 on eof/error.
  if ((fp == NULL) || feof(fp) || ferror(fp)) return negOneCell;

  buf = buf + off;

  result.ival = fread(buf, 1, len, fp);
  return result;
}

// bool FileStore.doWrite(Obj, int)
Cell sys_FileStore_doWrite(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  int32_t b = params[1].ival;
  int32_t r;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  r = fputc(b, fp);

  return r == b ? trueCell : falseCell;
}

// bool FileStore.doWriteBytes(Obj, byte[], int, int)
Cell sys_FileStore_doWriteBytes(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  uint8_t* buf = (uint8_t*)params[1].aval;
  int32_t  off = params[2].ival;
  int32_t  len = params[3].ival;
  int32_t  r;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  buf = buf + off;

  r = fwrite(buf, 1, len, fp);

  return r == len ? trueCell : falseCell;
}

// int FileStore.doTell(Obj)
Cell sys_FileStore_doTell(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  Cell r;

  // sanity check arguments
  if (fp == NULL) return negOneCell;

  r.ival = ftell(fp);

  return r;
}

// bool FileStore.doSeek(Obj, int)
Cell sys_FileStore_doSeek(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  int32_t pos = params[1].ival;
  int32_t  r;

  // sanity check arguments
  if (fp == NULL) return negOneCell;

  r = fseek(fp, pos, SEEK_SET);

  return r == 0 ? trueCell : falseCell;
}

// void FileStore.doFlush(Obj)
Cell sys_FileStore_doFlush(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;

  // sanity check arguments
  if (fp == NULL) return negOneCell;

  fflush(fp);

  return nullCell;
}

// bool FileStore.doClose(Obj)
Cell sys_FileStore_doClose(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;

  // sanity check arguments
  if (fp == NULL) return negOneCell;

  if (fclose(fp) != 0)
  {
    printf("ERROR: Cannot close file\n");
    return falseCell;
  }

  return trueCell;
}


// static bool FileStore.rename(Str from, Str to)
Cell sys_FileStore_rename(SedonaVM* vm, Cell* params)
{
 #ifdef IMPL_SCHEME_CONVENTION
  const char* from = expandFilePath(params[0].aval);
  const char* to   = expandFilePath(params[1].aval);
 #else 
  const char* from = params[0].aval;
  const char* to   = params[1].aval;
 #endif 

  int r;
  struct stat statBuf;

  if ((stat(to, &statBuf) == 0) && (remove(to) != 0))
    return falseCell;

  r = rename(from, to);
  return r == 0 ? trueCell : falseCell;
}


