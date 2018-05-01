pip3 install twine wheel
copy ..\..\build\install\Python_Examples\MalmoPython.lib package\marlo
copy ..\..\build\install\Python_Examples\MalmoPython.pyd package\marlo
cd package
python setup.py bdist_wheel
del package\marlo\MalmoPython.lib package\marlo\MalmoPython.pyd
