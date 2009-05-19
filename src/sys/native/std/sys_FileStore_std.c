//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 May 09  Brian Frank  Move methods from sys_File_std.c
//

#include "sedona.h"


// int File.doSize(Str name)
Cell sys_FileStore_doSize(SedonaVM* vm, Cell* params)
{
  const char* name = params[0].aval;
  FILE* fp;
  Cell result;

  // sanity check arguments
  if (name == NULL) return negOneCell;

  // open file, if we can't open it return -1
  fp = fopen(name, "rb");
  if (fp == NULL) return negOneCell;

  // seek to end to get file size
  fseek(fp, 0, SEEK_END);
  result.ival = ftell(fp);
  fclose(fp);

  return result;
}

// Obj File.doOpen(Str name, Str mode)
Cell sys_FileStore_doOpen(SedonaVM* vm, Cell* params)
{
  const char* name = params[0].aval;
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

// int File.doRead(Obj)
Cell sys_FileStore_doRead(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  Cell result;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  result.ival = fgetc(fp);
  return result;
}

// int File.doReadBytes(Obj, byte[], int, int)
Cell sys_FileStore_doReadBytes(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  uint8_t* buf = (uint8_t*)params[1].aval;
  int32_t  off = params[2].ival;
  int32_t  len = params[3].ival;
  Cell result;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;
  
  buf = buf + off;

  result.ival = fread(buf, 1, len, fp);
  return result;
}

// bool File.doWrite(Obj, int)
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

// bool File.doWriteBytes(Obj, byte[], int, int)
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

// int File.doTell(Obj)
Cell sys_FileStore_doTell(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  Cell r;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  r.ival = ftell(fp);
  
  return r;
}

// bool File.doSeek(Obj, int)
Cell sys_FileStore_doSeek(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  int32_t pos = params[1].ival;
  int32_t  r;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  r = fseek(fp, pos, SEEK_SET);

  return r == 0 ? trueCell : falseCell;
}

// void File.doFlush(Obj)
Cell sys_FileStore_doFlush(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  fflush(fp);

  return nullCell;
}

// bool File.doClose(Obj)
Cell sys_FileStore_doClose(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;

  // sanity check arguments
  if (fp == NULL ) return negOneCell;

  if (fclose(fp) != 0)
  {
    printf("ERROR: Cannot close file\n");
    return falseCell;
  }

  return trueCell;
}

