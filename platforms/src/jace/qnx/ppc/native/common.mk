# This is an automatically generated record.
# The area between QNX Internal Start and QNX Internal End is controlled by
# the QNX IDE properties.

ifndef QCONFIG
QCONFIG=qconfig.mk
endif
include $(QCONFIG)

#===== USEFILE - the file containing the usage message for the application. 
USEFILE=
NAME=svm

#===== LIBS - a space-separated list of library items to be included in the link.
LIBS+=socket m

#===== CCFLAGS - add the flags to the C compiler command line. 
#CCFLAGS+=-DPLAT_BUILD_VERSION="1.2.21" -DSOCKET_FAMILY_INET
#NOTE: for generation using python scripts from sedona, the above CCFLAGS
#      are passed as command line args to the make utility in QNX
#      See "makeqnxvm.py" as an example of how to construct the "make" command line.


include $(MKFILES_ROOT)/qmacros.mk
ifndef QNX_INTERNAL
QNX_INTERNAL=$(PROJECT_ROOT)/.qnx_internal.mk
endif
include $(QNX_INTERNAL)

include $(MKFILES_ROOT)/qtargets.mk

OPTIMIZE_TYPE_g=none
OPTIMIZE_TYPE=$(OPTIMIZE_TYPE_$(filter g, $(VARIANTS)))

