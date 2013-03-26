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


/*
void printAddr(const char* label, void* loc, int len)
{
  int n;
  printf("  %s ptr = 0x%x, value = %04x", label, (uint32_t)loc, ntohs(*(uint16_t*)loc));
  for (n=1; n<len; n++) printf(":%04x", ntohs(*((uint16_t*)loc+n)));
  printf("\n");
}
*/


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
  int addrLen, rc;

  struct sockaddr_storage addr;
  memset(&addr, 0, sizeof(addr));

#ifdef SOCKET_FAMILY_INET

  {
    struct sockaddr_in* paddr = (struct sockaddr_in*)&addr;
    addrLen = sizeof(*paddr);

#ifdef __QNX__
    paddr->sin_len         = sizeof(struct sockaddr_in);
#endif
    paddr->sin_family      = AF_INET;
    paddr->sin_addr.s_addr = INADDR_ANY;
    paddr->sin_port        = htons(port);
  }

#elif defined( SOCKET_FAMILY_INET6 )

  {
    struct sockaddr_in6* paddr = (struct sockaddr_in6*)&addr;
    addrLen = sizeof(*paddr);

#ifdef __QNX__
    paddr->sin6_len    = sizeof(struct sockaddr_in6);
#endif
    paddr->sin6_family   = AF_INET6;
    paddr->sin6_addr     = in6addr_any;
    paddr->sin6_port     = htons(port);
    paddr->sin6_flowinfo = 0x0;   // Use 0 if not supporting flowinfo
    paddr->sin6_scope_id = 0x0;   // Interface #, I guess 0 is okay for in6addr_any
  }

#endif

  rc = bind(sock, (struct sockaddr *)&addr, addrLen);
  
  return rc;
}


/**
 * Copy a inet::IpAddr and port to a struct sockaddr.
 */
int inet_toSockaddr(struct sockaddr_storage* addr, uint32_t* ipAddr, int port, int scope, int flow)
{
#if defined( SOCKET_FAMILY_INET )

  struct sockaddr_in* paddr = (struct sockaddr_in*)addr;

#ifdef __QNX__
  paddr->sin_len    = sizeof(struct sockaddr_in);
#endif
  paddr->sin_port   = htons(port);
  paddr->sin_family = AF_INET;

  if (ipAddr == NULL)
    paddr->sin_addr.s_addr = INADDR_ANY;
  else 
    paddr->sin_addr.s_addr = ipAddr[3];

#elif defined( SOCKET_FAMILY_INET6 )

  struct sockaddr_in6* paddr = (struct sockaddr_in6*)addr;
  uint32_t* x = (uint32_t*)&(paddr->sin6_addr);

#ifdef __QNX__
  paddr->sin6_len      = sizeof(struct sockaddr_in6);
#endif
  paddr->sin6_port     = htons(port);
  paddr->sin6_family   = AF_INET6;
  paddr->sin6_scope_id = htonl(scope);
  paddr->sin6_flowinfo = htonl(flow);

  x[0] = ipAddr[0];
  x[1] = ipAddr[1];
  x[2] = ipAddr[2];
  x[3] = ipAddr[3];

#endif

  return 0;
}

/**
 * Copy a struct sockaddr to an inet::IpAddr and port.
 */
int inet_fromSockaddr(struct sockaddr_storage* addr, uint32_t* ipAddr, int* port, int* scope, int* flow)
{
#if defined( SOCKET_FAMILY_INET )

  struct sockaddr_in* paddr = (struct sockaddr_in*)addr;

  *port = ntohs(paddr->sin_port);

  ipAddr[0] = 0;
  ipAddr[1] = 0;
  ipAddr[2] = MASKED_IPV4_3RD;
  ipAddr[3] = (uint32_t)paddr->sin_addr.s_addr;

#elif defined( SOCKET_FAMILY_INET6 )

  struct sockaddr_in6* paddr = (struct sockaddr_in6*)addr;
  uint32_t* x = (uint32_t*)&(paddr->sin6_addr);

  *port  = ntohs(paddr->sin6_port);
  *scope = ntohl(paddr->sin6_scope_id);
  *flow  = ntohl(paddr->sin6_flowinfo);

  ipAddr[0] = x[0];
  ipAddr[1] = x[1];
  ipAddr[2] = x[2];
  ipAddr[3] = x[3];

#endif

  return 0;
}



#ifdef _WIN32
//
// Convenience for Winsock error messages
//   Copied from http://msdn.microsoft.com/en-us/library/windows/desktop/ms738639(v=vs.85).aspx
//
LPSTR PrintError(int ErrorCode)
{
    static char Message[1024];

    // If this program was multithreaded, we'd want to use
    // FORMAT_MESSAGE_ALLOCATE_BUFFER instead of a static buffer here.
    // (And of course, free the buffer when we were done with it)

    FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS |
                  FORMAT_MESSAGE_MAX_WIDTH_MASK, NULL, ErrorCode,
                  MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                  (LPSTR) Message, 1024, NULL);
    return Message;
}
#endif

