# Pip installable package for Malmo #

Malmo can be built from source ([Instructions for building from source](https://github.com/Microsoft/malmo/tree/master/doc)) 
or easily installed from the pre-build version 
([Install from pre-built version](https://github.com/Microsoft/malmo/releases)). 
However, it is often even simpler and sufficient to install Malmo as a python native platform wheel for Windows, MacOS or Linux.

## Prerequisites ##

In order to `pip3 install malmo` there are some environment and OS dependency requirements that have to be met:

1.	Java 8 must be installed and the JAVA_HOME environment variable set up for Java8.
2.  [git](https://git-scm.com/) should be installed and on your shell search path.
3.	MALMO_XSD_PATH must be set to where the XSD schema files are located (more on this below).
4.	There are a few OS specific dependencies that must be pre-installed. 

    *	For [Ubuntu Linux](https://github.com/Microsoft/malmo/blob/master/doc/install_linux.md) these are follows:
        `python3-pip ffmpeg openjdk-8-jdk git`,
        We'll add more Linux flavours specifics here soon but meanwhile the docker build scripts are a good place to start.

    *	Windows - please use the [manual install](https://github.com/Microsoft/malmo/blob/master/doc/install_windows_manual.md) to install dependencies.
        (The dependencies are python3, ffmpeg, 7zip, Java8 and additionally git.)

    *	MacOSX  - please see [MacOSX](https://github.com/Microsoft/malmo/blob/master/doc/install_macosx.md).

If you are unsure of what to pre-install for your Linux flavour,
the Malmo docker build files might be a good place to start
([Docker build files](https://github.com/Microsoft/malmo/tree/master/scripts/docker)).

If you are not using the default/usual Python 3 version and are not finding a compatible wheel on pypi.org (the default) you could try installing from test.pypi.org:

```
pip3 install --index-url https://test.pypi.org/simple/ malmo
```

## Using the prebuilt Docker image ##

Rather than installing these dependencies manually it's simper to use docker to launch a docker container using our prebuilt docker image. 

Our docker image already has the Malmo Python package installed, as well as the source code and Minecraft 
(and the dependencies of course), along with a Jupyter server so can start coding right away!

The docker container will launch the Malmo Minecraft Mod and a Jupyter server on start up,
and is set up to allow remote access via the 
[VNC remote desktop display protocol](https://en.wikipedia.org/wiki/Virtual_Network_Computing) 
so that you are able to see the Minecraft game running inside the container.

```
docker run -it -p 5901:5901 -p 6901:6901 -p8888:8888 -e VNC_PW=vncpassword andkram/malmo
```

You can add a `-v drive:/somedir:/somedir` option to the above docker run command to mount a directory 
for sharing your coding project files. You should also select a different hard to guess password.

To access the container browse to `http://localhost:6901/?password=vncpassword` (or connect to port 5901 using a VNC client).

Once Minecraft is completely launched in the container (which can take some minutes the first time the container is run) 
you should see it in the VNC desktop in your open browser tab.

After launching Minecraft, a Jupiter server is also started up and a connection advise hint is written on the docker container's output.
Please follow the advice to cut & paste the url into another browser tab but substituting `localhost` for `0.0.0.0` or address URL part
(as we are bridging port 8888 to the docker container).

The output looks something like this:

```
Copy/paste this URL into your browser when you connect for the first time,
    to login with a token:
        http://0.0.0.0:8888/?token=1c6390221431ca75146946c52e253f063431b6488420bbac
```
(Here you should have used: `http://localhost:8888/?token=1c6390221431ca75146946c52e253f063431b6488420bbac`for Jupyter.)

To run a sample mission in Jupyter, create a python3 notebook and enter `from malmo.run_mission import run; run()` and execute the notebook.

## Install using pip locally ##

If you would rather install Malmo locally without docker you can do that 
(after satisfying the OS & environment variable requirements above) using:

```
pip3 install malmo
```

Create or change into a working directory where you would like Malmo to be installed (in a sub directory named MalmoPlatform) and do:

```
python3 -c 'import malmo.minecraftbootstrap; malmo.minecraftbootstrap.download()'
```

This command will create a new directory (called MalmoPlatform) containing the Malmo GitHub project in your (current) working directory. By default, the master branch is downloaded. You can specify a branch using a string keyword argument (named branch) to the download function - which can be necessary if the malmo package was not installed recently and the download is for some reason done again.

Please remember to set up the MALMO_XSD_PATH environment variable to point to the MalmoPlatform/Schemas directory. 
i.e. full path of working dir and MalmoPlatform/Schema.

Alternatively, you could set it in Python temporarily with a `malmo.minecraftbootstrap.set_malmo_xsd_path();` statement after the 
`import malmo.minecraftbootstrap;` module import in the python command below.

You can now launch Minecraft from your working directory:

```
python3 -c 'import malmo.minecraftbootstrap; malmo.minecraftbootstrap.launch_minecraft()'
```

This may take some time (minutes if it's the first run as it needs to build Minecraft Forge).

The malmo package includes a simple test mission which you can run as follows in another shell / terminal session:

```
python3 -c 'from malmo.run_mission import run; run()'
```

(In your sessond shell or terminal session, add a `malmo.minecraftbootstrap.set_malmo_xsd_path()` statement if you have not already set up the MALMO_XSD_PATH.)

You can also run the mission from Jupyter. Simply create a Python3 notebook and 
add `from malmo.run_mission import run; run()` to the notebook and execute it.

To start coding you could try `import malmo; help(malmo)`. 
The MalmoPython sub-module (`import malmo.MalmoPython`) is the native library used by all the Malmo Python examples.

## Running existing examples ## 

If you wish to run old Malmo examples against the pip "malmo" module without editing imports 
then you may want to copy in MalmoPython.py and malmoutil.py from the
[pip package backwards compatibility directory](https://github.com/Microsoft/malmo/tree/master/scripts/python-wheel/backwards-compatible-imports) or include them on your PYTHONPATH.

## Deinstall ##

To deinstall delete the (downloaded) MalmoPlatform directory and it's contents and do a `pip3 uninstall malmo`.

