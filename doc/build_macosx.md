
## Build on MacOSX ##

These instructions were tested on MacOSX 10.11.1 (El Capitan).

1. Install Homebrew: http://www.howtogeek.com/211541/homebrew-for-os-x-easily-installs-desktop-apps-and-terminal-utilities/

2. Install dependencies:

  ```
  brew update
  brew upgrade
  brew install boost --with-python
  brew install ffmpeg swig boost-python xerces-c doxygen git cmake
  sudo brew cask install java
  brew install xsd
  brew unlink xsd
  brew install mono
  brew link --overwrite xsd
  ```

3. Clone and build Project Malmo:
    1. `git clone https://github.com/Microsoft/malmo.git ~/MalmoPlatform`  
    2. `wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P ~/MalmoPlatform/Schemas`
    3. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` to your `~/.bashrc` and do `source ~/.bashrc`
    3. `cd MalmoPlatform`
    4. `mkdir build`
    5. `cd build`
    6. `cmake ..`
    7. `make install`
    8. Then you can run the samples that are installed ready-to-run in e.g. `install/Python_Examples`

4. Run the tests:
    1. `cd MalmoPlatform/build`
    2. `ctest -E Integration` to run a smaller set of tests (exclude the integration tests).
    3. `ctest` to run all the tests
