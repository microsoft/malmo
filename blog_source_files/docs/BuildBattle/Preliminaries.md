
### Prerequisites

This tutorial assumes that you have already completed the build instructions and are able to create a
build successfully. If this is not the case, have a look at the "How do I get up and running?" section under
"Getting Started" for the build instructions as well as some troubleshooting guidelines.

Apart from being able to create a build, there is not much else that this tutorial assumes other than some basic programming background. Most of the tutorial can be followed simply by going through the code and documentation already present in the Malm&oul; platform and doing some pattern matching. Pointers to 
  resources and documentation will be provided when relevant. That said, some knowledge of XML and Java or similar equivalents, say HTML and C++ respectively, will be greatly beneficial.

### Setting up an IDE

Using an Integrated Development Environment can greatly help in developing MinecraftForge mods and hence working with
the Project Malm&ouml; platform. Since Minecraft is programmed in Java, some IDEs are
<a href="http://www.minecraftforge.net/wiki/Installation/Source#IDEs"> recommended and used by the
Minecraft modding community </a>. These are <a href="https://www.jetbrains.com/idea/download"> IntelliJ IDEA </a> and
<a href="https://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/neonr"> Eclipse </a>.

At this point, if you do not already have one of the above IDEs, choose an IDE you like, download and install it.

Now, open a command prompt or terminal and change the directory to the Malm&ouml; directory (root) and then to the directory
inside titled Minecraft. Now, the next few steps will
<a href="http://www.minecraftforge.net/wiki/Installation/Source#Installation"> vary depending on the IDE you choose to use </a>.

If you are using Eclipse and are on

  * Windows, run: ``` gradlew eclipse ```

  * Linux/Mac OS, run:  ```./gradlew eclipse```

Now, open Eclipse and point it to the eclipse folder

If you are using IDEA, open IDEA and select Import Project. Now, navigate to the Minecraft folder within the Malm&ouml; directory.
Select ```build.gradle``` within the Minecraft folder.

Once IDEA finishes importing the project, close IDEA. In your command window, run

  - For Windows: ```gradlew genIntellijRuns```

  - For Linux/Mac OS: ```./gradlew genIntellijruns```

