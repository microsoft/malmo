# Malmo
Project Malmo is a platform for Artificial Intelligence experimentation and research built on top of Minecraft. We aim to inspire a new generation of research into challenging new problems presented by this unique environment.

---

# Build Instructions #

## Build on Windows: ##

1. Install CMake:
    1. Download and run e.g. `cmake-3.5.0-win32-x86.msi` from https://cmake.org/download/
    2. If you are new to CMake, see [some notes](doc/cmake_readme.md) [(doc link)](@ref md_doc_cmake_readme).

2. Install FFMPEG: 
    1. Download [64-bit Static](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z) from [Zeranoe](http://ffmpeg.zeranoe.com/builds/).
    2. Unpack the contents of the zip (bin folder etc.) to `C:\ffmpeg`
    3. Add `C:\ffmpeg\bin` to your `PATH` ([How To](https://support.microsoft.com/en-us/kb/310519))
    4. Check that typing `ffmpeg` at a command prompt works.

3. Install git and Visual Studio and Python 2.7 (64-bit) and the JDK (64-bit). Hints:
    1. Get the latest Windows git from https://git-scm.com/downloads
    2. Check that git and msbuild and python are on your path.  
       N.B. MSBuild lives in an odd place: e.g. `C:\Program Files (x86)\MSBuild\12.0\Bin`
    3. Set JAVA_HOME to be the location of the JDK installation, e.g. `C:\Program Files\Java\jdk1.8.0_71`
    4. Add e.g. `C:\Program Files\Java\jdk1.8.0_71\bin` to your PATH variable. ([How To](https://support.microsoft.com/en-us/kb/310519))
    5. Check that `java -version` and `javac -version` and `set JAVA_HOME` all report the same 64-bit version.
    
4. Download and install Doxygen
    1. Download e.g. `doxygen-1.8.11-setup.exe` from http://www.stack.nl/~dimitri/doxygen/download.html
    2. Run the exe to install.

5. Download and install ZLib
    1. Download e.g. `zlib-1.2.8.zip` from http://zlib.net/
    2. Extract to `C:\zlib-1.2.8\`
    3. Open a Visual Studio 2013 x64 command prompt with Admin rights ([How-To](https://technet.microsoft.com/en-us/library/cc947813(v=ws.10).aspx))
    4. `cd C:\zlib-1.2.8\`
    5. `cmake -G "Visual Studio 12 2013 Win64" .`
    6. `cmake --build . --config Debug --target install`
    7. `cmake --build . --config Release --target install`
    8. Add `C:\Program Files\zlib\bin` to your PATH ([How To](https://support.microsoft.com/en-us/kb/310519))

6. Install and build Boost 1.59.0 or later:
    1. Download e.g. `boost_1_59_0.zip` from http://boost.org
    2. Extract to `c:\boost`
    3. Open a Visual Studio 2013 x64 command prompt with Admin rights ([How-To](https://technet.microsoft.com/en-us/library/cc947813(v=ws.10).aspx))
    4. e.g. `cd c:\boost\boost_1_59_0`
    5. `bootstrap.bat`
    6. `b2.exe toolset=msvc-12.0 address-model=64 -sZLIB_SOURCE="C:\zlib-1.2.8"`   
    7. For more information on installing Boost with ZLib support, see [here](http://www.boost.org/doc/libs/1_59_0/libs/iostreams/doc/installation.html)

7. Install SWIG
    1. Browse to http://swig.org/download.html and download the latest version of `swigwin`.
    2. Unzip the directory and copy it to your `C:\` drive.
    3. Add (e.g.) `C:\swigwin-3.0.7` to your PATH. CMake should then find swig automatically.
    
8. Install CodeSynthesis XSD to its default location (`C:\Program Files (x86)\CodeSynthesis XSD 4.0\`):
    1. Download from: http://www.codesynthesis.com/products/xsd/download.xhtml
    2. Install to the default location. You don't need to set up the VS search paths.
    
9. Install xsltproc:
    1. Visit ftp://ftp.zlatkovic.com/libxml/ and download libxslt, libxml2, zlib and iconv:
        1. Download e.g. `libxslt-1.1.26.win32.zip` and extract to `C:\XSLT`
        2. Download e.g. `libxml2-2.7.8.win32.zip` and extract to `C:\XSLT`
        3. Download e.g. `zlib-1.2.5.win32.zip` and extract to `C:\XSLT`
        4. Download e.g. `iconv-1.9.2.win32.zip` and extract to `C:\XSLT`
    2. Add their `bin` folders to your PATH: ([How To](https://support.microsoft.com/en-us/kb/310519))
        1. Add `C:\XSLT\libxslt-1.1.26.win32\bin` to your PATH.
        2. Add `C:\XSLT\libxml2-2.7.8.win32\bin` to your PATH.
        3. Add `C:\XSLT\iconv-1.9.2.win32\bin` to your PATH.
    3. Copy `C:\XSLT\zlib-1.2.5\bin\zlib1.dll` to `C:\XSLT\libxslt-1.1.26.win32\bin`
    4. Check that running `xsltproc` from a new command prompt works, printing the options.

10. Install the SlimDX SDK:
    1. Visit https://slimdx.org/download.php and download the SDK.

11. Build Malmo:
    1. `mkdir MalmoPlatform` (wherever you want)
    2. `cd MalmoPlatform`
    3. `git clone <MALMO_URL> .`
    4. Save xs3p.xsl from https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl to the Schemas folder.
    5. `mkdir build`
    6. `cd build`
    7. `cmake -G "Visual Studio 12 2013 Win64" ..`
    8. If it fails to find things, use `cmake-gui ..` and give hints, as described above.  
       If you have cygwin installed, check that cmake isn't using the cygwin python and lua executables.
    9. For a Debug build: `msbuild Malmo.sln`  
       For a Release build: `msbuild Malmo.sln /p:Configuration=Release`  
       Or open `Malmo.sln` in Visual Studio.
 
12. Test Malmo:
    1. After building Debug: `ctest -C Debug`
    2. After building Release: `ctest -C Release`
    3. Or build the RUN_TESTS project in Visual Studio and look in the Output tab.
    4. Add `-VV` to get verbose output.
    5. For testing the scripts and samples: `msbuild INSTALL.vcxproj` This installs the executables in an 'install' folder from where you can run them.

13. Make a distributable:
    1. Run all the tests.
    2. Change the version number in CMakeLists.txt and Minecraft/src/main/java/com/microsoft/Malmo/MalmoMod.java, and commit.
    3. (If a core dev) Tag the git commit:
    
       `git tag -a v0.1.1 -m "Version 0.1.1"`
       
       `git push origin v0.1.1`

    4. `msbuild PACKAGE.vcxproj /p:Configuration=Release`


## Build on Linux: ##

1. If installing in a virtual machine:
    1. Bear in mind that Torch doesn't support Debian at the moment. Ubuntu (latest) is recommended.
    2. Choose the 32- or 64-bit OS version that you want to target. If you don't know, use 64-bit.
    3. Provide at least 4GB of memory and 20GB of virtual disk space.

2. Install dependencies.
    1. On Ubuntu 15.10:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev liblua5.1-0-dev openjdk-8-jdk swig xsdcxx libxerces-c-dev doxygen xsltproc ffmpeg`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/`  
         
    2. On Ubuntu 14.04:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev liblua5.1-0-dev openjdk-7-jdk swig libxerces-c-dev doxygen xsltproc libav-tools`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`  
         
    3. On Debian 8:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libboost-all-dev libpython2.7-dev liblua5.1-0-dev openjdk-7-jdk swig xsdcxx libxerces-c-dev doxygen xsltproc libav-tools`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`  
         
    4. On Debian 7:  
    
         `sudo apt-get install build-essential git cmake cmake-qt-gui libbz2-dev python2.7-dev liblua5.1-0-dev openjdk-7-jdk swig libxerces-c-dev doxygen xsltproc ffmpeg`  
         
         `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/`
         
         Also, remove Java 6 and make sure that `java -version` returns the right version (1.7).
         
    5. `sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

4. Install Torch: (if supported by your platform)
    1. Follow the instructions at http://torch.ch/docs/getting-started.html
    2. Test: `th`

5. Install Mono
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
     
8. Install Luabind:
    1. `git clone https://github.com/rpavlik/luabind.git ~/rpavlik-luabind`
    2. `cd rpavlik-luabind`
    3. `mkdir build`
    4. `cd build`
    5. `cmake -DCMAKE_BUILD_TYPE=Release ..`  
       On Debian 7 only: `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Release ..`
    6. `make`
    7. Test: `ctest`  
       (A few of the tests fail currently but this doesn't seem to affect us.)

9. Install ALE: (optional - skip this if you don't want to provide ALE support)
    1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
    2. If you want a GUI, you need to install SDL:
       `sudo apt-get install libsdl1.2-dev`
    3. `cd ~/ALE`  
       `cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .`  
       `make`  
       (If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)
    4. You will need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so:  
       Add `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/` to your ~/.bashrc  
      `source ~/.bashrc`
       
10. Build Malmo:
    1. `git clone <MALMO_URL> ~/MalmoPlatform`
    2. `wget https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl -P ~/MalmoPlatform/Schemas`
    3. `cd ~/MalmoPlatform`
    4. `mkdir build`
    5. `cd build`
    6. On Debian 7 and Ubuntu 14.04 only: edit ~/MalmoPlatform/cmake/FindXSD.cmake and add 'xsd' to `FIND_PROGRAM(XSD_EXECUTABLE NAMES` on line 31.
    7. For a Debug build: `cmake -DCMAKE_BUILD_TYPE=Debug ..`
       On Debian 7 only: `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Debug ..`
    8. For a Release build: `cmake -DCMAKE_BUILD_TYPE=Release ..`
       On Debian 7 only: `cmake -DBoost_INCLUDE_DIR=/home/$USER/boost/boost_1_60_0/include -DCMAKE_BUILD_TYPE=Release ..`
    9. `make`

10. Test Malmo:
    1. After building: `ctest`
    2. Or `ctest -VV` to get verbose output.
    3. For testing the scripts and samples: `make install` This installs the executables in an 'install' folder from where you can run them.

11. Make a distributable:
    1. Run all the tests.
    2. Change the version number in CMakeLists.txt and Minecraft/src/main/java/com/microsoft/Malmo/MalmoMod.java, and commit.
    3. (If a core dev) Tag the git commit:
    
       `git tag -a v0.1.1 -m "Version 0.1.1"`
       
       `git push origin v0.1.1`

    4. `cmake -DCMAKE_BUILD_TYPE=Release ..`
    5. `make package`

### For interactive debugging in Linux: ###

1. Don't make a build directory or run cmake yet.
2. Open KDevelop. `Project > Open/Import Project...`
3. Navigate to the git repo and open the root CMakeLists.txt file.
4. Make a build folder for KDevelop4.
5. Build and debug from within the KDevelop4 GUI.


## Build on MacOSX ##

These instructions were tested on MacOSX 10.11.1 (El Capitan).

1. Install git:
    1. Open a Terminal window: Finder > Applications > Utilities > Terminal
    2. Type `git` and follow the instructions.

2. Install CMake:
    1. Visit http://cmake.org and download e.g. `cmake-3.4.3-Darwin-x86_64.dmg`
    2. Open the file and drag CMake.app to the Applications folder.

3. Install JDK:
    1. Visit http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html and download e.g. `jdk-8u71-macosx-x64.dmg`
    2. Open the file follow the instructions.

4. Install Mono:
    1. Visit http://www.mono-project.com/docs/getting-started/install/mac/ and follow the instructions.

5. Install Doxygen:
    1. Visit http://www.stack.nl/~dimitri/doxygen/download.html and download e.g. `Doxygen-1.8.11.dmg`
    2. Open the file and drag Doxygen.app to the Applications folder.

6. Install FFMPEG:
    1. Open a Terminal window and type `brew install ffmpeg`

7. Install SWIG:
    1. Open a Terminal window and type `brew install swig`

8. Install boost-python:
    1. Open a Terminal window and type `brew install boost-python`

9. Install CodeSynthesis XSD:
    1. Visit http://www.codesynthesis.com/products/xsd/download.xhtml and download e.g. `xsd-4.0.0-i686-macosx.tar.bz2`
    2. Open the file and extract the contents to e.g. `/Users/admin/xsd`

10. Clone the Project Malmo code:
    1. Open a Terminal window and type `git clone <MALMO_URL> MalmoPlatform`  
       (or to whatever destination folder you prefer)

11. Install xs3p stylesheet:
    1. Save xs3p.xsl from https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl to the Schemas folder.  
       (License: https://github.com/bitfehler/xs3p )

12. Build Malmo:
    1. Open a Terminal window
    2. `cd MalmoPlatform`
    3. `mkdir build`
    4. `cd build`
    5. `cmake .. -DXSD_INCLUDE_DIR=/Users/admin/xsd/libxsd/` (give the location of the libxsd folder in your xsd installation)
    6. `make`
