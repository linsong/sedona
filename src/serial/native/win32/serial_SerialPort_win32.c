//
// Copyright (c) 2008 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jan 08 Robert Adams  Creation
//

#include "sedona.h"
#include "serialWin32.h"


SerialData *pSd[HANDLE_ARRAY_LEN];
BOOL debug = FALSE;

Cell errCell = { -2 };

// Set the serial parameters for the port.  If any params are bad, do
//  nothing and return -1, otherwise return 0.
//  int SerialPort doInit(int port, int baud, int datab, int stopb, int par, int rts)
Cell serial_SerialPort_doInit(SedonaVM* vm, Cell* params)
{
  char port[256];
  SerialData *pData;
  int32_t portNum  = params[1].ival;
  int32_t baudRate = params[2].ival;
  int32_t dataBits = params[3].ival;
  int32_t stopBits = params[4].ival;
  int32_t parity   = params[5].ival;
  int32_t rtsLevel = params[6].ival;

  //
  // Initialize physical port here
  //
  if(portNum<0 || portNum>=HANDLE_ARRAY_LEN)
  {
    printf("Invalid port %d\n",portNum);
    return negOneCell;
  }

  pData  = (SerialData *)malloc(sizeof(SerialData));
  memset(pData,0,sizeof(SerialData));

  sprintf(port, "\\\\.\\COM%d",portNum);
  pData->hFile = CreateFile(port,
                       GENERIC_READ | GENERIC_WRITE,
                       0,
                       NULL,
                       OPEN_EXISTING,
                       FILE_FLAG_OVERLAPPED,
                       NULL);
  if(pData->hFile==INVALID_HANDLE_VALUE)
  {
    DWORD dwLastErr = GetLastError();
    printf("Error: CreateFile  err=%d\n",dwLastErr);
    return negOneCell;
  }

  pSd[portNum] = pData;
  SetTimeouts(pData);
  SetCommParams(pData->hFile, dataBits, stopBits, parity, rtsLevel, baudRate);

  // Create
  pData->hWait = CreateEvent(
        NULL,    // no security attributes
        FALSE,   // auto reset event
        TRUE,   // initial state signalled
        NULL);   // no name
  pData->hWriteEvent = CreateEvent(
        NULL,    // no security attributes
        FALSE,   // auto reset event
        FALSE,   // initial state non-signalled
        NULL);   // no name

  // Init critical section object
  InitializeCriticalSection(&pData->cr);

  // Create & start the read thread
  pData->done = FALSE;
  pData->readThreadHandle =
      CreateThread(NULL, 0, readThreadEntry, pData, 0, &pData->readThreadId);
  if(pData->readThreadHandle==NULL)
  {
    DWORD dwLastErr = GetLastError();
    printf("Error: CreateThread  err=%d\n",dwLastErr);
    return negOneCell;
  }

  // Return zero if nothing went wrong
  return zeroCell;
}


// Shut down the serial port.  Return 0 if successful.
// int SerialPort.doClose(int port)
Cell serial_SerialPort_doClose(SedonaVM* vm, Cell* params)
{
  int32_t portNum  = params[1].ival;
  SerialData *pData = pSd[portNum];
  //
  // Shut down physical port here
  //
  printf("SerialPort.doClose COM%d platform 'default'.\n",portNum);

  pData->done = TRUE;
  CloseHandle(pData->hFile);
  CloseHandle(pData->readThreadHandle);
  CloseHandle(pData->hWait);
  DeleteCriticalSection(&pData->cr);
  free(pData);
  pSd[portNum]=NULL;
  return zeroCell;
}



// Read one byte from port.  Return byte value, or -1 if no byte was
// available.  (non-blocking)
// int  SerialPort.doRead(int port)
Cell serial_SerialPort_doRead(SedonaVM* vm, Cell* params)
{
  uint8_t ch;
  int32_t bytesRead;
  Cell  ret;
  int32_t portNum = params[1].ival;

  bytesRead = read(pSd[portNum], &ch, 1);

  if (bytesRead < 0) return errCell;
  if(bytesRead != 1) return negOneCell;
  ret.ival = ch;
  return ret;
}


// Write one byte to port.  Return -1 if any error, or 0 if successful.
// int  SerialPort.doWrite(int port, int c)
Cell serial_SerialPort_doWrite(SedonaVM* vm, Cell* params)
{
  int32_t  portNum = params[1].ival;
  uint8_t  ch      = (uint8_t)params[2].ival;
  int32_t bytesWritten;

  bytesWritten = write(pSd[portNum], &ch, 1);
  if(bytesWritten != 1) return negOneCell;
  return zeroCell;
}



