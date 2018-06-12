pip3 install twine wheel
copy ..\..\build\install\Python_Examples\MalmoPython.lib package\malmo
copy ..\..\build\install\Python_Examples\MalmoPython.pyd package\malmo
copy ..\..\build\install\Python_Examples\malmoutils.py package\malmo
copy ..\..\build\install\Python_Examples\run_mission.py package\malmo
copy ..\..\Minecraft\launch_minecraft_in_background.py package\malmo

cd package
python setup.py bdist_wheel
del package\malmo\MalmoPython.lib package\malmo\MalmoPython.pyd
twine upload dist/*
