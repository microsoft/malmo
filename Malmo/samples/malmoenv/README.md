Malmoenv - An OpenAI gym Enviroment for Malmo (directly implemented Python to Minecraft Java).

cd Minecraft
echo "malmomod.version=0.36.0" > ./src/main/resources/version.properties

A single agent example mission (run each command in different cmd prompt/shells):

launchClient.bat -port 9000 -env
python run.py --mission missions/mobchase_single_agent.xml --port 9000 --rounds 1

A two agent example mission (run each command in different cmd prompt/shells):

launchClient.bat -port 9000 -env
launchClient.bat -port 9001 -env
python run.py --mission missions/mobchase_two_agents.xml --port 9000 --rounds 1 --role 0 --experimentUniqueId "test1"
python run.py --mission missions/mobchase_two_agents.xml --port 9000 --port2 9001 --rounds 1 --role 1  --experimentUniqueId "test1"
