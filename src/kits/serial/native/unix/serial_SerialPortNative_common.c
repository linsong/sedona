//
// Copyright (c) 2019 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//


#include "sedona.h"
#include "serial_SerialPort.h"
#include "serial_SerialPortNative_unix.h"

#include <termios.h>
#include <sys/ioctl.h>
#include <stdbool.h>
#include <stdint.h>
#include <fcntl.h>
#include <unistd.h>

static bool debug = false;

#define maxPortNum 32
static struct SerialPortUnix portHandles[maxPortNum+1];

int __attribute__((weak)) getMaxPortNum()
{
	return maxPortNum;
}

const char* __attribute__((weak)) getPortName(int portNum)
{
	static char port[32];
	snprintf(port, sizeof(port), "/dev/ttyS%d",portNum);
	return port;
}

static inline bool checkPortNum(int portNum)
{
	if (portNum > maxPortNum) {
		fprintf(stderr, "portNum (%d) is greater than last supported port (%d)\n",
			portNum,
			maxPortNum);
		return false;
	}
	return ((portNum>0) && (portNum<=getMaxPortNum()));
}



static void printBytes(char *msgp, unsigned length)
{
	unsigned i;

	for( i=0 ; i<length ; i++)
		printf("%2.2X ", msgp[i] & 0x0ff );
	printf("\n");
}

static bool isOpen(int port)
{
	if (!checkPortNum(port))
		return false;

	return (portHandles[port].fd != 0);
};



static void doClose(struct SerialPortUnix* h)
{
	if (h->fd) {
		close(h->fd);
		h->fd = 0;
	}
}

static void doFlush(struct SerialPortUnix* h)
{
	// purge input and output buffers
	tcflush(h->fd, TCIOFLUSH);
}

static int doReadBytes(struct SerialPortUnix *h, uint8_t* buf, int nbytes)
{
	int bytesRead;
	bytesRead = read(h->fd, buf, nbytes);
	if(debug && (bytesRead>0)) {
		printf("read: ");
		printBytes(buf, bytesRead);
	}
	return bytesRead;
}

// return number of bytes written -
static int doWriteBytes(struct SerialPortUnix *h, uint8_t* pu8Buf, int32_t  nbytes)
{
	if (debug) {
		printf("write: ");
		printBytes(pu8Buf, nbytes);
	}
	return write( h->fd, pu8Buf, nbytes );
}


static bool doApplySettings(struct SerialPortUnix *h,
			int     baudRate,
			int     dataBits,
			int     parity,
			int     stopBits,
			int     rts,
			bool    rs485)
{
	if (! doResetSettings(  h           )) return false;
	if (! doApplyBaudrate(  h, baudRate )) return false;
	if (! doApplyParity(    h, parity   )) return false;
	if (! doApplyByteSize(  h, dataBits )) return false;

	if (rs485)
		if (! doApplyRs485Setting(h, rts)) return false;
	else
		if (! doApplyRtsControl(h, rts      )) return false;

	doFlush(h);

	return true;
}




// int SerialPort.doInit(int port, int baud, int stopb, int wlen, int parity)
Cell serial_SerialPortNative_doInitNative(SedonaVM* vm, Cell* params)
{
	int portNum  = params[1].ival;
	int baudRate = params[2].ival;
	int dataBits = params[3].ival;
	int stopBits = params[4].ival;
	int parity   = params[5].ival;
	int rtsLevel = params[6].ival;
	bool rs485    = params[7].ival;

	if (!checkPortNum(portNum)) {
		printf("Invalid port %d\n", portNum);
		return negOneCell;
	}

	struct SerialPortUnix *h = &portHandles[portNum];
	if (isOpen(portNum))
		doClose(h);

	const char *portName = getPortName(portNum);
	h->fd = open(portName , O_RDWR | O_NOCTTY | O_NONBLOCK | O_SYNC );
	if (h->fd == -1) {
		printf("Can't open ComPort %s: Error invalid handle\n", portName );
		h->fd = 0;
		return negOneCell;
	}

	// Set exclusive use flag to block other open calls with EBUSY
	if (ioctl(h->fd, TIOCEXCL, 0) == -1) {
		doClose(h);
		printf("Can't open ComPort %s exclusively\n", portName);
		return negOneCell;
	}

	if (!doApplySettings(h,	baudRate, dataBits, parity, stopBits, rtsLevel, rs485)) {
		doClose(h);
		printf("Can't apply settings ComPort\n");
		return negOneCell;
	}
	doFlush(h);
	return zeroCell;
}




