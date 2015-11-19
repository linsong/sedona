#ifndef __COMMON_H__
#define __COMMON_H__

#include "MQTTClient.h"

typedef struct 
{
  Network * pNetwork;
  Client * pClient;
} MQTTHandle;

#endif /* __COMMON_H__ */
