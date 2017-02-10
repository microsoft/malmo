## Installing dependencies on Windows (automated) ##

We provide a PowerShell script for installing all required dependencies. Please note that this script is still experimental.

Prerequisites:

- Download the current pre-built release for Windows from https://github.com/Microsoft/malmo/releases (for example: `Malmo-0.20.0-Windows-64bit.zip`)
- Extract the contents to a directory (for example `$env:HOMEPATH\Malmo-0.20.0-Windows-64bit`)

Steps:

1. Open a Powershell and run `Set-ExecutionPolicy -Scope CurrentUser Unrestricted` ([Details](https://msdn.microsoft.com/en-us/powershell/reference/5.1/microsoft.powershell.security/set-executionpolicy#example-4-set-the-scope-for-an-execution-policy))
1. `cd $env:HOMEPATH\Malmo-0.20.0-Windows-64bit\scripts`
1. `.\malmo_install.ps1`

In case you run into problems, please follow the [manual installation](https://github.com/Microsoft/malmo/blob/build_ps_fixes/doc/install_windows_manual.md) instead. If you find a problem that has not yet been reported, please raise a [new issue](https://github.com/Microsoft/malmo/issues/new).

To test if everything is installed correctly, you can launch minecraft with the Malmo mod, and launch an example agent, as detailed [here](https://github.com/Microsoft/malmo#getting-started).
