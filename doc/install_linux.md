## Installing dependencies for Linux ##

For a minimal installation of running a python agent, follow step 1. Then see the Getting Started section below.

### 1. Install dependencies available from the standard repositories: ###

On Ubuntu 15.10: 

`sudo apt-get install libboost-all-dev libpython2.7 openjdk-8-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev ffmpeg python-tk python-imaging-tk`  

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

On Ubuntu 14.04 or Debian 8:

`sudo apt-get install libboost-all-dev libpython2.7 openjdk-7-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev libav-tools python-tk python-imaging-tk`  

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

On Debian 7:

`sudo apt-get install python2.7 openjdk-7-jdk lua5.1 libxerces-c3.1 liblua5.1-0-dev ffmpeg python-tk python-imaging-tk`  

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

On Fedora 23:

`su -c 'dnf install http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm'` (for ffmpeg)  

`sudo dnf install boost python java-1.8.0-openjdk xerces-c ffmpeg mono compat-lua lua-socket-compat tkinter python-pillow-tk`

### 2. Optional: Install Torch: ###

If you don't want to use Malmo from Torch then you can skip this step.

Follow the instructions at http://torch.ch/docs/getting-started.html

Check that typing `th` works.

### 3. Optional: Install Mono: (if not on Fedora) ###

If you don't want to use Malmo from C# then you can skip this step.

Follow the instructions here: http://www.mono-project.com/docs/getting-started/install/linux/

### 4. Optional: Install ALE: ###

If you want to use the Atari Learning Environment as an alternative back-end to Minecraft, you need a build that includes "_withALE"
and will need to install ALE as described here. If you don't want to use ALE then use a build that doesn't have "_withALE".

1. `git clone https://github.com/mgbellemare/Arcade-Learning-Environment.git ~/ALE`
2. If you want a GUI, you need to install SDL:
`sudo apt-get install libsdl1.2-dev`
3. Make sure you have cmake installed:
`sudo apt-get install cmake`

Then:

`cd ~/ALE`

`git checkout ed3431185a527c81e73f2d71c6c2a9eaec6c3f12 .`

`cmake -DUSE_SDL=ON -DUSE_RLGLUE=OFF -DBUILD_EXAMPLES=ON -DCMAKE_BUILD_TYPE=RELEASE .`

`make`

(If you don't want a GUI, use `-DUSE_SDL=OFF`, or leave it unspecified - it's off by default.)

4. You will need to put ~/ALE on your LD_LIBRARY_PATH so that Malmo can find libAle.so:
Add `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/ALE/` to your ~/.bashrc
`source ~/.bashrc`

