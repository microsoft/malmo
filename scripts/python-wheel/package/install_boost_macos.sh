#!/bin/bash

set -x

cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread

if [[ "$(file $(which python))" == *x86_64* ]]; then
    B2_ARCH=x86
    CXX_ARCH=x86_64
else
    B2_ARCH=arm
    CXX_ARCH=arm64
fi

PY_INCLUDE_PATH="$(python -c 'from sysconfig import get_paths; print(get_paths()["include"])')"
./b2 architecture=$B2_ARCH cxxflags="-arch $CXX_ARCH -I${PY_INCLUDE_PATH}" install --prefix=. -d0
