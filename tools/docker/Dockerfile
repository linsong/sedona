#
# Copyright 2015 XXX. All Rights Reserved.
# Licensed under the Academic Free License version 3.0
#
# History:
#    2015 Oct 15   Divisuals   Initial Docker Image
#    2016 Feb 23   Divisuals   Updated to latest JDK 8_73
#

FROM toopher/centos-i386:centos6
MAINTAINER Divisuals <divisuals.net@gmail.com>

LABEL Vendor="Sedona"
LABEL version=develop

# download JDK 8 u73
ENV	JAVA_HOME /jdk1.8.0_73

RUN	yum install -y wget which tar git gcc glibc-static python-argparse.noarch \
	&& wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u73-b02/jdk-8u73-linux-i586.tar.gz" \
	&& tar xf jdk-8u73-linux-i586.tar.gz \
	&& rm -f /jdk-8u73-linux-i586.tar.gz \
	&& rm -rf $JAVA_HOME/*src.zip \
						$JAVA_HOME/lib/missioncontrol \
						$JAVA_HOME/lib/visualvm \
						$JAVA_HOME/lib/*javafx* \
						$JAVA_HOME/jre/lib/plugin.jar \
						$JAVA_HOME/jre/lib/ext/jfxrt.jar \
						$JAVA_HOME/jre/bin/javaws \
						$JAVA_HOME/jre/lib/javaws.jar \
						$JAVA_HOME/jre/lib/desktop \
						$JAVA_HOME/jre/plugin \
						$JAVA_HOME/jre/lib/deploy* \
						$JAVA_HOME/jre/lib/*javafx* \
						$JAVA_HOME/jre/lib/*jfx* \
						$JAVA_HOME/jre/lib/amd64/libdecora_sse.so \
						$JAVA_HOME/jre/lib/amd64/libprism_*.so \
						$JAVA_HOME/jre/lib/amd64/libfxplugins.so \
						$JAVA_HOME/jre/lib/amd64/libglass.so \
						$JAVA_HOME/jre/lib/amd64/libgstreamer-lite.so \
						$JAVA_HOME/jre/lib/amd64/libjavafx*.so \
						$JAVA_HOME/jre/lib/amd64/libjfx*.so \
	&& rm -rf /usr/share/locale/* \
	&& yum clean all

ENV PATH $PATH:$JAVA_HOME/bin
