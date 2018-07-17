#
# Licensed under the Academic Free License version 3.0
#
# History:
#    2015 Nov 02   Vincent   Initial Docker Image(Ubuntu)
#

FROM phusion/baseimage
MAINTAINER Vincent <linsong.qizi@gmail.com>

LABEL Vendor="Sedona"
LABEL version=develop

RUN apt-get update &&\
 apt-get install -y build-essential libc6-dev-i386 python default-jdk &&\
 apt-get purge -y man &&\
 apt-get clean autoclean &&\
 apt-get autoremove -y

ENV JAVA_HOME /usr/lib/jvm/default-java
ENV PATH $PATH:$JAVA_HOME/bin
