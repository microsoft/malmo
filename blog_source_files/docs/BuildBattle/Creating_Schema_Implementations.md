### Adding a Java Class 

If you have not already, open the Project Malm&ouml; project in your 
IDE. I use IntelliJ, and so will be presenting the steps for it, but the 
steps in Eclipse should be about the same. 

Now, we are going to add a new Java class. In IntelliJ, look for the 
folder ```MissionHandlers```. Then, right-click on the folder, 
select ```New``` and then ```Java Class```. When a pop-up window opens,
type the name as ```RewardForStructureCopyingImplementation```. 
The name must be exactly this since the function 
```createHandlerFromParams``` in the class ```MissionBehavior``` looks 
for a class named ```RewardForStructureCopying``` (the Schema name) 
followed by ```Implementation```. Try to verify this by opening the 
```MissionBehavior``` class. To do so, you can use one of the many nifty 
tools IntelliJ and IDEs in general provide: Search. In IntelliJ, you 
can click the shift button twice to bring up Search.

### Subclassing and Interfaces 

Open the ```RewardForStructureConstruction``` class again. 
At this point, have a look at the code for some of the other Reward 
Handlers under ```MissionHandlers```. There are a few things
that are worth noting. Firstly, all of them extend or in other words 
subclass```HandlerBase``` (meaning some fields or functions are 
inherited from the superclass ```HandlerBase``` while others must be
implemented or "overriden" so as to change the functionality of some 
methods). Also, all of the Reward Handlers implement 
```IRewardProducer```, an interface. This means that all the Reward 
Handlers follow some protocol and can be treated in the same manner 
given that they implement the methods necessary according to the 
Interface.

With the above said, change the ```RewardForStructureCopying``` class 
so that you make the two changes. This means that you will now have 
something like this: 

```Java
public class RewardForStructureCopyingImplementation extends HandlerBase implements IRewardProducer{

}
```

### Generate Superclass and Interface functions 

You might now notice that the IDE complains about some errors. These
errors are due to the fact that we have not yet implemented some 
functions we have guaranteed to based on our class definition (in 
particular the extends and implements bit).

We will now add some functions from the classes we have inherited or 
the interfaces we have said we will implement. In IntelliJ, right-click
anywhere inside the brackets enclosing the class definitions and fields.
Now, choose ```Generate...``` followed by ```Override Methods...```. 
Alternatively, place your cursor between the brackets and hit 
```Ctrl + o```. Now, choose 
```parseParameters(params:Object):boolean``` under 
```com.microsoft.Malmo.MissionHandlers.HandlerBase```. Next, 
hold the ```Ctrl``` key and without leaving the key select 
```getReward(missionInit:MissionInit):float```, 
```prepare(missionInit:MissionInit):void``` and 
```cleanup():void```, all of which are under 
```com.microsoft.Malmo.MissionHandlerInterfaces.IRewardProducer```. 
You may leave the ```Ctrl``` key and now using the using the mouse, 
if they are not already checked, 
click on ```Copy JavaDoc``` and ```Insert @Override```. 

Note: the ```Ctrl``` key may not work if you are on a Mac for example. 
      In this case, just choose the above methods one by one by 
      repeating the steps.

You should now have something like this: 

```java
public class RewardForStructureConstructionImplementation extends HandlerBase implements IRewardProducer {
    /**
     * Attempt to parse the given object as a set of parameters for this handler.
     *
     * @param params the parameter block to parse
     * @return true if the object made sense for this handler; false otherwise.
     */
    @Override
    public boolean parseParameters(Object params) {
        return super.parseParameters(params);
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
 
    }
 
    /**
     * Called once after the mission ends - use for any necessary cleanup.
     */
    @Override
    public void cleanup() {
 
    }
}
```

