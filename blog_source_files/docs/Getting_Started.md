
### How does the platform work?

At the heart of Project Malm&ouml; is the ability to create "Missions" which are essentially descriptions of
tasks which, in broad terms, consists of the:

  - Minecraft environment (world) such as the blocks, items, weather and enemies,
  - Observations that an agent receives as it traverses the Minecraft environment,
  - Reward handlers which define what "good" and "bad" agent behavior is, or, in other words,
    implement reward functions, and finally,
  - Quit producers that define when a mission ends.

Project Malm&ouml; provides a simple API (a set of useful function) for creating tasks; however, for most development,
a commonly used file format which can easily be extended and is compatible with many programming languages, namely XML,
is used.

One Missions are created using a programming language of your choice, agents can begin interacting with the environment
and try to complete the mission successfully by sending commands to Minecraft and receiving rewards and observations.

The backend on the Minecraft side uses the well known tool in the modding community, MinecraftForge, and communication
between agents and Minecraft uses sockets and JavaScript Object Notation (JSON) formatted objects.

---

### How do I get up and running?

For playing around with Project Malm&ouml;, a release version of the platform should suffice. However, for
contributing changes to the platform and creating Missions with functionality not already provided by Malm&ouml;,
a full build will be needed.

To get up and running with a Release version
visit the <a href="https://www.github.com/Microsoft/malmo">GitHub Repo</a>, scroll down to the
README and follow the instructions under Getting Started.

For a full build, you can visit
    <a href="https://github.com/Microsoft/malmo/blob/master/doc/build_linux.md"> Linux Build </a>,
    <a href="https://github.com/Microsoft/malmo/blob/master/doc/build_macosx.md"> OSX Build </a>, or
    <a href="https://github.com/Microsoft/malmo/blob/master/doc/build_windows.md"> Windows Build </a> as appropriate for
    the build instruction.

!!! Note
    The Download Link on the Right Pane downloads a copy of the GitHub Repository as a .zip. This works for a build.
    However, if you would like a release, please visit the GitHub page and follow the instructions in the readme there.


Most of the steps in the instruction should be quite straightforward. Should you face any difficulties,
have a look at the
<a href="https://github.com/Microsoft/malmo/wiki/Troubleshooting">Troubleshooting Wiki page</a>
on GitHub for solving some common problems; however, if you do not see your problem discussed there, do post an issue on
<a href="https://github.com/Microsoft/malmo/issues/new">GitHub</a>
or a message on the <a href="https://gitter.im/Microsoft/malmo">Gitter chat room</a>
if there are other problems you are facing.

As suggested in the readme, to get familiar with the interface once all the installation is done and
things seems to work, you can go through the
<a href="http://microsoft.github.io/malmo/0.14.0/Python_Examples/Tutorial.pdf">Tutorial</a>
which is currently based on Python. At this point, it is probably
good to note that while the platform works with other programming languages, currently most of it is focused on Python
simply because many AI and Machine Learning frameworks use Python and it works well as a scripting language with
a gentle learning curve, which is especially useful when creating Missions.

The Documentation is also be very useful. More specifically, for getting more familiar with the agent side of things,
such as the functions Malm&ouml; provides for agents to perform actions and receive observations, take a look at the
<a href="http://microsoft.github.io/malmo/0.14.0/Documentation/annotated.html">API Documentation</a>. On the
other hand, for the Mission Generation side of things, take a look at the
<a href="http://microsoft.github.io/malmo/0.14.0/Schemas/Mission.html"> XML Schema Documentation</a>.