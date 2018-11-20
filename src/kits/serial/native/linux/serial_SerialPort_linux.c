//
// Copyright (c) 2016 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//


#include "sedona.h"
#include "serial_SerialPort.h"

#include <termios.h>
#include <sys/ioctl.h>
#include <stdbool.h>
#include <stdint.h>
#include <linux/serial.h>
#include <fcntl.h>
#include <unistd.h>

bool debug = false;

struct SerialPortLinux {
	int fd;

};

#define maxPortNum 32
static struct SerialPortLinux portHandles[maxPortNum+1];

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



static void doClose(struct SerialPortLinux* h)
{
	if (h->fd) {
		close(h->fd);
		h->fd = 0;
	}
}

static void doFlush(struct SerialPortLinux* h)
{
	// purge input and output buffers
	tcflush(h->fd, TCIOFLUSH);
}

static int doReadBytes(struct SerialPortLinux *h, uint8_t* buf, int nbytes)
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
static int doWriteBytes(struct SerialPortLinux *h, uint8_t* pu8Buf, int32_t  nbytes)
{
	if (debug) {
		printf("write: ");
		printBytes(pu8Buf, nbytes);
	}
	return write( h->fd, pu8Buf, nbytes );
}



static int rateToConstant(int baudrate)
{
#define B(x) case x: return B##x
	switch(baudrate) {
		B(50);     B(75);     B(110);    B(134);    B(150);
		B(200);    B(300);    B(600);    B(1200);   B(1800);
		B(2400);   B(4800);   B(9600);   B(19200);  B(38400);
		B(57600);  B(115200); B(230400); B(460800); B(500000);
		B(576000); B(921600); B(1000000);B(1152000);B(1500000);
		default: return 0;
	}
#undef B
}


static bool doApplyBaudrate(struct SerialPortLinux *h, int baudrate)
{
	struct termios options;
	struct serial_struct serinfo;
	int speed = 0;
	int fd = h->fd;

	speed = rateToConstant(baudrate);

	if ((speed == 0) && (baudrate)) {
		/* Custom divisor */
		serinfo.reserved_char[0] = 0;
		if (ioctl(fd, TIOCGSERIAL, &serinfo) < 0)
			return false;
		serinfo.flags &= (int)(~ASYNC_SPD_MASK);
		serinfo.flags |= (int)ASYNC_SPD_CUST;
		serinfo.custom_divisor = (serinfo.baud_base + (baudrate / 2)) / baudrate;
		if (serinfo.custom_divisor < 1)
			serinfo.custom_divisor = 1;
		if (ioctl(fd, TIOCSSERIAL, &serinfo) < 0)
			return false;
		if (ioctl(fd, TIOCGSERIAL, &serinfo) < 0)
			return false;
		if (serinfo.custom_divisor * baudrate != serinfo.baud_base) {
			printf("actual baudrate is %d / %d = %f",
			    serinfo.baud_base, serinfo.custom_divisor,
			    ((float)serinfo.baud_base) / serinfo.custom_divisor );
		}
	}

	tcgetattr(fd, &options);
	cfsetispeed(&options, (speed_t)(speed ? speed : B38400));
	cfsetospeed(&options, (speed_t)(speed ? speed : B38400));
	cfmakeraw(&options);
	options.c_cflag |= (CLOCAL | CREAD);

	if  (tcsetattr(fd, TCSANOW, &options) == -1) {
		printf("%s: failed to write settings\n", __FUNCTION__ );
		return false;
	}
	return true;
}

static bool doApplyRtsControl(struct SerialPortLinux *h, int rts)
{
	struct termios options;
	int fd = h->fd;

	tcgetattr(fd, &options);

	/* there is no RTS level control on GNU/Linux */
	/* CTS/RTS control is always disabled */
	options.c_cflag &= ~CRTSCTS;
	options.c_cflag &= ~CRTSCTS;

	if  (tcsetattr(fd, TCSANOW, &options) == -1) {
		printf("%s: failed to write settings\n", __FUNCTION__ );
		return false;
	}
	return true;
}


