//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   02 Jul 07 Elizabeth McKenney  Creation
//

#include "sedona.h"

// int SerialPort.doInit(int port, int baud, int stopb, int wlen, int parity)
Cell serial_SerialPort_doInit(SedonaVM* vm, Cell* params)
{
  //
  // Initialize physical port here
  //
  printf("SerialPort.doInit not yet implemented for platform 'default'.\n"); 

  // Return zero if nothing went wrong
  return zeroCell;
}



// int SerialPort.doClose(int port)
Cell serial_SerialPort_doClose(SedonaVM* vm, Cell* params)
{
  //
  // Shut down physical port here
  //
  printf("SerialPort.doClose not yet implemented for platform 'default'.\n"); 

  return zeroCell;
}



// int  SerialPort.doRead(int port)
Cell serial_SerialPort_doRead(SedonaVM* vm, Cell* params)
{
  int32_t portNum = params[1].ival;
  int32_t i32Chr  = -1;
  Cell    ret;

  // reading from real port not yet implemented
  return negOneCell;
}


// int  SerialPort.doWrite(int port, int c)
Cell serial_SerialPort_doWrite(SedonaVM* vm, Cell* params)
{
  int32_t portNum = params[1].ival;
  uint8_t u8Chr   = params[2].ival & 0xff;
  int32_t rc;

  // writing to real port not yet implemented
  return negOneCell;
}



// int  SerialPort.doReadBytes(int port, byte[] y, int n)
Cell serial_SerialPort_doReadBytes(SedonaVM* vm, Cell* params)
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



// int  SerialPort.doWriteBytes(int port, byte[] y, int c)
Cell serial_SerialPort_doWriteBytes(SedonaVM* vm, Cell* params)
{
  int32_t  portNum = params[1].ival;
  uint8_t* ybuf    = params[2].aval;
  int32_t  nbytes  = params[3].ival;
  int32_t  k       = -1;
  Cell     ret;

  // writing to real port not yet implemented
  return negOneCell;
}


