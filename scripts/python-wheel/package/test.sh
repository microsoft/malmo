#!/bin/bash

trap 'trap - SIGTERM && kill 0' SIGINT SIGTERM EXIT

set -x

if [[ "$(uname -s)" =~ Linux* ]]; then
    DISPLAY=:1
    Xvfb $DISPLAY -screen 0 1280x1024x24 -v &
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

python -m malmo.minecraft launch --timeout 360 &
until [ "$(bash -c 'exec 3<> /dev/tcp/localhost/10000;echo $?' 2>/dev/null)" = "0" ]; do sleep 1; done
python "${SCRIPT_DIR}/tests/run_mission.py" -v
