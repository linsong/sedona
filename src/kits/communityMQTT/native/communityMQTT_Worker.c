#ifdef __DARWIN__
#include <sys/types.h>
#endif

#include <stdio.h>
#include "MQTTClient.h"
#include "communityMQTT_common.h"

#include <stdlib.h>
#include <signal.h>
#include <memory.h>
#include <sys/time.h>

#include <pthread.h>
#include "uthash.h"
#include "log.h"

#include "sedona.h"

static pthread_key_t thread_key;
static pthread_once_t thread_key_once = PTHREAD_ONCE_INIT;

///////////////////////////////////////////////////////
// Worker Thread Functions
///////////////////////////////////////////////////////

static void makeThreadKey()
{
  (void)pthread_key_create(&thread_key, NULL);
}

void mssleep(long ms)
{
  struct timespec ts;
  ts.tv_sec = ms/1000L;
  ts.tv_nsec = ms%1000L * 1000000L;
  nanosleep(&ts, NULL);
}

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
    pSession->pHandle = NULL;
  }
  
  SubscribeResponse * pResponse = pSession->pResponse;
  if (pResponse)
  {
    SubscribeResponse * current = NULL;
    SubscribeResponse * tmp = NULL;
    HASH_ITER(hh, pResponse, current, tmp) {
      HASH_DEL(pResponse, current);
      if (current->topic)
        free(current->topic);
      if (current->payload)
        free(current->payload);
      free(current);
    }
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
  pHandle->pClient = malloc(sizeof(MQTTClient));
  /* pHandle->command_timeout_ms = 1000; */
  pHandle->command_timeout_ms = 3000;

  NetworkInit(pHandle->pNetwork);
  MQTTClientInit(pHandle->pClient, pHandle->pNetwork, pHandle->command_timeout_ms, pHandle->buf, pHandle->buf_len, pHandle->readbuf, pHandle->readbuf_len);

  int rc = 0;
  rc = NetworkConnect(pHandle->pNetwork, pData->host, pData->port);
  if (rc != SUCCESS)
    return false;
 
  MQTTPacket_connectData data = MQTTPacket_connectData_initializer;       
  data.willFlag = 0;
  data.MQTTVersion = 3;
  data.clientID.cstring = pData->clientid;
  data.username.cstring = pData->username;
  data.password.cstring = pData->password;

  data.keepAliveInterval = 10;
  data.cleansession = 1;
  
  rc = 0;
  rc = MQTTConnect(pHandle->pClient, &data);
  
  if (rc == SUCCESS)
    log_info(" * [MQTTService] Connectted to %s:%d (rc: %d)", pData->host, pData->port, rc);
  else {
    char * error = NULL;
    // refer to MQTT spec section 3.2.2.3 "ConnectReturn code" for details
    switch(rc) { 
      case 1:
        error = "unacceptable protocol version";
        break;
      case 2:
        error = "identifier rejected";
        break;
      case 3:
        error = "Server unavailable";
        break;
      case 4:
        error = "bad username or password";
        break;
      case 5: 
        error = "not authorized";
        break;
      default:
        error = "unknown error";
        break;
    }
    log_info(" * [MQTTService] Failed to connect to %s:%d (rc: %d, error: %s)", pData->host, pData->port, rc, error);
  }

  return rc == SUCCESS;
}

int publish(SessionHandle * pSession, PublishData * pData)
{
  if (!pSession)
    return FAILURE;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient)
  {
    log_warn(" * [MQTTService] Invalid Handle");
    return FAILURE;
  }
  if (!pHandle->pClient->isconnected)
  {
    log_info(" * [MQTTService] Connection lost");
    return FAILURE;
  }

  MQTTMessage msg;
  msg.qos = pData->qos;
  msg.retained = 1;
  msg.dup = 0;
  msg.payload = pData->payload;
  msg.payloadlen = pData->payload_len;
  int rc = MQTTPublish(pHandle->pClient, pData->topic, &msg);
  log_trace(" * [MQTTService] Published to %s (rc: %d)", pData->topic, rc);
  return rc;
}

