//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Sep 06  Brian Frank  Creation
//   07 May 07  Brian Frank  Port from C++ old Sedona
//   23 Aug 07  Elizabeth McKenney Split into std & Jennic ports
//

#ifndef __INET_UTIL_H
#define __INET_UTIL_H

// includes
#include "sedona.h"

// C++
#ifdef __cplusplus
extern "C" {
#endif

// TcpSocket, TcpServer, UdpSocket getter/setter
#define getClosed(self)      getByte(self, 0)
#define setClosed(self, val) setByte(self, 0, val)
#define getSocket(self)      getInt(self, 4)
#define setSocket(self, val) setInt(self, 4, val)

#ifdef __cplusplus
}
#endif

#endif
