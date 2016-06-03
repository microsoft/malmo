
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
    1. Save xs3p.xsl from https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl to the Schemas folder.  
       (License: https://github.com/bitfehler/xs3p )

12. Build Malmo:
    1. Open a Terminal window
    2. `cd MalmoPlatform`
    3. `mkdir build`
    4. `cd build`
    5. `cmake .. -DXSD_INCLUDE_DIR=/Users/admin/xsd/libxsd/` (give the location of the libxsd folder in your xsd installation)
    6. `make`
