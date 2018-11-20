//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   02 Jul 07 Elizabeth McKenney  Creation
//

#include "sedona.h"

// int SerialPort.doInit(int port, int baud, int stopb, int wlen, int parity)
Cell serial_SerialPortNative_doInitNative(SedonaVM* vm, Cell* params)
{
  //
  // Initialize physical port here
  //
  printf("SerialPortNative.doInitNative not yet implemented for platform 'default'.\n");

  // Return zero if nothing went wrong
  return zeroCell;
}



// int SerialPort.doClose(int port)
Cell serial_SerialPortNative_doCloseNative(SedonaVM* vm, Cell* params)
{
  //
  // Shut down physical port here
  //
  printf("SerialPortNative.doCloseNative not yet implemented for platform 'default'.\n");

  return zeroCell;
}



// int  SerialPortNative.doRead(int port)
Cell serial_SerialPortNative_doReadNative(SedonaVM* vm, Cell* params)
{
  int32_t portNum = params[1].ival;
  int32_t i32Chr  = -1;
  Cell    ret;

  // reading from real port not yet implemented
  return negOneCell;
}


// int  SerialPortNative.doWrite(int port, int c)
Cell serial_SerialPortNative_doWriteNative(SedonaVM* vm, Cell* params)
{
  int32_t portNum = params[1].ival;
  uint8_t u8Chr   = params[2].ival & 0xff;
  int32_t rc;

  // writing to real port not yet implemented
  return negOneCell;
}



// int  SerialPortNative.doReadBytes(int port, byte[] y, int n)
Cell serial_SerialPortNative_doReadBytesNative(SedonaVM* vm, Cell* params)
{
  int32_t  portNum = params[1].ival;
  uint8_t* ybuf    = params[2].aval;
  int32_t  nbytes  = params[3].ival;
  int32_t  i32Chr  = -1;
  int32_t  k       = -1;
  Cell     ret;

  // reading from real port not yet implemented
  return negOneCell;
}



// int  SerialPortNative.doWriteBytes(int port, byte[] y, int c)
Cell serial_SerialPortNative_doWriteBytesNative(SedonaVM* vm, Cell* params)
{
  int32_t  portNum = params[1].ival;
  uint8_t* ybuf    = params[2].aval;
  int32_t  nbytes  = params[3].ival;
  int32_t  k       = -1;
  Cell     ret;

  // writing to real port not yet implemented
  return negOneCell;
}


