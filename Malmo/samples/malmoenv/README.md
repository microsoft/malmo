cd Minecraft
echo "malmomod.version=0.36.0" > ./src/main/resources/version.properties

SingleAgent (in different cmd prompt/shells):
launchClient.bat -port 9000 -env
python run.py --mission agentSingle.xml --port 9000 --rounds 1

Double-agent (in different cmd prompt/shells):
launchClient.bat -port 9000 -env
launchClient.bat -port 9001 -env
python run.py --mission agentDouble.xml --port 9000 --rounds 1 --role 0 --experimentUniqueId "test1"
python run.py --mission agentDouble.xml --port 9000 --port2 9001 --rounds 1 --role 1  --experimentUniqueId "test1"
