#!/bin/bash
pip3 install twine wheel
cp ../../build/install/Python_Examples/MalmoPython.so package/marlo
cd package
python3 setup.py bdist_wheel
rm package/marlo/MalmoPython.so