/* Shut down the serial port.  Return 0 if successful. */
/* int SerialPort.doClose(int port) */
Cell serial_SerialPortNative_doCloseNative(SedonaVM* vm, Cell* params)
{
	int32_t portNum  = params[1].ival;

	if (!isOpen(portNum))
		return negOneCell;


	doClose(&portHandles[portNum]);
	return zeroCell;
}


 /* Read one byte from port.  Return byte value, or -1 if no byte was */
 /* available.  (non-blocking) */
/* int  SerialPort.doRead(int port) */
Cell serial_SerialPortNative_doReadNative(SedonaVM* vm, Cell* params)
{
	int32_t portNum = params[1].ival;
	int32_t bytesRead;
	uint8_t ch;
	Cell    ret;

	if (!isOpen(portNum))
		return negOneCell;

	bytesRead = doReadBytes(&portHandles[portNum], &ch, 1);

	if (bytesRead != 1)
		return negOneCell;

	ret.ival = ch;
	return ret;
}


/* int  SerialPort.doWrite(int port, int c) */
Cell serial_SerialPortNative_doWriteNative(SedonaVM* vm, Cell* params)
{
	int32_t  portNum = params[1].ival;
	uint8_t  ch      = (uint8_t)params[2].ival;
	int32_t bytesWritten;

	if (!isOpen(portNum))
		return negOneCell;

	bytesWritten = doWriteBytes(&portHandles[portNum], &ch, 1);
	return 	bytesWritten != 1 ? negOneCell : zeroCell;
}


/* Read up to n bytes from port into array y.  Return number of bytes */
/* read, or -1 if an error occurred.  (non-blocking) */
/* int  SerialPort.doReadBytes(int port, byte[] y, int off, int len) */
Cell serial_SerialPortNative_doReadBytesNative(SedonaVM* vm, Cell* params)
{
	Cell     ret;
	int32_t  portNum = params[1].ival;
	uint8_t* pu8Buf  = params[2].aval;
	int32_t  off     = params[3].ival;
	int32_t  nbytes  = params[4].ival;

	int32_t  bytesRead;

	if (!isOpen(portNum))
		return negOneCell;

	pu8Buf = pu8Buf + off;
	bytesRead = doReadBytes(&portHandles[portNum], pu8Buf, nbytes);
	ret.ival = bytesRead;
	return ret;
}



/* Write up to n bytes to port from array y.  Return number of bytes */
/* written, or -1 if an error occurred. */
/* int  SerialPort.doWriteBytes(int port, byte[] y, int off, int len) */
Cell serial_SerialPortNative_doWriteBytesNative(SedonaVM* vm, Cell* params)
{
	Cell     ret;
	int32_t  portNum = params[1].ival;
	uint8_t* pu8Buf  = params[2].aval;
	int32_t  off     = params[3].ival;
	int32_t  nbytes  = params[4].ival;
	int32_t  bytesWritten;

	if (!isOpen(portNum))
		return negOneCell;

	pu8Buf = pu8Buf + off;
	bytesWritten = doWriteBytes(&portHandles[portNum], pu8Buf, nbytes);
	if (bytesWritten==-1)
		return negOneCell;
	ret.ival = bytesWritten;
	return ret;

}
