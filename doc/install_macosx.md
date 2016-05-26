## Installing on MacOSX ##

1. Install JDK:
    1. Visit http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html and download e.g. `jdk-8u71-macosx-x64.dmg`
    2. Open the file follow the instructions.
    
2. Install Homebrew:
    1. Follow http://www.howtogeek.com/211541/homebrew-for-os-x-easily-installs-desktop-apps-and-terminal-utilities/
    
3. Install dependencies:
    1. `brew install boost --with-python`
    2. `brew install boost-python`
    3. `brew install ffmpeg`
    4. `brew install xerces-c`
    5. `brew install mono`

4. Install ALE: (Currently untested on Mac)
    1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
    2. If you want a GUI, you need to install SDL:
    `sudo apt-get install libsdl1.2-dev`
    3. Make sure you have cmake installed:
    `sudo apt-get install cmake`

        Then:

        `cd ~/ALE`
        `cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON .`
        `make`

        (If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)

    4. You may need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so

These instructions were tested on MacOSX 10.11 (El Capitan). 

