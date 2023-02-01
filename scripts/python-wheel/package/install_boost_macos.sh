#!/bin/bash

set -x

cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread
if [[ "$(file $(which python))" == *x86_64* ]]; then
    ./b2 architecture=x86 cxxflags="-arch x86_64" install --prefix=.
else
    ./b2 architecture=arm cxxflags="-arch arm64" install --prefix=.
fi
