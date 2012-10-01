//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Sep 06  Brian Frank  Creation
//   07 May 07  Brian Frank  Port from C++ old Sedona
//

#ifndef __INET_UTIL_STD_H
#define __INET_UTIL_STD_H

#include "inet_util.h"

// Must define this to select IPv4 vs. IPv6 sockets
#ifndef SOCKET_FAMILY_INET
 #ifndef SOCKET_FAMILY_INET6
  //#warning No SOCKET_FAMILY specified: assuming SOCKET_FAMILY_INET (IPv4).            // normal
  #pragma message ( "No SOCKET_FAMILY specified: assuming SOCKET_FAMILY_INET (IPv4)." )  // MSVC
  #define SOCKET_FAMILY_INET
 #endif
#endif


// we never select more than one socket at
// a time, so conserve memory.
// NOTE: gcc gives a lot of warnings about redefining FD_SETSIZE
// if we do not first undefine it. FD_SETSIZE is originally defined
// in sys/select.h. Presumably this is ok since we were previously
// stomping it anyway.
#undef FD_SETSIZE
#define FD_SETSIZE 1

////////////////////////////////////////////////////////////////
// Windows
////////////////////////////////////////////////////////////////

// Windows
#ifdef _WIN32

#include <Winsock2.h>
#include <Ws2tcpip.h>

typedef SOCKET socket_t;

#define SOCKADDR_PARAM struct sockaddr

LPSTR PrintError(int ErrorCode);

#define ERRNO_MSG() (PrintError(WSAGetLastError()))

#endif

////////////////////////////////////////////////////////////////
// QNX
////////////////////////////////////////////////////////////////

// QNX
#ifdef __QNX__

#include <sys/types.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <netdb.h>
#include <errno.h>
#include <ioctl.h>

#define SD_BOTH 2

typedef int socket_t;

#define SOCKADDR_PARAM struct sockaddr

#define ERRNO_MSG() (strerror(errno))

#define closesocket(x) close(x)

#define INVALID_SOCKET -1
#define SOCKET_ERROR   -1

#endif

////////////////////////////////////////////////////////////////
// UNIX
////////////////////////////////////////////////////////////////

#ifdef __UNIX__

#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netdb.h>
#include <errno.h>

#define SD_BOTH 2

typedef int socket_t;

#define SOCKADDR_PARAM struct sockaddr

#define ERRNO_MSG() (strerror(errno))

#define closesocket(x) close(x)

#define INVALID_SOCKET -1
#define SOCKET_ERROR   -1

#endif

////////////////////////////////////////////////////////////////
// Cygwin
////////////////////////////////////////////////////////////////

#ifdef __CYGWIN__
#include <netdb.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <netdb.h>
#include <errno.h>
#include <sys/ioctl.h>

#define SD_BOTH 2

typedef int socket_t;

#define SOCKADDR_PARAM sockaddr

#define ERRNO_MSG() (strerror(errno))

#define closesocket(x) close(x)

#define INVALID_SOCKET -1
#define SOCKET_ERROR   -1

#endif

////////////////////////////////////////////////////////////////
// NetOS
////////////////////////////////////////////////////////////////

#ifdef NETOS_GNU_TOOLS

#error The NETOS_GNU_TOOLS compile switch is not fully supported at this time.  Contact Tridium Engineering if you need to use this to build your Sedona VM.

#include <sockapi.h>
#include <narmapi.h>

typedef SOCKET socket_t;

#define inet_addr(x) NAInet_addr(x)
#define INADDR_NONE 0

#define SOCKADDR_PARAM struct sockaddr_in

#define ERRNO_MSG() (strerror(errno))

#endif



////////////////////////////////////////////////////////////////
// Common
////////////////////////////////////////////////////////////////

#define INET_READ  0x01
#define INET_WRITE 0x02


// util forwards
extern int inet_setNonBlocking(socket_t sock);
extern bool inet_errorIsWouldBlock();
extern int inet_bind(socket_t sock, int port);
extern int inet_toSockaddr(struct sockaddr_storage* addr, uint32_t* ipAddr, int port, int scope, int flow);
extern int inet_fromSockaddr(struct sockaddr_storage* addr, uint32_t* ipAddr, int* port, int* scope, int* flow);


#endif
