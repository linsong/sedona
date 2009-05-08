//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Apr 07  Brian Frank  Creation
//

#include "sedona.h"


////////////////////////////////////////////////////////////////
// Native Methods
////////////////////////////////////////////////////////////////

// int File.doSize(Str, Str)
Cell sys_File_doSize(SedonaVM* vm, Cell* params)
{
  const char* name = params[0].aval;
  FILE* fp;
  Cell result;

  // open file, if we can't open it return -1
  fp = fopen(name, "rb");
  if (fp == NULL)
  {
    result.ival = -1;
    return result;
  }

  // seek to end to get file size
  fseek(fp, 0, SEEK_END);
  result.ival = ftell(fp);
  fclose(fp);

  return result;
}

// Obj File.doOpen(Str, Str)
Cell sys_File_doOpen(SedonaVM* vm, Cell* params)
{
  const char* name = params[0].aval;
  const char* mode = params[1].aval;
  const char* fopenMode;
  Cell result;
  FILE* fp;

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
Cell sys_File_doRead(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  Cell result;

  result.ival = fgetc(fp);
  return result;
}

// int File.doReadBytes(Obj, byte[], int, int)
Cell sys_File_doReadBytes(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  uint8_t* buf = (uint8_t*)params[1].aval;
  int32_t  off = params[2].ival;
  int32_t  len = params[3].ival;
  Cell result;
  
  buf = buf + off;

  result.ival = fread(buf, 1, len, fp);
  return result;
}

// bool File.doWrite(Obj, int)
Cell sys_File_doWrite(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  int32_t b = params[1].ival;
  int32_t r;

  r = fputc(b, fp);

  return r == b ? trueCell : falseCell;
}

// bool File.doWriteBytes(Obj, byte[], int, int)
Cell sys_File_doWriteBytes(SedonaVM* vm, Cell* params)
{
  FILE* fp  = (FILE*)params[0].aval;
  uint8_t* buf = (uint8_t*)params[1].aval;
  int32_t  off = params[2].ival;
  int32_t  len = params[3].ival;
  int32_t  r;

  buf = buf + off;

  r = fwrite(buf, 1, len, fp);

  return r == len ? trueCell : falseCell;
}

// int File.doTell(Obj)
Cell sys_File_doTell(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  Cell r;

  r.ival = ftell(fp);
  
  return r;
}

// bool File.doSeek(Obj, int)
Cell sys_File_doSeek(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  int32_t pos = params[1].ival;
  int32_t  r;

  r = fseek(fp, pos, SEEK_SET);

  return r == 0 ? trueCell : falseCell;
}

// void File.doFlush(Obj)
Cell sys_File_doFlush(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;

  fflush(fp);

  return nullCell;
}

// bool File.doClose(Obj, Str, Str)
Cell sys_File_doClose(SedonaVM* vm, Cell* params)
{
  FILE* fp = (FILE*)params[0].aval;
  const char* name = params[1].aval;
  //const char* mode = params[2].aval;  // mode is third param but not currently used

  if (fclose(fp) != 0)
  {
    printf("ERROR: Cannot close %s\n", name);
    return falseCell;
  }

  return trueCell;
}

// static bool File.rename(Str from, Str to)
Cell sys_File_rename(SedonaVM* vm, Cell* params)
{                                          
  const char* from = params[0].aval;
  const char* to   = params[1].aval;
  int r;
  
  r = rename(from, to);
  
  return r == 0 ? trueCell : falseCell;
}


