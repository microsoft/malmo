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

FROM fedora:26

# Need to install sudo first:
RUN dnf install -y sudo

RUN su -c 'dnf install -y http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm'

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --no-log-init --groups wheel malmo
RUN sudo bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'


# While we are still root, install the necessary dependencies for Malmo:
RUN sudo dnf install -y \
    git \
    make \
    python3-devel \
    java-1.8.0-openjdk-devel \
    swig \
    doxygen \
    libxslt \
    ffmpeg \
    ffmpeg-devel \
    gcc-c++ \
    bzip2-devel \
    python3-tkinter \
    python3-pillow-tk \
    wget \
    xpra \
	mesa-libGL \
    python3-pip \
    zlib-devel

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk/" >> /home/malmo/.bashrc

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

# BOOST:
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
RUN wget http://sourceforge.net/projects/boost/files/boost/1.66.0/boost_1_66_0.tar.gz
RUN tar xvf boost_1_66_0.tar.gz
RUN echo "using python : 3.6 : /usr/bin/python3 : /usr/include/python3.6m : /usr/lib ;" > /home/malmo/user-config.jam
WORKDIR /home/malmo/boost/boost_1_66_0
RUN ./bootstrap.sh --prefix=.
RUN ./b2 link=static cxxflags=-fPIC install

# CMAKE:
RUN mkdir /home/malmo/cmake
WORKDIR /home/malmo/cmake
RUN wget https://cmake.org/files/v3.11/cmake-3.11.0.tar.gz
RUN tar xvf cmake-3.11.0.tar.gz
WORKDIR /home/malmo/cmake/cmake-3.11.0
RUN ./bootstrap
RUN make -j4
RUN sudo make install

RUN sudo dnf update -y && sudo dnf -y install dos2unix
COPY ./build.sh /home/malmo
RUN sudo dos2unix /home/malmo/build.sh
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas
ENTRYPOINT ["/home/malmo/build.sh", "-boost", "1_66_0", "-python", "3.6"]
