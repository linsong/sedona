<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Aug 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Introduction

## Overview
The Sedona Framework is a software framework designed to make it easy to build
smart, networked embedded devices.  Some of the Sedona Framework highlights:

- **Sedona Language**: This is a general purpose component-oriented
programming language very similar to Java or C#.  The Sedona language
is used to write your own custom functionality

- **Sedona Virtual Machine**: The virtual machine is
a small interpreter written in ANSI C and designed for portability.  It
allows code written in the Sedona programming language to be written
once, but run on any Sedona Framework-enabled device.  The VM itself is designed to be
highly portable to new microprocessors and operating systems

- **Small Devices**: The Sedona Framework is ideal for very small embedded devices.  
It can run on platforms with less than 100KB of memory!

- **Component Oriented Programming**: The Sedona Framework enables a style of
programming where pre-built components are assembled into applications.
Components can act as services or be explicitly linked together to
create data and control flow.  This model is especially suited to
graphical programming tools

- **Networking**: Several protocols are bundled with the Sedona Framework
to provision, program, and communicate with Sedona Framework-enabled devices over various
network topologies.  You can remotely add, remove, and modify the components
in your application in real-time.  You can even upgrade the firmware itself
over the network.  Sedona Framework networking is designed to work over any
IP network, including [6LoWPAN]('http://en.wikipedia.org/wiki/6LoWPAN')

- **Open Source Ecosystem**: The core Sedona Framework technology uses
a flexible academic styled license.  This makes it easy for manufacturers
to Sedona Framework-enable their devices.  Tools and applications written in Sedona
are guaranteed portable to any Sedona Framework device
