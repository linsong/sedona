//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   28 Apr 09  Brian Frank  Creation
//

#include "sedona.h"

#include <time.h>
#include <stdio.h>
#include <errno.h>


// Obj doOpen(Str filename, int size)
Cell pstoreFS_FsPstoreService_doOpen(SedonaVM* vm, Cell* params)
{       
  const char* filename = params[0].aval;
  int size             = params[1].ival;
  FILE* fp;
  Cell result;

  // first open the file just to make sure it is created
  fp = fopen(filename, "a");
  if (fp == NULL) return nullCell;
  fclose(fp);
  
  // now re-open the file for read/write
  fp = fopen(filename, "r+b");
  if (fp == NULL) return nullCell;

  // if file is not sized correctly for random access, 
  // then write a zero at the last seekable position
  fseek(fp, 0, SEEK_END);
  if (ftell(fp) < size)
  {
    fseek(fp, size, SEEK_SET);
    fputc(0, fp);
  }
    
  result.aval = fp;
  return result;
}  

// void doClose(Object fp)
Cell pstoreFS_FsPstoreService_doClose(SedonaVM* vm, Cell* params)
{
  FILE* fp = params[0].aval;
  if (fp != NULL) fclose(fp);
  return nullCell;
}

// int doRead(Object fp, int offset)
Cell pstoreFS_FsPstoreService_doRead(SedonaVM* vm, Cell* params)
{
  FILE* fp       = params[0].aval;
  int32_t offset = params[1].ival;
  Cell result;
  
  if (fp == NULL) return negOneCell;
  
  fseek(fp, offset, SEEK_SET); 
  result.ival = fgetc(fp);
  
  return result;
}


// bool doWrite(Object fp, int offset, int val)  
Cell pstoreFS_FsPstoreService_doWrite(SedonaVM* vm, Cell* params)
{
  FILE* fp       = params[0].aval;
  int32_t offset = params[1].ival;
  int32_t val    = params[2].ival;

  if (fp == NULL) return falseCell;

  fseek(fp, offset, SEEK_SET);
  return fputc(val, fp) == val ? trueCell : falseCell;
}

