## Build on Windows: ##

1. Install CMake:
    1. Download and run e.g. `cmake-3.11.0-win32-x86.msi` from https://cmake.org/download/
    2. If you are new to CMake, see [some notes](cmake_readme.md) [(doc link)](@ref md_doc_cmake_readme).

2. Install FFMPEG: 
    1. Download [64-bit Static](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.zip) from [Zeranoe](http://ffmpeg.zeranoe.com/builds/).
    2. Unpack the contents of the zip (bin folder etc.) to `C:\ffmpeg`
    3. Add `C:\ffmpeg\bin` to your `PATH` ([How To](https://support.microsoft.com/en-us/kb/310519))
    4. Check that typing `ffmpeg` at a command prompt works.

3. Install git and Visual Studio and [Python 3.6 (64-bit)](https://www.python.org/ftp/python/3.6.0/python-3.6.0.amd64.exe) and the JDK (64-bit). Hints:
    1. Get the latest Windows git from https://git-scm.com/downloads
    2. Check that git and msbuild and python are on your path. (Hint: Run the Visual Studio command prompt)
       N.B. MSBuild lives in an odd place: e.g. `C:\Program Files (x86)\MSBuild\12.0\Bin`
    3. Set JAVA_HOME to be the location of the JDK installation, e.g. `C:\Program Files\Java\jdk1.8.0_71`
    4. Add e.g. `C:\Program Files\Java\jdk1.8.0_71\bin` to your PATH variable. ([How To](https://support.microsoft.com/en-us/kb/310519))
    5. Check that `java -version` and `javac -version` and `set JAVA_HOME` all report the same 64-bit version.
       The Minecraft version currently requires Java 8. 
    
4. Download and install Doxygen
    1. Download e.g. `doxygen-1.8.11-setup.exe` from http://www.stack.nl/~dimitri/doxygen/download.html
    2. Run the exe to install.

5. Download and install ZLib
    1. Download e.g. `zlib-1.2.11.zip` from http://zlib.net/
    2. Extract to `C:\zlib-1.2.11\`
    3. Open a Visual Studio 2017 x64 command prompt with Admin rights ([How-To](https://technet.microsoft.com/en-us/library/cc947813(v=ws.10).aspx))
    4. `cd C:\zlib-1.2.11\`
    5. `cmake -G "Visual Studio 15 2017 Win64" .`
    6. `cmake --build . --config Debug --target install`
    7. `cmake --build . --config Release --target install`
    8. Add `C:\Program Files\zlib\bin` to your PATH ([How To](https://support.microsoft.com/en-us/kb/310519))

6. Install and build Boost 1.66.0 or later:
    1. Download e.g. `boost_1_66.zip` from http://boost.org
    2. Extract to `c:\boost`
    3. Open a Visual Studio 2017 x64 command prompt with Admin rights ([How-To](https://technet.microsoft.com/en-us/library/cc947813(v=ws.10).aspx))
    4. e.g. `cd c:\boost\boost_1_66`
    *If you are using Python 3, (rather than deprecated Python 2):*
        a. Create a user-config.jam file in the root of your boost installation (eg c:\boost\boost_1_66_0\user-config.jam) - the easiest way to do this is copy the example one from boost's tools\build\example
        b. Find the section on Python, and uncomment the "using python" line, changing it to point to your python3 installation - eg `using python : 3.6 : C:/python36 : C:/python36/include : C:/python36/lib ;`
    5. `bootstrap.bat`
    6. `b2.exe toolset=msvc-12.0 address-model=64 -sZLIB_SOURCE="C:\zlib-1.2.11"`   
    7. For more information on installing Boost with ZLib support, see [here](http://www.boost.org/doc/libs/1_66_0/libs/iostreams/doc/installation.html)

7. Install SWIG
    1. Browse to http://swig.org/download.html and download the latest version of `swigwin`.
    2. Unzip the directory and copy it to your `C:\` drive.
    3. Add (e.g.) `C:\swigwin-3.0.12` to your PATH. CMake should then find swig automatically.
    
8. Install xsltproc:
    1. Visit ftp://ftp.zlatkovic.com/libxml/ and download libxslt, libxml2, zlib and iconv: _(NOTE: you can also get the binaries from http://xmlsoft.org/sources/win32/)_
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

9. Build Malmo:
    1. Open a Visual Studio 2017 x64 command prompt
    2. `mkdir MalmoPlatform` (wherever you want)
    3. `cd MalmoPlatform`
    4. `git clone https://github.com/Microsoft/malmo.git .`
    5. Save xs3p.xsl from https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl to the Schemas folder.
    6. Add a new environment variable `MALMO_XSD_PATH` and set it to the path to `MalmoPlatform\Schemas`. You will need to open a fresh command prompt for this to take effect.
    7. `mkdir build`
    8. `cd build`
    9. `cmake -G "Visual Studio 15 2017 Win64" ..`
    10. If it fails to find things, use `cmake-gui ..` and give hints, as described above.  
        If you have cygwin installed, check that cmake isn't using the cygwin python executables.
    11. For a Debug build: `msbuild INSTALL.vcxproj`
        For a Release build: `msbuild INSTALL.vcxproj /p:Configuration=Release`  
        You can then run the samples from e.g. `install\Python_Examples`  
        If you want to use Visual Studio to build, open `Malmo.sln`.
 
10. Test Malmo:
    1. After building Debug: `ctest -C Debug`
    2. After building Release: `ctest -C Release`
    3. Add `-E Integration` to exclude the integration tests.
    4. Add `-VV` to get verbose output.
    5. Or build the RUN_TESTS project in Visual Studio and look in the Output tab.

11. Make a distributable:
    1. Run all the tests.
    2. Change the version number in CMakeLists.txt and Minecraft/src/main/java/com/microsoft/Malmo/MalmoMod.java, and commit.
    3. `msbuild PACKAGE.vcxproj /p:Configuration=Release`
