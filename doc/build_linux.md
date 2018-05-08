## Build on Linux: ##

1. If installing in a virtual machine:
    1. Choose the 32- or 64-bit OS version that you want to target. If you don't know, use 64-bit.
    2. Provide at least 4GB of memory and 20GB of virtual disk space.

2. Install dependencies. The scripts/docker directory contains build files that contain commands to install all required dependencies for various Linux flavors. Please consult these if you are having problems installing.

    1. On Ubuntu 16.04:  
    
         `sudo apt-get install build-essential git libboost-all-dev libpython2.7-dev openjdk-8-jdk swig doxygen xsltproc ffmpeg python-tk python-imaging-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/`
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)
         
    3. On Debian 8:  
    
         `sudo apt-get install build-essential git libboost-all-dev libpython2.7-dev openjdk-8-jdk swig doxygen xsltproc libav-tools python-tk python-imaging-tk`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/`  
         
         `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)
         
    5. On Fedora 26:  

        `su -c 'dnf install http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm'` (for ffmpeg)  

        `sudo dnf install git boost-devel python-devel java-1.8.0-openjdk-devel swig doxygen libxslt ffmpeg gcc-c++ tkinter python-pillow-tk`
    
    6. On CentOS 7:

        `sudo rpm --import http://li.nux.ro/download/nux/RPM-GPG-KEY-nux.ro`  
        `sudo rpm -Uvh http://li.nux.ro/download/nux/dextop/el7/x86_64/nux-dextop-release-0-5.el7.nux.noarch.rpm` (for ffmpeg)
        
        `sudo yum install git boost-devel python-devel java-1.8.0-openjdk-devel swig doxygen libxslt ffmpeg ffmpeg-devel gcc-c++ bzip2-devel tkinter python-pillow-tk`

    7. On Arch Linux (may be out of date):

        `sudo pacman -S --needed git gcc jdk8-openjdk swig doxygen ffmpeg`

   
3. Install CMake (if newer version required for your platform which must be 3.8 or greater)
   1. `mkdir ~/cmake`
   2. `cd ~/cmake`
   3. `wget https://cmake.org/files/v3.11/cmake-3.11.0.tar.gz`
   4. `tar xvf cmake-3.11.0.tar.gz`
   5. `cd cmake-3.11.0`
   6. `./bootstrap`
   7. `make -j4`
   8. `sudo make install`
 
4. Build Boost
    1. `mkdir ~/boost`
    2. `cd ~/boost`
    3. `wget http://sourceforge.net/projects/boost/files/boost/1.66.0/boost_1_66_0.tar.gz`
    4. `tar xvf boost_1_66_0.tar.gz`
    5. `cd boost_1_66_0`
    6. `./bootstrap.sh --prefix=.`
    7. `./b2 link=static cxxflags=-fPIC install`

5. Install ALE: (optional - skip this if you don't want to provide ALE support)
    1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
    2. If you want a GUI, you need to install SDL:  
       `sudo apt-get install libsdl1.2-dev` (`sudo dnf install SDL-devel zlib-devel` on Fedora, `sudo pacman -S sdl zlib` on Arch Linux)
    3. `cd ~/ALE`  
       `git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .`  
       `cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .`  
       `make`
       (If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)
    4. You will need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so:  
       Add `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/` to your ~/.bashrc  
      `source ~/.bashrc`
       
    **Note:** To include ALE in your malmo build, add `-DINCLUDE_ALE=ON` to your cmake command in step 10. If you stored your ALE directory somewhere other than ~/ALE, also add `-DROOT_ALE_DIR=/path/to/your/ALE`
       
6. Install necessary Python dependencies:
    1. pip install future
    2. pip install pillow

7. Build Malmo:
    1. `git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform`
    2. `wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas`
    3. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` to your `~/.bashrc` and do `source ~/.bashrc`
    4. `cd ~/MalmoPlatform`
    5. `mkdir build`
    6. `cd build`
    7. For a Debug build:  
       `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_66_0/include -DCMAKE_BUILD_TYPE=Debug ..`
    8. For a Release build:  
       `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_66_0/include -DCMAKE_BUILD_TYPE=Release ..`  
    9. `make install`
    10. You can then run the samples from e.g. `install/Python_Examples`

8. Test Malmo:
    1. `ctest`
    2. `ctest -E Integration` to exclude the integration tests.
    3. `ctest -VV` to get verbose output.

9. Make a distributable:
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

