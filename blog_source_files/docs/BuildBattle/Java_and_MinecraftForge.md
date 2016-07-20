### Java 

Java is a programming language that has become ubiquitous given its 
ability to be used in many different hardware and software configurations
through the Java Virtual Machine (JVM). It is quite similar in syntax 
to other strongly typed languages and compiled like C++. Given that this
tutorial is aimed at learning about how to create a Project Malm&ouml; 
task and not a Java tutorial, should you be unfamiliar with Java 
or have never used Java before, it might be a good idea to go through 
some Java resources: <a href="http://tutorialspoint.com/java">Tutorials Point</a>, 
<a href="https://docs.oracle.com/javase/tutorial">Java Docs at Oracle</a>, 
<a href="http://minecraft.gamepedia.com/Mods/Creating_mods/Helpful_links">Minecraft Gamepedia Helpful Links for Creating Mods</a>.

### Minecraft Forge

As alluded to above, Minecraft is written in Java and in fact uses the LightWeight Java Game Library. 
Thus, to be able to able to create mods, it is a good idea to use 
Java itself. MinecraftForge, commonly known simply as Forge, is one 
such modding platform which uses Java that is quite mature and has been used for many 
Minecraft mods. At it's core, Forge provides access to the state of the 
Minecraft world, and entities like players, enemies, villagers, and 
the weather. Additionally, Event Handlers provide hooks for getting 
access into events like block place and break events which
we will be using.

There is not much thorough and up-to-date documentation on Forge. This 
is one reason using an IDE helps a lot since being able to go through 
the Minecraft and Forge libraries itself serves as quite a good reference. 
That said, many of the functions and arguments are "obfuscated." 
For example, you might see a function like ```func_152917_b(String p_152917_1_)```.
This is generally the case for methods and parameters deep and internal to
Minecraft. For many of these, you can find more information and possibly 
the true name for the method and arguments on an Internet Relay Chat website, 
<a href="https://webchat.esper.net">EsperNet</a>, in the #MCPBot chatroom. 

Apart from the above, there are indeed many resources and websites; however, 
as stated before, proceed with caution when using such resources as they 
may be outdated. Here are some that can be very useful: 

  * <a href="http://minecraftforge.net/Wiki/Tutorials">Minecraft Forge Tutorial Wiki</a>
  * <a href="http://minecraftforge.net/forum/index.php">Minecraft Forge Forums</a>
  * <a href="https://dl.dropboxusercontent.com/s/h777x7ugherqs0w/forgeevents.html">A Complete list of all Forge Events available</a>
  * <a href="http://greyminecraftcoder.blogspot.no/p/list-of-topics.html">Grey Minecraft Coder's Modder Guide</a>
  * <a href="http://bedrockminer.jimdo.com/modding-tutorials">Minecraft Modding Tutorials by \_Bedrock_Miner\_</a>
  * <a href="http://jabelarminecraft.blogspot.com">Jabelar's Minecraft Forge Modding Tutorials</a>
  * <a href="http://wuppy29.com/minecraft">Wuppy29 Minecraft Modding and Java Tutorials and Torubleshooting</a>
  * <a href="https://github.com/coolAlias/Forge_Tutorials">GitHub repo with Forge Tutorials and links to corresponding MinecraftForge Forums posts</a>
  * <a href="http://planetminecraft.com/forums">Planet Minecraft Forums</a>
  * <a href="http://www.minecraftforum.net/forums/mapping-and-modding/mapping-and-modding-tutorials/1571568-tutorial-1-6-2-changing-vanilla-without-editing">Bytecode and ASM manipulation guide in a MinecraftForge forum post</a>
  * <a href="http://mazetar.com/mctuts/searchform.php">Mazetar Modding Tutorial Databse</a>
  * <a href="http://www.minecraftforge.net/forum/index.php/topic,20135.0.html">MinecraftForge forum poast about client/server communication and threading</a>
  
 
In the next section, we will begin writing the Forge side code. The steps will be presented in a manner much like the XSD instruction given before. 
So, a deep understanding of Forge or Java isn't really required. That said, have a look around some of the resources 
presented above and take a look at the Project Malm&ouml; code in the IDE you have installed. 