### Parsing Parameters

Take a look at the ```parseParameters(Object params)``` function in 
the various classes in ```MissionHandlers```. This should give a good
idea about what it is we are about to do.

The code we will add for the ```parseParameters``` function is given below. 
Note that we remove the code already within ```parseParameters``` which 
simply calls a superclass method. This is not really what we would like. 
We want to override the superclass method and change the return values.

```java
    @Override
    public boolean parseParameters(Object params) {

        if (params == null || !(params instanceof RewardForStructureCopying))
            return false;

        RewardForStructureCopying rscparams = (RewardForStructureCopying) params;

        BlockPos minPlayerStructurePos = 
            this.blockPosFromPos(rscparams.getPlayerStructureBounds().getMin());
        BlockPos maxPlayerStructurePos = 
            this.blockPosFromPos(rscparams.getPlayerStructureBounds().getMax());
        BlockPos minGoalStructurePos = 
            this.blockPosFromPos(rscparams.getGoalStructureBounds().getMin());
        BlockPos maxGoalStructurePos = 
            this.blockPosFromPos(rscparams.getGoalStructureBounds().getMax());

        this.playerStructureAABB = new AxisAlignedBB(minPlayerStructurePos, maxPlayerStructurePos);
        this.goalStructureAABB = new AxisAlignedBB(minGoalStructurePos, maxGoalStructurePos);

        if(playerStructureAABB.intersectsWith(goalStructureAABB))
        {
            System.out.print("Warning! RewardForStructureCopying got player structure bounds" + 
                             " and goal structure bounds that intersect!" +
                             " Undefined behavior can follow!\n");
        }

        this.structureVolume = volumeOfAABB(playerStructureAABB);
        
        if(this.structureVolume != volumeOfAABB(goalStructureAABB))
        {
            System.out.print("Warning! RewardForStructureCopying got player structure bounds" + 
                             " and goal structure bounds that have unequal volumes!" + 
                             " Undefined behavior can follow!\n");
        
        }

        this.delta = new Vec3i(goalStructureAABB.minX - playerStructureAABB.minX,
                goalStructureAABB.minY - playerStructureAABB.minY,
                goalStructureAABB.minZ - playerStructureAABB.minZ);

        this.rewardDensity = rscparams.getRewardDensity();

        DrawBlockBasedObjectType blockBasedObjectType = rscparams.getBlockTypeOnCorrectPlacement();

        this.blockTypeOnCorrectPlacement = BlockDrawingHelper.applyModifications(
                MinecraftTypeHelper.ParseBlockType(blockBasedObjectType.getType().value()),
                blockBasedObjectType.getColour(),
                blockBasedObjectType.getFace(),
                blockBasedObjectType.getVariant());

        blockBasedObjectType = rscparams.getBlockTypeOnIncorrectPlacement();

        this.blockTypeOnIncorrectPlacement = BlockDrawingHelper.applyModifications(
                MinecraftTypeHelper.ParseBlockType(blockBasedObjectType.getType().value()),
                blockBasedObjectType.getColour(),
                blockBasedObjectType.getFace(),
                blockBasedObjectType.getVariant());

        this.dimension = rscparams.getDimension();

        return true;
    }
```

The first thing we do is check if the ```params``` object is null or is
not of the Schema type we are expect, i.e., not of the type ```RewardForStructureCopying```. 
If either is the case, we return ```false``` presumably telling the caller of this 
function that the Schmea given is not something the Implementation was designed for.

On the other hand, if the ```params``` object is indeed of type 
```RewardForStructureCopying```, we perform a cast from ```Object```, 
Java's only class that has no superclass and from which all other classes
inherit, to ```RewardForStructureCopying``` and set the instance to 
a variance named ```rscparams``` following the convention of the other 
Reward Handlers (rsc stands for Reward, Structure and copying resp.)

Once we have a ```RewardForStructureCopying``` instance, we can query 
it for the variables we'd like to store. 

Firstly, we get four ```BlockPos``` instances. This is the first Minecraft 
type we are seeing! ```BlockPos``` is essentially an object that contains information 
about a block's position or in other words contains data similar to a 3-tuple or 3-dimensional vector.
Note the use of the method ```blockPosFromPos``` which is given below: 

