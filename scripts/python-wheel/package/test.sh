#!/bin/bash

trap 'trap - SIGTERM && kill 0' SIGINT SIGTERM

set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

python -m malmo.minecraft launch --timeout 360 &
MC_PID=$!
until [ "$(bash -c 'exec 3<> /dev/tcp/localhost/10000;echo $?' 2>/dev/null)" = "0" ] || ! kill -0 "$MC_PID"; do
    sleep 5; jobs
done
python "${SCRIPT_DIR}/tests/run_mission.py" -v

kill $(jobs -p)
