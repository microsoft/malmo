#!/bin/bash
pip3 install twine wheel
cp ../../build/install/Python_Examples/MalmoPython.so package/malmo
cp ../../build/install/Python_Examples/malmoutils.py package/malmo
cp ../../build/install/Python_Examples/run_mission.py package/malmo
cd package
python3 setup.py bdist_wheel
rm package/malmo/MalmoPython.so