// Read up to n bytes from port into array y.  Return number of bytes
// read, or -1 if an error occurred.  (non-blocking)
// int  SerialPort.doReadBytes(int port, byte[] y, int off, int len)
Cell serial_SerialPort_doReadBytes(SedonaVM* vm, Cell* params)
{
  Cell     ret;
  int32_t  portNum = params[1].ival;
  uint8_t* pu8Buf  = params[2].aval;
  int32_t  off     = params[3].ival;
  int32_t  nbytes  = params[4].ival;

  int32_t  bytesRead;
  
  pu8Buf = pu8Buf + off;

  bytesRead = read(pSd[portNum], pu8Buf, nbytes);
  ret.ival = bytesRead;
  return ret;

}

// Write up to n bytes to port from array y.  Return number of bytes
// written, or -1 if an error occurred.
// int  SerialPort.doWriteBytes(int port, byte[] y, int off, int len)
Cell serial_SerialPort_doWriteBytes(SedonaVM* vm, Cell* params)
{
  Cell     ret;
  int32_t  portNum = params[1].ival;
  uint8_t* pu8Buf  = params[2].aval;
  int32_t  off     = params[3].ival;
  int32_t  nbytes  = params[4].ival;

  int32_t  bytesWritten;

  pu8Buf = pu8Buf + off;

  bytesWritten = write(pSd[portNum], pu8Buf, nbytes);
  if(bytesWritten==-1) return negOneCell;
  ret.ival = bytesWritten;
  return ret;
}


// return number of bytes read
int read(SerialData *pData, uint8_t* pu8Buf, int32_t  nbytes)
{
  DWORD bytesAvail;
  DWORD bytesRead = 0;

  // Copy data in critical section
  EnterCriticalSection(&pData->cr);

  if (pData->err)
  {
    bytesRead = -2;
    SetEvent(pData->hWait);                // signal read thread to read new data
  }
  else
  {
    bytesAvail = pData->count - pData->pos;

    if(bytesAvail>0)
    {
      bytesRead = bytesAvail > nbytes ? nbytes : bytesAvail;
      memcpy(pu8Buf, &pData->inMsg[pData->pos], bytesRead);

      if(bytesAvail>bytesRead)
        pData->pos += bytesRead;
      else
      {
        pData->count = pData->pos = 0;
        SetEvent(pData->hWait);            // signal read thread to read new data
      }
    }
  }

  LeaveCriticalSection(&pData->cr);

  return bytesRead;
}

// return number of bytes written -
int write(SerialData *pData, uint8_t* pu8Buf, int32_t  nbytes)
{
  int32_t bytesWritten;
  OVERLAPPED   os;
  unsigned i;

  if(debug)
  {
    printf("write: ");
    printBytes(pu8Buf, nbytes);
  }
  memset(&os, 0, sizeof(OVERLAPPED) );
  os.hEvent = pData->hWriteEvent;

  if(!WriteFile(pData->hFile,  // __in         HANDLE hFile,
                pu8Buf,        // __in         LPCVOID lpBuffer,
                nbytes,        // __in         DWORD nNumberOfBytesToWrite,
                &bytesWritten, // __out_opt    LPDWORD lpNumberOfBytesWritten
                &os ) )        // __inout_opt  LPOVERLAPPED lpOverlapped
  {
    DWORD dwLastErr = GetLastError();
    switch(dwLastErr)
    {
      case ERROR_IO_PENDING:
        if(!GetOverlappedResult( pData->hFile, &os, &bytesWritten, TRUE))
        {
          printf("serial:WriteFile returns err %d\n", GetLastError());
          return -1;
        }
        break;

     // Handle eof and aborts the same
     // case ERROR_OPERATION_ABORTED:
      default:
        printf("ERROR in WriteVserialPort\n");
        return -1;
    }
  }

  return bytesWritten;
}

void SetCommParams(HANDLE hFile ,
                  int     dataBits   ,
                  int     stopBits   ,
                  int     parity     ,
                  int     flowControl,
                  int     baudRate    )
{
  DCB dcb;

  GetCommState(hFile, &dcb);
  dcb.BaudRate     = baudRate ; // can pass actual value
  dcb.ByteSize     = dataBits ; // javax.com value matches
  switch(stopBits)
  {
    case 1 : dcb.StopBits = ONESTOPBIT ;    break;
    case 3 : dcb.StopBits = ONE5STOPBITS ;  break;
    case 2 : dcb.StopBits = TWOSTOPBITS ;   break;
  }

  dcb.Parity       = parity   ; // javax.com enum matches
  dcb.fRtsControl  = 1        ; // enable
  SetCommState(hFile, &dcb);
}
/*
typedef struct _COMMTIMEOUTS {
  DWORD ReadIntervalTimeout;
  DWORD ReadTotalTimeoutMultiplier;
  DWORD ReadTotalTimeoutConstant;
  DWORD WriteTotalTimeoutMultiplier;
  DWORD WriteTotalTimeoutConstant;
} COMMTIMEOUTS,
 *LPCOMMTIMEOUTS;

If an application sets ReadIntervalTimeout and ReadTotalTimeoutMultiplier to MAXDWORD and sets
ReadTotalTimeoutConstant to a value greater than zero and less than MAXDWORD, one of the
following occurs when the ReadFile function is called:

    * If there are any bytes in the input buffer, ReadFile returns immediately with the bytes in the buffer.
    * If there are no bytes in the input buffer, ReadFile waits until a byte arrives and then returns immediately.
    * If no bytes arrive within the time specified by ReadTotalTimeoutConstant, ReadFile times out.

*/
void SetTimeouts(SerialData *pData)
{
  COMMTIMEOUTS cto;

  GetCommTimeouts(pData->hFile, &cto);

  cto.ReadIntervalTimeout = MAXDWORD;
  cto.ReadTotalTimeoutMultiplier = MAXDWORD;
  cto.ReadTotalTimeoutConstant = 20;
  SetCommTimeouts(pData->hFile, &cto);
}

