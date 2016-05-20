CMake Notes
===========

CMake: http://cmake.org

See elsewhere for instructions on how to use CMake and tutorials. This is just a set
of notes we wanted written down somewhere.

It's a good tactic to delete the build folder if there is any doubt about how things are set up. 

Don't check-in any Visual Studio solution or project files! We generate these as needed.

In Visual Studio, don't change any project settings or add files to projects! These changes won't get saved, since
the project files are not checked-in. Instead make the required changes to the relevant CMakeLists.txt file.

When you switch branches, hit build in Visual Studio. As part of its normal build it will run CMake, then ask to
reload the project files if these have changed. 

If you want to run the tests from within Visual Studio, build the RUN_TESTS project. Open the Output tab to see the results.

To help understand how CMake works, or to fix problems, use cmake-gui instead of just cmake. You can launch it from the command-line
in the same way as cmake (`cmake-gui ..`) or separately. It shows the current variables and allows you to change them - hit Configure
each time. You might do this if you have installed Boost to a non-standard location, for example - change BOOST_ROOT to help CMake
find it.

On Linux there is also `ccmake`, which is a console version of `cmake-gui`.


