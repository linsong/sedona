//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Aug 06  Brian Frank  Creation
//   07 May 07  Brian Frank  Port from C++ old Sedona
//

#include "inet_util_std.h"

//
// Connect this socket to the specified IP address and port.
// The address must be in dotted notation, hostname resolution
// is not supported.  This method is non-blocking.  Poll the
// socket using finishConnect() to determine when the connection
// been completed (either successfully or not).  Return false
// if there is an immediate failure, or true if this call
// succeeds.
//
// bool connect(Str addr, int port)
//
Cell inet_TcpSocket_connect(SedonaVM* vm, Cell* params)
{
  void* self     = params[0].aval;
  uint32_t* addr = params[1].aval;
  int port       = params[2].ival;
#ifdef _WIN32
  WSADATA wsaData;
#endif
  struct sockaddr_in sa;
  socket_t sock;

  // windoze startup
#ifdef _WIN32
  if (WSAStartup(MAKEWORD(2,2), &wsaData) != 0)
    return falseCell;
#endif

  // initialize the sockaddr from inet::IpAddr and port
  inet_toSockaddr((struct sockaddr_storage*)&sa, addr, port, 0, 0);

  // create socket
  sock = socket(sa.sin_family, SOCK_STREAM, 0);
  if (sock < 0)
    return falseCell;

  // set non-blocking
  if (inet_setNonBlocking(sock) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // async connect
  if (connect(sock, (SOCKADDR_PARAM*)&sa, sizeof(sa)) != 0)
  {
    if (!inet_errorIsWouldBlock())
    {
      closesocket(sock);
      return falseCell;
    }
  }

  // save file descriptor
  setSocket(self, sock);
  return trueCell;
}

//
// Poll the socket to see if the connection has completed.
// Return false if the connection is still in-progress.  If
// the connection attempt has completed then return true and
// check isClosed() for success.
//   - Pending: return false
//   - Success: return true, closed=false
//   - Failed:  return true, closed=true
//
// In the case that the state after a finishConnect() call is Failed, you
// must still call close() to properly free the socket that was opened in the
// connect() call.
//
// bool finishConnect()
//
Cell inet_TcpSocket_finishConnect(SedonaVM* vm, Cell* params)
{
  void* self = params[0].aval;
  socket_t sock = getSocket(self);
  struct timeval timeout;
  fd_set writable, excepts;
  int r;

  // timeout
  timeout.tv_sec = 0;
  timeout.tv_usec = 0;

  // selectors
  FD_ZERO(&writable);
  FD_ZERO(&excepts);
  FD_SET(sock, &writable);
  FD_SET(sock, &excepts);

  // select the socket
  r = select(sock+1, NULL, &writable, &excepts, &timeout);

  // if zero then connection is still pending
  if (r == 0) return falseCell;

  // on Window's the exception select is used
  // to report a failure
  if (FD_ISSET(sock, &excepts)) return trueCell;

  // this means that writable select was triggered;
  // on Unix a failure is reported by an error from
  // select with the failure error accessible in
  // getsockopt(SO_ERROR)
  if (r < 0) return trueCell;

  //  On QNX, select can return 0 even if the socket connection
  //  was not made.  Check the socket error status.
#ifndef _WIN32
  {
    int arglen = sizeof(int);
    int sock_err = 0;
#ifdef NETOS_GNU_TOOLS
    getsockopt(sock, SOL_SOCKET, SO_ERROR, (char*)&sock_err, &arglen);
#else
    getsockopt(sock, SOL_SOCKET, SO_ERROR, &sock_err, &arglen);
#endif
    if (sock_err != 0)
      return trueCell;
  }
#endif

  // assume success
  setClosed(self, 0);
  return trueCell;
}

//
// Shutdown and close this socket.
//
// void close()
//
Cell inet_TcpSocket_close(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  socket_t sock = getSocket(self);

  closesocket(sock);
  setClosed(self, 1);
  setSocket(self, -1);

  return nullCell;
}

//
// Send the specified bytes over the socket.  Return the
// number of bytes actually written which may be equal to
// or less than len.  If the connection is terminated or
// there is any other error close the socket and return -1.
//
// int write(byte[] b, int off, int len)
//
Cell inet_TcpSocket_write(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  uint8_t* buf  = params[1].aval;
  int32_t  off  = params[2].ival;
  int32_t  len  = params[3].ival;
  socket_t sock = getSocket(self);
  Cell result;

  buf = buf + off;

  result.ival = send(sock, buf, len, 0);
  if (result.ival >= 0) return result;  // 0+ is ok
  if (inet_errorIsWouldBlock()) return zeroCell;
  inet_TcpSocket_close(vm, params);
  return negOneCell;
}

//
// Receive the specified bytes from the socket.  Return the
// number of bytes actually read which may be equal to
// or less than len.  If the connection is terminated or
// there is any other error close the socket and return -1.
//
// int read(byte[] b, int off, int len)
//
Cell inet_TcpSocket_read(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  uint8_t* buf  = params[1].aval;
  int32_t  off  = params[2].ival;
  int32_t  len  = params[3].ival;
  socket_t sock = getSocket(self);
  Cell result;

  buf = buf + off;

  result.ival = recv(sock, buf, len, 0);
  if (result.ival > 0)
    return result;  // 1+ is ok, zero is graceful shutdown
  else if (result.ival==0)
    inet_TcpSocket_close(vm, params);
  else if (inet_errorIsWouldBlock())
    return zeroCell;

  return negOneCell;
}



