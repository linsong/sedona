//
// Copyright (c) 2019 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//

#ifndef _SERIAL_SERIALPORTNATIVE_UNIX_H_
#define _SERIAL_SERIALPORTNATIVE_UNIX_H_

struct SerialPortUnix {
	int fd;
};

bool doApplyBaudrate(struct SerialPortUnix *h, int baudrate);
bool doApplyRtsControl(struct SerialPortUnix *h, int rts);
bool doApplyByteSize(struct SerialPortUnix *h, int dataBits);
bool doApplyParity(struct SerialPortUnix *h, int parity);
bool doApplyStopBits(struct SerialPortUnix *h, int stopBits);
bool doApplyRs485Setting(struct SerialPortUnix *h, int rts);
bool doResetSettings(struct SerialPortUnix *h);

#endif


