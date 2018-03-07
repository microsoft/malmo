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

FROM debian:wheezy

# Need to install sudo first:
RUN apt-get update && apt-get install -y sudo

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --no-log-init --groups sudo malmo
RUN sudo bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# While we are still root, install the necessary dependencies for Malmo:
RUN sudo apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    cmake \
    cmake-qt-gui \
    python3.2-dev \
    lua5.1 \
    liblua5.1-0-dev \
    swig \
    libxerces-c-dev \
    doxygen \
    xsltproc \
    ffmpeg \
    python3-tk \
    wget \
    luarocks \
    libbz2-dev \
    python3-pip \
    software-properties-common \
    xpra \
    libgl1-mesa-dri

# Need to old version of Java 7, because latest version has problems with "EC parameter error",
# and Java 8 hasn't been backported to Debian 7.
RUN apt-get update && apt-get install -y openjdk-7-jre-headless=7u95-2.6.4-1~deb7u1 \
                                         openjdk-7-jre=7u95-2.6.4-1~deb7u1 \
                                         openjdk-7-jdk=7u95-2.6.4-1~deb7u1
   
RUN sudo update-ca-certificates -f

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/" >> /home/malmo/.bashrc

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

# TORCH not supported on Debian, so nothing to do here.

# MONO:
RUN echo "Installing mono..."
RUN sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
RUN echo "deb http://download.mono-project.com/repo/debian stable-wheezy main" | sudo tee /etc/apt/sources.list.d/mono-official-stable.list
RUN sudo apt-get -y update && sudo apt-get -y install mono-devel mono-complete

# BOOST:
# Use 1.64.0 - from 1.65.0, the NumPy minimum requirements changed to be greater than the
# version provided by Wheezy.
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
RUN echo "using python : 3.2 : /usr/bin/python3 : /usr/include/python3.2 : /usr/lib ;" > /home/malmo/user-config.jam
#COPY ./boost_1_64_0.tar.gz /home/malmo/boost
RUN wget http://sourceforge.net/projects/boost/files/boost/1.64.0/boost_1_64_0.tar.gz

RUN tar xvf boost_1_64_0.tar.gz
WORKDIR /home/malmo/boost/boost_1_64_0
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
RUN cmake -DBoost_INCLUDE_DIR=/home/malmo/boost/boost_1_64_0/include -DCMAKE_BUILD_TYPE=Release ..
RUN make

# The pip which comes with Wheezy doesn't work - need to get it to upgrade itself.
RUN sudo pip-3.2 install --index-url=https://pypi.python.org/simple/ --upgrade pip
RUN sudo pip-3.2 install future pillow
# Need to install unzip on Wheezy for luarocks to work
RUN sudo apt-get update && sudo apt-get install unzip
# The version of luarocks on Wheezy is old and weak, and we have to jump through
# the following hoops to get it to work:
# First, install luasec - without this, luarocks won't work. Unfortunately, the way
# to install luasec is WITH luarocks.
RUN sudo luarocks install --only-server=http://rocks.moonscript.org luasec OPENSSL_LIBDIR=/usr/lib/x86_64-linux-gnu/
# Now we can install luasocket:
RUN sudo luarocks install luasocket

COPY ./build.sh /home/malmo
# Need dos2unix to deal with line endings, and the lsb-release package so that Malmo's
# modification to CMake's FindXSD code will work.
RUN sudo apt-get update && sudo apt-get install -y dos2unix lsb-release
RUN sudo dos2unix /home/malmo/build.sh
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas
ENTRYPOINT ["/home/malmo/build.sh", "-boost", "1_64_0", "-python", "3.2"]
