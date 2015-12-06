#include <stdio.h>
#include "MQTTClient.h"
#include "communityMQTT_common.h"

#include <stdio.h>
#include <signal.h>
#include <memory.h>

#include <sys/time.h>

#include "sedona.h"

// native method slots
Cell communityMQTT_Message_doPublish(SedonaVM* vm, Cell* params)
{
  SessionHandle * pSession = (SessionHandle *)params[0].aval;
  uint8_t * topic = params[1].aval;
  uint8_t * payload = params[2].aval;
  int32_t payload_len = params[3].ival;
  int32_t qos = params[4].ival;
  
  Payload * pPayload = malloc(sizeof(Payload));
  pPayload->type = PublishTask;
  PublishData * pData = malloc(sizeof(PublishData));
  pData->topic = malloc(strlen(topic)+1);
  strcpy(pData->topic, topic);

  pData->payload = malloc(payload_len);
  memcpy(pData->payload, payload, payload_len);
  pData->payload_len = payload_len;
  pData->qos = qos;
  pPayload->pPublishData = pData;

  pushPayload(pSession, pPayload);
  return nullCell;
}
