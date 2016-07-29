## Build on Linux: ##

1. If installing in a virtual machine:
    1. Bear in mind that Torch doesn't support Debian at the moment. Ubuntu (latest) is recommended.
    2. Choose the 32- or 64-bit OS version that you want to target. If you don't know, use 64-bit.
    3. Provide at least 4GB of memory and 20GB of virtual disk space.

2. Install dependencies.
    1. On Ubuntu 15.10:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev lua5.1 liblua5.1-0-dev openjdk-8-jdk swig xsdcxx libxerces-c-dev doxygen xsltproc ffmpeg python-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/`  
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)
         
    2. On Ubuntu 14.04:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev lua5.1 liblua5.1-0-dev openjdk-7-jdk swig libxerces-c-dev doxygen xsltproc libav-tools python-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`  
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)
         
    3. On Debian 8:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev lua5.1 liblua5.1-0-dev openjdk-7-jdk swig xsdcxx libxerces-c-dev doxygen xsltproc libav-tools python-tk python-imaging-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`  
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)
         
    4. On Debian 7:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libbz2-dev python2.7-dev lua5.1 liblua5.1-0-dev openjdk-7-jdk swig libxerces-c-dev doxygen xsltproc ffmpeg python-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`  
         
         Also, remove Java 6 and make sure that `java -version` returns the right version (1.7).  
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

    5. On Fedora 23:  

        `su -c 'dnf install http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm'` (for ffmpeg)  

        `sudo dnf install git cmake cmake-gui boost-devel python-devel java-1.8.0-openjdk-devel swig xsd xerces-c-devel doxygen libxslt ffmpeg gcc-c++ mono-devel compat-lua compat-lua-devel lua-socket-compat tkinter python-pillow-tk`

4. Install Torch: (if supported by your platform)
    1. Follow the instructions at http://torch.ch/docs/getting-started.html
    2. Test: `th`

5. Install Mono: (if not Fedora)
    1. The Mono Project has an excellent [Getting Started](http://www.mono-project.com/docs/) guide, please read it.
    2. For the impatient, Linux details are [here](http://www.mono-project.com/docs/getting-started/install/linux/)
    
6. On Debian 7 only: Build Boost
    1. `mkdir ~/boost`
    2. `cd ~/boost`
    3. `wget http://sourceforge.net/projects/boost/files/boost/1.60.0/boost_1_60_0.tar.gz`
    4. `tar xvf boost_1_60_0.tar.gz`
    5. `cd boost_1_60_0`
    6. `./bootstrap.sh --prefix=.`
    7. `./b2 link=static cxxflags=-fPIC install`

7. On Debian 7 and Ubuntu 14.04 only: Install CodeSynthesis XSD:
    1. `wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd_4.0.0-1_amd64.deb`
    2. `sudo dpkg -i --force-all xsd_4.0.0-1_amd64.deb`  
    3. `sudo apt-get install -f`  
       This step is needed because we require xsd version 4.0.  
       (When mono-devel is updated, you will need to manually remove then reinstall xsd as above, because of the package conflicts.)
     
8. Install Luabind:
    1. `git clone https://github.com/rpavlik/luabind.git ~/rpavlik-luabind`
    2. `cd rpavlik-luabind`
    3. `mkdir build`
    4. `cd build`
    5. `cmake -DCMAKE_BUILD_TYPE=Release ..`  
       On Debian 7 only:  
       `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Release ..`
    6. `make`
    7. Test: `ctest`  
       (A few of the tests fail currently but this doesn't seem to affect us.)

9. Install ALE: (optional - skip this if you don't want to provide ALE support)
    1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
    2. If you want a GUI, you need to install SDL:  
       `sudo apt-get install libsdl1.2-dev` (`sudo dnf install SDL-devel zlib-devel` on Fedora)
    3. `cd ~/ALE`  
       `git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .`  
       `cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .`  
       `make`  
       (If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)
    4. You will need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so:  
       Add `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/` to your ~/.bashrc  
      `source ~/.bashrc`
       
10. Build Malmo:
    1. `git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform`
    2. `wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas`
    3. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` to your `~/.bashrc` and do `source ~/.bashrc`
    4. `cd ~/MalmoPlatform`
    5. `mkdir build`
    6. `cd build`
    7. For a Debug build:  
       `cmake -DCMAKE_BUILD_TYPE=Debug ..`  
       On Debian 7 only:  
       `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Debug ..`
    8. For a Release build:  
       `cmake -DCMAKE_BUILD_TYPE=Release ..`  
       On Debian 7 only:  
       `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Release ..`
    9. `make install`
    10. You can then run the samples from e.g. `install/Python_Examples`

10. Test Malmo:
    1. `ctest`
    2. `ctest -E Integration` to exclude the integration tests.
    3. `ctest -VV` to get verbose output.

11. Make a distributable:
    1. Run all the tests.
    2. Change the version number in CMakeLists.txt and Minecraft/src/main/java/com/microsoft/Malmo/MalmoMod.java, and commit.
    3. `cmake -DCMAKE_BUILD_TYPE=Release ..`
    4. `make package`

### For interactive debugging in Linux: ###

1. Don't make a build directory or run cmake yet.
2. Open KDevelop. `Project > Open/Import Project...`
3. Navigate to the git repo and open the root CMakeLists.txt file.
4. Make a build folder for KDevelop4.
5. Build and debug from within the KDevelop4 GUI.

