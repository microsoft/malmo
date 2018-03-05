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

FROM ubuntu:14.04

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --no-log-init --groups sudo malmo
RUN sudo bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# While we are still root, install the necessary dependencies for Malmo:
RUN sudo apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    cmake \
    cmake-qt-gui \
    libboost-all-dev \
    libpython2.7-dev \
    lua5.1 \
    liblua5.1-0-dev \
    swig \
    libxerces-c-dev \
    doxygen \
    xsltproc \
    libav-tools \
    python-tk \
    python-imaging-tk \
    wget \
    luarocks \
    libbz2-dev \
    python-pip \
    software-properties-common \
    xpra \
	libgl1-mesa-dri

# Need to use Java 8, because Java 7 has problems with "EC parameter error" on Ubuntu 14.04
RUN sudo add-apt-repository ppa:openjdk-r/ppa
RUN sudo apt-get update && apt-get install -y openjdk-8-jdk
RUN sudo update-ca-certificates -f

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> /home/malmo/.bashrc

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

# TORCH:
RUN echo "Installing torch..."
RUN git clone https://github.com/torch/distro.git /home/malmo/torch --recursive
WORKDIR /home/malmo/torch
RUN bash install-deps
RUN ./install.sh -b
#RUN source /home/malmo/torch/install/bin/torch-activate
RUN /home/malmo/torch/install/bin/th -e "print 'Torch installed correctly'"

# MONO:
RUN echo "Installing mono..."
RUN sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
RUN echo "deb http://download.mono-project.com/repo/ubuntu stable-trusty main" | sudo tee /etc/apt/sources.list.d/mono-official-stable.list
RUN sudo apt-get -y update && sudo apt-get -y install mono-devel mono-complete

# BOOST:
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
RUN wget http://sourceforge.net/projects/boost/files/boost/1.60.0/boost_1_60_0.tar.gz
RUN tar xvf boost_1_60_0.tar.gz
WORKDIR /home/malmo/boost/boost_1_60_0
RUN ./bootstrap.sh --prefix=.
RUN ./b2 link=static cxxflags=-fPIC install

# XSD:
RUN wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd_4.0.0-1_amd64.deb
RUN sudo dpkg -i --force-all xsd_4.0.0-1_amd64.deb
RUN sudo apt-get -y install -f

# LUABIND:
RUN git clone https://github.com/rpavlik/luabind.git /home/malmo/rpavlik-luabind
WORKDIR /home/malmo/rpavlik-luabind
RUN mkdir build
WORKDIR /home/malmo/rpavlik-luabind/build
RUN cmake -DCMAKE_BUILD_TYPE=Release ..
RUN make

RUN sudo luarocks install luasocket
RUN sudo pip install future
RUN sudo pip install pillow

COPY ./build.sh /home/malmo
RUN sudo apt-get update && sudo apt-get install -y dos2unix
RUN sudo dos2unix /home/malmo/build.sh
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas
ENTRYPOINT ["/home/malmo/build.sh", "-boost", "1_60_0"]
