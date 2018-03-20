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

FROM centos:7

# Need to install sudo first:
RUN yum install -y sudo epel-release

RUN sudo rpm --import http://li.nux.ro/download/nux/RPM-GPG-KEY-nux.ro
RUN sudo rpm -Uvh http://li.nux.ro/download/nux/dextop/el7/x86_64/nux-dextop-release-0-5.el7.nux.noarch.rpm

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --no-log-init --groups wheel malmo
RUN sudo bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# Add repo for xpra:
RUN rpm --import "http://winswitch.org/gpg.asc"
RUN su -c 'curl https://winswitch.org/downloads/CentOS/winswitch.repo | tee /etc/yum.repos.d/winswitch.repo'

# Add repo for mono:
RUN rpm --import "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"
RUN su -c 'curl https://download.mono-project.com/repo/centos7-stable.repo | tee /etc/yum.repos.d/mono-centos7-stable.repo'

# While we are still root, install the necessary dependencies for Malmo:
RUN sudo yum install -y \
    git \
    cmake \
    cmake-gui \
    python-devel \
    java-1.8.0-openjdk-devel \
    swig \
    xerces-c-devel \
    doxygen \
    libxslt \
    ffmpeg \
    ffmpeg-devel \
    gcc-c++ \
    lua \
    lua-devel \
    bzip2-devel \
    tkinter \
    python2-pillow-tk \
    wget \
    luarocks \
    software-properties-common \
    xpra \
	libgl1-mesa-dri \
    make \
    python-pip \
    zlib-devel \
    mono-devel \
    mono-complete

#    libbz2-dev \
#	zlib1g-dev

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/" >> /home/malmo/.bashrc

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

# TORCH not supported on CentOS, so nothing to do here.

# BOOST:
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
#COPY ./boost_1_65_0.tar.gz /home/malmo/boost
RUN wget http://sourceforge.net/projects/boost/files/boost/1.65.0/boost_1_65_0.tar.gz
RUN tar xvf boost_1_65_0.tar.gz
WORKDIR /home/malmo/boost/boost_1_65_0
RUN ./bootstrap.sh --prefix=.
RUN ./b2 link=static cxxflags=-fPIC install

# XSD:
RUN wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd-4.0.0-1.x86_64.rpm
RUN sudo rpm -i --force xsd-4.0.0-1.x86_64.rpm

# LUABIND:
RUN git clone https://github.com/rpavlik/luabind.git /home/malmo/rpavlik-luabind
WORKDIR /home/malmo/rpavlik-luabind
RUN mkdir build
WORKDIR /home/malmo/rpavlik-luabind/build
RUN cmake -DBoost_INCLUDE_DIR=/home/malmo/boost/boost_1_65_0/include -DCMAKE_BUILD_TYPE=Release ..
RUN make

RUN sudo pip install future && sudo pip install pillow && sudo pip install matplotlib
RUN sudo luarocks install luasocket

RUN sudo yum update -y && sudo yum -y install dos2unix
COPY ./build.sh /home/malmo
RUN sudo dos2unix /home/malmo/build.sh
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas
ENTRYPOINT ["/home/malmo/build.sh", "-boost", "1_65_0"]
