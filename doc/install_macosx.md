## Installing dependencies for MacOSX ##

1. Install Homebrew: http://www.howtogeek.com/211541/homebrew-for-os-x-easily-installs-desktop-apps-and-terminal-utilities/
    
2. Install dependencies:
    1. `brew install python3`
    2. `brew install ffmpeg boost-python3`
    3. `brew cask install java8`

3. If you are installing from source (and not using "pip3 install malmo"), unzip the Malmo zip to some location (e.g. your home folder as assumed in the next step).
4. Add `export MALMO_XSD_PATH=~/MalmoPlatform/Schemas` (or your Schemas location) to your `~/.bashrc` and do `source ~/.bashrc`
5. When you update Malmo you will need to update the MALMO_XSD_PATH too.

## Notes: ##

These instructions were tested on MacOSX 10.13.3 (High Sierra).

