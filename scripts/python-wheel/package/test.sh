#!/bin/bash

trap 'trap - SIGTERM && kill 0' SIGINT SIGTERM

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

python -m malmo.minecraft launch --timeout 360 &
until [ "$(bash -c 'exec 3<> /dev/tcp/localhost/10000;echo $?' 2>/dev/null)" = "0" ]; do sleep 1; jobs; done
python "${SCRIPT_DIR}/tests/run_mission.py" -v

kill $(jobs -p)