```java 
private BlockPos blockPosFromPos(@Nonnull Pos pos) {
    return new BlockPos(pos.getX().intValueExact(), 
                        pos.getY().intValueExact(), 
                        pos.getZ().intValueExact());
}
```

The above method just returns a new ```BlockPos``` instance from 
a ```pos``` instance which JAXB seems to have created (recall that 
our ```UnnamedGridDefinition``` used a type defined in the Schema, 
namely ```Pos```.) It turns out that ```Pos``` comprises of ```BigDecimal```s 
since ```Pos``` uses ```xs:decimal```s. This is a nice type for 
performing accurate and exact arithmetic operations; however, for 
comparing two grids of the same volume, we will be comparing the blocks
within the grid themselves and we don't really care for decimals or 
floating point arithmetic. We can make do with integers. Thus, we 
create a new ```BlockPos``` with ```intValueExact()``` of the three 
```BigDecimals```. Finally, note the use of the annotation ```@NonNull```
which helps IDEs find places where ```blockPosFromPos``` might have been 
called with a ``null``` argument and raise an error for the user (even 
before compile-time).

Next, we define two Axis Aligned Bounding Blocks which in essence are 
analogous to ```UnnamedGridDefinition```s, but hold block positions 
using ```BlockPos```' rather than ```Pos```'. We use these to 
immediately perform a simple check: do the player structure and goal 
structure intersect? If they do, we quit; else, we continue.

We now go on to find and ensure that the structure volumes of the two 
structures are the same. This uses the following member function: 

```java 
private float volumeOfAABB(AxisAlignedBB axisAlignedBB) {
    return (float) (axisAlignedBB.maxX - axisAlignedBB.minX + 1) *
            (float) (axisAlignedBB.maxY - axisAlignedBB.minY + 1) *
            (float) (axisAlignedBB.maxZ - axisAlignedBB.minZ + 1);
}
```
 
Moving on, we find ```delta``` which is a 3-dimensional vector specifying
the vector to add to a particular block position in the player 
structure to get the corresponding goal structure
block position. We will use this vector when we compare the two
structures. Continuing on, we store the Reward Density. 

The ```BlockDrawingHelper``` class and ```MinecraftTypeHelper``` classes are definitely something you
should look at, these are used by the ```DrawingDecorators``` and are 
also useful in parsing information of Schema like ```DrawBlockBasedObjectType```
to get ```IBlockState```s, Minecraft's internal representation of Block
States. And so, penultimately, we get two ```IBlockStates``` 
corresponding to Block States to use for the Correct Block Placement and 
Incorrect Block Placement cases. Finally, we store the dimension along 
which rewards for this instance of ```RewardForStructureCopying``` are 
to be sent.

If all of the above succeeds, we have completed parsing a 
```RewardForStructureCopying``` Schema and can return ```true```.


### Prepare and Cleanup 

Again, taking a look at the other Mission Handlers, and, in particular, 
the Reward Handlers, we see that in the ```prepare``` methods, we 
remove ```QuitCode```s that are present in the ```MalmoMod``` properties
and perform registration of the class. The latter step, registering a 
class to the Minecraft Forge ```EVENT_BUS``` is crucial when we begin 
adding Event Handlers. Without it, the event handlers will not fire. 
Note that there is another bus for adding event handlers to, the
```FMLCommonHandler.bus()```. To see when you need to register event 
handlers to that bus, take a look at the Complete list of Forge Event 
Handlers, given as a resource for Minecraft Forge in the Java and 
MinecraftForge section. Thus, prepare is as follows: 

```java
@Override
public void prepare(MissionInit missionInit) {
    try
    {
        if (MalmoMod.getPropertiesForCurrentThread().containsKey("QuitCode"))
            MalmoMod.getPropertiesForCurrentThread().remove("QuitCode");
    }
    catch (Exception e)
    {
        System.out.println("Failed to get properties.");
    }

    MinecraftForge.EVENT_BUS.register(this);
}
```

Cleanup is very simple in that the only thing we do is unregister the 
class, like so: 

```java
@Override
public void cleanup() {
    MinecraftForge.EVENT_BUS.unregister(this);
}
```

The end result of this section along with some member variables that 
were not shown is as follows: 

```java
public class RewardForStructureConstructionImplementation extends HandlerBase implements IRewardProducer {
    private AxisAlignedBB playerStructureAABB;
    private AxisAlignedBB goalStructureAABB;

