Import-Module .\pslib\malmo_lib.psm1

# Make a temp directory for all the downloaded installers:
cd $env:HOMEPATH
if (-Not (Test-Path .\temp))
{
    mkdir temp
}

# Install dependencies:
Install-7Zip
Install-Ffmpeg
Install-Java
Install-Python
Install-XSD
Install-VCRedist
Install-Mesa

# Now install Malmo:
Display-Heading "Installing Visual C++ runtime"
Download-File "https://github.com/Microsoft/malmo/releases/download/0.19.0/Malmo-0.19.0-Windows-64bit.zip" ($env:HOMEPATH + "\temp\malmo.zip")
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\malmo.zip -o$env:HOMEPATH\MalmoPlatform | Out-Host
if ($?)
{
    Add-MalmoXSDPathEnv
    cd MalmoPlatform\Minecraft
    launchClient.bat
    cd MalmoPlatform\Python_Examples
}
else
{
    Write-Host "FAILED TO INSTALL MALMO"
    exit 1
}