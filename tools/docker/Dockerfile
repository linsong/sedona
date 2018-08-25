#
# Copyright 2015 XXX. All Rights Reserved.
# Licensed under the Academic Free License version 3.0
#
# History:
#    2015 Oct 15   Divisuals   Initial Docker Image
#    2016 Feb 23   Divisuals   Updated to latest JDK 8_73
#    2018 Apr 22   Divisuals   Updated to latest JDK 8_171, centos (i386)
#    2018 Aug 25   Divisuals   Updated to latest JDK 8_181, centos (i386)
#

FROM i386/centos:latest
MAINTAINER Divisuals <divisuals.net@gmail.com>

LABEL Vendor="Sedona"
LABEL version=develop

# 32-bit
# http://download.oracle.com/otn-pub/java/jdk/8u181-b13/96a7b8442fe848ef90c96a2fad6ed6d1/jdk-8u181-linux-i586.tar.gz
# sha d78a023abffb7ce4aade43e6db64bbad5984e7c82c54c332da445c9a79c1a904

# download JDK 8 u181 (i586)
ENV JAVA_VERSION_MAJOR 8
ENV JAVA_VERSION_MINOR 181
ENV JAVA_VERSION_BUILD 13
ENV JAVA_PACKAGE=jdk
ENV JAVA_SHA256_SUM=d78a023abffb7ce4aade43e6db64bbad5984e7c82c54c332da445c9a79c1a904
ENV JAVA_URL_ELEMENT=96a7b8442fe848ef90c96a2fad6ed6d1
ENV	JAVA_HOME=/opt/jdk

# Copy .bashrc file
COPY .bashrc /root

# download packages, JDK, untar and cleanup
# Fix centos repo URL error
# NOT FOUND: http://mirror.centos.org/altarch/7/os/x86_64/repodata/repomd.xml
RUN	echo "[docker] installing OS packages..." &&\
  sed -i -e "s|\$basearch|i386|g" /etc/yum.repos.d/CentOS-Base.repo &&\
  yum install -y wget which tar git gcc glibc-static python-argparse.noarch &&\
  mkdir -p /opt && cd /tmp &&\
  echo "[docker] downloading ${JAVA_PACKAGE} tar..." &&\
  curl -jkLH "Cookie: oraclelicense=accept-securebackup-cookie" -o java.tar.gz\
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/${JAVA_URL_ELEMENT}/${JAVA_PACKAGE}-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-i586.tar.gz &&\
  echo "[docker] checking SHA for downloaded bits..." &&\
  echo "$JAVA_SHA256_SUM  java.tar.gz" | sha256sum -c - &&\
  echo "[docker] extracting contents..." &&\
  tar -xzf java.tar.gz -C /opt &&\
  ln -s /opt/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR} /opt/jdk &&\
  rm -rf  $JAVA_HOME/*JAVAFX.txt \
          $JAVA_HOME/*src.zip \
          $JAVA_HOME/lib/missioncontrol \
          $JAVA_HOME/lib/visualvm \
          $JAVA_HOME/lib/*javafx* \
          $JAVA_HOME/jre/bin/javaws \
          $JAVA_HOME/jre/bin/jjs \
          $JAVA_HOME/jre/bin/keytool \
          $JAVA_HOME/jre/bin/orbd \
          $JAVA_HOME/jre/bin/pack200 \
          $JAVA_HOME/jre/bin/policytool \
          $JAVA_HOME/jre/bin/rmid \
          $JAVA_HOME/jre/bin/rmiregistry \
          $JAVA_HOME/jre/bin/servertool \
          $JAVA_HOME/jre/bin/tnameserv \
          $JAVA_HOME/jre/bin/unpack200 \
          $JAVA_HOME/jre/lib/javaws.jar \
          $JAVA_HOME/jre/lib/desktop \
          $JAVA_HOME/jre/lib/deploy* \
          $JAVA_HOME/jre/lib/*javafx* \
          $JAVA_HOME/jre/lib/*jfx* \
          $JAVA_HOME/jre/lib/oblique-fonts \
          $JAVA_HOME/jre/lib/plugin.jar \
          $JAVA_HOME/jre/lib/amd64/libdecora_sse.so \
          $JAVA_HOME/jre/lib/amd64/libprism_*.so \
          $JAVA_HOME/jre/lib/amd64/libfxplugins.so \
          $JAVA_HOME/jre/lib/amd64/libglass.so \
          $JAVA_HOME/jre/lib/amd64/libgstreamer-lite.so \
          $JAVA_HOME/jre/lib/amd64/libjavafx*.so \
          $JAVA_HOME/jre/lib/amd64/libjfx*.so \
          $JAVA_HOME/jre/lib/ext/nashorn.jar \
          $JAVA_HOME/jre/lib/ext/jfxrt.jar \
          $JAVA_HOME/jre/plugin \
          /usr/share/locale/* &&\
  yum clean all && rm -fr /tmp/* /var/cache/yum/*

ENV PATH $PATH:$JAVA_HOME/bin
