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


from typing import Optional, List
import os
import argparse
import sys
import socket
import subprocess
import malmo.version
import platform
import shutil
import time

malmo_dir = os.path.dirname(__file__)
minecraft_dir = os.path.join(malmo_dir, "Minecraft")
malmo_version = malmo.version.version


class MinecraftError(RuntimeError):
    pass


def _port_has_listener(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = sock.connect_ex(("127.0.0.1", port))
    sock.close()
    return result == 0


def get_java_home() -> str:
    if platform.system() == "Darwin":
        # Look specifically for jdk-8u152 due to this issue:
        # https://github.com/microsoft/malmo/issues/907
        java_home_output = subprocess.run(
            ["/usr/libexec/java_home", "-V"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        ).stderr.decode()
        java_home_output_lines = java_home_output.splitlines()
        for line in java_home_output_lines[1:]:  # First line is a header.
            java_home = line[line.find("/") :]
            javac_path = os.path.join(java_home, "bin", "javac")
            if not os.path.exists(javac_path):
                continue

            version_output = subprocess.run(
                [javac_path, "-version"],
                stderr=subprocess.PIPE,
            ).stderr.decode()
            if "1.8.0_152" in version_output:
                return java_home

        # If we didn't find specific version, raise an error.
        raise MinecraftError(
            "On macOS, Java 1.8.0_152 is required to run Malmo. It can be downloaded "
            "and installed from here: "
            "https://mirrors.huaweicloud.com/java/jdk/8u152-b16/. See "
            "https://github.com/microsoft/malmo/issues/907 for details."
        )

    # Option 1: look in $JAVA_HOME.
    if os.environ.get("JAVA_HOME"):
        return os.environ["JAVA_HOME"]

    # Option 2: find javac and look up two directories.
    which_javac = shutil.which("javac")
    if which_javac is not None:
        return os.path.dirname(os.path.dirname(os.path.realpath(which_javac)))

    # Option 3: use java -XshowSettings:properties.
    java_properties_output = subprocess.run(
        ["java", "-XshowSettings:properties", "-version"],
        stderr=subprocess.PIPE,
    ).stderr.decode()
    for line in java_properties_output.splitlines():
        line = line.strip()
        if line.startswith("java.home = "):
            return line[len("java.home = ") :]

    raise MinecraftError("Unexpected error while finding JAVA_HOME. Is Java installed?")


def launch(num_instances=1, *, ports: Optional[List[int]] = None, timeout=60):
    """
    Launch one or more Minecraft instances which Malmo can connect to.
    """

    java_home = get_java_home()
    os.environ["JAVA_HOME"] = java_home
    os.chdir(minecraft_dir)

    # Download and patch ForgeGradle
    subprocess.run(["./gradlew", "dependencies"])
    forge_gradle_jar_path = subprocess.check_output(
        [
            "find",
            os.path.expanduser("~/.gradle/caches"),
            "-name",
            "ForgeGradle-2.2-SNAPSHOT.jar",
        ]
    ).splitlines()[0]
    os.chdir(os.path.join(minecraft_dir, "forgegradle"))
    subprocess.run(
        [
            "zip",
            forge_gradle_jar_path,
            "net/minecraftforge/gradle/common/Constants$1.class",
            "net/minecraftforge/gradle/common/Constants$2.class",
            "net/minecraftforge/gradle/common/Constants$SystemArch.class",
            "net/minecraftforge/gradle/common/Constants.class",
            "net/minecraftforge/gradle/tasks/DownloadAssetsTask$1.class",
            "net/minecraftforge/gradle/tasks/DownloadAssetsTask$Asset.class",
            "net/minecraftforge/gradle/tasks/DownloadAssetsTask$GetAssetTask.class",
            "net/minecraftforge/gradle/tasks/DownloadAssetsTask.class",
        ]
    )
    os.chdir(minecraft_dir)

    # Compile Minecraft with Malmo mod.
    subprocess.run(["./gradlew", "setupDecompWorkspace"])
    subprocess.run(["./gradlew", "build"])

    # Start Minecraft instances.
    processes: List[subprocess.Popen] = []
    for instance_index in range(num_instances):
        if ports is None:
            port = 10000
            while _port_has_listener(port):
                port += 1
        else:
            port = ports[instance_index]
        config_dir = os.path.join(minecraft_dir, "run", "config")
        os.makedirs(config_dir, exist_ok=True)
        with open(
            os.path.join(config_dir, "malmomodCLIENT.cfg"), "w"
        ) as client_config_file:
            client_config_file.write(
                f"""
# Configuration file
# Autogenerated from malmo.minecraft.launch()

malmoports {{
  I:portOverride={port}
}}
"""
            )
        process = subprocess.Popen(["./gradlew", "runClient"])
        timeout_remaining = timeout
        while not _port_has_listener(port):
            time.sleep(1)
            timeout_remaining -= 1
            if timeout_remaining == 0:
                raise MinecraftError(
                    f"Timeout after {timeout} seconds waiting for Minecraft to start."
                )
            if process.poll() is not None:
                raise MinecraftError("Minecraft unexpectedly crashed on launch.")
        processes.append(process)

    return processes


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(title="subcommands")

    launch_parser = subparsers.add_parser("launch", help="launch Minecraft instances")
    launch_parser.add_argument(
        "-n", "--num_instances", type=int, default=1, help="number of instances"
    )
    launch_parser.add_argument(
        "-p", "--port", type=int, nargs="+", help="port(s) to listen on"
    )
    launch_parser.add_argument(
        "--timeout", type=int, default=60, help="timeout in seconds"
    )
    launch_parser.set_defaults(command="launch")

    args = parser.parse_args()

    if args.command == "launch":
        processes = launch(
            num_instances=args.num_instances,
            ports=args.port,
        )
        sys.exit(max(process.wait() for process in processes))


if __name__ == "__main__":
    main()
