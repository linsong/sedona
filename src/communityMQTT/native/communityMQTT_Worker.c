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
	unsigned char buf[100];
	unsigned char readbuf[100];

  MQTTHandle * pHandle = malloc(sizeof(MQTTHandle));

  pHandle->pNetwork = malloc(sizeof(Network));
  pHandle->pClient = malloc(sizeof(Client));

	NewNetwork(pHandle->pNetwork);
	ConnectNetwork(pHandle->pNetwork, host, port);
	MQTTClient(pHandle->pClient, pHandle->pNetwork, 1000, buf, 100, readbuf, 100);
 
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