void messageArrived(MessageData * pMsgData)
{
  MQTTMessage* message = pMsgData->message;
  if (!message)
    return;
  
  SessionHandle * pSession = (SessionHandle *)pthread_getspecific(thread_key);
  if (!pSession)
    return;

  SubscribeResponse * pResponse = NULL;

  char * topic = malloc(pMsgData->topicName->lenstring.len+1);
  memset(topic, 0, pMsgData->topicName->lenstring.len+1);
  strncpy(topic, pMsgData->topicName->lenstring.data, pMsgData->topicName->lenstring.len);
  log_trace(" * [MQTTService] Got message for topic '%s'", topic);
  HASH_FIND_STR(pSession->pResponse, topic, pResponse);
  if (!pResponse)
  {
    pResponse = malloc(sizeof(SubscribeResponse));
    memset(pResponse, 0, sizeof(SubscribeResponse));

    pResponse->topic = topic;
    HASH_ADD_STR(pSession->pResponse, topic, pResponse);
  }
  else
    free(topic);

  if (pResponse->payload)
    free(pResponse->payload);
  pResponse->payload = malloc(pMsgData->message->payloadlen);
  memset(pResponse->payload, 0, pMsgData->message->payloadlen);
  memcpy(pResponse->payload, pMsgData->message->payload, pMsgData->message->payloadlen);
  pResponse->payload_len = pMsgData->message->payloadlen;
}

bool subscribe(SessionHandle * pSession, SubscribeData * pData)
{
  if (!pSession)
    return false;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient)
  {
    log_warn(" * [MQTTService] Invalid Handle");
    return false;
  }
  if (!pHandle->pClient->isconnected)
  {
    log_info(" * [MQTTService] Connection lost");
    return false;
  }

  //TODO: support wildcard in topic, refer to 'deliverMessage' method

  int rc = MQTTSubscribe(pHandle->pClient, pData->topic, pData->qos, messageArrived);
  log_info(" * [MQTTService] Subscribed to '%s'(qos: %d) (rc: %d)", pData->topic, pData->qos, rc);
  return rc == SUCCESS;
}

bool unsubscribe(SessionHandle * pSession, UnsubscribeData * pData)
{
  if (!pSession)
    return false;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient)
  {
    log_warn(" * [MQTTService] Invalid Handle");
    return false;
  }
  if (!pHandle->pClient->isconnected)
  {
    log_info(" * [MQTTService] Connection lost");
    return false;
  }
  
  log_info(" * [MQTTService] Unsubscribe to '%s'", pData->topic);

  int rc = MQTTUnsubscribe(pHandle->pClient, pData->topic);
  return rc == SUCCESS;
}

bool yield(MQTTHandle * pHandle, int ms)
{
  if (!pHandle || !pHandle->pClient)
  {
    log_warn(" * [MQTTService] Invalid MQTTHandle");
    mssleep(500);
    return false;
  }
  if (!pHandle->pClient->isconnected) {
    mssleep(500);
    return false;
  }

  int rc = MQTTYield(pHandle->pClient, ms); 
  return rc == SUCCESS;
}

bool stopSession(SessionHandle * pSession)
{
  if (!pSession)
    return false;

  MQTTHandle * pHandle = pSession->pHandle;
  if (!pHandle || !pHandle->pClient || !pHandle->pNetwork) 
  {
    log_warn(" * [MQTTService] Invalid MQTTHandle");
    releaseSession(pSession);
    return false;
  }

  if (pHandle->pClient->isconnected)
    MQTTDisconnect(pHandle->pClient);

  NetworkDisconnect(pHandle->pNetwork);
  releaseSession(pSession);
  return true;
}

