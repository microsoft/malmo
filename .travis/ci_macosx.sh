#!/bin/bash
set -e
set -o pipefail

# This script is used for our continuous integration tests and installs from scratch on MacOSX. 
# You should not normally run this script.

# install dependencies:

brew update
brew install boost --with-python
brew install ffmpeg swig boost-python xerces-c mono doxygen xsd
brew cask install java

# build Malmo:

# git clone <MalmoURL> $TRAVIS_BUILD_DIR  (our CI environment does this for us)
wget https://raw.githubusercontent.com/bitfehler/xs3p/master/xs3p.xsl -P $TRAVIS_BUILD_DIR/Schemas
cd $TRAVIS_BUILD_DIR
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
ctest -VV