    private Vec3i delta;
    private float structureVolume;

    private RewardDensityForBuildAndBreak rewardDensity;

    private IBlockState blockTypeOnCorrectPlacement;
    private IBlockState blockTypeOnIncorrectPlacement;

    private int dimension;
   
    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params) {
        if (params == null || !(params instanceof RewardForStructureCopying))
            return false;

        RewardForStructureCopying rscparams = (RewardForStructureCopying) params;

        BlockPos minPlayerStructurePos = 
            this.blockPosFromPos(rscparams.getPlayerStructureBounds().getMin());
        BlockPos maxPlayerStructurePos = 
            this.blockPosFromPos(rscparams.getPlayerStructureBounds().getMax());
        BlockPos minGoalStructurePos = 
            this.blockPosFromPos(rscparams.getGoalStructureBounds().getMin());
        BlockPos maxGoalStructurePos = 
            this.blockPosFromPos(rscparams.getGoalStructureBounds().getMax());

        this.playerStructureAABB = new AxisAlignedBB(minPlayerStructurePos, maxPlayerStructurePos);
        this.goalStructureAABB = new AxisAlignedBB(minGoalStructurePos, maxGoalStructurePos);

        assert(!playerStructureAABB.intersectsWith(goalStructureAABB));

        this.structureVolume = volumeOfAABB(playerStructureAABB);
        assert(this.structureVolume == volumeOfAABB(goalStructureAABB));

        this.delta = new Vec3i(goalStructureAABB.minX - playerStructureAABB.minX,
                goalStructureAABB.minY - playerStructureAABB.minY,
                goalStructureAABB.minZ - playerStructureAABB.minZ);

        this.rewardDensity = rscparams.getRewardDensity();

        DrawBlockBasedObjectType blockBasedObjectType = rscparams.getBlockTypeOnCorrectPlacement();

        this.blockTypeOnCorrectPlacement = BlockDrawingHelper.applyModifications(
                MinecraftTypeHelper.ParseBlockType(blockBasedObjectType.getType().value()),
                blockBasedObjectType.getColour(),
                blockBasedObjectType.getFace(),
                blockBasedObjectType.getVariant());

        blockBasedObjectType = rscparams.getBlockTypeOnIncorrectPlacement();

        this.blockTypeOnIncorrectPlacement = BlockDrawingHelper.applyModifications(
                MinecraftTypeHelper.ParseBlockType(blockBasedObjectType.getType().value()),
                blockBasedObjectType.getColour(),
                blockBasedObjectType.getFace(),
                blockBasedObjectType.getVariant());

        this.dimension = rscparams.getDimension();

        return true;
    }
    
    private BlockPos blockPosFromPos(@Nonnull Pos pos) {
        return new BlockPos(pos.getX().intValueExact(), pos.getY().intValueExact(), 
                            pos.getZ().intValueExact());
    }

    private float volumeOfAABB(AxisAlignedBB axisAlignedBB) {
        return (float) (axisAlignedBB.maxX - axisAlignedBB.minX + 1) *
                (float) (axisAlignedBB.maxY - axisAlignedBB.minY + 1) *
                (float) (axisAlignedBB.maxZ - axisAlignedBB.minZ + 1);
    }
 
    /**
     * Get the reward value for the current Minecraft state.
     *
     * @param missionInit the MissionInit object for the currently running mission, 
     *        which may contain parameters for the reward requirements.
     * @return a float determining the current reward signal for the learning agent
     */
    @Override
    public float getReward(MissionInit missionInit) {
        return 0;
    }
 
    /**
     * Called once before the mission starts - use for any necessary initialisation.
     *
     * @param missionInit
     */
    @Override
    public void prepare(MissionInit missionInit) {
        try
        {
            if (MalmoMod.getPropertiesForCurrentThread().containsKey("QuitCode"))
             MalmoMod.getPropertiesForCurrentThread().remove("QuitCode");
        }
        catch (Exception e)
        {
            System.out.println("Failed to get properties.");
        }

        MinecraftForge.EVENT_BUS.register(this);
    }
 
    /**
     * Called once after the mission ends - use for any necessary cleanup.
     */
    @Override
    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
```

 