/************************************************
* Entry point for the read thread.
*************************************************/
DWORD WINAPI readThreadEntry(LPVOID param)
{
  // Receive Data
  DWORD bytesRead;
  char  buf[600];
  OVERLAPPED  os;
  BOOL bRet;
  SerialData *pData = (SerialData *)param;
  HANDLE hReadEvent = CreateEvent( NULL,    // no security attributes
                                   FALSE,   // auto reset event
                                   FALSE,   // initial state non-signalled
                                   NULL);   // no name
  COMSTAT comStat;
  DWORD dwErrors;

  while(!pData->done)
  {
    memset( &os, 0, sizeof(OVERLAPPED) );
    os.hEvent = hReadEvent;
    bRet = ReadFile(pData->hFile,      // __in         HANDLE hFile,
                         buf,          // __out        LPVOID lpBuffer,
                         600,          // __in         DWORD nNumberOfBytesToRead,
                         &bytesRead,   // __out_opt    LPDWORD lpNumberOfBytesRead,
                         &os );        // __inout_opt  LPOVERLAPPED lpOverlapped

    if (!bRet)
    {
      DWORD dwLastErr = GetLastError();
      switch(dwLastErr)
      {
        case ERROR_IO_PENDING:
          bRet = GetOverlappedResult( pData->hFile, &os, &bytesRead, TRUE);
          if(!bRet)
          {
            printf("serial win32 error: GetOverlappedResult returns err %d\n", GetLastError());
            pData->done = TRUE;
            CloseHandle(pData->hFile);
            CloseHandle(pData->readThreadHandle);
            CloseHandle(pData->hWait);
            DeleteCriticalSection(&pData->cr);
            free(pData);
          }
          break;

        // Handle eof and aborts the same
        case ERROR_OPERATION_ABORTED:
        default:
          printf("EOF???\n");
          continue;
      }
    }
if (pData->done) break;
    if (!ClearCommError(pData->hFile,   // __in         HANDLE hFile,
                        &dwErrors,      // __out_opt    LPDWORD lpErrors,
                        &comStat))      // __out_opt    LPCOMSTAT lpStat
    {
      printf("ClearCommError returns err %d\n", GetLastError());
    }

    if (dwErrors != 0)
    {
      if (debug)
        printf("ERROR! bytesRead=%d, dwErrors=%x\n", bytesRead, dwErrors);

      // Copy data in critical section
      EnterCriticalSection(&pData->cr);
      pData->err = (dwErrors != 0);  // err if any flags set
      pData->count = 0;//bytesRead;
      pData->pos = 0;
      memcpy(pData->inMsg,buf,bytesRead);
      ResetEvent(pData->hWait);
      LeaveCriticalSection(&pData->cr);
    }
    else if (bytesRead > 0)
    {
      DWORD rtn;
      // Wait for signal previous data has been read
      rtn = WaitForSingleObject(pData->hWait, INFINITE);
      if(rtn==WAIT_FAILED) printf("failed WaitForSingleObject(pData->hWait) err=%d\n",GetLastError());
      if(debug)
      {
        printf("read: ");
        printBytes(buf, bytesRead);
      }

      // Copy data in critical section
      EnterCriticalSection(&pData->cr);
      pData->err = FALSE;  // err if any flags set
      pData->count = bytesRead;
      pData->pos = 0;
      memcpy(pData->inMsg,buf,bytesRead);
      ResetEvent(pData->hWait);
      LeaveCriticalSection(&pData->cr);
    }
    else
    {
      EnterCriticalSection(&pData->cr);
      pData->err = FALSE;
      pData->count = 0;//bytesRead;
      pData->pos = 0;
      LeaveCriticalSection(&pData->cr);
    }
  } // while
  CloseHandle(pData->hFile);


  return 0;
}

void printBytes(char *msgp, unsigned length)
{
  unsigned i;

  for( i=0 ; i<length ; i++)
  {
    printf("%2.2X ", msgp[i] & 0x0ff );
  }
  printf("\n");
}
