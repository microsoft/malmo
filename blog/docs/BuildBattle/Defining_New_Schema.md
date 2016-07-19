### Mission Structure

 Take a look at the Mission.xsd in the Schemas folder of the 
 root directory where Malm&ouml; is installed. In there you will find 
 Schema definitions for elements that form the overall structure of 
 Missions. The general structure of a Mission XML can be inferred 
 from there and from the tutorial code examples provided as such: 
 
```XML
    <Mission> 
    <About>
        <Summary>A Summary of the Mission</Summary>
        <Description>A more detailed Description of the Mission</Description>
    </About>
    <ModSettings>
        <MsPerTick>50</MsPerTick>
        <PrioritiseOffscreenRendering>false</PrioritiseOffscreenRendering>
    </ModSettings> 
    <ServerSection> 
        <ServerInitialConditions>
            <Time> 
                <StartTime>1000</StartTime>
                <AllowPassageOfTime>false</AllowPassageOfTime>
            </Time>
            <Weather>clear</Weather>
            <AllowSpawning>false</AllowSpawning>
        </ServerInitialConditions>
        <ServerHandlers>
            <!-- One of the following: 
                
                <FlatWorldGenerator>, <FileWorldGenerator> and 
                <DefaultWorldGenerator>
                    ...
                </ChosenWorldGenerator> 
                
            --> 
            
            <!-- Some number of the following: 
                 
                 <DrawingDecorator>, <MazeDecorator>, 
                 <ClassroomDecorator> and <SnakeDecorator>
                    ...
                 </ChosenDecorator> 
            -->
            
            <!-- Some number of the following: 
                 
                 <ServerQuitFromTimeUp> and 
                 <ServerQuitWhenAnyAgentFinishes>
                    ...
                 </ChosenQuitProducer>  
            -->
        </ServerHandlers>
    </ServerSection>
    <AgentSection> 
        <!-- Some number of the following. 
             Note: AgentMissionHandlers are defined as a large xs:group in Mission.xsd
                   These include: Observation Producers, Reward Handlers, Command Handlers, 
                                  and Agent Quit Producers  
                                  
             <AnAgentMissionHandler> 
                ...
             </TheAgentMissionHandler>
             
         -->
    </AgentSection>
  </Mission>
```


Hopefully the above gives a good representation of what Missions look 
like in Malm&ouml;. Wherever possible, common sensible values have been 
replaced such as for the Weather and StartTime. XML comments are 
defined using ```<!--``` and ```-->``` with the contents within. 
The comments above talk about what is to be in place of the comments 
in the given regions of the XML document. 

### Mission Handlers

This tutorial is focused on creating a new task, the Build Battle Task, 
and specifically for that we need a new Agent Mission Handler and in 
particular a Reward Handler.

Now, we will add our first line of code! Scroll to the ```xs:group``` named 
```AgentMissionHandlers``` in the ```Mission.xsd``` file. There should be a section 
of code with Reward Handlers. Add an element at the end of the section 
with ```ref``` set to ```RewardForStructureCopying``` and ```minOccurs``` set to ```0```. 

As we have talked about in the previous section, ```minOccurs``` is an 
occurrence indicator that specifies the minimum number of times an 
element needs to appear (in this case in the all section of the group). 
The ref attribute references an element that is declared elsewhere, 
which may or may not be in the same schema document. Take a look at the
top of the ```Mission.xsd``` file. There you will see some xs:includes. We 
will be writing the schema for the ```RewardForStructureCopying``` element in 
```MissionHandler.xsd``` where all the other RewardHandlers and indeed 
(Agent)MissionHandlers are defined.

After completing the above, you should have something like what is shown
below:

```XML
    <xs:element ref="RewardForTouchingBlockType" minOccurs="0" />
    <xs:element ref="RewardForSendingCommand" minOccurs="0" />
    <xs:element ref="RewardForCollectingItem" minOccurs="0" />
    <xs:element ref="RewardForDiscardingItem" minOccurs="0" />
    <xs:element ref="RewardForReachingPosition" minOccurs="0"/>
    <xs:element ref="RewardForMissionEnd" minOccurs="0"/>
    <xs:element ref="RewardForStructureCopying" minOccurs="0"/>
```

Now, let us write the XML Schema Definition for 
```RewardForStructureCopying```. First, we open 
```MissionHandler.xsd```. Now, scroll down to the section just after 
the comment which contains the phrase "REWARD PRODUCERS."

To get an idea of what we will be adding first, take a look at the 
simpleType definition ```Behavior```:
 
```XML
<xs:simpleType name="Behaviour">
    <xs:restriction base="xs:string">
      <xs:enumeration value="onceOnly" />
      <xs:enumeration value="oncePerBlock" />
      <xs:enumeration value="oncePerTimeSpan" />
      <xs:enumeration value="constant" />
    </xs:restriction>
  </xs:simpleType>
```
 
As can be seen above, Behavior is a simpleType that restricts a string 
to take one of four possible values (an enumeration with four possible
values.) Functionally, if one looks at the naming and the Java files
associated with this Schema, Behavior specifies what is commonly 
known in reinforcement learning as reward density.
 
As the names imply: onceOnly signifies that an agent will receive a 
reward once only, oncePerBlock signifies that an agent 
will receive rewards once per block, oncePerTimeSpan specifies that 
a reward is received only once in some time span, and finally, consant 
signifies that a reward is constantly given. 

