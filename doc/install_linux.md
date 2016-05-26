## Installing on Linux ##

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

