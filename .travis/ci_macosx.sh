#!/bin/bash
set -e
set -o pipefail

# This script is used for our continuous integration tests and installs from scratch on MacOSX. 
# You should not normally run this script.

# install dependencies:

brew update
brew install boost-python ffmpeg swig xerces-c doxygen
brew cask install java

# mono and CodeSynthesis XSD both contain 'xsd' executables and we want the CodeSynthesis
# one, so we force it:
brew install xsd
brew unlink xsd
brew install mono
brew link --overwrite xsd

# build Malmo:

# git clone <MalmoURL> $TRAVIS_BUILD_DIR  (our CI environment does this for us)
wget https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl -P $TRAVIS_BUILD_DIR/Schemas
export MALMO_XSD_PATH=$TRAVIS_BUILD_DIR/Schemas
cd $TRAVIS_BUILD_DIR
mkdir build
cd build
cmake -DPYTHON_EXECUTABLE="/usr/bin/python" -DCMAKE_BUILD_TYPE=Release ..
make
ctest -E Integration -VV
