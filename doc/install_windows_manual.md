## Installing dependencies on Windows (manual) ##

Note: alternative instructions using an experimental script for automating these steps can be found [here](https://github.com/Microsoft/malmo/blob/master/doc/install_windows.md)

For a minimal installation of running a python agent, follow steps 1, 2, 3, 4, 5, 6 and 8. Then see the Getting Started section below.

If you just want to run the Minecraft Mod (maybe your agents run on a different machine) then you only need to follow step 5.

### 1. Install 7-Zip: ###

If you already have 7-Zip installed then you can skip this step.

Visit http://7-zip.org/ and click the link for "Download .exe 64-bit x86." (or the 32-bit one).

Run the downloaded file to install 7-Zip.

### 2. Install FFMPEG: ###

1. Download [64-bit Static](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.7z) from [Zeranoe](http://ffmpeg.zeranoe.com/builds/).
2. Unpack the contents of the zip (bin folder etc.) to `C:\ffmpeg`
3. Add `C:\ffmpeg\bin` to your `PATH` ([How To](https://support.microsoft.com/en-us/kb/310519))
4. Check that typing `ffmpeg` at a command prompt works.

### 3. Install CodeSynthesis: ###

Visit http://www.codesynthesis.com/products/xsd/download.xhtml and download eg [xsd-4.0.msi](http://www.codesynthesis.com/download/xsd/4.0/windows/i686/xsd-4.0.msi)

Run the downloaded file to install CodeSynthesis.

### 4. Install Python: ###

If you don't want to use Malmo from Python then you can skip this step. But for testing your installation we recommend installing python.

Visit https://www.python.org/ and download the latest version of Python 2.7 64-bit. e.g. [python-2.7.12.amd64.msi](https://www.python.org/ftp/python/2.7.12/python-2.7.12.amd64.msi)

Run the downloaded file to install Python.

Check that typing `python` works in a command prompt. You may need to add e.g. `C:\Python27` to your PATH.

### 5. Install the Java SE Development Kit (JDK): ###

Visit http://www.oracle.com/technetwork/java/javase/downloads/index.html and download the latest 64-bit version 
e.g. `jdk-8u77-windows-x64.exe`

Run the downloaded file to install the JDK. Make a note of the install location.

Add the bin folder (e.g. `C:\Program Files\Java\jdk1.8.0_77\bin` ) to your PATH ([How To](https://support.microsoft.com/en-us/kb/310519))

Set the JAVA_HOME environment variable to be the location of the JDK:

  * Open the Control Panel: e.g. in Windows 10: right-click on `This PC` in File Explorer and select `Properties`
  * Navigate to: Control Panel > System and Security > System
  * Select `Advanced system settings` on the left
  * Select `Environment variables...`
  * Under `User variables` select `New...`
  * Enter `JAVA_HOME` as the varible name
  * Enter e.g. `C:\Program Files\Java\jdk1.8.0_77` as the variable value. Replace this with the location of your  
    JDK installation.
  
Check that `java -version` and `javac -version` and `set JAVA_HOME` all report the same 64-bit version.
 
### 6. Install the Microsoft Visual Studio 2013 redistributable: ###

Visit: https://support.microsoft.com/en-us/help/3179560/update-for-visual-c-2013-and-visual-c-redistributable-package

Download `vcredist_x64.exe` and run.

### 7. Optional: Install the dotNET runtime: ###

If you don't want to use Malmo from C# then you can skip this step.

Visit https://www.microsoft.com/net to download and install the latest dotNET framework.

### 8. Set MALMO_XSD_PATH to the location of the schemas: ###

1. If you have not already done so, unzip the Malmo zip to some location (e.g. your `C:\` drive).
2. Make a new environment variable called MALMO_XSD_PATH. ([How To](https://support.microsoft.com/en-us/kb/310519) )
3. Set it to the location of the Schemas folder where you unzipped the Malmo package. e.g. `C:\Malmo\Schemas`
4. When you update Malmo you will need to update the MALMO_XSD_PATH too.
