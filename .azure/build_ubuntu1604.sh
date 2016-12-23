#!/bin/bash
while getopts 'shv' x; do
    case "$x" in
        h)
            echo "usage: $0
This script will install, build, test, and package Malmo.
    -s      Force static linking of Boost (will also build Boost)
    -v      Verbose output (very verbose!)
"
            exit 2
            ;;
        s)
            BUILD_BOOST=1
            ;;
        v)
            VERBOSE_MODE=1
            ;;
    esac
done

if [ $VERBOSE_MODE ]; then
    exec 4>&2 3>&1
else
    exec 4>/dev/null 3>/dev/null
fi

# Extra dependencies needed for running headless integration tests, and mounting azure blob storage.
EXTRA_DEPS="xinit apt-file"

# Determine the operating system:
KERNEL=`uname -s`
if [ "$KERNEL" == 'Darwin' ]; then
    # Do OSX installation stuff...
    exit 0
elif [ "$KERNEL" == 'Linux' ]; then
    # Determine Unix distribution and version...
    # First try to use /etc/os-release:
    if [ -f /etc/os-release ]; then
        SPECS=`( . /etc/os-release &>/dev/null; echo $ID $VERSION_ID )`
        DIST=${SPECS%% *}
        VERSION=${SPECS##* }
    fi
else
    echo "Not an OS we understand. This script currently supports Ubuntu 14-16, Debian 7-8, Fedora, OSX."
    exit 1
fi

# Strip minor version numbers:
VERSION=${VERSION%%.*}
echo "==============================================================================="
echo "Building Malmo for $DIST version $VERSION."
echo "==============================================================================="

#    DIST=`lsb_release -is`
#    VERSION=`lsb_release -rs`

LIB_PYTHON="libpython2.7-dev"

if [ "$DIST" == 'ubuntu' ]; then
    INSTALL_TORCH=1
    if [ "$VERSION" -ge 15 ]; then
        # Ubuntu 15 and 16:
        BOOST_VERSION_NUMBER=62
        AVLIB=ffmpeg
        JAVA_VERSION=8
    elif [ "$VERSION" -ge 14 ]; then
        # Ubuntu 14:
        BOOST_VERSION_NUMBER=60
        AVLIB=libav-tools
        JAVA_VERSION=7
        BUILD_XSD=1
    else
        echo "Ubuntu versions lower than 14 are not supported."
        exit 1
    fi
elif [ "$DIST" == 'debian' ]; then
    if [ "$VERSION" -ge 8 ]; then
        # Debian 8:
        BOOST_VERSION_NUMBER=60
        AVLIB=libav-tools
        JAVA_VERSION=7
    elif [ "$VERSION" -ge 7 ]; then
        # Debian 7:
        BOOST_VERSION_NUMBER=60
        AVLIB=ffmpeg
        JAVA_VERSION=7
        BUILD_XSD=1
        BUILD_BOOST=1
        LIB_PYTHON="python2.7-dev"
    else
        echo "Debian versions lower than 7 are not supported."
        exit 1
    fi
else
    echo "Linux ${DIST} version ${VERSION} isn't currently supported by this installation script."
    echo "But please consider adding the necessary code and making a pull request!"
    exit 1
fi

# If we are building boost ourselves, we don't need to install it, but we will need to install zlib.
if [ "$BUILD_BOOST" ]; then
    LIB_BZ="libbz2-dev"
else
    LIB_BOOST="libboost-all-dev"
fi

# If we are building xsd ourselves, we don't need to install it.
if [ -z "$BUILD_XSD" ]; then
    LIB_XSD="xsdcxx"
fi

rm -rf /home/$USER/build_logs
mkdir /home/$USER/build_logs

# Install malmo dependencies:
echo "Installing dependencies..."
sudo apt-get update | tee /home/$USER/build_logs/install_deps_malmo.log >&3
sudo apt-get -y install build-essential \
                git \
                cmake \
                cmake-qt-gui \
                ${LIB_BZ} \
                ${LIB_BOOST} \
                ${LIB_PYTHON} \
                lua5.1 \
                liblua5.1-0-dev \
                openjdk-${JAVA_VERSION}-jdk \
                swig \
                ${LIB_XSD} \
                libxerces-c-dev \
                doxygen \
                xsltproc \
                ${AVLIB} \
                ${EXTRA_DEPS} \
                python-tk \
                python-imaging-tk | tee -a /home/$USER/build_logs/install_deps_malmo.log >&3
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to install dependencies."
        exit $result
fi

# Set JAVA_HOME:
export JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64/
sudo echo "export JAVA_HOME=/usr/lib/jvm/java-"${JAVA_VERSION}"-openjdk-amd64/" >> /home/$USER/.bashrc

# Update certificates (http://stackoverflow.com/a/29313285/126823)
echo "Updating certificates..."
sudo update-ca-certificates -f | tee /home/$USER/build_logs/certificates.log >&3

# Install Torch:
if [ $INSTALL_TORCH ]; then
    echo "Installing torch..."
    git clone https://github.com/torch/distro.git /home/$USER/torch --recursive | tee /home/$USER/build_logs/clone_torch.log >&3
    cd /home/$USER/torch
    bash install-deps | tee /home/$USER/build_logs/install_deps_torch.log >&3
    ./install.sh -b | tee /home/$USER/build_logs/install_torch.log >&3
    source /home/$USER/torch/install/bin/torch-activate
    th -e "print 'Torch installed correctly'"
    result=$?;
    if [ $result -ne 0 ]; then
            echo "Failed to install Torch."
            exit 1
    fi
fi

# Install Mono:
echo "Installing mono..."
{
if [ "$DIST" == 'debian' ] || [ "$DIST" == 'ubuntu' ]; then
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
    echo "deb http://download.mono-project.com/repo/debian wheezy main" | sudo tee /etc/apt/sources.list.d/mono-xamarin.list
    sudo apt-get -y update
    if [ "$VERSION" -ne 7 ]; then
        # Ubuntu >=13.10 and Debian >=8 also need this:
        echo "deb http://download.mono-project.com/repo/debian wheezy-apache24-compat main" | sudo tee -a /etc/apt/sources.list.d/mono-xamarin.list
    fi
    if [ "$VERSION" -ge 8 ] && [ "&DIST" == 'debian' ]; then
        # Debian >=8 also needs this:
        echo "deb http://download.mono-project.com/repo/debian wheezy-libjpeg62-compat main" | sudo tee -a /etc/apt/sources.list.d/mono-xamarin.list
    fi
    sudo apt-get -y install mono-devel
    sudo apt-get -y install mono-complete
elif [ "$DIST" == 'fedora' ]; then
    sudo rpm --import "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"
    sudo dnf config-manager --add-repo http://download.mono-project.com/repo/centos/
fi
} | tee /home/$USER/build_logs/install_mono.log >&3
mono -V | tee /home/$USER/build_logs/mono_version.log >&3
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to install Mono."
        exit 1
fi

# Build Boost:
if [ "$BUILD_BOOST" ]; then
    echo "Building boost..."
    {
    mkdir /home/$USER/boost
    cd /home/$USER/boost
    wget http://sourceforge.net/projects/boost/files/boost/1.${BOOST_VERSION_NUMBER}.0/boost_1_${BOOST_VERSION_NUMBER_0}.tar.gz
    tar xvf boost_1_${BOOST_VERSION_NUMBER}_0.tar.gz
    cd boost_1_${BOOST_VERSION_NUMBER}_0
    ./bootstrap.sh --prefix=.
    ./b2 link=static cxxflags=-fPIC install
    } | tee /home/$USER/build_logs/build_boost.log >&3
    result=$?;
    if [ $result -ne 0 ]; then
        echo "Failed to build boost version "${BOOST_VERSION_NUMBER}
        exit $result
    fi
    BOOST_PATH_FOR_CMAKE="-DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_${BOOST_VERSION_NUMBER}_0/include"
    BOOST_CMAKE_FLAGS="-DSTATIC_BOOST=ON"
fi

# Install Code-Synthesis XSD:
if [ $BUILD_XSD ]; then
    echo "Installing Code-Synthesis XSD:"
    {
    wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd_4.0.0-1_amd64.deb
    sudo dpkg -i --force-all xsd_4.0.0-1_amd64.deb
    sudo apt-get -y install -f
    } | tee /home/$USER/build_logs/install_codesynthesis.log >&3
    result=$?;
    if [ $result -ne 0 ]; then
        echo "Failed to install CodeSynthesis."
        exit $result
    fi
fi

# Install Luabind:
echo "Building luabind..."
{
git clone https://github.com/rpavlik/luabind.git /home/$USER/rpavlik-luabind
cd /home/$USER/rpavlik-luabind
mkdir build
cd build
cmake $BOOST_PATH_FOR_CMAKE -DCMAKE_BUILD_TYPE=Release ..
make
} | tee /home/$USER/build_logs/build_luabind.log >&3
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to build LuaBind."
        exit $result
fi

# Install lua dependencies:
echo "Installing lua dependencies:"
sudo apt-get -y install luarocks | tee /home/$USER/build_logs/install_deps_lua.log >&3
sudo luarocks install luasocket | tee -a /home/$USER/build_logs/install_deps_lua.log >&3

# Install ALE:
echo "Building ALE..."
{
git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git /home/$USER/ALE
sudo apt-get -y install libsdl1.2-dev
cd /home/$USER/ALE
git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .
cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .
make
} | tee /home/$USER/build_logs/build_ALE.log >&3
result=$?;
if [ $result -ne 0 ]; then
        echo "Failed to build ALE."
        exit $result
fi
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/$USER/ALE/
sudo echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/" >> /home/$USER/.bashrc

# Build Malmo:
echo "Building Malmo..."
{
git clone -b azureBuildFarm https://github.com/Microsoft/malmo.git /home/$USER/MalmoPlatform
wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P /home/$USER/MalmoPlatform/Schemas
export MALMO_XSD_PATH=/home/$USER/MalmoPlatform/Schemas
sudo echo "export MALMO_XSD_PATH=~/MalmoPlatform/Schemas" >> /home/$USER/.bashrc
cd /home/$USER/MalmoPlatform
mkdir build
cd build
cmake $BOOST_CMAKE_FLAGS $BOOST_PATH_FOR_CMAKE -DCMAKE_BUILD_TYPE=Release ..
make install
} | tee /home/$USER/build_logs/build_malmo.log >&3
result=$?;
if [ $result -ne 0 ]; then
    echo "Error building Malmo."
    exit $result
fi

# Run the tests:
echo "Running integration tests..."
{
nohup sudo xinit & disown
export DISPLAY=:0.0
ctest -VV
} | tee /home/$USER/build_logs/test_malmo.log >&3
result=$?;
if [ $result -ne 0 ]; then
    echo "Malmo tests failed!! Please inspect /home/$USER/build_logs/test_malmo.log for details."
    exit $result
fi

# Build the package:
echo "Building Malmo package..."
make package | tee /home/$USER/build_logs/build_malmo_package.log >&3
result=$?;
if [ $result -eq 0 ]; then
    echo "MALMO BUILT OK - HERE IS YOUR BINARY:"
    ls *.zip
fi

# Copy the binary?
sudo mkdir /mnt/drive
sudo mount -t cifs //malmobuildartifacts.file.core.windows.net/builds /mnt/drive -o vers=3.0,username=malmobuildartifacts,password=brRWGDPSvrV35273GDkJHt+Hhuxcx1GStH+oK1lWVvvtlNHxTyYnW0RI6oXZV+Gaq4R3wSgK+U0Q3lSiis2qVQ==,dir_mode=0777,file_mode=0777
cp *.zip /mnt/drive/
