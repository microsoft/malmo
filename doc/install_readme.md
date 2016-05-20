Malmo is a platform for AI research.

# Installation #

You can use Malmo from several different languages. Currently supported: C++, C#, Java, Lua, Python, Torch. Depending on which of these you want to use you can skip some of the dependencies below.

----

## Installing on Windows: ##

For a minimal installation of running a python agent, follow steps 1, 2, 3, 4, 6 and 7. Then see the Getting Started section below.

If you just want to run the Minecraft Mod (maybe your agents run on a different machine) then you only need to follow step 6.

### 1. Install 7-Zip: ###

If you already have 7-Zip installed then you can skip this step.

Visit http://7-zip.org/ and click the link for "Download .exe 64-bit x86." (or the 32-bit one).

Run the downloaded file to install 7-Zip.

### 2. Install FFMPEG: ###

    1. Download [64-bit Static](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z) from [Zeranoe](http://ffmpeg.zeranoe.com/builds/).
    2. Unpack the contents of the zip (bin folder etc.) to `C:\ffmpeg`
    3. Add `C:\ffmpeg\bin` to your `PATH` ([How To](https://support.microsoft.com/en-us/kb/310519))
    4. Check that typing `ffmpeg` at a command prompt works.

### 3. Install CodeSynthesis: ###

Visit http://www.codesynthesis.com/products/xsd/download.xhtml and download `xsd-4.0.msi`

Run the downloaded file to install CodeSynthesis.

### 4. Install Python: ###

If you don't want to use Malmo from Python then you can skip this step.

Visit https://www.python.org/ and download the latest version of Python 2.7 32-bit. e.g. `python-2.7.11.msi`

Run the downloaded file to install Python.

Check that typing `python` works in a command prompt. You may need to add e.g. `C:\Python27` to your PATH.

### 5. Install Lua: ###

If you don't want to use Malmo from Lua then you can skip this step.

Visit https://github.com/rjpcomputing/luaforwindows/releases and download e.g. `LuaForWindows_v5.1.4-50.exe`

Run the downloaded file to install Lua.

Check that typing `lua` works in a command prompt.

### 6. Install the Java SE Development Kit (JDK): ###

Visit http://www.oracle.com/technetwork/java/javase/downloads/index.html and download the latest 64-bit version 
e.g. `jdk-8u77-windows-x64.exe`

Run the downloaded file to install the JDK. Make a note of the install location.

Add the bin folder (e.g. `C:\Program Files\Java\jdk1.8.0_77\bin` ) to your PATH ([How To](https://support.microsoft.com/en-us/kb/310519))

Set the JAVA_HOME environment variable to be the location of the JDK:

  * Open the Control Panel: e.g. in Windows 10: right-click on `This PC` in File Explorer and select `Properties`
  * Navigate to: Control Panel > System and Security > System
  * Select `Advanced system settings` on the left
  * Select `Environment variables...`
  * Under `User variables` select `New...`
  * Enter `JAVA_HOME` as the varible name
  * Enter e.g. `C:\Program Files\Java\jdk1.8.0_77` as the variable value. Replace this with the location of your  
    JDK installation.
  
Check that `java -version` and `javac -version` and `set JAVA_HOME` all report the same 64-bit version.
 
### 7. Install the Microsoft Visual Studio 2013 redistributable: ###

Visit: https://www.microsoft.com/en-us/download/details.aspx?id=40784

Download `vcredist_x64.exe` and run.

### 8. Install the dotNET runtime: ###

If you don't want to use Malmo from C# then you can skip this step.

Visit https://www.microsoft.com/net to download and install the latest dotNET framework.

### 9. Install SlimDX: ###

If you don't want to use the Human Action component then you can skip this step.

Visit https://slimdx.org/download.php and download the 64-bit .NET 4.0 download .msi file, and install it.

----

## Installing on Linux: ##

For a minimal installation of running a python agent, follow steps 1 and 3. Then see the Getting Started section below.

### 1. Install dependencies available from the standard repositories: ###

On Ubuntu 15.10: 

`sudo apt-get install libboost-all-dev libpython2.7 openjdk-8-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev ffmpeg`

On Ubuntu 14.04 or Debian 8:

`sudo apt-get install libboost-all-dev libpython2.7 openjdk-7-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev libav-tools`

On Debian 7:

`sudo apt-get install python2.7 openjdk-7-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev ffmpeg`

Then:

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

### 2. Install Torch: ###

If you don't want to use Malmo from Torch then you can skip this step.

Follow the instructions at http://torch.ch/docs/getting-started.html

Check that typing `th` works.

### 3. Install Mono: ###

If you don't want to use Malmo from C# then you can skip this step.

Follow the instructions here: http://www.mono-project.com/docs/getting-started/install/linux/

### 4. Install ALE: ###

If you want to use the Atari Learning Environment as an alternative back-end to Minecraft, you need a build that includes "_withALE"
and will need to install ALE as described here. If you don't want to use ALE then use a build that doesn't have "_withALE".

1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
2. If you want a GUI, you need to install SDL:
`sudo apt-get install libsdl1.2-dev`
3. Make sure you have cmake installed:
`sudo apt-get install cmake`

Then:

`cd ~/ALE`
`cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .`
`make`

(If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)

4. You will need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so:
Add `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/` to your ~/.bashrc
`source ~/.bashrc`

----

## Installing on MacOSX: ##

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

----
    
# Getting Started #

To run one of our sample agents, you first need to launch Minecraft with our Mod:

### Launching the Mod: ###

`cd Minecraft`

On Windows:

`launchClient`

or, e.g.

`launchClient -port 10001`

On Linux or MacOSX:

`./launchClient`

or, e.g.

`./launchClient -port 10001`

*NB: If you run this from a terminal, the bottom line will say something like "Building 95%" - ignore this - don't wait for 100%! As long as a Minecraft game window has opened and is displaying the main menu, you are good to go.*

By default the Mod chooses port 10000 if available, and will search upwards for a free port if not, up to 11000.
The port chosen is shown in the Mod config page.

To change the port while the Mod is running, use the `portOverride` setting in the Mod config page.

The Mod and the agents use other ports internally, and will find free ones in the range 10000-11000 so if administering
a machine for network use these TCP ports should be open.

With Minecraft running you can then (at a different command prompt) launch the agent:

### Running a Python agent: ###

`cd Python_Examples`

`python run_mission.py`

### Running a Lua agent: ###

`cd Lua_Examples`

`lua run_mission.lua`

### Running a Torch agent: ###

`cd Torch_Examples`

`th run_mission.lua`

### Running a C++ agent: ###

`cd Cpp_Examples`

To run the pre-built sample:

`run_mission` (on Windows)  
`./run_mission` (on Linux or MacOSX)

To build the sample yourself:

`cmake .`  
`cmake --build .`  
`./run_mission` (on Linux or MacOSX)  
`Debug\run_mission.exe` (on Windows)

### Running a C# agent: ###

To run the pre-built sample:

`cd CSharp_Examples`  
`CSharpExamples_RunMission.exe` (on Windows)  
`mono CSharpExamples_RunMission.exe` (on Linux or MacOSX)

To build the sample yourself, open CSharp_Examples/RunMission.csproj in Visual Studio or MonoDevelop.

Or from the command-line:

`cd CSharp_Examples`

Then, on Windows:  
`msbuild RunMission.csproj /p:Platform=x64`  
`bin\x64\Debug\CSharpExamples_RunMission.exe`

On Linux or MacOSX:  
`xbuild RunMission.csproj /p:Platform=x64`  
`mono bin/x64/Debug/CSharpExamples_RunMission.exe`

### Running a Java agent: ###

`cd Java_Examples`

`java -cp MalmoJavaJar.jar:JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Linux or MacOSX)

`java -cp MalmoJavaJar.jar;JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Windows)

### Running an Atari agent: ###

eg:
`cd Python_Examples`

`python ALE_HAC.py`