Now, the above explanations might seem a little vague, but it should do
for what we are going to add. ```RewardDensityForBuildAndBreak``` is 
type very similar to ```Behavior``` in that it is also a reward density 
type. Ideally we should reuse types if at all possible, however; just to 
illustrate the definitions of types in XSD as well as make a more 
functional and meaning type for the particular task we are creating, 
we define this new type. Go ahead and add the following code snippet
right after the last Schema definition in the Reward Producers section.

```XML
  <xs:simpleType name="RewardDensityForBuildAndBreak">
    <xs:restriction base="xs:string">
      <xs:enumeration value="PER_BLOCK"/>
      <xs:enumeration value="ACCUMULATED"/>
      <xs:enumeration value="MISSION_END"/>
    </xs:restriction>
  </xs:simpleType>
```

The above simpleType definition also uses an ```xs:restriction``` with
the type ```xs:string```. It can take one of three values, 
```PER_BLOCK``` which signifies that a reward is given per block placed
or broken, ```ACCUMULATED``` signifying some sort of reward 
accumulation and ```MISSION_END``` signifying that a reward is only 
given at a mission's end. Again, these may seem like vague description, 
but they will become clear when we add the Minecraft Forge code.

Now, in the Build Battle, we will be comparing a given area where the 
player (agent) is to construct a structure that is already built. A 
simple way to define such areas is using a cuboid. Given that Minecraft
is comprised of voxels (individual cubes), we can describe a cuboid 
using just 2 block positions: a minimum and maximum that form a diagonal
of the cuboid. Take a look at the definition of ```Grid``` and 
```DrawCuboid``` in the same ```MissionHandler.xsd``` file. Both of them
are indeed what we would like to some extent; however, we don't really
need to have a ```Grid``` that is named nor do we need to specify block 
types and other block properties which the DrawCuboid requires. We thus
can define another type, namely ```UnnamedGridDefinition```. 

```XML
  <xs:complexType name="UnnamedGridDefinition">
    <xs:sequence>
      <xs:element name="min" type="Pos" />
      <xs:element name="max" type="Pos" />
    </xs:sequence>
  </xs:complexType>
```

Note that the above type is complex and consists of a sequence with 
two elements, a ```Pos``` named ```min``` and a ```Pos``` named ```max```
where ```Pos``` is on closer inspection (in the same ```MissionHandler.xsd``` file once again),
a 3-tuple defining x, y and z coordinates. Importantly, it should also 
be said that ```xs:sequence``` tags by default use a ```minOccurs``` and
```maxOccurs``` of 1 thus required all ```UnnamedGridDefinition``` to 
have both a ```max``` and ```min```.

Last, but certainly not least, we define the actual ```RewardForStructureCopying```
Schema. It will take two ```UnnamedGridDefinition```s defining the 
player structure bounds and built or goal structure bounds that are 
to be compared. Also, it will take two ```DrawBlockBasedObjectType```s
that allow for providing visual feedback to agents and choosing the 
block properties to use when blocks are switched for visual feedback. 
For example, if the player places a block outside the player structure
boundary, upon block placement the type of the block could change to 
redstone (giving the block a red appearance) along with a negative 
reward. Finally, we have a ```RewardDensityForBuildAndBreak``` also. 
This gives the following code.

```XML  
  <xs:element name="RewardForStructureCopying">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="PlayerStructureBounds" type="UnnamedGridDefinition" />
        <xs:element name="GoalStructureBounds" type="UnnamedGridDefinition" />
        <xs:element name="BlockTypeOnCorrectPlacement"
                    type="DrawBlockBasedObjectType" nillable="true" />
        <xs:element name="BlockTypeOnIncorrectPlacement"
                    type="DrawBlockBasedObjectType" nillable="true" />
        <xs:element name="RewardDensity" type="RewardDensityForBuildAndBreak" />
      </xs:sequence>
      <xs:attributeGroup ref="RewardProducerAttributes"/>
    </xs:complexType>
  </xs:element>
```
 
The inclusion of the attributeGroup ```RewardProducerAttributes``` is 
yet unexplained. Examine the other Reward Producers, you will in fact 
see this attributeGroup in their definitions as well. The reason this 
is added is to allow for Reward Producers to share some attributes. In 
this case, we'd like to give Rewards a dimension. The idea is to be able
to associate certain types of rewards with a dimension. For example, 
we might want to associate rewards for reaching goal locations with 
say dimension 1 while rewards for defeating enemies with another 
dimension, giving a total of two dimensions and corresponding to 
two-dimensional vectors.

Upon adding the above three code snippets to the end of the Reward 
Producers section, we are now ready to start writing the Forge side code
and making changes to the Minecraft mod.

At this point, ensure that you have added the single line of code to 
your ```Mission.xsd``` file that adds a ref to ```RewardForStructureCopying``` as well
as the above three code snippets in the given order right at the end
of the Reward Producers section in the ```MissionHandlers.xsd``` file.

To test if everything is working right, try rebuilding the platform and 
in particular, try running the gradle task JAXB by executing: 
 
  * Windows: ```gradlew jaxb```
  * Linux/MacOS: ```./gradlew jaxb```
 
The above will perform a task whereby necessary Java definitions of the 
schemas we just defined will be added. These will in fact be used 
shortly.