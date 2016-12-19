#!/bin/bash

# Install malmo dependencies:
sudo apt-get -y install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev lua5.1 liblua5.1-0-dev openjdk-8-jdk swig xsdcxx libxerces-c-dev doxygen xsltproc ffmpeg python-tk python-imaging-tk
# Set JAVA_HOME:
sudo echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> ~/.bashrc

# Update certificates (http://stackoverflow.com/a/29313285/126823)
sudo update-ca-certificates -f

# Install Torch:
git clone https://github.com/torch/distro.git ~/torch --recursive
cd ~/torch; bash install-deps;
./install.sh -b

# Install Mono:
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
echo "deb http://download.mono-project.com/repo/debian wheezy main" | sudo tee /etc/apt/sources.list.d/mono-xamarin.list
sudo apt-get -y update
echo "deb http://download.mono-project.com/repo/debian wheezy-apache24-compat main" | sudo tee -a /etc/apt/sources.list.d/mono-xamarin.list
sudo apt-get -y install mono-devel
sudo apt-get -y install mono-complete

# Install Luabind:
git clone https://github.com/rpavlik/luabind.git ~/rpavlik-luabind
cd ~/rpavlik-luabind
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make

# Install lua dependencies:
sudo apt-get -y install luarocks
sudo luarocks install luasocket

# Install ALE:
git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE
sudo apt-get -y install libsdl1.2-dev
cd ~/ALE
git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .
cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .
make
sudo echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/" >> ~/.bashrc

source ~/.bashrc

# Build Malmo:
git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform
wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas
sudo echo "export MALMO_XSD_PATH=~/MalmoPlatform/Schemas" >> ~/.bashrc
source ~/.bashrc
cd ~/MalmoPlatform
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make package
