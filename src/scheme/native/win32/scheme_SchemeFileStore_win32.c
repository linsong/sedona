//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   13 Oct 11  Elizabeth McKenney    Creation
//



#include "sedona.h"

#include "stdlib.h"
#include "string.h"


// Externs
Cell sys_FileStore_doSize(SedonaVM* vm, Cell* params);
Cell sys_FileStore_doOpen(SedonaVM* vm, Cell* params);



// Buf to hold complete path
char fullpath[1024];   

#define SCHEME_DELIM (':')


// Map of scheme strings to paths
#define MAX_NUM_SCHEMES 2
const char* schemes[MAX_NUM_SCHEMES]     = { "m", "k", };
const char* schemePaths[MAX_NUM_SCHEMES] = { "manifests/", "kits/", };


//
// expandFilePath
//   - captures scheme name, if any, in ord & constructs full local path
//
char* expandFilePath( char* schemestr, char* path )
{
  char *delim, *pathptr, *dash, *rem = path;
  int s;

  // If delimiter is still in filename, skip to byte after it
  delim = strchr(path, SCHEME_DELIM);
  if (delim!=NULL) rem = delim+1;


  // If scheme is in map then copy corresponding path into buf (o/w do nothing)
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
    strcpy(pathptr, schemePaths[s]);    // strcpy copies null term. too
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

  // Finally, copy actual filename into path string
  strcpy(pathptr, rem);   

  return fullpath;
}


//
// static int doSchemeSize(Str scheme, Str name)
//
Cell scheme_SchemeFileStore_doSchemeSize(SedonaVM* vm, Cell* params)
{
  char* scheme = (char*)params[0].aval;
  char* fname  = (char*)params[1].aval;

  Cell newparams[1];

  newparams[0].aval = expandFilePath(scheme, fname);

  return sys_FileStore_doSize(vm, newparams);
}



//
// static Obj doSchemeOpen(Str scheme, Str name, Str mode)
//
Cell scheme_SchemeFileStore_doSchemeOpen(SedonaVM* vm, Cell* params)
{
  char* scheme = (char*)params[0].aval;
  char* fname  = (char*)params[1].aval;
  char* fmode  = (char*)params[2].aval;

  Cell newparams[2];

  newparams[0].aval = expandFilePath(scheme, fname);
  newparams[1].aval = fmode;

  return sys_FileStore_doOpen(vm, newparams);
}


