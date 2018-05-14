#ifndef __COMMON_H__
#define __COMMON_H__

typedef int bool;
#define true 1
#define false 0

#include "MQTTClient.h"
#include "uthash.h"

typedef struct 
{
  Network * pNetwork;
  MQTTClient * pClient;
  unsigned char * buf;
  int buf_len;
  unsigned char * readbuf;
  int readbuf_len;
  unsigned int command_timeout_ms;
} MQTTHandle;

enum TaskType {
    StartSessionTask,
    StopSessionTask,
    PublishTask,
    SubscribeTask,
    UnsubscribeTask
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

typedef struct {
    char * topic; 
    int32_t qos;
} SubscribeData;

typedef struct {
    char * topic; 
} UnsubscribeData;

typedef struct _Payload {
    enum TaskType type;
    union {
        StartSessionData * pStartSessionData;
        PublishData * pPublishData;
        SubscribeData * pSubscribeData;
        UnsubscribeData * pUnsubscribeData;
    };
    struct _Payload * pNext;
} Payload;

typedef struct {
    char * topic;
    char * payload;
    int32_t payload_len;
    UT_hash_handle hh;
} SubscribeResponse;

typedef struct {
    MQTTHandle * pHandle; 
    Payload * pHead;
    SubscribeResponse * pResponse;
} SessionHandle;

bool pushPayload(SessionHandle * pSession, Payload * pPayload);
Payload * curPayload(SessionHandle * pSession);
bool popPayload(SessionHandle * pSession);
int payloadSize(SessionHandle * pSession);

// helpers 
char * gen_fmt_str(const char * fmt, ...);

#endif /* __COMMON_H__ */
