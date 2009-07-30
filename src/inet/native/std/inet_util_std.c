//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Aug 06  Brian Frank  Creation
//   07 May 07  Brian Frank  Port from C++ old Sedona
//

#include "inet_util_std.h"

// this value is the third 32-bit in a masked IPv4 address
#ifdef IS_BIG_ENDIAN
#define MASKED_IPV4_3RD 0xffff
#else
#define MASKED_IPV4_3RD 0xffff0000
#endif


/**
 * Configure specified socket as non-blocking.
 * Return 0 on success, non-zero on error.
 */
int inet_setNonBlocking(socket_t sock)
{
  unsigned long nonBlocking = 1;

#ifdef _WIN32
  return ioctlsocket(sock, FIONBIO, &nonBlocking);
#elif defined(NETOS_GNU_TOOLS)
  return setsockopt(sock, SOL_SOCKET, SO_NBIO, (char *)&nonBlocking, sizeof(nonBlocking));
#else
  return ioctl(sock, FIONBIO, &nonBlocking);
#endif
}

/**
 * Return if the last error code was simply an indication
 * that the operation didn't complete immediately because
 * the socket is non-blocking.
 */
bool inet_errorIsWouldBlock()
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#elif defined(NETOS_GNU_TOOLS)
  return getErrno() == EWOULDBLOCK || getErrno() == EINPROGRESS;
#else
  return errno == EWOULDBLOCK || errno == EINPROGRESS;
#endif
}

/**
 * Bind the socket to a local port.
 * Return 0 on success, non-zero on error.
 */
int inet_bind(socket_t sock, int port)
{
  struct sockaddr_in addr;

  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(port);
  return bind(sock, (SOCKADDR_PARAM*)&addr, sizeof(addr));
}

/**
 * Return if the the inet::IpAddr is a masked IPv4 address.
 */
int inet_isIPv4(uint32_t* ipAddr)
{
  return ipAddr[0] == 0 && ipAddr[1] == 0 && ipAddr[2] == MASKED_IPV4_3RD;
}

/**
 * Copy a inet::IpAddr and port to a struct sockaddr.
 */
int inet_toSockaddr(struct sockaddr_in* addr, uint32_t* ipAddr, int port)
{
  addr->sin_port = htons(port);
  if (ipAddr == NULL)
  {
    addr->sin_family = AF_INET;
    addr->sin_addr.s_addr = INADDR_ANY;
  }
  else if (inet_isIPv4(ipAddr))
  {
    addr->sin_family = AF_INET;
    addr->sin_addr.s_addr = ipAddr[3];
    // printf("   -> %s\n", inet_ntoa(addr->sin_addr));
  }
  else
  {
    uint32_t* x = (uint32_t*)(&((struct sockaddr_in6*)addr)->sin6_addr);
    addr->sin_family = AF_INET6;
    x[0] = ipAddr[0];
    x[1] = ipAddr[1];
    x[2] = ipAddr[2];
    x[3] = ipAddr[3];
  }
  return 0;
}

/**
 * Copy a struct sockaddr to an inet::IpAddr and port.
 */
int inet_fromSockaddr(struct sockaddr_in* addr, uint32_t* ipAddr, int* port)
{
  *port = ntohs(addr->sin_port);
  if (addr->sin_family == AF_INET)
  {
    ipAddr[0] = 0;
    ipAddr[1] = 0;
    ipAddr[2] = MASKED_IPV4_3RD;
    ipAddr[3] = (uint32_t)addr->sin_addr.s_addr;
  }
  else
  {
    uint32_t* x = (uint32_t*)(&((struct sockaddr_in6*)addr)->sin6_addr);
    ipAddr[0] = x[0];
    ipAddr[1] = x[1];
    ipAddr[2] = x[2];
    ipAddr[3] = x[3];
  }
  return 0;
}




