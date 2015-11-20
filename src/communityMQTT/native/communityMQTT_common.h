#ifndef __COMMON_H__
#define __COMMON_H__

#include "MQTTClient.h"

typedef struct 
{
  Network * pNetwork;
  Client * pClient;
  char * buf;
  int buf_len;
  char * readbuf;
  int readbuf_len;
  unsigned int command_timeout_ms;
} MQTTHandle;

#endif /* __COMMON_H__ */
