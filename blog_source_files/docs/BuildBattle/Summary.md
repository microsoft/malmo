
### Logging the initial similarity and finalizing the Reward Handler

We still need to calculate the initial similarity between the two
structures. The idea is that some portion of the player structure
can be given when the mission starts as a hint, say 80%. The similarity
will then be 0.8 times the structure volume. The code for calculating
this, with a simple triple nested for-loop, is given below:

```
void logInitialSimilarity( World w ) {

    // No need to calculate the initial similarity
    if(rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED || 
       rewardDensity == RewardDensityForBuildAndBreak.MISSION_END) {

        Integer numExactMatchBlocks = 0;

        for (int x = (int) playerStructureAABB.minX; x <= playerStructureAABB.maxX; x++) {
            for (int y = (int) playerStructureAABB.minY; y <= playerStructureAABB.maxY; y++) {
                for (int z = (int) playerStructureAABB.minZ; z <= playerStructureAABB.maxZ; z++) {
                    BlockPos playerStructurePos = new BlockPos(x, y, z);
                    BlockPos goalStructurePos = playerStructurePos.add(delta);

                    if (w.getBlockState(playerStructurePos).equals(goalStructurePos))
                        numExactMatchBlocks++;
                }
            }
        }

        reward =  (float) numExactMatchBlocks / structureVolume;
        bootstrapReward = reward;
    }
}
```

Finally, we can write the function that returns rewards that get sent to
the agent:

```
@Override
public void getReward(MissionInit missionInit, MultidimensionalReward multidimReward) {
    if(rewardDensity == RewardDensityForBuildAndBreak.PER_BLOCK) {
        Float f = this.reward;
        this.reward = 0.0F;
        multidimReward.add( this.dimension, f );
    }

    else if(rewardDensity == RewardDensityForBuildAndBreak.ACCUMULATED) {
        Float f = reward;
        reward = bootstrapReward;
        multidimReward.add( this.dimension, f );
    }

    else  { // rewardDensity == RewardDensityForBuildAndBreak.MISSION_END
        try {
            // Return a reward only if the QuitCode is present.
            Hashtable<String, Object> properties = MalmoMod.getPropertiesForCurrentThread();
            if (properties.containsKey("QuitCode"))
                multidimReward.add( this.dimension, this.reward );
            else
                multidimReward.add( this.dimension, 0 );
        }

        catch (Exception e)
        {
            System.out.print("Caught exception getting properties while searching for QuitCode!");
            // FMLCommonHandler.instance().exitJava(-1, false);
            // Note: The above commented line of code is a simple way to quit Minecraft on errors. 
            // For the purposes of this caught exception, notifying the user with 
            // a print output suffices.
        }
    }
}
```

### Conclusion
Hopefully everything worked all right and you enjoyed constructing the build battle task.
Please do take a look at and review the resources provided as necessary.
Other than that, what's left is now arguably the more fun part: to create missions using the Reward Handler we have added to the Mod and to create AI agents that can solve it.
For the former, the sample missions provided will help, but it essentially boils down to writing some XML. As for the latter, it's really an open question! Some instances of the build battle task we have defined here are easy (say small open 2-dimensional structures.) In general, this task is somewhat easy for humans (try it yourself by entering into Human mode with the Enter key once you have started a mission.) However, it is not so easy and tractable for current AI and reinforcement learning agents! The idea of the platform is indeed this, to be able to create tasks with a broad spectrum of difficulties and by working at it's forefront, advance the state of AI.