void * workerThreadFunc(void * pThreadData)
{
  SessionHandle * pSession = (SessionHandle *)pThreadData;
  if (!pSession)
    return NULL;
  
  pthread_setspecific(thread_key, pSession);

  log_info(" * [MQTTService] MQTT worker thread started %x", (unsigned int)pSession);
  while (true) 
  {
    Payload * pPayload = curPayload(pSession);
    if (!pPayload)
    {
      /* log_debug("### %x null payload ...", (unsigned int)pSession); */
      if (pSession->pHandle)
      {
        //run mqtt event loop
        yield(pSession->pHandle, 2000);
        /* if (!yield(pSession->pHandle, 2000)) */
          /* log_warn(" failed"); */
      }
      continue;
    }
    else
    {
      bool result = true;
      int rc = SUCCESS;
      switch (pPayload->type)
      {
        case StartSessionTask:
          result = startSession(pSession, pPayload->pStartSessionData);
          break;
        case PublishTask:
          rc = publish(pSession, pPayload->pPublishData);
          result = rc == SUCCESS;
          if (rc == FAILURE)
            log_warn(" * [MQTTService] failed to publish msg.");
          else if (rc == BUFFER_OVERFLOW)
            log_warn(" * [MQTTService] write buffer overflowed.");
          break;
        case SubscribeTask:
          result = subscribe(pSession, pPayload->pSubscribeData);
          break;
        case UnsubscribeTask:
          result = unsubscribe(pSession, pPayload->pUnsubscribeData);
          break;
        case StopSessionTask:
          stopSession(pSession);
          pSession = NULL;
          pthread_setspecific(thread_key, pSession);
          log_info(" * [MQTTService] MQTT worker thread exited");
          pthread_exit(NULL);
          break;
        default:
          log_warn(" * [MQTTService] invalid payload");
          break;
      }
      /* log_info("###  * %d action result: %d \n", pPayload->type, result); */
      popPayload(pSession);
    }
  }   
  log_info(" * [MQTTService] MQTT worker thread exited %x", (unsigned int)pSession);
  return NULL;
}

///////////////////////////////////////////////////////
// Native Method Slots
///////////////////////////////////////////////////////
Cell communityMQTT_Worker_startSession(SedonaVM* vm, Cell* params)
{
  char * host = params[0].aval;
  int32_t port = params[1].ival;
  char * clientid = params[2].aval;
  char * username = params[3].aval;
  char * password = params[4].aval;
  
  pthread_once(&thread_key_once, makeThreadKey);
  
  SessionHandle * pSession = malloc(sizeof(SessionHandle));
  pSession->pHandle = NULL;
  pSession->pHead = NULL;
  pSession->pResponse = NULL;

  StartSessionData * pData = malloc(sizeof(StartSessionData));
  memset(pData, 0, sizeof(StartSessionData));

  pData->host = malloc(strlen(host)+1);
  strcpy(pData->host, host);
  pData->port = port;

  if (clientid && strlen(clientid) > 0) {
    pData->clientid = malloc(strlen(clientid)+1);
    strcpy(pData->clientid, clientid);
  } else
    pData->clientid = NULL;

  if (username && strlen(username) > 0) {
    pData->username = malloc(strlen(username)+1);
    strcpy(pData->username, username);
  } else
    pData->username = NULL;

  if (password && strlen(password) > 0) {
    pData->password = malloc(strlen(password)+1);
    strcpy(pData->password, password);
  } else 
    pData->password = NULL;

  Payload * pPayload = malloc(sizeof(Payload));
  pPayload->type = StartSessionTask;
  pPayload->pStartSessionData = pData;

  pushPayload(pSession, pPayload); 

  pthread_t * pthread = malloc(sizeof(pthread_t));
  int rc = pthread_create(pthread, NULL, &workerThreadFunc, pSession);
  if (rc)
  {
    log_warn(" * [MQTTService] Thread creation failed: %d", rc);
    free(pthread);
    pthread = NULL;
    
    releaseSession(pSession);
    pSession = NULL;
  }
  else
    pthread_detach(*pthread); //reclaim thread resource after it terminates

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
  if (!pSession)
    return falseCell;

  MQTTHandle * pHandle = pSession->pHandle;
  if (pHandle && pHandle->pClient && pHandle->pClient->isconnected)
    return trueCell;
  else
    return falseCell;
}

