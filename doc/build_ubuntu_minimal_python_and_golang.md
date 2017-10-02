## Build on Ubuntu (16.04+/64-bit): ##

1. Environment variables (add to your `~/.bashrc` or `~/.bash_aliases`)
    1. `export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/`
    2. `export MALMO_PATH=$HOME/MalmoPlatform`      (change `$MALMO_PATH` as desired)
    3. `export MALMO_BUILD_PATH=$HOME/build_malmo`  (change `$MALMO_BUILD_PATH` as desired)
    4. `export MALMO_XSD_PATH=$MALMO_PATH/Schemas`
    5. `export MALMO_LIB_PATH=$MALMO_BUILD_PATH/install/Cpp_Examples/lib`

2. Install dependencies.
    1. `sudo apt-get install build-essential git cmake cmake-curses-gui cmake-qt-gui libboost-all-dev libpython2.7-dev openjdk-8-jdk swig xsdcxx libxerces-c-dev xsltproc ffmpeg python-tk python-imaging-tk`  
    2. `sudo update-ca-certificates -f`

3. Install CodeSynthesis XSD:
    1. `wget http://www.codesynthesis.com/download/xsd/4.0/linux-gnu/x86_64/xsd_4.0.0-1_amd64.deb`
    2. `sudo dpkg -i --force-all xsd_4.0.0-1_amd64.deb`  
    3. `sudo apt-get install -f`  
       This step is needed because we require xsd version 4.0.  
       (When mono-devel is updated, you will need to manually remove then reinstall xsd as above, because of the package conflicts.)

4. Build Malmo:
    1. `git clone https://github.com/Microsoft/malmo.git $MALMO_PATH`
    2. `wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P $MALMO_PATH/Schemas`
    3. `mkdir -p $MALMO_BUILD_PATH`
    4. `cd $MALMO_BUILD_PATH`
    5. Start CMake. Example `ccmake $MALMO_PATH`
    6. Turn OFF lua, java, csharp, torch as desired.
    7. Choose build type or leave empty.
    8. Select configure [c] twice and generate [g]
    9. Close CMake
    10. `make`
    11. `make install`

5. Start Minecraft (in one terminal):
    1. `cd $MALMO_BUILD_PATH/install/Minecraft`

6. Run examples (in another terminal):
    1. `./launchClient.sh`

7. Test Malmo:
    1. `ctest`
    2. `ctest -E Integration` to exclude the integration tests.
    3. `ctest -VV` to get verbose output.
