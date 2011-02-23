//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   05 Sep 06  Brian Frank  Creation
//   07 May 07  Brian Frank  Port from C++ old Sedona
//

#include "inet_util_std.h"

//////////////////////////////////////////////////////////////////////////
// Datagram
//////////////////////////////////////////////////////////////////////////

// TODO: this is pretty hackish, but lets us map
// the compiler memory layout to/from C

struct UdpDatagram
{
  void*    addr;
  int32_t  port;
  uint8_t* buf;
  int32_t  off;
  int32_t  len;
};

static void getUdpDatagram(int8_t* sedona, struct UdpDatagram* c)
{
  if (sizeof(void*) == 4)
  {
    c->addr = getRef(sedona, 0);
    c->port = getInt(sedona, 4);
    c->buf  = getRef(sedona, 8);
    c->off  = getInt(sedona, 12);
    c->len  = getInt(sedona, 16);
  }
  else
  {
    printf("UdpSocket unsupported pointer size\n");
  }
}

static void setUdpDatagram(int8_t* sedona, struct UdpDatagram* c)
{
  if (sizeof(void*) == 4)
  {
    setRef(sedona, 0,  c->addr);
    setInt(sedona, 4,  c->port);
    setRef(sedona, 8,  c->buf);
    setInt(sedona, 12, c->off);
    setInt(sedona, 16, c->len);
  }
  else
  {
    printf("UdpSocket unsupported pointer size\n");
  }
}

//////////////////////////////////////////////////////////////////////////
// Native Methods
//////////////////////////////////////////////////////////////////////////
 
//
// What is the maximum number of bytes which can 
// sent by this UDP implementation.
//
// static int maxPacketSize()
//
Cell inet_UdpSocket_maxPacketSize(SedonaVM* vm, Cell* params)
{                  
  // for now limit to 512 which is max SoxService buffer       
  Cell ret;
  ret.ival = 512;  
  return ret;       
}                                

//
// What is the ideal maximum number of bytes sent by this 
// UDP implementation.  This is typically driven by the 
// lower levels of the IP stack - for instance when running 
// 6LoWPAN over 802.15.4, this is the max UDP packet size 
// which doesn't require fragmenting across multiple 
// 802.15.4 frames.
//
// static int idealPacketSize()
//
Cell inet_UdpSocket_idealPacketSize(SedonaVM* vm, Cell* params)
{                                
  // for now limit to 512 which is max SoxService buffer       
  Cell ret;
  ret.ival = 512;
  return ret;       
}                                

//
// Initialize this socket which allocates a socket handle
// to this instance.  This method must be called before using
// the socket.  Return true on success, false on failure.
//
// bool open()
//
Cell inet_UdpSocket_open(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  bool closed   = getClosed(self);
  socket_t sock = getSocket(self);
#ifdef _WIN32
  WSADATA wsaData;
#endif
  // windoze startup
#ifdef _WIN32
  if (WSAStartup(MAKEWORD(2,2), &wsaData) != 0)
    return falseCell;
#endif

  // check that not already initialized
  if (!closed || sock != -1) return falseCell;

  // create socket
  sock = socket(AF_INET, SOCK_DGRAM,  0);
  if (sock < 0) return falseCell;

  // make socket non-blocking
  if (inet_setNonBlocking(sock) != 0)
  {
    closesocket(sock);
    return falseCell;
  }

  // udate UdpSocket instance
  setClosed(self, 0);
  setSocket(self, sock);
  return trueCell;
}

//
// Bind this socket the specified well-known port on this
// host. Return true on success, false on failure.
//
// bool bind(int port)
//
Cell inet_UdpSocket_bind(SedonaVM* vm, Cell* params)
{
  void* self    = params[0].aval;
  int32_t port  = params[1].ival;
  bool closed   = getClosed(self);
  socket_t sock = getSocket(self);

  if (closed)
    return falseCell;

  if (inet_bind(sock, port) != 0)
    return falseCell;

  return trueCell;
}

