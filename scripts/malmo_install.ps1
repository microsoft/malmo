param (
    [switch]$headless = $false
)

Import-Module .\pslib\malmo_lib.psm1

# get the malmo install dir
$MALMO_HOME = (Get-Item -Path "..\" -Verbose).FullName

# Make a temp directory for all the downloaded installers:
cd $env:HOMEDRIVE$env:HOMEPATH
if (-Not (Test-Path .\temp))
{
    mkdir temp
}

# Force PowerShell to use TLS 1.2 which is a requirement for some download locations
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Attempt to get version information from the Malmo properties file.
# (This file was added in 0.34.0)
try {
    Write-Host "Looking for $MALMO_HOME\malmo.properties"
    $versions = Get-Content $MALMO_HOME\malmo.properties | Out-String | ConvertFrom-StringData
} catch {
    Write-Host $error -foreground Red
    Write-Host "An error occurred attempting to read $MALMO_HOME\malmo.properties - please check this file exists."
}

$error.clear()
try {
    # Install dependencies:
    Display-Heading "Checking dependencies"
    $InstallList = "Install-7Zip;
         Install-Ffmpeg;
         "
    if ($headless) {
         $InstallList += "Install-Mesa;"
    }
    if ($versions.python_version -ge 3) {
        $InstallList += "Install-Python3"
    }
    else {
        $InstallList += "Install-Python2"
    }
    Write-Host $InstallList

    if (-Not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator"))
    {
        Write-Host "Elevating to admin ..."
        $InstallScript = [ScriptBlock]::Create("cd $env:HOMEDRIVE$env:HOMEPATH; Import-Module $MALMO_HOME\scripts\pslib\malmo_lib.psm1;" + $InstallList + " Check-Error")
        $process = Start-Process -FilePath powershell.exe -ArgumentList $InstallScript -verb RunAs -WorkingDirectory $env:HOMEDRIVE$env:HOMEPATH -PassThru -Wait
        if ($process.ExitCode)
        {
            throw new-object System.ApplicationException "Error while installing dependencies"
        }
    }
    else
    {
        $InstallScript = [ScriptBlock]::Create($InstallList)
        Invoke-Command $InstallScript
    }

    # update pythonpath
    $newPyPath = $MALMO_HOME + "\Python_Examples"
    if (!$env:PYTHONPATH) {
        [Environment]::SetEnvironmentVariable("PYTHONPATH", $newPyPath, "User")
        [Environment]::SetEnvironmentVariable("PYTHONPATH", $newPyPath, "Process")
    } else {
        $parts = $env:PYTHONPATH.split(";")
        if (-Not ($parts -Contains $newPyPath))
        {
            $newPyPath = $env:PYTHONPATH + ";" + $newPyPath
            [Environment]::SetEnvironmentVariable("PYTHONPATH", $newPyPath, "User")
            [Environment]::SetEnvironmentVariable("PYTHONPATH", $newPyPath, "Process")
        }
    }

    # Now "install" Malmo
    Display-Heading ("Installing Malmo in " + $MALMO_HOME)
    cd $MALMO_HOME
    Add-MalmoXSDPathEnv $MALMO_HOME

} catch {
    Write-Host $error -foreground Red
    Write-Host "An error occurred, please check for error messages above. If the error persists, please raise an issue on https://github.com/Microsoft/malmo."
}

if (!$error) {

    Write-Host "Malmo installed successfully" -foreground Green

    Display-Heading "Run your first Malmo example"
    Write-Host "1. Launch the Malmo client" -foreground Yellow
    Write-Host ("   > cd " + $MALMO_HOME + "\Minecraft")
    Write-Host  "   > .\launchClient.bat"
    Write-Host "2. Start a second PowerShell and start an agent:" -foreground Yellow
    Write-Host ("   > cd " + $MALMO_HOME + "\Python_Examples")
    Write-Host ("   > python tabular_q_learning.py")

    Write-Host ""
    Write-Host "To enable Malmo for other projects or programming languages, check the examples (e.g., " + $MALMO_HOME + "\Python_Examples) and ensure that the required libraries are correctly included in your project (e.g., place MalmoPython.pyd in your project folder), or make the libraries globally available (e.g., copy MalmoPython.pyd to your python site-packages folder - typically C:\Python27\Lib\site-packages)"
    Write-Host ""

    Display-Heading "Further reading & getting help"
    Write-Host (" - Tutorial " + $MALMO_HOME + "\Python_Examples\Tutorial.pdf")
    Write-Host (" - More python examples: " + $MALMO_HOME + "\Python_Examples")
    Write-Host (" - Detailed documentation: " + $MALMO_HOME + "\Documentation\index.html.")
    Write-Host (" - Wiki (troubleshooting and more) : https://github.com/Microsoft/malmo/wiki.")
    Write-Host (" - Malmo gitter channel for community discussions: https://gitter.im/Microsoft/malmo.")
    Write-Host ""
}
