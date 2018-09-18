# malmoenv #

An OpenAI gym like Environment for Malmo, directly implemented Python to Java Minecraft.

A python "env" is created to run an agent in a Malmo mission. Each env has a remote Minecraft 
associated to it (by IP and Port). For multi-agent missions, the first agent's (role 0)
Minecraft is used as a coordinator to allow all agents to rendezvous on mission (re)starts
(i.e. on env resets).

## Examples: ##

`cd Minecraft`

`echo "malmomod.version=0.36.0" > ./src/main/resources/version.properties`

A single agent example mission (run each command in different cmd prompt/shells):

`launchClient.bat -port 9000 -env`

`python3 run.py --mission missions/mobchase_single_agent.xml --port 9000 --episodes 1`

A two agent example mission (run each command in different cmd prompt/shells):

`launchClient.bat -port 9000 -env`

`launchClient.bat -port 9001 -env`

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --episodes 1 --role 0 --experimentUniqueId "test1"`

`python3 run.py --mission missions/mobchase_two_agents.xml --port 9000 --port2 9001 --episodes 1 --role 1  --experimentUniqueId "test1"`

## Multi-threaded example: ##

`python3 python runmultiagent.py --mission missions\`
