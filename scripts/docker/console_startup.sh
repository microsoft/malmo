#!/bin/bash
echo "start headless"
/dockerstartup/vnc_startup.sh echo "launching Malmo"
(cd /home/malmo/MalmoPlatform/Minecraft; python3 launch_minecraft_in_background.py)
echo "starting jupyter"
jupyter notebook --ip 0.0.0.0 --no-browser
