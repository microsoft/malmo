#!/bin/bash

set -x

tar xf /boost/boost_1_76_0.tar.bz2
cd boost_1_76_0
./bootstrap.sh --with-libraries=atomic,chrono,date_time,filesystem,iostreams,program_options,python,regex,system,thread
./b2 install -d0