static bool doApplyByteSize(struct SerialPortLinux *h, int dataBits)
{
	struct termios options;
	int fd = h->fd;
	tcgetattr(fd, &options);
	options.c_cflag &= ~CSIZE;

	switch( dataBits ) {
		case 5:
			options.c_cflag |= CS5;
			break;
		case 6:
			options.c_cflag |= CS6;
			break;
		case 7:
			options.c_cflag |= CS7;
			break;
		case 8:
			options.c_cflag |= CS8;
			break;
		default:
			printf( "Unknown byte size (%d)\n", dataBits);
			return false;
	}

	return tcsetattr(fd, TCSANOW, &options) == 0;
}

static bool doApplyParity(struct SerialPortLinux *h, int parity)
{
	struct termios options;
	int fd = h->fd;

	tcgetattr(fd, &options);

	switch(parity) {
		case PARITY_MARK:
			options.c_cflag |= CMSPAR;
			options.c_cflag |= PARODD;
			options.c_cflag |= PARENB;
			break;
		case PARITY_SPACE:
			options.c_cflag |= CMSPAR;
			options.c_cflag &=~PARODD;
			options.c_cflag |= PARENB;
			break;
		case PARITY_ODD:
			options.c_cflag &=~CMSPAR;
			options.c_cflag |= PARENB;
			options.c_cflag |= PARODD;
			break;
		case PARITY_EVEN:
			options.c_cflag &=~CMSPAR;
			options.c_cflag |= PARENB;
			options.c_cflag &=~PARODD;
			break;
		case PARITY_NONE:
			options.c_cflag &= ~PARENB;
			break;
		default:
			printf( "Unknown parity (%d)\n", parity);
			return false;
	}

	return tcsetattr(fd, TCSANOW, &options) == 0;
}

static bool doApplyStopBits(struct SerialPortLinux *h, int stopBits)
{
	struct termios options;
	int fd = h->fd;

	tcgetattr(fd, &options);
	switch( stopBits ) {
		case 1:
			options.c_cflag &=~CSTOPB;
			break;
		case 2:
			options.c_cflag |=CSTOPB;
			break;
		default:
			printf("Unknown stop bits (%d)\n", stopBits);
			return false;
	};

	return tcsetattr(fd, TCSANOW, &options) == 0;
}

static bool doApplyRs485Setting(struct SerialPortLinux *h, int rts)
{
	struct serial_rs485 rs485conf;
	int fd = h->fd;

	memset(&rs485conf, 0, sizeof(rs485conf));

	/* Enable RS485 mode: */
	rs485conf.flags |= SER_RS485_ENABLED;

	if (rts) {
		/* Set logical level for RTS pin equal to 1 when sending: */
		rs485conf.flags |= SER_RS485_RTS_ON_SEND;

		/* or, set logical level for RTS pin equal to 0 after sending: */
		rs485conf.flags &= ~(SER_RS485_RTS_AFTER_SEND);
	} else {
		/* or, set logical level for RTS pin equal to 0 when sending: */
		rs485conf.flags &= ~(SER_RS485_RTS_ON_SEND);

		/* Set logical level for RTS pin equal to 1 after sending: */
		rs485conf.flags |= SER_RS485_RTS_AFTER_SEND;
	}

	/* Set rts delay before send, if needed: */
	rs485conf.delay_rts_before_send = 0;

	/* Set rts delay after send, if needed: */
	rs485conf.delay_rts_after_send = 0;

	/* Set this flag if you want to receive data even whilst sending data */
	/* rs485conf.flags |= SER_RS485_RX_DURING_TX; */

	return ioctl(fd, TIOCSRS485, &rs485conf) == 0;
}

static bool doResetSettings(struct SerialPortLinux *h)
{
	struct termios options;
	tcgetattr(h->fd, &options);
	cfmakeraw(&options);

	return tcsetattr(h->fd, TCSANOW, &options) ==0;
}



static bool doApplySettings(struct SerialPortLinux *h,
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

	struct SerialPortLinux *h = &portHandles[portNum];
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
