//
// Copyright (c) 2016 Andrey Skvortsov <andrej.skvortzov@gmail.com>
// Licensed under the Academic Free License version 3.0
//

#if defined(__linux__)

#include "sedona.h"
#include "serial_SerialPort.h"
#include "serial_SerialPortNative_unix.h"

#include <termios.h>
#include <sys/ioctl.h>
#include <stdbool.h>
#include <stdint.h>
#include <linux/serial.h>
#include <fcntl.h>
#include <unistd.h>


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


bool doApplyBaudrate(struct SerialPortUnix *h, int baudrate)
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

bool doApplyRtsControl(struct SerialPortUnix *h, int rts)
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


bool doApplyByteSize(struct SerialPortUnix *h, int dataBits)
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

bool doApplyParity(struct SerialPortUnix *h, int parity)
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

bool doApplyStopBits(struct SerialPortUnix *h, int stopBits)
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

bool doApplyRs485Setting(struct SerialPortUnix *h, int rts)
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

bool doResetSettings(struct SerialPortUnix *h)
{
	struct termios options;
	tcgetattr(h->fd, &options);
	cfmakeraw(&options);

	return tcsetattr(h->fd, TCSANOW, &options) ==0;
}


#endif
