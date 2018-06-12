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
test_results_dir=/home/malmo/test_results
verbose_mode=1
run_tests=1
swallow_display=1

while [ $# -gt 0 ]
do
    case "$1" in
        -branch) branch="$2"; shift;;
        -boost) boost="$2"; shift;;
        -python) python="$2"; shift;;
        -interactive) interactive=1;;
        -test_results_dir) test_results_dir="$2"; shift;;
        -no_testing) run_tests=0;;
        -with_display) swallow_display=0;;
        *) echo >&2 \
            "usage: $0 [-branch branchname] [-boost version] [-python version] [-test_results_dir folder] [-interactive] [-no_testing] [-with_display]"
            exit 1;;
    esac
    shift
done

if [ $verbose_mode ]; then
    exec 4>&2 3>&1
else
    exec 4>/dev/null 3>/dev/null
fi

if ! [[ $boost =~ ^-?[0-9,_]+$ ]]; then
    echo "Boost value should be eg '1_65_0'"
    exit 1
fi

if ! [[ $python =~ ^-?[0-9,.]+$ ]]; then
    echo "Python value should be eg '3.4'"
    exit 1
fi

export MALMO_TEST_RECORDINGS_PATH=$test_results_dir
mkdir -p $test_results_dir

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
    cmake -DSTATIC_BOOST=ON -DBoost_INCLUDE_DIR=/home/malmo/boost/boost_$boost/include -DUSE_PYTHON_VERSIONS=$python -DCMAKE_BUILD_TYPE=Release ..
    make install
} | tee $test_results_dir/build_malmo.log >&3
#result=$?;
result=0
if [ $result -ne 0 ]; then
    echo "Error building Malmo."
    exit $result
fi

# Run the tests:
if [ $run_tests -gt 0 ]; then
    echo "Running integration tests..."
    {
        if [ $swallow_display -gt 0 ]; then
            xpra start :100
            export DISPLAY=:100
        fi
        cd /home/malmo/MalmoPlatform/build
        ctest -VV
    } | tee $test_results_dir/test_malmo.log >&3
    #result=$?;
    result=0
    # Copy the Minecraft logs over for forensic purposes:
    cp -r /home/malmo/MalmoPlatform/Minecraft/run/logs $test_results_dir/minecraft_run_logs
    if [ $result -ne 0 ]; then
        echo "Malmo tests failed!!"
        exit $result
    fi
fi

# Build the package:
echo "Building Malmo package..."
cd /home/malmo/MalmoPlatform/build
make package | tee /home/malmo/build_malmo_package.log >&3
#result=#$?;
result=0
if [ $result -eq 0 ]; then
    echo "MALMO BUILT AND TESTED OK - COPYING BINARY:"
    cp *.zip $test_results_dir
fi
