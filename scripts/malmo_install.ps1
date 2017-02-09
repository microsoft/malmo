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
Display-Heading "Installing Minecraft"

cd ..
$MALMO_HOME = (Get-Item -Path ".\" -Verbose).FullName
Add-MalmoXSDPathEnv $MALMO_HOME

Write-Output "Malmo installed successfully"
Write-Output ""
Write-Output "To start a Malmo client, use:"
Write-Output ("cd " + $MALMO_HOME + "\Minecraft")
Write-Output ".\launchClient.bat"
Write-Output "To run a first Malmo example, open a separate PowerShell and run:"
Write-Output ("cd " + $MALMO_HOME + "\Python_Examples")
Write-Output ("python tabular_q_learning.py")
Write-Output ""
Write-Output ("For more details see the tutorial in " + $MALMO_HOME + "\Documentation\index.html")