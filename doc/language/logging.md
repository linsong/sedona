
<!--
[//]: # (Copyright &#169; 2007 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    25 Sep 07  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Logging

## Overview

The Sedona Framework includes a built-in logging facility with the
following features:

-   Simple syntax to embed logging in your source code
-   Ability to selectively enable/disable logging at runtime
-   Ability to selectively compile logging in or out of an scode image

## Log Levels

There are five logging severity levels:

-   `Log.NONE`: all logging is disabled
-   `Log.ERROR`: indicates a failure condition
-   `Log.WARNING`: indicates an unexpected condition
-   `Log.MESSAGE`: indicates something of interest
-   `Log.TRACE`: used to embed debug tracing

## Log Definition

The [`sys::Log`](api/sys/Log) class is the primary API used for
logging. The `Log` class is a const class like `Type` or `Slot`. This
means you can't directly allocate instances yourself. Instead we use
the `define` keyword to define a Log instance:

    class MyService
    {
      define Log log
    }

Like other defines, the log is treated much like a static field. During
compilation all the log instances are compiled into the scode as const
data much like `Kit` instances. The APIs for log reflection follow a
similar pattern to kit reflection:
```
    class Sys
    {
      define int logsLen
      const static Log[logsLen] logs
      static inline byte[logsLen] logLevels

      static Log log(int id)
      static Log findLog(Str qname)

    }

    const class Log
    {
      define int NONE = 0
      define int ERROR = 1
      define int WARNING = 2
      define int MESSAGE = 3
      define int TRACE = 4

      int level()
      bool isError()
      bool isWarning()
      bool isMessage()
      bool isTrace()

      OutStream error(Str msg)
      OutStream warning(Str msg)
      OutStream message(Str msg)
      OutStream trace(Str msg)

      const short id
      const Str qname
    }
```

Note that the logging levels are stored separately from the `Log`
instances themselves. This is because the log objects are readonly and
stored in the code section. The log levels must be in dynamic memory to
allow runtime modification.

## Log Naming

All logs are identified by a [qname](/language/lang#namespaces) (qualified
name), which is based on the define field's qname:

1.  If the define field is named "log", then the log qname is the
    qname of the declaring type
2.  If the define field ends in "Log", then the log qname is the
    field's qname minus the "Log" suffix
3.  If none of the above applies, then log's qname is the field's
    qname

For example this class in a kit named "acme":

    class MyService
    {
      define Log log     // log qname is "acme::MyService"
      define Log reqLog  // log qname is "acme::MyService.req"
      define Log stuff   // log qname is "acme::MyService.stuff"
    }

By convention, your primary Log definition should be named "log" to
match rule 1. Sub-logging for extra tracing should use names with a
"Log" suffix.

## Logging

To embed logging into your code, call one of following the *logging
methods*:

-   `sys::Log.error`
-   `sys::Log.warning`
-   `sys::Log.message`
-   `sys::Log.trace`

All of the logging methods take a `Str` and return an `OutStream`, which
permits [string interpolation](/language/expr#string-interpolation):

    // these lines of code
    log.message("Started!")
    log.error("Cannot open port=$port")
    log.trace("Received $numBytes bytes from $addr")

    // would print something like
    -- MESSAGE [acme::MyServer] Started!
    -- ERROR [acme::MyServer] Cannot open port 8080
    -- TRACE [acme::MyServer] Received 5 bytes from 32

A few points to note:

-   Don't add a trailing newline to your message, one will be added
    automatically
-   Don't include any severity or log identity in your message string,
    this information is automatically included (the actual format is
    implementation specific)
-   You are required to call the logging methods on the field define
    itself. For example you can't assign the log reference to a local
    variable, then use the local variable to call one of the logging
    methods - this will result in a compile time error. The reason for
    this restriction is that it enables the compiler to generate level
    checking and conditional compilation checking.

## Runtime Configuration

Log levels are stored in the `Sys.logLevels` field. This array is
indexed by `Log.id` and stores the current level as a number between 0
and 4 (Log.NONE to Log.TRACE):

    // change my logging level to trace
    Sys.logLevels[log.id] = Log.TRACE

The current level defines the maximum severity that is logged. For
example a level or WARNING will log calls to `error` and `warning`, but
not calls to `message` and `trace`. All log levels default to MESSAGE on
startup.

The compiler automatically inserts code that jumps over a logging
statement if the current log level is set lower. This means the method
call and any embedded expressions are efficiently skipped:

    // this code
    log.trace("This is the ${count++} time")

    // is semantically equivalent to
    if (log.isTrace())
      log.trace("This is the ${count++} time")

### WebService Spy Page
-------------------

If a Sedona device is running the `web::WebService`, then you can use
the spy page at `"<device IP>/spy/logs"` to change the log levels for a
SVM at runtime.

## Compile-time Configuration

!!! Note
    This feature is not implemented yet
