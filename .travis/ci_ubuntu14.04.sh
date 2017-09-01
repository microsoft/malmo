#!/bin/bash
set -e
set -o pipefail

# This script is used for our continuous integration tests and installs from scratch on Ubuntu 14.04. 
# You should not normally run this script.

# add repository for Mono:

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
echo "deb http://download.mono-project.com/repo/debian wheezy main" | sudo tee /etc/apt/sources.list.d/mono-xamarin.list
sudo apt-get -qq update

# install dependencies:
sudo apt-get -y -q install libboost-all-dev libpython2.7-dev lua5.1 liblua5.1-0-dev swig libxerces-c-dev doxygen xsltproc libav-tools mono-devel

sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk
sudo update-alternatives --config java
sudo update-alternatives --config javac
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
sudo update-ca-certificates -f > /dev/null

# install Torch:

git clone https://github.com/torch/distro.git ~/torch --recursive
cd ~/torch
bash install-deps
./install.sh -b
~/torch/install/bin/torch-activate

# install CodeSynthesis XSD:

cd ~
wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd_4.0.0-1_amd64.deb
sudo dpkg -i --force-all xsd_4.0.0-1_amd64.deb
sudo apt-get -y -f install

# build Luabind:

cd ~
git clone https://github.com/rpavlik/luabind.git ~/rpavlik-luabind
cd rpavlik-luabind
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make

# build Malmo:

# git clone <MalmoURL> $TRAVIS_BUILD_DIR  (our CI environment does this for us)
wget https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl -P $TRAVIS_BUILD_DIR/Schemas
export MALMO_XSD_PATH=$TRAVIS_BUILD_DIR/Schemas
cd $TRAVIS_BUILD_DIR
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
ctest -E Integration -VV
