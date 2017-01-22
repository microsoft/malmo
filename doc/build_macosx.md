
## Build on MacOSX ##

These instructions were tested on MacOSX 10.12 (Sierra).

1. Install Homebrew: https://coolestguidesontheplanet.com/installing-homebrew-on-os-x-el-capitan-10-11-package-manager-for-unix-apps/

2. Install dependencies:

  ```
  brew update
  brew upgrade
  brew install boost --with-python
  brew install ffmpeg swig boost-python xerces-c doxygen git cmake
  brew cask install java
  brew install cmake
  brew install doxygen
  brew install swig
  brew install xsd
  brew unlink xsd
  brew install mono
  brew link --overwrite xsd
  ```
Installing some of these dependencies may create linking errors. Follow the instructions suggested by the error messages.
You may also need to follow the first three steps found here to update Java 3D to version 1.5: https://blogs.oracle.com/mart/entry/installing_java3d_1_5_on

3. Clone and build Project Malmo:
    1. `git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform`  
    2. `wget --no-check-certificate https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas`
    3. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` to your `~/.bashrc` and do `source ~/.bashrc`
    3. `cd MalmoPlatform`
    4. `mkdir build`
    5. `cd build`
    6. `cmake ..`
    7. `make install` (this could take awhile)
    8. `cd ../Minecract`
    9. `./launchClient.sh`
    8. Then you can run the samples from another terminal window that are installed ready-to-run in e.g. `install/Python_Examples`

4. Run the tests:
    1. `cd MalmoPlatform/build`
    2. `ctest -E Integration` to run a smaller set of tests (exclude the integration tests).
    3. `ctest` to run all the tests
