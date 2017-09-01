## Installing dependencies for MacOSX ##

1. Install Homebrew: http://www.howtogeek.com/211541/homebrew-for-os-x-easily-installs-desktop-apps-and-terminal-utilities/
    
2. Install dependencies:
    1. `brew install boost --with-python`
    2. `brew install boost-python ffmpeg xerces-c mono`
    3. `sudo brew cask install java`

3. If you have not already done so, unzip the Malmo zip to some location (e.g. your home folder).
4. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` (or your Schemas location) to your `~/.bashrc` and do `source ~/.bashrc`
5. When you update Malmo you will need to update the MALMO_XSD_PATH too.

## Notes: ##

These instructions were tested on MacOSX 10.11 (El Capitan). 

On MacOSX we currently only support the system python, so please use `/usr/bin/python` for running agents, if it is not already the default. 

