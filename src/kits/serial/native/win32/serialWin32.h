//
// Copyright (c) 2009 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//

#ifndef SERIAL_WIN32_H
#define SERIAL_WIN32_H

#include <windows.h>

////////////////////////////////////////////////////////////////
//  Constants
///////////////////////////////////////////////////////////////

  #define HANDLE_ARRAY_LEN  15

////////////////////////////////////////////////////////////////
//  Data struct
///////////////////////////////////////////////////////////////
typedef struct _SerialData
{
  HANDLE   hFile;
  HANDLE   hWait;
  HANDLE   hWriteEvent;
  HANDLE   readThreadHandle; //uintptr_t
  DWORD    readThreadId;
  CRITICAL_SECTION cr;

  BOOL done;
  int count;
  int pos;
  char inMsg[600];
  BOOL err;

} SerialData;


void SetCommParams(HANDLE   hFile ,
                   int32_t  dataBits   ,
                   int32_t  stopBits   ,
                   int32_t  parity     ,
                   int32_t  flowControl,
                   int32_t  baudRate    );

int write(SerialData *pData, uint8_t* pu8Buf, int32_t  nbytes);
int read(SerialData *pData, uint8_t* pu8Buf, int32_t  nbytes);

DWORD WINAPI readThreadEntry(LPVOID param);

void SetTimeouts(SerialData *pData);
void printBytes(char *msgp, unsigned length);



#endif