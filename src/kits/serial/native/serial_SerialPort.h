#ifndef _serial_SerialPort_h_
#define _serial_SerialPort_h_

#define PARITY_DISABLED 0

/* ** Value of parity field for no parity */
#define PARITY_NONE      0

/* ** Value of parity field for odd parity */
#define PARITY_ODD      1

/* ** Value of parity field for even parity */
#define PARITY_EVEN     2

/* ** Value of parity field for mark parity */
#define PARITY_MARK     3

/* ** Value of parity field for space parity */
#define PARITY_SPACE    4

/* ** Value of rtsLevel field if RTS should initially be low (active high) */
#define RTS_LOW         0

/* ** Value of rtsLevel field if RTS should initially be high (active low) */
#define RTS_HIGH        1


#endif
