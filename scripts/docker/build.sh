#!/bin/bash
#
# Copyright (c) 2017 Microsoft Corporation.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
# documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
#  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
# and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of
# the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
# THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
#  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================================================================

branch=master
boost=1_65_0
python=2.7
interactive=0

while [ $# -gt 0 ]
do
    case "$1" in
        -branch) branch="$2"; shift;;
        -boost) boost="$2"; shift;;
        -python) python="$2"; shift;;
        -interactive) interactive=1;;
        *) echo >&2 \
            "usage: $0 [-branch branchname] [-boost version] [-python version] [-interactive]"
            exit 1;;
    esac
    shift
done
  
if ! [[ $boost =~ ^-?[0-9,_]+$ ]]; then
    echo "Boost value should be eg '1_65_0'"
    exit 1
fi

if ! [[ $python =~ ^-?[0-9,.]+$ ]]; then
    echo "Python value should be eg '3.4'"
    exit 1
fi

echo "Fetching Malmo..."
{
    git clone --branch $branch https://github.com/Microsoft/malmo.git /home/malmo/MalmoPlatform
    wget https://raw.githubusercontent.com/bitfehler/xs3p/1b71310dd1e8b9e4087cf6120856c5f701bd336b/xs3p.xsl -P /home/malmo/MalmoPlatform/Schemas
    cd /home/malmo/MalmoPlatform
}

if [ $interactive -gt 0 ]; then
    echo "Not building, launching sub-shell - do what you like."
    /bin/bash
    echo "Bye!"
    exit 0
fi

echo "Building Malmo..."
{
    mkdir build
    cd build
    cmake -DSTATIC_BOOST=ON -DBoost_INCLUDE_DIR=/home/malmo/boost/$boost/include -DUSE_PYTHON_VERSIONS=$python -DCMAKE_BUILD_TYPE=Release ..
    make install
}
# | tee /home/malmo/build_malmo.log >&3
result=$?;
if [ $result -ne 0 ]; then
    echo "Error building Malmo."
    exit $result
fi

# Run the tests:
echo "Running integration tests..."
{
    xpra start :100
    export DISPLAY=:100
    ctest -VV
}
# | tee /home/malmo/test_malmo.log >&3
result=$?;
if [ $result -ne 0 ]; then
    echo "Malmo tests failed!!"
    exit $result
fi

# Build the package:
echo "Building Malmo package..."
make package | tee /home/malmo/build_malmo_package.log >&3
result=$?;
if [ $result -eq 0 ]; then
    echo "MALMO BUILT OK - HERE IS YOUR BINARY:"
    ls *.zip
fi
