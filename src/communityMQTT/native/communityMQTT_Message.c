#include <stdio.h>
#include "MQTTClient.h"
#include "communityMQTT_common.h"

#include <stdio.h>
#include <signal.h>
#include <memory.h>

#include <sys/time.h>

#include "sedona.h"

void publish(MQTTHandle * pHandle, char * topic, char * payload)
{
		printf("Publish to %s\n", topic);
		MQTTMessage msg;
		msg.qos = QOS1;
		msg.retained = 0;
		msg.dup = 0;
		msg.payload = (void *)payload;
		msg.payloadlen = strlen(payload)+1;
		int rc = MQTTPublish(pHandle->pClient, topic, &msg);
		printf("Published %d\n", rc);
}

// native method slots
Cell communityMQTT_Message_doPublish(SedonaVM* vm, Cell* params)
{
  MQTTHandle * pHandle = (MQTTHandle *)params[0].aval;
  uint8_t * topic = params[1].aval;
  uint8_t * payload = params[2].aval;

  publish(pHandle, topic, payload);
  return nullCell;
}
