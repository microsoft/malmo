function Get-Installed-Apps
{
    # Cribbed from here: http://stackoverflow.com/a/31714410
    $regpath = @(
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
        'HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    Get-ItemProperty $regpath | .{process{if($_.DisplayName -and $_.UninstallString) { $_ } }} | Select DisplayName, Publisher, InstallDate, DisplayVersion, UninstallString |Sort DisplayName
}
$installedapps = Get-Installed-Apps

function Add-to-Path
{
    # This function was cribbed from Peter Hahndorf's work here: https://peter.hahndorf.eu/blog/AddingToPathVariable.html
    # It avoids the common mistake of butchering the registry's path variable.
    Write-Host Adding $args[0] to PATH
    $regPath = "SYSTEM\CurrentControlSet\Control\Session Manager\Environment"
    $hklm = [Microsoft.Win32.Registry]::LocalMachine
    $regKey = $hklm.OpenSubKey($regPath, $FALSE)
    $oldpath = $regKey.GetValue("Path", "", [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
    $newLocation = $args[0]
    # Is location already in the path?
    $parts = $oldPath.split(";")
    If ($parts -contains $newLocation)
    {
        Write-Warning "Not added - already exists."
        Return
    }
    $newPath = $oldPath + ";" + $newLocation
    # Add to the current session:
    $env:path += ";$newLocation"
    # And save into registry:
    $regKey = $hklm.OpenSubKey($regPath, $TRUE)
    $regKey.SetValue("Path", $newPath, [Microsoft.Win32.RegistryValueKind]::ExpandString)
}

function Should-Install
{
    $result = $installedapps | where {$_.DisplayName -match $args[0]}
    return ($result -eq $null)
}

#Python 2.7

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
if (Should-Install "7-Zip")
{
    Display-Heading "Installing 7-Zip"
    Download-File "http://www.7-zip.org/a/7z1604-x64.exe" ".\temp\7z_install.exe"
    Start-Process ".\temp\7z_install.exe" /S -Wait
}

# Install ffmpeg:
if ($env:path -notmatch "ffmpeg")
{
    Display-Heading "Installing ffmpeg"
    Download-File "http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z" ".\temp\ffmpeg.7z"
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\ffmpeg.7z -oC:\ffmpeg
    cp -r C:\ffmpeg\ffmpeg-latest-win64-static\* C:\ffmpeg
    rm -r C:\ffmpeg\ffmpeg-latest-win64-static
    Add-to-Path "C:\ffmpeg\bin"
}

# Install git:
if (Should-Install "Git version")
{
    Display-Heading "Installing git"
    Download-File "https://github.com/git-for-windows/git/releases/download/v2.11.0.windows.1/Git-2.11.0-64-bit.exe" ".\temp\git_install.exe"
    .\temp\git_install.exe /SILENT
}

# Install JAVA:
if (Should-Install "Java SE Development")
{
    Display-Heading "Installing java"
    $source = "http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-windows-x64.exe"
    $destination = ".\temp\jdkinstaller.exe"
    $client = new-object System.Net.WebClient 
    # Need to do some cookie business to sign the oracle licence agreement:
    $cookie = "oraclelicense=accept-securebackup-cookie"
    $client.Headers.Add([System.Net.HttpRequestHeader]::Cookie, $cookie) 
    # Download Java:
    $client.downloadFile($source, $destination)
    # Silently install:
    .\temp\jdkinstaller.exe /s ADDLOCAL="ToolsFeature,SourceFeature,PublicjreFeature"
    # Add environment variables:
    [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk1.8.0_111", "Machine")
    [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk1.8.0_111", "Process")
    Add-to-Path "C:\Program Files\Java\jdk1.8.0_111\bin"
}

# Install CMake:
if (Should-Install "CMake")
{
    Display-Heading "Installing cmake"
    Download-File "https://cmake.org/files/v3.7/cmake-3.7.1-win64-x64.msi" ".\temp\cmake.msi"
    Start-Process "temp\cmake.msi" /qn -Wait
    Add-to-Path "C:\Program Files\CMake\bin"
}

# Install Python:
if (Should-Install "Python 2.7")
{
    Display-Heading "Installing python"
    Download-File "https://www.python.org/ftp/python/2.7.12/python-2.7.12.amd64.msi" ".\temp\python_install.msi"
    Start-Process "temp\python_install.msi" /qn -Wait
    Add-to-Path "C:\Python27"
}

# Add MSBuild to path:
Write-Host
Add-to-Path "C:\Program Files (x86)\MSBuild\12.0\Bin"

# Install Doxygen:
if ($env:path -notmatch "doxygen")
{
    Display-Heading "Installing doxygen"
    Download-File "http://ftp.stack.nl/pub/users/dimitri/doxygen-1.8.13.windows.x64.bin.zip" ".\temp\doxygen.zip"
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\doxygen.zip -oC:\doxygen
    Add-to-Path "C:\doxygen"
}

# Install and build ZLib:
if ($env:path -notmatch "zlib")
{
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
}

# Install and build Boost:
if (-Not (Test-Path C:\boost))
{
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
}

# Install SWIG:
if ($env:path -notmatch "swigwin")
{
    Display-Heading "Installing swig"
    Download-File "http://prdownloads.sourceforge.net/swig/swigwin-3.0.11.zip" ".\temp\swig.zip"
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\swig.zip -oC:\
    Add-to-Path "C:\swigwin-3.0.11"
}

# Install XSD:
if (Should-Install "CodeSynthesis")
{
    Display-Heading "Installing codesynthesis"
    Download-File "http://www.codesynthesis.com/download/xsd/4.0/windows/i686/xsd-4.0.msi" ".\temp\xsd.msi"
    Start-Process "temp\xsd.msi" /qn -Wait
    Add-to-Path "C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin64;C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin"
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
        $dstfile = ".\temp\" + $zipfile
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
mkdir MalmoPlatform
cd MalmoPlatform
git clone https://github.com/Microsoft/malmo.git .
Download-File "https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl" "Schemas/xs3p.xsl"
[Environment]::SetEnvironmentVariable("MALMO_XSD_PATH", $homepath + "MalmoPlatform\Schemas", "Machine")
[Environment]::SetEnvironmentVariable("MALMO_XSD_PATH", $homepath + "MalmoPlatform\Schemas", "Process")
mkdir build
cd build
cmake -G "Visual Studio 12 2013 Win64" ..
msbuild INSTALL.vcxproj /p:Configuration=Release
Display-Heading "TESTING..."
ctest -C Release
