#include <stdio.h>
#include "MQTTClient.h"
#include "communityMQTT_common.h"

#include <stdlib.h>
#include <signal.h>
#include <memory.h>
#include <sys/time.h>

#include <pthread.h>

#include "sedona.h"

///////////////////////////////////////////////////////
// Worker Thread Functions
///////////////////////////////////////////////////////
void releaseSession(SessionHandle * pSession)
{
  if (!pSession)
    return;
  
  //free all payloads
  while (curPayload(pSession))
    popPayload(pSession);

  MQTTHandle * pHandle = pSession->pHandle;
  if (pHandle)
  {
    free(pHandle->pClient);
    free(pHandle->pNetwork);
    free(pHandle->buf);
    free(pHandle->readbuf);
    free(pHandle);
  }
  free(pSession);
}

bool startSession(SessionHandle * pSession, StartSessionData * pData) 
{
  if (!pSession)
    return false;

  if (!pSession->pHandle)
    pSession->pHandle = malloc(sizeof(MQTTHandle));
  MQTTHandle * pHandle = pSession->pHandle;
  pHandle->buf_len = 100;
  pHandle->buf = malloc(sizeof(char)*pHandle->buf_len);
  pHandle->readbuf_len = 100;
  pHandle->readbuf = malloc(sizeof(char)*pHandle->readbuf_len);

  pHandle->pNetwork = malloc(sizeof(Network));
  pHandle->pClient = malloc(sizeof(Client));
  pHandle->command_timeout_ms = 1000;

  NewNetwork(pHandle->pNetwork);
  ConnectNetwork(pHandle->pNetwork, pData->host, pData->port);
  MQTTClient(pHandle->pClient, pHandle->pNetwork, pHandle->command_timeout_ms, pHandle->buf, pHandle->buf_len, pHandle->readbuf, pHandle->readbuf_len);
 
  MQTTPacket_connectData data = MQTTPacket_connectData_initializer;       
  data.willFlag = 0;
  data.MQTTVersion = 3;
  data.clientID.cstring = pData->clientid;
  data.username.cstring = pData->username;
  data.password.cstring = pData->password;

  data.keepAliveInterval = 10;
  data.cleansession = 1;
  printf("Connecting to %s:%d\n", pData->host, pData->port);
  
  int rc = 0;
  //FIXME: when connection failed, svm will exit
  rc = MQTTConnect(pHandle->pClient, &data);
  printf("Connected %d\n", rc);
  return rc == 0;
}

bool publish(SessionHandle * pSession, PublishData * pData)
{
  if (!pSession)
    return false;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient)
  {
    printf("Invalid Handle");
    return false;
  }
  if (!pHandle->pClient->isconnected)
  {
    printf("Connection lost");
    return false;
  }

  printf("Publish to %s\n", pData->topic);
  MQTTMessage msg;
  msg.qos = pData->qos;
  msg.retained = 0;
  msg.dup = 0;
  msg.payload = pData->payload;
  msg.payloadlen = pData->payload_len;
  int rc = MQTTPublish(pHandle->pClient, pData->topic, &msg);
  /* printf("Published %d\n", rc); */
  return rc == 0;
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

bool stopSession(SessionHandle * pSession)
{
  if (!pSession)
    return false;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient || !pHandle->pNetwork) 
  {
    printf("Invalid MQTTHandle\n");
    return false;
  }

  MQTTDisconnect(pHandle->pClient);
  pHandle->pNetwork->disconnect(pHandle->pNetwork);

  releaseSession(pSession);
  return true;
}

void * workerThreadFunc(void * pThreadData)
{
  SessionHandle * pSession = (SessionHandle *)pThreadData;
  if (!pSession)
    return;

  printf("worker thread started \n");
  while (true) 
  {
    Payload * pPayload = curPayload(pSession);
    if (!pPayload)
    {
      if (pSession->pHandle)
        //FIXME: need to handle when session timedout case
        yield(pSession->pHandle);
      continue;
    }
    else
    {
      switch (pPayload->type)
      {
        case StartSessionTask:
          startSession(pSession, pPayload->pStartSessionData);
          break;
        case PublishTask:
          publish(pSession, pPayload->pPublishData);
          break;
        case StopSessionTask:
          stopSession(pSession);
          pSession = NULL;
          printf("worker thread exits \n");
          pthread_exit(NULL);
          break;
        default:
          break;
      }
      popPayload(pSession);
    }
  }   
}

///////////////////////////////////////////////////////
// Native Method Slots
///////////////////////////////////////////////////////
Cell communityMQTT_Worker_startSession(SedonaVM* vm, Cell* params)
{
  uint8_t * host = params[0].aval;
  int32_t port = params[1].ival;
  uint8_t * clientid = params[2].aval;
  uint8_t * username = params[3].aval;
  uint8_t * password = params[4].aval;
  
  SessionHandle * pSession = malloc(sizeof(SessionHandle));
  pSession->pHandle = NULL;
  pSession->pHead = NULL;

  StartSessionData * pData = malloc(sizeof(StartSessionData));
  pData->host = malloc(strlen(host)+1);
  strcpy(pData->host, host);
  pData->port = port;
  pData->clientid = malloc(strlen(clientid)+1);
  strcpy(pData->clientid, clientid);
  pData->username = malloc(strlen(username)+1);
  strcpy(pData->username, username);
  pData->password = malloc(strlen(password)+1);
  strcpy(pData->password, password);

  Payload * pPayload = malloc(sizeof(Payload));
  pPayload->type = StartSessionTask;
  pPayload->pStartSessionData = pData;

  pushPayload(pSession, pPayload); 

  pthread_t * pthread = malloc(sizeof(pthread_t));
  int rc = 0;
  if (rc=pthread_create(pthread, NULL, &workerThreadFunc, pSession))
  {
    printf("Thread creation failed: %d\n", rc);
    free(pthread);
    pthread = NULL;
    
    releaseSession(pSession);
    pSession = NULL;
  }

  Cell result;
  result.aval = (void *)pSession;
  return result;
}

Cell communityMQTT_Worker_stopSession(SedonaVM* vm, Cell* params)
{
  SessionHandle * pSession = (SessionHandle *)params[0].aval;
  Payload * pPayload = malloc(sizeof(Payload));
  pPayload->type = StopSessionTask;
  pushPayload(pSession, pPayload);

  return nullCell;
}

Cell communityMQTT_Worker_isSessionLive(SedonaVM* vm, Cell* params)
{
  SessionHandle * pSession = (SessionHandle *)params[0].aval;

  MQTTHandle * pHandle = pSession->pHandle;
  if (pHandle && pHandle->pClient && pHandle->pClient->isconnected)
    return trueCell;
  else
    return falseCell;
}

