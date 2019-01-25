## Installing dependencies on Windows (automated) ##

We provide a PowerShell script for installing required dependencies, except for Java 8. Please note that this script is still experimental. You can of course follow the same install steps as the script manually. NOTE: Minecraft requires Java 8 and Malmo requires a 64 bit version of Python 3.

Prerequisites:
- Install OpenJDK Java 8 ([OpenJDK](https://openjdk.java.net/)).
- Download the current pre-built release for Windows from https://github.com/Microsoft/malmo/releases (for example: `Malmo-0.35.6-Windows-64bit_Python3.6.zip`)
- Extract the contents to a directory (for example `$env:HOMEPATH\Malmo-0.35.6-Windows-64bit_Python3.6`). NOTE: the directory path should not contain spaces or non-ascii characters.

Steps:

1. Open a Powershell and run `Set-ExecutionPolicy -Scope CurrentUser Unrestricted` ([Details](https://msdn.microsoft.com/en-us/powershell/reference/5.1/microsoft.powershell.security/set-executionpolicy#example-4-set-the-scope-for-an-execution-policy))
2. `cd $env:HOMEPATH\Malmo-0.35.6-Windows-64bit_Python3.6\scripts`
3. `.\malmo_install.ps1`

Depending on your setup, you may be asked to confirm a switch to "Run as Administrator" (for checking and installing dependencies). In case you run into problems, please follow the [manual installation](https://github.com/Microsoft/malmo/blob/master/doc/install_windows_manual.md) instead. If you find a problem that has not yet been reported, please raise a [new issue](https://github.com/Microsoft/malmo/issues/new) (please check the gitter channel and existing issues to see whether the problem is known, before raising a new issue).

To test if everything is installed correctly, you can launch minecraft with the Malmo mod, and launch an example agent, as detailed [here](https://github.com/Microsoft/malmo#getting-started).
