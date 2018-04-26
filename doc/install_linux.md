## Installing dependencies for Linux ##

For a minimal installation of running a python agent, follow step 1.

### 1. Install dependencies available from the standard repositories: ###

On Ubuntu 16.04:

`sudo apt-get install libboost-all-dev libpython3.5 openjdk-8-jdk ffmpeg python-tk python-imaging-tk`  

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

On Debian 8:

`sudo apt-get install libboost-all-dev libpython3.4 openjdk-8-jdk libav-tools python-tk python-imaging-tk`  

`sudo update-ca-certificates -f` (http://stackoverflow.com/a/29313285/126823)

On Fedora 26:

`su -c 'dnf install http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm'` (for ffmpeg)  

`sudo dnf install python java-1.8.0-openjdk ffmpeg tkinter python-pillow-tk`

On CentOS 7:

`sudo yum install -y java-1.8.0-openjdk-devel ffmpeg python34-tkinter`

### 2. Optional: Install ALE: ###

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

### 3. Set the MALMO_XSD_PATH to the location of the schemas: ###

1. If you have not already done so, unzip the Malmo zip to some location (e.g. your home folder).
2. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` (or your Schemas location) to your `~/.bashrc` and do `source ~/.bashrc`
3. When you update Malmo you will need to update the MALMO_XSD_PATH too.

### 4. Optional: Install required Python modules: ###

If you want to use the python samples, you will need:
       
1. pip install future
2. pip install pillow
