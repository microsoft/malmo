## Installing dependencies on Windows (manual) ##

Note: alternative instructions using an experimental script for automating these steps can be found [here](https://github.com/Microsoft/malmo/blob/master/doc/install_windows.md)

For a minimal installation of running a python agent, follow steps 1,2,3,4 and 6. Then see the Getting Started section below.

If you just want to run the Minecraft Mod (maybe your agents run on a different machine) then you only need to follow step 4.

### 1. Install 7-Zip: ###

If you already have 7-Zip installed then you can skip this step.

Visit http://7-zip.org/ and click the link for "Download .exe 64-bit x86." (or the 32-bit one).

Run the downloaded file to install 7-Zip.

### 2. Install FFMPEG: ###

1. Download [64-bit Static](http://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.zip) from [Zeranoe](http://ffmpeg.zeranoe.com/builds/).
2. Unpack the contents of the zip (bin folder etc.) to `C:\ffmpeg`
3. Add `C:\ffmpeg\bin` to your `PATH` ([How To](https://support.microsoft.com/en-us/kb/310519))
4. Check that typing `ffmpeg` at a command prompt works.

### 3. Install Python: ###

If you don't want to use Malmo from Python then you can skip this step. But for testing your installation we recommend installing python.

Visit https://www.python.org/ and download the latest version of Python 3.6 64-bit. e.g. [python-3.6.6.amd64.msi](https://www.python.org/ftp/python/3.6.6/python-3.6.6.exe)

Run the downloaded file to install Python.

Check that typing `python` works in a command prompt. You may need to add it to your PATH and/or relaunch your cmd prompt.

### 4. Install the Java OpenJDK 8. ###
 
Please make sure that you have set the JAVA_HOME environment variable to your installation directory.

### 5. Optional: Install the dotNET runtime: ###

If you don't want to use Malmo from C# then you can skip this step.

Visit https://www.microsoft.com/net to download and install the latest dotNET framework.

### 6. Set MALMO_XSD_PATH to the location of the schemas: ###

1. If you have not already done so, unzip the Malmo zip to some location (e.g. your `C:\` drive).
2. Make a new environment variable called MALMO_XSD_PATH. ([How To](https://support.microsoft.com/en-us/kb/310519) )
3. Set it to the location of the Schemas folder where you unzipped the Malmo package. e.g. `C:\Malmo\Schemas`
4. When you update Malmo you will need to update the MALMO_XSD_PATH too.
