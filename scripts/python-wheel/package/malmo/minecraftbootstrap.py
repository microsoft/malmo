# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------
import os
import subprocess
import pathlib
from malmo.launch_minecraft_in_background import launch_minecraft_in_background

malmo_install_dir = "MalmoPlatform"

import malmo.version
malmo_version = malmo.version.version

""" 
# Example usage:
import malmo.minecraftbootstrap
# Download Malmo from github
malmo.minecraftbootstrap.download()
# Set the MALMO_XSD_PATH environment variable (you probably want to set that up for your shell). 
malmo.minecraftbootstrap.set_malmo_xsd_path()
# Launch Minecraft with Malmo Mod. The first launch will take several minuites to build the Mod.
malmo.minecraftbootstrap.launch_minecraft()
from malmo.run_mission import run
# Run a test mission 
run()
""" 

def download(branch=None, buildMod=False):
    """Download Malmo from github and optionaly build the Minecraft Mod.
    Args:
        branch: optional branch to clone. Default is release version.
        buildMod: don't build the Mod unless build arg is given as True.
    Returns:
        The path for the Malmo Minecraft mod.
    """
    gradlew = "./gradlew"
    if os.name == 'nt':
        gradlew = "gradlew.bat"

    if branch is None:
        branch = malmo_version

    subprocess.check_call(["git", "clone", "-b", branch, "https://github.com/Microsoft/malmo.git" , malmo_install_dir])

    os.chdir(malmo_install_dir)
    os.chdir("Minecraft")
    try:
        # Create the version properties file.
        pathlib.Path("src/main/resources/version.properties").write_text("malmomod.version={}\n".format(malmo_version))

        # Optionally do a test build.
        if buildMod:
            subprocess.check_call([gradlew, "setupDecompWorkspace", "build", "testClasses", "-x", "test", "--stacktrace", "-Pversion={}"
                .format(malmo_version)])

        minecraft_dir = os.getcwd()
    finally:
        os.chdir("../..")

    if "MALMO_XSD_PATH" not in os.environ:
        print("Please make sure you set the MALMO_XSD_PATH environment variable to \"{}/Schemas\"!"
                 .format(str(pathlib.Path(malmo_install_dir).absolute())))
    return minecraft_dir

def launch_minecraft(ports = [], wait_timeout = 360):
    """Launch Malmo Minecraft Mod in one or more clients from 
       the Minecraft directory on the (optionally) given ports.
       Args:
           ports: an optionsl list of ports to start minecraft clients on. 
           Defaults to a single Minecraft client on port 10000.
           wait_timeout: optional time in seconds to wait (defaults to 3 mins).
    """
    if "MALMO_XSD_PATH" not in os.environ:
        print("Please set the MALMO_XSD_PATH environment variable.")
        return
    cwd = os.getcwd()
    try:
        os.chdir(malmo_install_dir + "/Minecraft")
        launch_minecraft_in_background(os.getcwd(), ports, wait_timeout)
    finally:
        os.chdir(cwd)

def set_malmo_xsd_path():
    """Set the MAMLMO_XSD_PATH environment variable in current process."""
      
    os.environ["MALMO_XSD_PATH"] = str(pathlib.Path(malmo_install_dir + "/Schemas").absolute())
    print(os.environ["MALMO_XSD_PATH"])

