# Malmö #

Project Malmö is a platform for Artificial Intelligence experimentation and research built on top of Minecraft. We aim to inspire a new generation of research into challenging new problems presented by this unique environment.

Malmo home: [https://github.com/Microsoft/malmo](https://github.com/Microsoft/malmo)

Documentation: [[Tutorial]](../Python_Examples/Tutorial.pdf) - [[API]](annotated.html) - [[XML format]](../Schemas/Mission.html)

----
    
## Getting Started ##

1. Install the dependencies for your OS: [Windows](@ref md_doc_install_windows), [Linux](@ref md_doc_install_linux), [MacOSX](@ref md_doc_install_macosx).

2. Launch Minecraft with our Mod installed. Instructions below.

3. Launch one of our sample agents, as Python, Lua, C#, C++ or Java. Instructions below.


----

### Launching Minecraft with our Mod: ###

Minecraft needs to create windows and render to them with OpenGL, so the machine you do this from must have a desktop environment.

Go to the folder where you unzipped the release, then:

`cd Minecraft`

`launchClient` (On Windows)  
`./launchClient.sh` (On Linux or MacOSX)

or, e.g. `launchClient -port 10001` to launch Minecraft on a specific port.

On Linux or MacOSX: `./launchClient.sh -port 10001`

*NB: If you run this from a terminal, the bottom line will say something like "Building 95%" - ignore this - don't wait for 100%! As long as a Minecraft game window has opened and is displaying the main menu, you are good to go.*

By default the Mod chooses port 10000 if available, and will search upwards for a free port if not, up to 11000.
The port chosen is shown in the Mod config page.

To change the port while the Mod is running, use the `portOverride` setting in the Mod config page.

The Mod and the agents use other ports internally, and will find free ones in the range 10000-11000 so if administering
a machine for network use these TCP ports should be open.

----

### Launch an agent: ###

#### Running a Python agent: ####

`cd Python_Examples`

`python run_mission.py`

On MacOSX we currently only support the system python, so please use `/usr/bin/python run_mission.py` if it is not the default. 

#### Running a Lua agent: ####

`cd Lua_Examples`

`lua run_mission.lua`

#### Running a Torch agent: ####

`cd Torch_Examples`

`th run_mission.lua`

#### Running a C++ agent: ####

`cd Cpp_Examples`

To run the pre-built sample:

`run_mission` (on Windows)  
`./run_mission` (on Linux or MacOSX)

To build the sample yourself:

`cmake .`  
`cmake --build .`  
`./run_mission` (on Linux or MacOSX)  
`Debug\run_mission.exe` (on Windows)

#### Running a C# agent: ####

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

#### Running a Java agent: ####

`cd Java_Examples`

`java -cp MalmoJavaJar.jar:JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Linux or MacOSX)

`java -cp MalmoJavaJar.jar;JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Windows)

#### Running an Atari agent: ####

eg:
`cd Python_Examples`

`python ALE_HAC.py`

----

## Problems: ##

We're building up a [Troubleshooting](https://github.com/Microsoft/malmo/wiki/Troubleshooting) page of the wiki for frequently encountered situations. If that doesn't work then please ask a question on our [chat page](https://gitter.im/Microsoft/malmo) or open a [new issue](https://github.com/Microsoft/malmo/issues/new).

----

## Next steps ##

1. Explore the samples. You'll find most of them in the Python_Examples folder.
2. Follow the Tutorial in [Python_Examples/Tutorial.pdf](../Python_Examples/Tutorial.pdf)
3. Explore the documentation. The '[Classes](annotated.html)' tab above explains the API and the mission format is documented in the [Schemas folder](../Schemas/Mission.html).
4. Report any problems. Please keep in touch at the web address above.
