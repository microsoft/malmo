import os
import subprocess
import pathlib
from malmo.launch_minecraft_in_background import launch_minecraft_in_background

malmo_install_dir = "MalmoPlatform"
malmo_version = "0.34.0"

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

def download(buildMod=False):
    """Download Malmo from github and optionaly build the Minecraft Mod.
    Args:
        buildMod: don't build the Mod unless build arg is given as True.
    Returns:
        The path for the Malmo Minecraft mod.
    """
    gradlew = "./gradlew"
    if os.name == 'nt':
        gradlew = "gradlew.bat"

    subprocess.check_call(["git", "clone", "-b", "package", "https://github.com/Microsoft/malmo.git" , malmo_install_dir])

    os.chdir(malmo_install_dir + "/Minecraft") 
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

def launch_minecraft():
    """Launch Malmo Minecraft Mod in Minecraft directory. 
       Wait for about 3 minutes for Minecraft to start.  
    """
    if "MALMO_XSD_PATH" not in os.environ:
        print("Please set the MALMO_XSD_PATH environment variable.")
        return
    cwd = os.getcwd()
    try:
        os.chdir(malmo_install_dir + "/Minecraft")
        launch_minecraft_in_background(os.getcwd(), [], 360)
    finally:
        os.chdir(cwd)

def set_malmo_xsd_path():
    """Set the MAMLMO_XSD_PATH environment variable in current process."""
      
    os.environ["MALMO_XSD_PATH"] = str(pathlib.Path(malmo_install_dir + "/Schemas").absolute())
    print(os.environ["MALMO_XSD_PATH"])

