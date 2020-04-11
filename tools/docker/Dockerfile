#
# Copyright 2015 XXX. All Rights Reserved.
# Licensed under the Academic Free License version 3.0
#
# History:
#    2015 Oct 15   Divisuals   Initial Docker Image
#    2016 Feb 23   Divisuals   Updated to latest JDK 8_73
#    2018 Apr 22   Divisuals   Updated to latest JDK 8_171, centos (i386)
#    2018 Aug 25   Divisuals   Updated to latest JDK 8_181, centos (i386)
#    2020 Apr 11   Divisuals   openJDK, centos-release-7-5.1804.4.el7.centos.i.i686)
#

FROM i386/centos:latest
MAINTAINER Divisuals <divisuals.net@gmail.com>

LABEL Vendor="Sedona"
LABEL version=develop

# Copy .bashrc file
COPY .bashrc /root

# download packages, JDK and cleanup
# Fix centos repo URL error
# NOT FOUND: http://mirror.centos.org/altarch/7/os/x86_64/repodata/repomd.xml
RUN	echo "[docker] installing OS packages..." &&\
  sed -i -e "s|\$basearch|i386|g" /etc/yum.repos.d/CentOS-Base.repo &&\
  yum install -y which git gcc glibc-static python3 python-argparse.noarch java-1.8.0-openjdk-devel &&\
  cd /tmp && curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py && python3 get-pip.py &&\
  pip install mkdocs mkdocs-material mkdocs-git-revision-date-localized-plugin &&\
  rm -rf /usr/share/locale/* &&\
  yum clean all && rm -fr /tmp/* /var/cache/yum/*

ENV JAVA_HOME /usr/lib/jvm/java
ENV LANG en_US.utf8
ENV LC_ALL en_US.utf8
