from __future__ import print_function
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

# Used for integration tests.

from builtins import str
from builtins import range
import io
import os
import platform
import socket
import subprocess
import sys
import time


def _port_has_listener(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = sock.connect_ex(('127.0.0.1', port))
    sock.close()
    return result == 0


def launch_minecraft_in_background(minecraft_path, ports=None, timeout=360, replaceable=False, score=False):
    if ports is None:
        ports = []
    if len(ports) == 0:
        ports = [10000]  # Default
    processes = []
    for port in ports:
        if _port_has_listener(port):
            print('Something is listening on port', port, '- will assume Minecraft is running.')
            continue
        replaceable_arg = " -replaceable " if replaceable else ""
        scorepolicy_arg = " -scorepolicy " if score else ""
        scorepolicy_value = " 2 " if score else ""
        print('Nothing is listening on port', port, '- will attempt to launch Minecraft from a new terminal.')
        if os.name == 'nt':
            args = [minecraft_path + '/launchClient.bat', '-port', str(port), replaceable_arg.strip(),
                    scorepolicy_arg.strip(), scorepolicy_value.strip()]
            p = subprocess.Popen([arg for arg in args if arg != ""],
                                 creationflags=subprocess.CREATE_NEW_CONSOLE, close_fds=True)
        elif sys.platform == 'darwin':
            # Can't pass parameters to launchClient via Terminal.app, so create a small launch
            # script instead.
            # (Launching a process to run the terminal app to run a small launch script to run
            # the launchClient script to run Minecraft... is it possible that this is not the most
            # straightforward way to go about things?)
            launcher_file = "/tmp/launcher_" + str(os.getpid()) + ".sh"
            tmp_file = open(launcher_file, "w")
            tmp_file.write(minecraft_path + '/launchClient.sh -port ' + str(port) +
                           replaceable_arg + scorepolicy_arg + scorepolicy_value)
            tmp_file.close()
            os.chmod(launcher_file, 0o700)
            p = subprocess.Popen(['open', '-a', 'Terminal.app', launcher_file])
        else:
            p = subprocess.Popen(minecraft_path + "/launchClient.sh -port " + str(port) +
                                 replaceable_arg + scorepolicy_arg + scorepolicy_value,
                                 close_fds=True, shell=True,
                                 stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        processes.append(p)
        print('Giving Minecraft some time to launch... ')
        launched = False
        for _ in range(timeout // 3):
            print('.', end=' ')
            time.sleep(3)
            if _port_has_listener(port):
                print('ok')
                launched = True
                break
        if not launched:
            print('Minecraft not yet launched. Giving up.')
            exit(1)
    return processes


if __name__ == "__main__":
    minecraft_launch_path = os.path.dirname(os.path.abspath(__file__))
    launch_ports = [int(port_arg) for port_arg in sys.argv[1:] if port_arg != "--replaceable" and port_arg != "--score"]
    launch_minecraft_in_background(minecraft_launch_path, launch_ports, 300, 
                                   replaceable="--replaceable" in sys.argv,
                                   score="--score" in sys.argv)
