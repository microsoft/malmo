#!/bin/bash
rm -rf ~/build_logs
mkdir ~/build_logs

# Install malmo dependencies:
echo "Installing dependencies..."
sudo apt-get -y install build-essential \
                git \
                cmake \
                cmake-qt-gui \
                libboost-all-dev \
                libpython2.7-dev \
                lua5.1 \
                liblua5.1-0-dev \
                openjdk-8-jdk \
                swig \
                xsdcxx \
                libxerces-c-dev \
                doxygen \
                xsltproc \
                ffmpeg \
                python-tk \
                xinit \
                apt-file \
                python-imaging-tk &>~/build_logs/install_deps_malmo.log
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to install dependencies."
        exit $result
fi

# Set JAVA_HOME:
sudo echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> ~/.bashrc

# Update certificates (http://stackoverflow.com/a/29313285/126823)
echo "Updating certificates..."
sudo update-ca-certificates -f &>~/build_logs/certificates.log

# Install Torch:
echo "Installing torch..."
git clone https://github.com/torch/distro.git ~/torch --recursive &>~/build_logs/clone_torch.log
cd ~/torch
bash install-deps &>~/build_logs/install_deps_torch.log
./install.sh -b &>~/build_logs/install_torch.log
source ~/.bashrc
th -e "print 'Torch installed correctly'"
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to install Torch."
        exit 1
fi

# Install Mono:
echo "Installing mono..."
{
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
echo "deb http://download.mono-project.com/repo/debian wheezy main" | sudo tee /etc/apt/sources.list.d/mono-xamarin.list
sudo apt-get -y update
echo "deb http://download.mono-project.com/repo/debian wheezy-apache24-compat main" | sudo tee -a /etc/apt/sources.list.d/mono-xamarin.list
sudo apt-get -y install mono-devel
sudo apt-get -y install mono-complete
} &>~/build_logs/install_mono.log
mono -V &>~/build_logs/mono_version.log
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to install Mono."
        exit 1
fi

# Install Luabind:
echo "Building luabind..."
{
git clone https://github.com/rpavlik/luabind.git ~/rpavlik-luabind
cd ~/rpavlik-luabind
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
} &>~/build_logs/build_luabind.log
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to build LuaBind."
        exit $result
fi

# Install lua dependencies:
echo "Installing lua dependencies:"
sudo apt-get -y install luarocks &> ~/build_logs/install_deps_lua.log
sudo luarocks install luasocket &>> ~/build_logs/install_deps_lua.log

# Install ALE:
echo "Building ALE..."
{
git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE
sudo apt-get -y install libsdl1.2-dev
cd ~/ALE
git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .
cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .
make
} &>~/build_logs/build_ALE.log
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to build ALE."
        exit $result
fi
sudo echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/" >> ~/.bashrc
source ~/.bashrc

# Build Malmo:
echo "Building Malmo..."
{
git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform
wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas
sudo echo "export MALMO_XSD_PATH=~/MalmoPlatform/Schemas" >> ~/.bashrc
source ~/.bashrc
cd ~/MalmoPlatform
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make install
} &>~/build_logs/build_malmo.log
result=$?;
if [ $result -ne 0 ]; then
    echo "Error building Malmo."
    exit $result
fi

# Run the tests:
echo "Running integration tests..."
{
nohup xinit & disown
export DISPLAY=:0.0
ctest -VV
} &>~/build_logs/test_malmo.log
result=$?;
if [ $result -ne 0 ]; then
    echo "Malmo tests failed!! Please inspect /build_logs/test_malmo.log for details."
    exit $result
fi

# Build the package:
echo "Building Malmo package..."
make package &>~/build_logs/build_malmo_package.log
if [ $result -eq 0 ]; then
    echo "MALMO BUILT OK - HERE IS YOUR BINARY:"
    ls *.zip
fi

# Copy the binary?
sudo mkdir /mnt/drive
sudo mount -t cifs //malmobuildartifacts.file.core.windows.net/builds /mnt/drive -o vers=3.0,username=malmobuildartifacts,password=brRWGDPSvrV35273GDkJHt+Hhuxcx1GStH+oK1lWVvvtlNHxTyYnW0RI6oXZV+Gaq4R3wSgK+U0Q3lSiis2qVQ==,dir_mode=0777,file_mode=0777
cp *.zip /mnt/drive/builds

