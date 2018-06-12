## Custom Dockerfile allowing remote display of Malmo after build
FROM consol/ubuntu-xfce-vnc
ENV REFRESHED_AT 2018-03-18

## Install as root 
USER 0

# Change wallpaper
COPY xfce4-desktop.xml /headless/.config/xfce4/xfconf/xfce-perchannel-xml/

# 16.04 image doesn't contain sudo - install that first:
RUN apt-get update && apt-get install -y sudo

# Create a user called "malmo", give it sudo access and remove the requirement for a password:
RUN useradd --create-home --shell /bin/bash --no-log-init --groups sudo malmo
RUN sudo bash -c 'echo "malmo ALL=(ALL:ALL) NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# While we are still root, install the necessary dependencies for Malmo:
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    libpython3.5-dev \
    openjdk-8-jdk \
    swig \
    doxygen \
    xsltproc \
    ffmpeg \
    python3-tk \
    python3-pil.imagetk \
    wget \
    libbz2-dev \
    python3-pip \
    software-properties-common \
    libgl1-mesa-dri \
    zlib1g-dev

RUN sudo update-ca-certificates -f

# Note the trailing slash - essential!
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> /home/malmo/.bashrc

# Switch to the malmo user:
USER malmo
WORKDIR /home/malmo

# BOOST:
RUN mkdir /home/malmo/boost
WORKDIR /home/malmo/boost
RUN wget http://sourceforge.net/projects/boost/files/boost/1.66.0/boost_1_66_0.tar.gz
RUN tar xvf boost_1_66_0.tar.gz
WORKDIR /home/malmo/boost/boost_1_66_0
RUN echo "using python : 3.5 : /usr/bin/python3 : /usr/include/python3.5 : /usr/lib ;" > ~/user-config.jam
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

RUN sudo pip3 install setuptools
RUN sudo pip3 install future pillow matplotlib

RUN sudo apt-get update && sudo apt-get install -y dos2unix
COPY ./build.sh /home/malmo/build.sh
RUN sudo dos2unix /home/malmo/build.sh

# Set MALMO_XSD_PATH to download install location.
ENV MALMO_XSD_PATH=/home/malmo/MalmoPlatform/Schemas

# Build Malmo - no install; no testing and branch specified for now!!!
RUN /home/malmo/build.sh -boost 1_66_0 -python 3.5 -with_display -branch master -no_testing

WORKDIR /home/malmo/MalmoPlatform

# Install Jupyter:
RUN sudo pip3 install jupyter

COPY ./console_startup.sh /home/malmo/console_startup.sh
RUN sudo dos2unix /home/malmo/console_startup.sh
ENTRYPOINT ["/home/malmo/console_startup.sh"]
