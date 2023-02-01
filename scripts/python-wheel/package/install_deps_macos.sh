#!/bin/bash

set -x

if [[ "$(which python)" == *macosx_x86* ]]; then
    B2_ARCH=x86
    CXX_ARCH=x86_64
elif [[ "$(which python)" == *macosx_arm64* ]]; then
    B2_ARCH=arm
    CXX_ARCH=arm64
else
    echo Unknown architecture.
    exit 1
fi
PY_INCLUDE_PATH="$(python -c 'from sysconfig import get_paths; print(get_paths()["include"])')"

cd deps
DEPS_DIR="$(pwd)"

# cd xz-5.4.1
# CFLAGS="-arch $CXX_ARCH" LDFLAGS="-arch $CXX_ARCH" ./configure --prefix="$DEPS_DIR"
# make install
# cd ..

# cd zstd-1.5.2
# rm -r builddir
# cmake -B builddir -S build/cmake -DCMAKE_INSTALL_PREFIX="$DEPS_DIR" -DCMAKE_INSTALL_RPATH="$DEPS_DIR" -DCMAKE_OSX_ARCHITECTURES=$CXX_ARCH
# cmake --build builddir
# cmake --install builddir
# cd ..

cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread
rm -rv ../lib/libboost_*
./b2 architecture=$B2_ARCH cxxflags="-arch $CXX_ARCH -I${PY_INCLUDE_PATH}" cflags="-arch $CXX_ARCH" linkflags="-arch $CXX_ARCH -L${DEPS_DIR}/lib -headerpad_max_install_names" install --build-dir=build_${B2_ARCH} --prefix=.. -d0
cd ..
