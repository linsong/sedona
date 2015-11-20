#include <stdio.h>
#include "MQTTClient.h"
#include "communityMQTT_common.h"

#include <stdlib.h>
#include <signal.h>
#include <memory.h>

#include <sys/time.h>

#include "sedona.h"

MQTTHandle * startSession(char * host, int port, char * clientid, char * username, char * password) 
{
  MQTTHandle * pHandle = malloc(sizeof(MQTTHandle));
  pHandle->buf_len = 100;
  pHandle->buf = malloc(sizeof(char)*pHandle->buf_len);
  pHandle->readbuf_len = 100;
  pHandle->readbuf = malloc(sizeof(char)*pHandle->readbuf_len);

  pHandle->pNetwork = malloc(sizeof(Network));
  pHandle->pClient = malloc(sizeof(Client));
  pHandle->command_timeout_ms = 300;

  NewNetwork(pHandle->pNetwork);
  ConnectNetwork(pHandle->pNetwork, host, port);
  MQTTClient(pHandle->pClient, pHandle->pNetwork, pHandle->command_timeout_ms, pHandle->buf, pHandle->buf_len, pHandle->readbuf, pHandle->readbuf_len);
 
  MQTTPacket_connectData data = MQTTPacket_connectData_initializer;       
  data.willFlag = 0;
  data.MQTTVersion = 3;
  data.clientID.cstring = clientid;
  data.username.cstring = username;
  data.password.cstring = password;

  data.keepAliveInterval = 10;
  data.cleansession = 1;
  printf("Connecting to %s:%d\n", host, port);
  
  int rc = 0;
  //FIXME: when connection failed, svm will exit
  rc = MQTTConnect(pHandle->pClient, &data);
  printf("Connected %d\n", rc);
  return pHandle;
}

void yield(MQTTHandle * pHandle)
{
  if (!pHandle || !pHandle->pClient)
  {
    printf("Invalid MQTTHandle\n");
    return;
  }
  MQTTYield(pHandle->pClient, 100); 
}

void stopSession(MQTTHandle * pHandle)
{
  if (!pHandle || !pHandle->pClient || !pHandle->pNetwork) 
  {
    printf("Invalid MQTTHandle\n");
    return;
  }

  MQTTDisconnect(pHandle->pClient);
  pHandle->pNetwork->disconnect(pHandle->pNetwork);

  if (pHandle->pClient)
    free(pHandle->pClient);
  if (pHandle->pNetwork)
    free(pHandle->pNetwork);
  if (pHandle->buf)
    free(pHandle->buf);
  if (pHandle->readbuf)
    free(pHandle->readbuf);
  free(pHandle);
}

// native method slots
Cell communityMQTT_Worker_startSession(SedonaVM* vm, Cell* params)
{
  uint8_t * host = params[0].aval;
  int32_t port = params[1].ival;
  uint8_t * clientid = params[2].aval;
  uint8_t * username = params[3].aval;
  uint8_t * password = params[4].aval;

  MQTTHandle * pHandle = startSession(host, port, clientid, username, password);
  Cell result;
  result.aval = (void *)pHandle;
  return result;
}

Cell communityMQTT_Worker_yield(SedonaVM* vm, Cell* params)
{
  MQTTHandle * pHandle = (MQTTHandle *)params[0].aval;
  yield(pHandle);
  return nullCell;
}

Cell communityMQTT_Worker_stopSession(SedonaVM* vm, Cell* params)
{
  MQTTHandle * pHandle = (MQTTHandle *)params[0].aval;
  stopSession(pHandle);
  return nullCell;
}
