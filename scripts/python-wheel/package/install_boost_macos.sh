#!/bin/bash

set -x

cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread

PY_PROCESSOR="$(python -c 'import platform; print(platform.processor())')"
if [[ "$PY_PROCESSOR" == *86 ]]; then
    B2_ARCH=x86
    CXX_ARCH=x86_64
elif [[ "$PY_PROCESSOR" == arm ]]; then
    B2_ARCH=arm
    CXX_ARCH=arm64
else
    echo Unknown processor: $PY_PROCESSOR
    exit 1
fi

PY_INCLUDE_PATH="$(python -c 'from sysconfig import get_paths; print(get_paths()["include"])')"
./b2 architecture=$B2_ARCH cxxflags="-arch $CXX_ARCH -I${PY_INCLUDE_PATH}" install --prefix=. -d0
