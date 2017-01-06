function Get-Installed-Apps
{
    # Cribbed from here: http://stackoverflow.com/a/31714410
    $regpath = @(
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*'
        'HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*'
    )
    Get-ItemProperty $regpath | .{process{if($_.DisplayName -and $_.UninstallString) { $_ } }} | Select DisplayName, Publisher, InstallDate, DisplayVersion, UninstallString |Sort DisplayName
}

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
    $installedapps = Get-Installed-Apps
    $target = $args[0]
    $result = $installedapps | where {$_.DisplayName -match [regex]::escape($target)}
    if ($result)
    {
        return $False
    }
    return $True
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