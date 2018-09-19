# MalmoEnv (Prototype) #

MalmoEnvis an OpenAI "gym" like Python Environment for Malmo/Minecraft, directly implemented Python to Java Minecraft.

A python "env" is created and used to run an agent in a Malmo mission. Each env has a remote Minecraft instance
associated to it (by IP and Port). For multi-agent missions, the first agent's (role 0) Minecraft is used as a 
coordinator to allow all agents to rendezvous on mission (re)starts (i.e. on env resets).

## Examples: ##

To prepare Minecraft (after cloning this repository):

`cd Minecraft`

`echo "malmomod.version=0.36.0" > ./src/main/resources/version.properties`

Running a single agent example mission (run each command in different cmd prompt/shells):

`./launchClient.sh -port 9000 -env` or `launchClient.bat -port 9000 -env`

(In another shell) `cd Malmo/samples/malmoenv`

`python3 run.py --mission missions/mobchase_single_agent.xml --port 9000 --episodes 1`

A two agent example mission (run each command in different cmd prompt/shells):

`./launchClient.sh -port 9000 -env`

`./launchClient.sh -port 9001 -env`

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --episodes 1 --role 0 --experimentUniqueId "test1"`

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --port2 9001 --episodes 1 --role 1  --experimentUniqueId "test1"`

## Multi-threaded example: ##

`python3 python runmultiagent.py --mission missions/mobchase_....xml`
