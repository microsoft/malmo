# Copyright (c) 2017 Microsoft Corporation.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
#  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
# and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of
# the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
# THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
#  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================================================================

# NB if building this on Windows/OSX, make sure Docker has been allowed enough memory - the default 2048Mb is not
# enough for the gradle Minecraft deobfuscation step.

# This dockerfile supports building a "manylinux1" (hopefully somewhat widely compatible) MalmoPython.so to use
# with Linux flavours  (please see https://github.com/pypa/manylinux for detials).
# Not everything is in place as Centos5 is past end of life. Use the following to build after launching a container interactively:
# cmake -DSTATIC_BOOST=ON -DBoost_INCLUDE_DIR=/home/malmo/boost/boost_1_66_0/include -DUSE_PYTHON_VERSIONS=3.5 -DCMAKE_BUILD_TYPE=Release -DBUILD_DOCUMENTATION=OFF -DINCLUDE_JAVA=OFF -DBUILD_MOD=OFF ..

FROM quay.io/pypa/manylinux1_x86_64

# Need to install sudo first:
RUN yum install -y sudo
# Can only run sudo from a terminal on CentOS 5.

# (Skip) desktop components.
#RUN sudo rpm --import http://li.nux.ro/download/nux/RPM-GPG-KEY-nux.ro
#RUN sudo rpm -Uvh http://li.nux.ro/download/nux/dextop/el7/x86_64/nux-dextop-release-0-5.el7.nux.noarch.rpm

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --groups wheel malmo
RUN bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# Centos 5 is end of life. Yum retired to:
COPY ./centos5.yum.repos.d/ /etc/yum.repos.d/

# (Skip) Add repo for xpra:
#RUN rpm --import "http://winswitch.org/gpg.asc"
#RUN su -c 'curl https://winswitch.org/downloads/CentOS/winswitch.repo | tee /etc/yum.repos.d/winswitch.repo'

# While we are still root, install the necessary dependencies for Malmo:
RUN yum update && yum install -y \
    git \
    python34-devel \
    java-1.8.0-openjdk-devel \
    swig \
    doxygen \
    libxslt \
    ffmpeg \
    ffmpeg-devel \
    gcc-c++ \
    bzip2-devel \
    python34-tkinter \
    wget \
    software-properties-common \
    xpra \
	libgl1-mesa-dri \
    make \
    python34-pip \
    zlib-devel
#   libbz2-dev \
#	zlib1g-dev

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/" >> /home/malmo/.bashrc

RUN rpm --import "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

RUN curl -O https://www.python.org/ftp/python/3.6.6/Python-3.6.6.tgz
RUN tar xf Python-3.6.6.tgz
WORKDIR /home/malmo/Python-3.6.6
RUN ./configure --enable-shared
RUN make

# Switch to root as sudo complains here on CentOS 5.
USER 0
WORKDIR /home/malmo/Python-3.6.6
RUN make install

USER malmo

# BOOST:
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
RUN echo "using python : 3.6 : /usr/local/bin/python3 : /usr/local/include/python3.6m : /usr/local/lib ;" > ~/user-config.jam
# wget is failing with "moved permanently". Copy in tar file instead.
# wget http://sourceforge.net/projects/boost/files/boost/1.66.0/boost_1_66_0.tar.gz
COPY boost_1_66_0.tar.gz .
RUN tar xvf boost_1_66_0.tar.gz
WORKDIR /home/malmo/boost/boost_1_66_0
RUN ./bootstrap.sh --prefix=.
RUN ./b2 link=static cxxflags=-fPIC install

# CMAKE:
RUN mkdir /home/malmo/cmake
WORKDIR /home/malmo/cmake
COPY cmake-3.11.3.tar.gz .
RUN tar xvf cmake-3.11.3.tar.gz
WORKDIR /home/malmo/cmake/cmake-3.11.3
RUN ./bootstrap
RUN make -j4

USER 0
RUN make install

# (Skip as no pip3)
# RUN pip3 install future && sudo pip3 install pillow && sudo pip3 install matplotlib

RUN yum update -y && yum -y install dos2unix
COPY ./build.sh /home/malmo
RUN dos2unix /home/malmo/build.sh
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas

USER malmo

#ENTRYPOINT ["/home/malmo/build.sh", "-boost", "1_66_0", "-python", "3.6"]
