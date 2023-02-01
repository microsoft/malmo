#!/bin/bash

set -x

cd deps/boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread
./b2 install -d0
