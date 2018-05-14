# Pip installable package for Malmo #

Malmo can be built from source ([Instructions for building from source](https://github.com/Microsoft/malmo/tree/master/doc)) 
or easily installed from the pre-build version 
([Install from pre-built version](https://github.com/Microsoft/malmo/releases)). 
However, it is often even simpler and sufficient to install Malmo as a python native platform wheel for Linux, Windows and MacOS.

## Prerequisites ##

In order to `pip3 install malmo` there some environment and dependency requirements that have to be met:

1.	Java 8 musst be installed and the JAVA_HOME environment variable must be set up for Java8.
2.	MALMO_XSD_PATH must be defined (see below)
3.	There are a few OS specific dependencies that must be pre-installed. 

    *	For Ubuntu Linux these are follows:
        `git, python3-pip, ffmpeg, openjdk-8-jdk`,
        We’ll add more Linux flavours here soon but meanwhile the Malmo/scripts/docker build scripts are a good place to start.

    *	Windows - please use the powershell scripts to install dependencies.
        (The dependencies are python3, ffmpeg, 7zip and Java8. You also need to have git installed.)

    *	MacOS  - please see [MacOs](ttps://github.com/Microsoft/malmo/blob/master/doc/install_macosx.md).
        You also need git if you want to download Malmo code and examples.

If you are unsure of what to pre-install for your Linux flavour, 
the Malmo docker build files might be a good place to start 
([Docker build files](https://github.com/Microsoft/malmo/tree/master/scripts/docker)).

## Using the prebuilt Docker image ##

Rather than installing these dependencies manually it’s simper to launch a docker container using our prebuilt docker image. 

Our docker image already has the Malmo Python package installed, as well as the source code and Minecraft, 
along with a Jupyter server so can start coding right away!

The docker container will launch the Malmo Minecraft Mod and a Jupyter server on start up, 
as well as allowing remote access via the 
[VNC remote desktop display protocol](https://en.wikipedia.org/wiki/Virtual_Network_Computing) 
so that you are able to see the Minecraft game running inside the container.

```
docker run -it -p 5901:5901 -p 6901:6901 -p8888:8888 -e VNC_PW=vncpassword andkram/malmo_headless_0_34_1
```

You can add a `-v drive:/somedir:/somedir` option to the above docker run command to mount a directory 
for sharing your coding project files. You should also select a different hard to guess password.

To access the container browse to `http://localhost:6901/?password=vncpassword` (or connect to port 5901 using a VNC client).

Once Minecraft is completely launched in the container (which can take some minutes the first time the container is run) 
you should see it in the VNC desktop in your open browser tab.

After launching Minecraft, a Jupiter server is also started and a connection advise hint is written on the docker container’s output.
Please follow the advice to cut & paste the url into a browser but substituting `localhost` for `0.0.0.0` or address URL part
(as we are bridging port 8888 to the docker container).

The advise looks something like this:

```
Copy/paste this URL into your browser when you connect for the first time,
    to login with a token:
        http://0.0.0.0:8888/?token=1c6390221431ca75146946c52e253f063431b6488420bbac
```
(Here you should have used: `http://localhost:8888/?token=1c6390221431ca75146946c52e253f063431b6488420bbac`for Jupyter.)

To run a sample mission, create a python3 notebook and enter `from malmo.run_mission import run; run()` and execute the notebook.

## Installing using pip locally ##

If you would rather install Malmo locally without docker you can do that 
(after satisfying the OS & environment variable requirements) using:

```
pip3 install malmo
```

Create or change into a working directory where you would like Malmo to be installed (in a sub directory named MalmoPlatform) and do:

```
python3 -c 'import malmo.minecraftbootstrap; malmo.minecraftbootstrap.download()'
```

This command will create a new directory (called MalmoPlatform) containing the Malmo GitHub project in your (current) working directory.

Setup the MALMO_XSD_PATH environment variable to point to the MalmoPlatform/Schemas directory. 
i.e. full path of working dir and MalmoPlaftorm/Schema.

Alternatively, you could set it in Python temporarily with a `malmo.minecraftbootstrap.set_xsd_path();` statement after the 
`import malmo.minecraftbootstrap;` module import in the python commands below.

You can now launch Minecraft from your working directory:

```
python3 -c 'import malmo.minecraftbootstrap; malmo.minecraftbootstrap.launch_minecraft()'
```

This may take some time (minutes if it’s the first run as it needs to build Minecraft Forge).

The malmo package includes a simple test mission which you can run as follows:

```
python3 -c 'from malmo.run_mission import run; run()'
```

(Again, add a `malmo.minecraftbootstrap.set_xsd_path()` statement if you have not set up the MALMO_XSD_PATH.)

You can also run the mission from Jupyter. Simply create a Python3 notebook and 
add `from malmo.run_mission import run; run()` to the notebook and execute it.

To de-install delete the MalmoPlatform directory (and it’s contents) and do a `pip3 uninstall malmo`.

