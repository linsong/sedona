//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Aug 06  Brian Frank  Creation
//   08 May 07  Brian Frank  Port from C++ old Sedona
//

#include "inet_util_std.h"

//
// Bind this socket the specified well-known port on this
// machine. Return true on success, false on failure.
//
// bool bind(int port)
//
Cell inet_TcpServerSocket_bind(SedonaVM* vm, Cell* params)
{
  void* self   = params[0].aval;
  int32_t port = params[1].ival;
  bool closed = getClosed(self);
  int val;

#ifdef _WIN32
  WSADATA wsaData;
#endif
  socket_t sock;

  // if already open
  if (!closed) return falseCell;

  // windoze startup
#ifdef _WIN32
  if (WSAStartup(MAKEWORD(2,2), &wsaData) != 0)
    return falseCell;
#endif

  // create socket
#ifdef SOCKET_FAMILY_INET
  sock = socket(AF_INET, SOCK_STREAM,  0);
#elif defined( SOCKET_FAMILY_INET6 )
  sock = socket(AF_INET6, SOCK_STREAM,  0);
#endif
  if (sock < 0) return falseCell;

  // set socket to reuse its port
  val = 1;
  if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (void*)&val, sizeof(val)) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // bind to port
  if (inet_bind(sock, port) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // listen
  if (listen(sock, 3) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // make socket non-blocking
  if (inet_setNonBlocking(sock) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // success
  setSocket(self, sock);
  setClosed(self, 0);
  return trueCell;
}

//
// Poll the server socket to see if there are any pending
// connections.  If a connection is pending then setup the
// specified socket instance to handle the incoming connection.
// The socket passed in must be closed, and if successful will
// be open on return.  Return true if a connection was successfully
// accepted or false if there are no pending connections.
//
// bool accept(TcpSocket socket)
//
Cell inet_TcpServerSocket_accept(SedonaVM* vm, Cell* params)
{
  void* self     = params[0].aval;
  void* accepted = params[1].aval;
  socket_t sock = getSocket(self);
  socket_t acceptedSock;

  // accept socket
  acceptedSock = accept(sock, NULL, 0);
  if (acceptedSock < 0) return falseCell;

  // set non-blocking
  if (inet_setNonBlocking(acceptedSock) != 0)
  {
    closesocket(acceptedSock);
    return falseCell;
  }

  // mark as open
  setSocket(accepted, acceptedSock);
  setClosed(accepted, 0);
  return trueCell;
}

//
// Shutdown and close this server socket.
//
// void close()
//
Cell inet_TcpServerSocket_close(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  socket_t sock = getSocket(self);
  bool closed   = getClosed(self);

  if (!closed)
  {
    closesocket(sock);
    setClosed(self, 1);
    setSocket(self, -1);
  }
  return nullCell;
}




