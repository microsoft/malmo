# MalmoEnv #

MalmoEnv is an OpenAI "gym" Python Environment for Malmo/Minecraft, directly implemented Python to Java Minecraft.

A Python "gym env" can be created and used to run an agent in a Malmo mission. Each such env has a remote Minecraft instance
associated to it (by DNS name or IP and Port). For multi-agent missions, the first agent's (role 0) Minecraft 
client instance is used as a coordinator to allow all agents to rendezvous on mission starts (i.e. on env resets).

As it's pure Python, you just need this one package, its direct dependencies and (Java) Minecraft! Example missions, including some from the 2018 MarLo competition can be found in the "missions" directory.

## Examples of use: ##

Install dependencies:

Java8 JDK ([AdoptOpenJDK](https://adoptopenjdk.net/)), python3, git

`pip3 install gym lxml numpy pillow`

To prepare Minecraft (after cloning this repository with 
`git clone https://github.com/Microsoft/malmo.git`):

`cd malmo/Minecraft`

`(echo -n "malmomod.version=" && cat ../VERSION) > ./src/main/resources/version.properties` 

Running a single agent example mission (run each command in different cmd prompt/shells - use launchClient.bat on Windows):

`./launchClient.sh -port 9000 -env` or (On Windows) `launchClient.bat -port 9000 -env`

(In another shell) `cd malmo/MalmoEnv` optionally run `python3 setup.py install`

`python3 run.py --mission missions/mobchase_single_agent.xml --port 9000 --episodes 10`

A two agent example mission (run each command in different cmd prompt/shells):

`./launchClient.sh -port 9000 -env`

`./launchClient.sh -port 9001 -env`

In the two agent case, running each agent in it's own shell, the run script (for agents other than the first) is given two ports 
- the first for the mission coordinator and a second (port2) for the other agent's Minecraft:

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --role 0 --experimentUniqueId "test1"`

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --port2 9001 --role 1  --experimentUniqueId "test1"`

## Running multi-agent examples using multiple Python threads: ##

`python3 runmultiagent.py --mission missions/mobchase_two_agents.xml`

## Installing with pip ##

MalmoEnv is available as a pip wheel.

If you install with `pip3 install malmoenv` then you can download the Minecraft mod 
(assuming you have git available from the command line) with: 

`python3 -c "import malmoenv.bootstrap; malmoenv.bootstrap.download()"`

The sample missions will be downloaded to ./MalmoPlatform/MalmoEnv/missions.

`python3 -c "import malmoenv.bootstrap; malmoenv.bootstrap.launch_minecraft(9000)"` can be used to start up the Malmo Minecraft Mod 
listening for MalmoEnv connections on port 9000 after downloading Malmo.

To test: `cd MalmoPlatform/MalmoEnv; python3 runmultiagent.py --mission missions/mobchase_single_agent.xml`
