#!/bin/bash
/dockerstartup/vnc_startup.sh echo "Starting Malmo Minecraft Mod"
cd /home/malmo
# Launch mimecraft (which may take several minutes first time)
python3 -c "import malmo.minecraftbootstrap;malmo.minecraftbootstrap.launch_minecraft()"
echo "Starting jupyter"
jupyter notebook --ip 0.0.0.0 --no-browser
