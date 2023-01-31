#!/bin/bash

set -x

cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread
./b2 install --prefix=. architecture=combined cxxflags="-arch x86_64 -arch arm64"
