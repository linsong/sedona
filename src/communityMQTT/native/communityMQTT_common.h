#ifndef __COMMON_H__
#define __COMMON_H__

typedef int bool;
#define true 1
#define false 0

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

enum TaskType {
    StartSessionTask,
    StopSessionTask,
    PublishTask,
    SubscribeTask
};

typedef struct {
    char * host;
    int port;
    char * clientid;
    char * username;
    char * password;
} StartSessionData;

typedef struct {
    char * topic; 
    void * payload;
    int32_t payload_len;
    int32_t qos;
} PublishData;

typedef struct _Payload {
    enum TaskType type;
    union {
        StartSessionData * pStartSessionData;
        PublishData * pPublishData;
    };
    struct _Payload * pNext;
} Payload;

typedef struct {
    MQTTHandle * pHandle; 
    Payload * pHead;
} SessionHandle;

bool pushPayload(SessionHandle * pSession, Payload * pPayload);
Payload * curPayload(SessionHandle * pSession);
bool popPayload(SessionHandle * pSession);

#endif /* __COMMON_H__ */
