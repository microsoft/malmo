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

# Now install Malmo: