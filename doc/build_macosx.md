
## Build on MacOSX ##

These instructions were tested on MacOSX 10.11.1 (El Capitan).

1. Install Homebrew: http://www.howtogeek.com/211541/homebrew-for-os-x-easily-installs-desktop-apps-and-terminal-utilities/

2. Install dependencies:

```
brew update
brew install boost --with-python
brew install ffmpeg swig boost-python xerces-c doxygen git make
sudo brew cask install java
brew install xsd
brew unlink xsd
brew install mono
brew link --overwrite xsd
```

3. Clone the Project Malmo code:
    1. Open a Terminal window and type `git clone <MALMO_URL> MalmoPlatform`  
       (or to whatever destination folder you prefer)

4. Install xs3p stylesheet:
    1. Save xs3p.xsl from https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl to the Schemas folder.  
       (License: https://github.com/bitfehler/xs3p )

5. Build Malmo:
    1. Open a Terminal window
    2. `cd MalmoPlatform`
    3. `mkdir build`
    4. `cd build`
    5. `cmake ..`
    6. `make`

6. Run the tests:
    1. `ctest`