//
// Send the specified datagram which encapsulates both the
// destination address and the data to send.  Return true
// on success, false on failure.  If the number of bytes
// sent does not match datagram.len then this call will
// fail and return false.
//
// bool send(UdpDatagram datagram)
//
Cell inet_UdpSocket_send(SedonaVM* vm, Cell* params)
{
  void* self      = params[0].aval;
  void* sDatagram = params[1].aval;
  socket_t sock   = getSocket(self);
  bool closed     = getClosed(self);
  struct UdpDatagram datagram;
  uint8_t* buf;
  int32_t len;
  struct sockaddr_in addr;
  int r;

  if (closed) return falseCell;
  if (sDatagram == NULL) return falseCell;

  getUdpDatagram(sDatagram, &datagram);
  if (datagram.buf == NULL) return falseCell;
  if (datagram.addr == 0) return falseCell;

  // Issue 18436 - the inet natives currently only create ipv4 sockets,
  // so we need to fail-fast if the addr is not ipv4.
  if (!inet_isIPv4(datagram.addr)) return falseCell;

  inet_toSockaddr(&addr, datagram.addr, datagram.port);
  buf = datagram.buf + datagram.off;
  len = datagram.len;

  r = sendto(sock, buf, len, 0, (SOCKADDR_PARAM*)&addr, sizeof(addr));
  if (r != len) return falseCell;
  return trueCell;
}

//
// Receive a datagram into the specified structure.  The
// datagram.buf must reference a valid byte buffer - bytes
// are read in starting at datagram.buf[0] with at most datagram.len
// bytes being received.  If successful then return true,
// datagram.len reflects the actual number of bytes received,
// and datagram.addr/datagram.port reflect the source socket
// address.  Note datagram.addr is only valid until the next
// call to receive().  On failure or if no packets are pending
// to read, then return false, len=0, port=-1, and addr=null.
//
// Note if the number of bytes available to be read is greater
// then len than this call works differently dependent on the
// platform.  In Java it silently ignores the remainder of
// the bytes (wrong way), and in C++ it returns false since
// the message received is not the same as the message sent.
//
// bool receive(UdpDatagram datagram)
//
Cell inet_UdpSocket_receive(SedonaVM* vm, Cell* params)
{
  void* self      = params[0].aval;
  void* sDatagram = params[1].aval;
  socket_t sock   = getSocket(self);
  bool closed     = getClosed(self);
  struct UdpDatagram datagram;
  uint8_t* buf;
  int32_t len;
  struct sockaddr_in addr;
  int addrLen = sizeof(addr);
  int r;
  void* receiveIpAddr;

  // we store an inline IpAddr in UdpSocket to use as the
  // storage location for the address to return in the datagram
  // the compiler lays it out at offset 8 (after closed and socket)
  receiveIpAddr = getInline(self, 8);

  if (closed) return falseCell;
  if (sDatagram == NULL) return falseCell;

  getUdpDatagram(sDatagram, &datagram);
  if (datagram.buf == NULL) return falseCell;

  buf = datagram.buf + datagram.off;
  len = datagram.len;

  r = recvfrom(sock, buf, len, 0, (SOCKADDR_PARAM*)&addr, &addrLen);

  if (r == SOCKET_ERROR)
  {
    datagram.len  = 0;
    datagram.addr = NULL;
    datagram.port = -1;
    setUdpDatagram(sDatagram, &datagram);
    return falseCell;
  }
  else
  {       
    inet_fromSockaddr(&addr, receiveIpAddr, &datagram.port);
    datagram.len  = r;
    datagram.addr = receiveIpAddr;
    setUdpDatagram(sDatagram, &datagram);
    return trueCell;
  }
}

//
// Shutdown and close this socket.
//
// void close()
//
Cell inet_UdpSocket_close(SedonaVM* vm, Cell* params)
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


