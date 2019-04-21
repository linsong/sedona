//
// Copyright (c) 2019 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//

#ifdef __DARWIN__

#include "sedona.h"
#include "serial_SerialPort.h"
#include "serial_SerialPortNative_unix.h"

#include <stdbool.h>
#include <stdint.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>


bool doApplyBaudrate(struct SerialPortUnix *h, int baudrate)
{
	printf("doApplyBaudrate not yet implemented for the platform.\n");
	return false;
}

bool doApplyRtsControl(struct SerialPortUnix *h, int rts)
{
	printf("doApplyRtsControl not yet implemented for the platform.\n");
	return false;
}


bool doApplyByteSize(struct SerialPortUnix *h, int dataBits)
{
	printf("doApplyByteSize not yet implemented for the platform.\n");
	return false;
}

bool doApplyParity(struct SerialPortUnix *h, int parity)
{
	printf("doApplyParity not yet implemented for the platform.\n");
	return false;
}

bool doApplyStopBits(struct SerialPortUnix *h, int stopBits)
{
	printf("doApplyStopBits not yet implemented for the platform.\n");
	return false;
}

bool doApplyRs485Setting(struct SerialPortUnix *h, int rts)
{
	printf("doApplyRs485Setting not yet implemented for the platform.\n");
	return false;
}

bool doResetSettings(struct SerialPortUnix *h)
{
	printf("doResetSettings not yet implemented for the platform.\n");
	return false;
}


#endif
