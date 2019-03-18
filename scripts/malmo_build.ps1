Import-Module .\pslib\malmo_lib.psm1

cd $env:HOMEPATH
if (-Not (Test-Path .\temp))
{
    mkdir temp
}

Install-7Zip
Install-Ffmpeg

# Install git:
if (Should-Install "Git version")
{
    Display-Heading "Installing git"
    Download-File "https://github.com/git-for-windows/git/releases/download/v2.11.0.windows.1/Git-2.11.0-64-bit.exe" ($env:HOMEPATH + "\temp\git_install.exe")
    .\temp\git_install.exe /SILENT
    if (-Not $?)
    {
        Write-Host "FAILED TO INSTALL GIT"
        exit 1
    }
}

# Install CMake:
if (Should-Install "CMake")
{
    Display-Heading "Installing cmake"
    Download-File "https://cmake.org/files/v3.11/cmake-3.11.1-win64-x64.msi" ($env:HOMEPATH + "\temp\cmake.msi")
    Start-Process "temp\cmake.msi" -ArgumentList "/qn" -Wait
    if ($?)
    {
        Append-Path "C:\Program Files\CMake\bin"
    }
    else
    {
        Write-Host "FAILED TO INSTALL CMAKE"
        exit 1
    }
}

Install-Python3

# Add MSBuild to path:
#Append-Path "C:\Program Files (x86)\MSBuild\12.0\Bin"

# Install Doxygen:
if ($env:path -notmatch "doxygen")
{
    Display-Heading "Installing doxygen"
    Invoke-WebRequest "http://doxygen.nl/files/doxygen-1.8.15.windows.x64.bin.zip" -OutFile ($env:HOMEPATH + "\temp\doxygen.zip")
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\doxygen.zip -oC:\doxygen
    if ($?)
    {
        Append-Path "C:\doxygen"
    }
    else
    {
        Write-Host "FAILED TO INSTALL DOXYGEN"
        exit 1
    }
}

# Install and build ZLib:
if ($env:path -notmatch "zlib")
{
    Display-Heading "Installing and building zlib"
    Download-File "http://zlib.net/zlib1211.zip" ($env:HOMEPATH + "\temp\zlib.zip")
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\zlib.zip -oC:\
    if (-Not $?)
    {
        Write-Host "FAILED TO DOWNLOAD/UNZIP ZLIB"
        exit 1
    }
    cd C:\zlib-1.2.11\
    cmake -G "Visual Studio 15 2017 Win64" .
    if (-Not $?)
    {
        Write-Host "FAILED TO CMAKE ZLIB"
        exit 1
    }
    cmake --build . --config Debug --target install
    if (-Not $?)
    {
        Write-Host "FAILED TO BUILD DEBUG ZLIB"
        exit 1
    }
    cmake --build . --config Release --target install
    if (-Not $?)
    {
        Write-Host "FAILED TO BUILD RELEASE ZLIB"
        exit 1
    }
    Append-Path "C:\Program Files\zlib\bin"
    cd $env:HOMEPATH
}

# Install and build Boost:
if (-Not (Test-Path C:\boost))
{
    Display-Heading "Downloading and building boost - be patient!"
    # Download:
    Download-File "https://sourceforge.net/projects/boost/files/boost/1.66.0/boost_1_66_0.7z" ($env:HOMEPATH + "\temp\boost.7z")
    # Unzip:
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\boost.7z -oC:\boost
    if (-Not $?)
    {
        Write-Host "FAILED TO UNZIP BOOST"
        exit 1
    }
    # Build:
    cd c:\boost\boost_1_66_0
    if (-Not $?)
    {
        Write-Host "SOMETHING WENT WRONG INSTALLING BOOST"
        exit 1
    }
    .\bootstrap.bat
    if (-Not $?)
    {
        Write-Host "FAILED TO BOOTSTRAP BOOST"
        exit 1
    }
    .\b2.exe toolset=msvc-14.1 address-model=64 -sZLIB_SOURCE="C:\zlib-1.2.11"
    if (-Not $?)
    {
        Write-Host "FAILED TO BUILD BOOST"
        exit 1
    }
    cd $env:HOMEPATH
}

# Install SWIG:
if ($env:path -notmatch "swigwin")
{
    Display-Heading "Installing swig"
    Download-File "http://prdownloads.sourceforge.net/swig/swigwin-3.0.11.zip" ($env:HOMEPATH + "\temp\swig.zip")
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\swig.zip -oC:\
    if (-Not $?)
    {
        Write-Host "FAILED TO UNZIP SWIG"
        exit 1
    }
    Append-Path "C:\swigwin-3.0.11"
}

# Install xsltproc:
if ($env:path -notmatch "XSLT")
{
    Display-Heading "Installing xsltproc"
    # Download and unzip:
    $client = new-object System.Net.WebClient
    $archive = "http://xmlsoft.org/sources/win32/"
    $files = "libxslt-1.1.26.win32", "libxml2-2.7.8.win32", "iconv-1.9.2.win32"
    foreach ($file in $files + "zlib-1.2.5.win32") {
        $zipfile = $file + ".zip"
        $dstfile = "temp\" + $zipfile
        $client.downloadFile(($archive + $zipfile), ($env:HOMEPATH + "\" + $dstfile))
        if (-Not $?)
        {
            Write-Host "FAILED TO DOWNLOAD $zipfile"
            exit 1
        }
        & 'C:\Program Files\7-Zip\7z.exe' x $dstfile -oC:\XSLT
        if (-Not $?)
        {
            Write-Host "FAILED TO UNZIP $zipfile"
            exit 1
        }
    }
    # Add to path:
    $add_to_path = ""
    foreach ($file in $files) {
        $add_to_path += ";C:\XSLT\" + $file + "\bin"
    }
    Append-Path $add_to_path
    cp C:\XSLT\zlib-1.2.5\bin\zlib1.dll C:\XSLT\libxslt-1.1.26.win32\bin
}

Install-Mesa

# Install and build Malmo:
Display-Heading "TIME TO BUILD MALMO!"
if (-Not (Test-Path MalmoPlatform))
{
    mkdir MalmoPlatform
}
cd MalmoPlatform
git clone https://github.com/Microsoft/malmo.git .
Download-File "https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl" ($env:HOMEPATH + "\MalmoPlatform\Schemas\xs3p.xsl")
Add-MalmoXSDPathEnv (($env:HOMEPATH) + "\MalmoPlatform")
mkdir build
cd build
cmake -G "Visual Studio 15 2017 Win64" ..
msbuild INSTALL.vcxproj /p:Configuration=Release
Display-Heading "TESTING..."
ctest -C Release
