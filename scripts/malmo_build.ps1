function Add-to-Path
{
    Write-Host Adding $args[0] to PATH
    [Environment]::SetEnvironmentVariable("PATH", [Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + $args[0], "Machine")
}

function Download-File
{
    Write-Host Downloading $args[0] to $args[1] ...
    $client = new-object System.Net.WebClient 
    $client.downloadFile($args[0], $args[1])
    Write-Host Downloaded.
}

function Display-Heading
{
    Write-Host
    Write-Host ("="*($args[0].Length))
    Write-Host $args[0]
    Write-Host ("="*($args[0].Length))
    Write-Host
}

$homepath = [Environment]::GetEnvironmentVariable("HOMEPATH")
cd $homepath
mkdir temp

# Install 7-Zip:
Display-Heading "Installing 7-Zip"
Download-File "http://www.7-zip.org/a/7z1604-x64.exe" ".\temp\7z_install.exe"
Start-Process ".\temp\7z_install.exe" /S -Wait

# Install ffmpeg:
Display-Heading "Installing ffmpeg"
Download-File "http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z" ".\temp\ffmpeg.7z"
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\ffmpeg.7z -oC:\ffmpeg
cp -r C:\ffmpeg\ffmpeg-latest-win64-static\* C:\ffmpeg
rm -r C:\ffmpeg\ffmpeg-latest-win64-static
Add-to-Path "C:\ffmpeg\bin"

# Install git:
Display-Heading "Installing git"
Download-File "https://github.com/git-for-windows/git/releases/download/v2.11.0.windows.1/Git-2.11.0-64-bit.exe" ".\temp\git_install.exe"
.\temp\git_install.exe /SILENT

# Install JAVA:
Display-Heading "Installing java"
powershell {
    $source = "http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-windows-x64.exe"
    $destination = ".\temp\jdkinstaller.exe"
    $client = new-object System.Net.WebClient 
    # Need to do some cookie business to sign the oracle licence agreement:
    $cookie = "oraclelicense=accept-securebackup-cookie"
    $client.Headers.Add([System.Net.HttpRequestHeader]::Cookie, $cookie) 
    # Download Java:
    $client.downloadFile($source, $destination)
}
# Silently install:
.\temp\jdkinstaller.exe /s ADDLOCAL="ToolsFeature,SourceFeature,PublicjreFeature"
# Add environment variables:
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk1.8.0_111", "Machine")
Add-to-Path "C:\Program Files\Java\jdk1.8.0_111\bin"

# Install CMake:
Display-Heading "Installing cmake"
Download-File "https://cmake.org/files/v3.7/cmake-3.7.1-win64-x64.msi" ".\temp\cmake.msi"
Start-Process "temp\cmake.msi" /qn -Wait
Add-to-Path "C:\Program Files\CMake\bin"

# Install Python:
Display-Heading "Installing python"
Download-File "https://www.python.org/ftp/python/2.7.12/python-2.7.12.amd64.msi" ".\temp\python_install.msi"
Start-Process "temp\python_install.msi" /qn -Wait
Add-to-Path "C:\Python27"

# Add MSBuild to path:
Write-Host
Add-to-Path "C:\Program Files (x86)\MSBuild\12.0\Bin"

# Install Doxygen:
Display-Heading "Installing doxygen"
Download-File "http://ftp.stack.nl/pub/users/dimitri/doxygen-1.8.13.windows.x64.bin.zip" ".\temp\doxygen.zip"
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\doxygen.zip -oC:\doxygen
Add-to-Path "C:\doxygen"

# Install and build ZLib:
Display-Heading "Installing and building zlib"
Download-File "http://zlib.net/zlib1210.zip" ".\temp\zlib.zip"
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\zlib.zip -oC:\
powershell {
    cd C:\zlib-1.2.10\
    cmake -G "Visual Studio 12 2013 Win64" .
    cmake --build . --config Debug --target install
    cmake --build . --config Release --target install
}
Add-to-Path "C:\Program Files\zlib\bin"

# Install and build Boost:
Display-Heading "Downloading and building boost - be patient!"
# Download:
Download-File "https://sourceforge.net/projects/boost/files/boost/1.63.0/boost_1_63_0.7z" ".\temp\boost.7z"
# Unzip:
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\boost.7z -oC:\boost
powershell {
    # Build:
    cd c:\boost\boost_1_63_0
    .\bootstrap.bat
    .\b2.exe toolset=msvc-12.0 address-model=64 -sZLIB_SOURCE="C:\zlib-1.2.10"
}

# Install SWIG:
Display-Heading "Installing swig"
Download-File "http://prdownloads.sourceforge.net/swig/swigwin-3.0.11.zip" ".\temp\swig.zip"
& 'C:\Program Files\7-Zip\7z.exe' x .\temp\swig.zip -oC:\
Add-to-Path "C:\swigwin-3.0.11"

# Install XSD:
Display-Heading "Installing codesynthesis"
Download-File "http://www.codesynthesis.com/download/xsd/4.0/windows/i686/xsd-4.0.msi" ".\temp\xsd.msi"
Start-Process "temp\xsd.msi" /qn -Wait
Add-to-Path "C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin64;C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin"

# Install xsltproc:
Display-Heading "Installing xsltproc"
powershell {
    # Download and unzip:
    $client = new-object System.Net.WebClient
    $archive = "http://xmlsoft.org/sources/win32/"
    $files = "libxslt-1.1.26.win32", "libxml2-2.7.8.win32", "iconv-1.9.2.win32"
    foreach ($file in $files + "zlib-1.2.5.win32") {
        $dstfile = $file + ".zip"
        $zipfile = ".\temp\" + $dstfile
        $client.downloadFile($archive + $zipfile, $dstfile)
        & 'C:\Program Files\7-Zip\7z.exe' x $dstfile -oC:\XSLT
    }
    # Add to path:
    $add_to_path = ""
    foreach ($file in $files) {
        $add_to_path += ";C:\XSLT\" + $file + "\bin"
    }
    Add-to-Path $add_to_path
    cp C:\XSLT\zlib-1.2.5\bin\zlib1.dll C:\XSLT\libxslt-1.1.26.win32\bin
}

# Install and build Malmo:
Display-Heading "TIME TO BUILD MALMO!"
powershell {
    mkdir MalmoPlatform
    cd MalmoPlatform
    git clone https://github.com/Microsoft/malmo.git .
    Download-File "https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl" "Schemas/xs3p.xsl"
    [Environment]::SetEnvironmentVariable("MALMO_XSD_PATH", $homepath + "MalmoPlatform\Schemas", "Machine")
    mkdir build
    cd build
    cmake -G "Visual Studio 12 2013 Win64" ..
    msbuild INSTALL.vcxproj /p:Configuration=Release
    Display-Heading "TESTING..."
    ctest -C Release
}