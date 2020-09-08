# ------------------------------------------------------------------------------------------------
# Copyright (c) 2020 Microsoft Corporation
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
from collections.abc import Iterable
import socket
import subprocess
import sys
import time

 # This script captures the Minecraft output into file called out.txt
DEFAULT_SCRIPT = "./launchClient_quiet.sh"

def launch_minecraft(ports, launch_script=DEFAULT_SCRIPT, keep_alive=False):
    """
    Launch Minecraft instances in the background.
    Function will block until all instances are ready to receive commands.
    ports - List of ports you want the instances to listen on for commands
    launch_script - Script to launch Minecraft. Default is ./launchClient_quiet.sh
    keep_alive - Automatically restart Minecraft instances if they exit
    """
    ports_collection = ports
    if not isinstance(ports_collection, Iterable):
        ports_collection = [ports_collection]

    minecraft_instances = []
    for port in ports_collection:
        args = [
            sys.executable, __file__, 
            "--script", launch_script,
            "--port", str(port)
        ]
        if keep_alive:
            args.append("--keepalive")

        proc = subprocess.Popen(args,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )
        minecraft_instances.append(proc)

    await_instances([
        ("127.0.0.1", int(port))
        for port in ports_collection
    ])

    # Determine if we need to return a collection or a single item based on the type passed for 
    # ports initially
    if isinstance(ports, Iterable):
        return minecraft_instances
    return minecraft_instances[0]

def await_instances(end_points):
    """
    Wait until the specified enpoints are all actively listening for connections.
    end_points - List of addresses made up of tuples of the form (HOST, PORT)
    """
    print(f"Waiting for {len(end_points)} instances...")

    while True:
        try:
            for end_point in end_points:
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                    s.settimeout(10)
                    s.connect(end_point)
                    s.close()

            print("Finished waiting for instances")
            return

        except (ConnectionError, socket.timeout):
            # If we fail to connect, most likely the instance isn't running yet
            time.sleep(5)


###################################################################################################
# The remainder of this file contains code for when this script is invoked directly rather than
# imported into another script.
# This is used to directly set up and launch the Minecraft process
###################################################################################################

def _parse_args():
    # Import locally so that we're not paying the import cost when they're not used
    import argparse

    parser = argparse.ArgumentParser(description="Malmo Launcher")
    parser.add_argument("--script", type=str, default=DEFAULT_SCRIPT, help="Script to launch Minecraft")
    parser.add_argument("--port", type=int, nargs="+", help="Command ports for Minecraft instances", required=True)
    parser.add_argument("--keepalive", action="store_true", default=False, help="Relaunch the Minecraft instance if it exits")

    return parser.parse_args()

def _exec(*args):
    proc = subprocess.Popen(args)
    proc.communicate()
    return proc.returncode

def _launch_minecraft_direct(launch_script, port, keep_alive):
    # Import locally so that we're not paying the import cost when this script is imported as a module
    import os
    import pathlib
    import shutil
    import tempfile

    # Make a copy of Minecraft into a unique temp directory as it's not possible to run multiple
    # instances from a single Minecraft directory
    target_dir = tempfile.mkdtemp(prefix="malmo_") + "/malmo"
    source_dir = str(pathlib.Path(__file__).parent.absolute()) + "/../.."
    print(f"Cloning {source_dir} into {target_dir}...")
    shutil.copytree(source_dir, target_dir)

    # Launch Minecraft using the specified script
    print(f"Launching Minecraft using {launch_script} with command port {port}...")
    os.chdir(target_dir + "/Minecraft")

    spawn = True
    while spawn:
        rc = _exec(launch_script, str(port))
        spawn = keep_alive

    print(f"Exit code: {rc}")

if __name__ == '__main__':
    args = _parse_args()

    if len(args.port) == 1:
        # If only a single port is specified, launch Minecraft directly
        _launch_minecraft_direct(args.script, args.port[0], args.keepalive)
    else:
        # If multiple ports are specified, launch each Minecraft instance in a new child process
        instances = launch_minecraft(args.port, launch_script=args.script, keep_alive=args.keepalive)
        print("Waiting for all instances to exit...")
        for instance in instances:
            instance.communicate()