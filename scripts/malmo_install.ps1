Import-Module .\pslib\malmo_lib.psm1

$homepath = [Environment]::GetEnvironmentVariable("HOMEPATH")
cd $homepath
if (-Not (Test-Path .\temp))
{
    mkdir temp
}

# Install 7-Zip:
if (Should-Install "7-Zip")
{
    Display-Heading "Installing 7-Zip"
    Download-File "http://www.7-zip.org/a/7z1604-x64.exe" ($homepath + "\temp\7z_install.exe")
    &.\temp\7z_install.exe /S | Out-Host
}

# Install ffmpeg:
if ($env:path -notmatch "ffmpeg")
{
    Display-Heading "Installing ffmpeg"
    Download-File "http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z" ($homepath + "\temp\ffmpeg.7z")
    & 'C:\Program Files\7-Zip\7z.exe' x .\temp\ffmpeg.7z -oC:\ffmpeg | Out-Host
    cp -r C:\ffmpeg\ffmpeg-latest-win64-static\* C:\ffmpeg
    rm -r C:\ffmpeg\ffmpeg-latest-win64-static
    Add-to-Path "C:\ffmpeg\bin"
}

# Install JAVA:
if (Should-Install "Java SE Development")
{
    Display-Heading "Installing java"
    $source = "http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-windows-x64.exe"
    $destination = ($homepath + "\temp\jdkinstaller.exe")
    $client = new-object System.Net.WebClient 
    # Need to do some cookie business to sign the oracle licence agreement:
    $cookie = "oraclelicense=accept-securebackup-cookie"
    $client.Headers.Add([System.Net.HttpRequestHeader]::Cookie, $cookie) 
    # Download Java:
    $client.downloadFile($source, $destination)
    # Silently install:
    .\temp\jdkinstaller.exe /s ADDLOCAL="ToolsFeature,SourceFeature,PublicjreFeature" | Out-Host
    # Add environment variables:
    [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk1.8.0_111", "Machine")
    [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk1.8.0_111", "Process")
    Add-to-Path "C:\Program Files\Java\jdk1.8.0_111\bin"
}

# Install Python:
if (Should-Install "Python 2.7")
{
    Display-Heading "Installing python"
    Download-File "https://www.python.org/ftp/python/2.7.12/python-2.7.12.amd64.msi" ($homepath + "\temp\python_install.msi")
    & .\temp\python_install.msi /qn | Out-Host
    Add-to-Path "C:\Python27"
}

# Install XSD:
if (Should-Install "CodeSynthesis")
{
    Display-Heading "Installing codesynthesis"
    Download-File "http://www.codesynthesis.com/download/xsd/4.0/windows/i686/xsd-4.0.msi" ($homepath + "\temp\xsd.msi")
    &.\temp\xsd.msi /qn | Out-Host
    Add-to-Path "C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin64;C:\Program Files (x86)\CodeSynthesis XSD 4.0\bin"
}

# Install VCRedist:
if (Should-Install 'Microsoft Visual C++ 2012 Redistributable (x64)')
{
    Display-Heading "Installing Visual C++ runtime"
    Download-File "http://download.microsoft.com/download/2/E/6/2E61CFA4-993B-4DD4-91DA-3737CD5CD6E3/vcredist_x64.exe" ($homepath + "\temp\vcredist_x64.exe")
    &.\temp\vcredist_x64.exe /quiet /norestart | Out-Host
}