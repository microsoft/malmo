### Block Place Event 

The first event handler we will write is the block place event handler.
We begin with the following code snippet: 

```java
@SubscribeEvent
public void onBlockPlacement(BlockEvent.PlaceEvent e) {
    
}
```

A few things to note: 

 * All Event Handlers must be declared public for 
    Forge to be able to access.
 * The annotation ```@SubscribeEvent``` should be used for registering
    a method as an event handler.
 * The parameter of the event handler is crucial. Forge uses the type
    of the parameter to infer what event the event handler wants a hook
    into. In our case, we'd like a hook into the event where a block is
    placed.
    
We now grab the ```World```, which allows for querying blocks and
entities, as well as a ```BlockPos``` denoting the position where a 
block is being placed. 

```java
@SubscribeEvent
public void onBlockPlacement(BlockEvent.PlaceEvent e) {

        World w = e.world;
        BlockPos pos = e.pos;
}
```

Now, we define another helper function which tells us whether a block 
position, taken as a 3-D Vector, is within an Axis Aligned Bound Box: 

```java
public boolean pointInAABB(AxisAlignedBB aabb, Vec3 vec) {
        return (aabb.minX <= vec.xCoord && aabb.maxX >= vec.xCoord &&
                aabb.minY <= vec.yCoord && aabb.maxY >= vec.yCoord &&
                aabb.minZ <= vec.zCoord && aabb.maxZ >= vec.zCoord);
}
```

We use the above function to cancel block place events that have the 
block position as outside the player structure boundary: 

```java
@SubscribeEvent
public void onBlockPlacement(BlockEvent.PlaceEvent e) {

        World w = e.world;
        BlockPos pos = e.pos;
        
        if(!pointInAABB(playerStructureAABB, new Vec3(pos.getX(), pos.getY(), pos.getZ()))) {
            e.setCanceled(true);
            return;
        }
}
```

Next, we find the corresponding goal structure block position and 
calculate the reward to get the following. Note below that some Java 
code comments, denoted with ```//```, have been added for a better 
understanding.

```java
@SubscribeEvent
public void onBlockPlacement(BlockEvent.PlaceEvent e) {

    World w = e.world;
    BlockPos pos = e.pos;

    // Cancel the build event if the player is trying to place a
    // block outisde the player structure boundary.
    if(!pointInAABB(playerStructureAABB, new Vec3(pos.getX(), pos.getY(), pos.getZ()))) {
        e.setCanceled(true);
        return;
    }

    BlockPos goalPos = pos.add(delta);

    // This block was air, and a correct block has been placed (Positive Reward).
    if (w.getBlockState(pos).equals(w.getBlockState(goalPos))) {
    
        // When a correct block is placed, change the state of both the 
        // block being placed and the corresponding goal structure block to 
        // blockTypeOnCorrectPlacement
        if(blockTypeOnCorrectPlacement != null) {
        
            // blockStateSave store the current goal block state before it 
            // changes to the blockTypeOnCurrentPlacement. This will be used 
            // when a player tries to break the block being placed now to 
            // change the corresponding goal block back to what it was.
            blockStateSave.put(goalPos, w.getBlockState(goalPos));

            w.setBlockState(pos, blockTypeOnCorrectPlacement);
            w.setBlockState(goalPos, blockTypeOnCorrectPlacement);
        }
    
        // If the Reward Density is PER_BLOCK or MISSION_END, add the 
        // delta change in exact similarity (1 / Structure Volume) 
        if(rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK || 
           rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
            reward += 1 .0F/ structureVolume;
            
        // Else, the reward density is ACCUMULATED. Add the total similarity to the reward. 
        // Store the total similarity to bootstrapReward.
        else if(rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED) {
            reward = reward + (reward + 1.0F / structureVolume);
            bootstrapReward += 1.0F / structureVolume;
        }
    }

    else {
        // 1. Case where both player structure blocks and goal structure blocks were Air,
        // no need to place a block (Reward Penalty).
        // 2. Case where the block being placed is not the same as the goal structure block.
        if(rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK || 
           rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
                reward -= 1 / structureVolume;
        else if(rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED) {
                reward = (reward - (1.0F / structureVolume)) / 2.0F;
                bootstrapReward -= 1.0F / structureVolume;
        }

        if(blockTypeOnIncorrectPlacement != null) {
            w.setBlockState(pos, blockTypeOnIncorrectPlacement);
        }
    }
}
```


### Block Break Event 

The second event handler we will write is very similar, the code 
for which is given below.

```
@SubscribeEvent
public void onBlockBreak(BlockEvent.BreakEvent e) {
    World w = e.world;
    BlockPos pos = e.pos;

    if(!pointInAABB(playerStructureAABB, new Vec3(pos.getX(), pos.getY(), pos.getZ()))) {
        e.setCanceled(true);
        return;
    }

    BlockPos goalPos = pos.add(delta);

    // Case where the block states matched, no need to break 
    // the player structure block (Reward Penalty).
    if (w.getBlockState(pos).equals(w.getBlockState(goalPos))) {
        if(rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK || 
           rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
            reward -= 1 / structureVolume;
        else if(rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED) {
            reward = (reward - (1 / structureVolume)) / 2;
            bootstrapReward -= 1 / structureVolume;
        }

        if(blockTypeOnCorrectPlacement != null) {
            w.setBlockState(goalPos, blockStateSave.get(goalPos));
        }
    }

    else
    {
        // Goal Structure block is an Air Block or is a different block than the 
        // player structure block. Breaking increases similarity (Positive Reward).
        if(w.isAirBlock(goalPos)) {
            if (rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK || 
                rewardDensity == RewardDensityForBuildAndBreak.MISSION_END)
                reward += 1 / structureVolume;
            else if (rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED) {
                reward = reward + (reward + 1 / structureVolume);
                bootstrapReward += 1 / structureVolume;
            }
        }
    }
}
```
