// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.MissionHandlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWorldDecorator;
import com.microsoft.Malmo.Schemas.AgentQuitFromReachingPosition;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.BlockOrItemSpec;
import com.microsoft.Malmo.Schemas.BlockType;
import com.microsoft.Malmo.Schemas.Colour;
import com.microsoft.Malmo.Schemas.DrawItem;
import com.microsoft.Malmo.Schemas.MazeBlock;
import com.microsoft.Malmo.Schemas.MazeDecorator;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.ObservationFromSubgoalPositionList;
import com.microsoft.Malmo.Schemas.PointWithToleranceAndDescription;
import com.microsoft.Malmo.Schemas.PosAndDirection;
import com.microsoft.Malmo.Schemas.Variation;
import com.microsoft.Malmo.Utils.BlockDrawingHelper;
import com.microsoft.Malmo.Utils.BlockDrawingHelper.XMLBlockState;
import com.microsoft.Malmo.Utils.MinecraftTypeHelper;

public class MazeDecoratorImplementation extends HandlerBase implements IWorldDecorator
{
    private MazeDecorator mazeParams = null;
    
    // Random number generators for path generation / block choosing:
    private Random pathrand;
    private Random blockrand;

    private XMLBlockState startBlock;
    private XMLBlockState endBlock;
    private XMLBlockState floorBlock;
    private XMLBlockState pathBlock;
    private XMLBlockState optimalPathBlock;
    private XMLBlockState subgoalPathBlock;
    private XMLBlockState gapBlock;
    private XMLBlockState waypointBlock;
    private ItemStack waypointItem;

    private int startHeight;
    private int endHeight;
    private int pathHeight;
    private int optimalPathHeight;
    private int subgoalHeight;
    private int gapHeight;
    private PosAndDirection startPosition = null;
    private AgentQuitFromReachingPosition quitter = null;
    private ObservationFromSubgoalPositionList navigator = null;

    int width;
    int length;
    int gaps;
    int maxPathLength;
    int xOrg;
    int yOrg;
    int zOrg;
    
    // Simple class to keep track of a position:
    private class Cell
    {
        public int x;
        public int z;
        // Used for tracking path:
        public int dist = MazeDecoratorImplementation.this.width * MazeDecoratorImplementation.this.length;
        public boolean isOnOptimalPath = false;
        public boolean isSubgoal = false;
        public boolean isWaypoint = false;
        public boolean isVisited = false;
        public Cell predecessor = null;

        Cell()
        {
            // Initialise to random position:
            this.x = MazeDecoratorImplementation.this.pathrand.nextInt(MazeDecoratorImplementation.this.width);
            this.z = MazeDecoratorImplementation.this.pathrand.nextInt(MazeDecoratorImplementation.this.length);
        }
        Cell(int x, int z)
        {
            this.x = x;
            this.z = z;
        }
    }

    @Override
    public boolean parseParameters(Object params)
    {
        if (params == null || !(params instanceof MazeDecorator))
            return false;
        this.mazeParams = (MazeDecorator)params;
        return true;
    }

    private void initRNGs()
    {
        // Initialise a RNG for this scene:
        long seed = 0;
        if (this.mazeParams.getSeed() == null || this.mazeParams.getSeed().equals("random"))
            seed = System.currentTimeMillis();
        else
            seed = Long.parseLong(this.mazeParams.getSeed());

        this.pathrand = new Random(seed);
        this.blockrand = new Random(seed);

        // Should we initialise a separate RNG for the block types?
        if (this.mazeParams.getMaterialSeed() != null)
        {
            long bseed = 0;
            if (this.mazeParams.getMaterialSeed().equals("random"))
                bseed = System.currentTimeMillis();
            else
                bseed = Long.parseLong(this.mazeParams.getMaterialSeed());
            this.blockrand = new Random(bseed);
        }
    }
    
    private void initBlocksAndHeights()
    {
        this.startBlock = getBlock(this.mazeParams.getStartBlock(), this.blockrand);
        this.endBlock = getBlock(this.mazeParams.getEndBlock(), this.blockrand);
        this.floorBlock = getBlock(this.mazeParams.getFloorBlock(), this.blockrand);
        this.pathBlock = getBlock(this.mazeParams.getPathBlock(), this.blockrand);
        this.optimalPathBlock = this.mazeParams.getOptimalPathBlock() != null ? getBlock(this.mazeParams.getOptimalPathBlock(), this.blockrand) : this.pathBlock;
        this.subgoalPathBlock = this.mazeParams.getSubgoalBlock() != null ? getBlock(this.mazeParams.getSubgoalBlock(), this.blockrand) : this.optimalPathBlock;
        this.gapBlock = getBlock(this.mazeParams.getGapBlock(), this.blockrand);
        if (this.mazeParams.getWaypoints() != null)
        {
            if (this.mazeParams.getWaypoints().getWaypointBlock() != null)
                this.waypointBlock = getBlock(this.mazeParams.getWaypoints().getWaypointBlock(), this.blockrand);
            else
            {
                BlockOrItemSpec bis = this.mazeParams.getWaypoints().getWaypointItem();
                DrawItem di = new DrawItem();
                di.setType(bis.getType().get(this.blockrand.nextInt(bis.getType().size())));
                if (bis.getColour() != null && !bis.getColour().isEmpty())
                    di.setColour(bis.getColour().get(this.blockrand.nextInt(bis.getColour().size())));
                if (bis.getVariant() != null && !bis.getVariant().isEmpty())
                    di.setVariant(bis.getVariant().get(this.blockrand.nextInt(bis.getVariant().size())));
                this.waypointItem = MinecraftTypeHelper.getItemStackFromDrawItem(di);
            }
        }
        
        this.startHeight = getHeight(this.mazeParams.getStartBlock(), this.pathrand);
        this.endHeight = getHeight(this.mazeParams.getEndBlock(), this.pathrand);
        this.pathHeight = getHeight(this.mazeParams.getPathBlock(), this.pathrand);
        this.optimalPathHeight = this.mazeParams.getOptimalPathBlock() != null ? getHeight(this.mazeParams.getOptimalPathBlock(), this.pathrand) : this.pathHeight;
        this.subgoalHeight = this.mazeParams.getSubgoalBlock() != null ? getHeight(this.mazeParams.getSubgoalBlock(), this.pathrand) : this.optimalPathHeight;
        this.gapHeight = getHeight(this.mazeParams.getGapBlock(), this.pathrand);
    }

    private void initDimensions()
    {
        // Get dimensions of maze:
        this.width = this.mazeParams.getSizeAndPosition().getWidth();
        this.length = this.mazeParams.getSizeAndPosition().getLength();
        int totalCells = width * length;

        // Figure out how many gaps we need to create:
        float gapProbability = this.mazeParams.getGapProbability().getValue().floatValue();
        float variance = this.mazeParams.getGapProbability().getVariance().floatValue();
        gapProbability += (this.pathrand.nextFloat() - 0.5) * 2 * variance;
        gapProbability = gapProbability < 0 ? 0 : (gapProbability > 1 ? 1 : gapProbability);
        this.gaps = (int)((float)(this.width * this.length) * gapProbability);

        // Check that what has been requested is actually possible:
        this.maxPathLength = totalCells - gaps;
        if (this.maxPathLength < 2)  // A path from a start cell to a distinct end cell needs to contain at least two cells, clearly!
        {
            System.out.println("Error - Impossible to construct a path across a " + width + " by " + length + " field with " + gaps + " missing cells. Changing requirements.");
            // Adjust the number of gaps we will create so that a path is still possible:
            this.maxPathLength = length;
            this.gaps = totalCells - maxPathLength;
        }

        // Get origin information:
        this.xOrg = this.mazeParams.getSizeAndPosition().getXOrigin();
        this.yOrg = this.mazeParams.getSizeAndPosition().getYOrigin();
        this.zOrg = this.mazeParams.getSizeAndPosition().getZOrigin();
    }
    
    private Cell createStartCell()
    {
        // Choose a start position at random.
        // If the end cell is to be attached to the wall, we need to make sure the start cell is within range.
        int offset = this.mazeParams.getEndBlock().isFixedToEdge() ? Math.max(0, this.length - this.maxPathLength) : 0;
        int startx = this.pathrand.nextInt(this.width);
        int startz = offset + this.pathrand.nextInt(this.length - offset);
        Cell start = new Cell(startx, startz);

        // Should the start be fixed to the wall?
        if (this.mazeParams.getStartBlock().isFixedToEdge())
        {
            start.z = 0;
            if (offset != 0)
            {
                System.out.println("Can't fix both start and end to the edges - not enough blocks available to complete the path. Changing requirements.");
                this.maxPathLength = this.length;
                this.gaps = (this.width * this.length) - this.maxPathLength;
            }
        }
        return start;
    }
    
    private Cell createEndCell(Cell start, boolean allowDiags)
    {
        // Choose an end position at random.
        Cell end = null;
        // If the maxPathLength is great enough that we can reach any cell from the start point, then simply choose at random:
        if (this.maxPathLength > this.length + this.width)
        {
            end = new Cell(start.x, start.z);
            while (end.x == start.x && end.z == start.z)
            {
                end = new Cell();   // Initialised to random position.
                if (this.mazeParams.getEndBlock().isFixedToEdge())
                    end.z = this.length - 1;
            }
        }
        else
        {
            // Otherwise, we need to choose a cell we can actually get to.
            // Pick a rectangular area which is large enough to contain all possible end cells,
            // then use rejection sampling to pick a random end point within that area.
            int minx = Math.max(0,  start.x - (this.maxPathLength - 1));
            int maxx = Math.min(this.width - 1,  start.x + (this.maxPathLength - 1));
            int minz = Math.max(0, start.z - (this.maxPathLength - 1));
            int maxz = Math.min(this.length - 1, start.z + (this.maxPathLength - 1));
            while (end == null)
            {
                int x = this.pathrand.nextInt(1 + maxx - minx) + minx;
                int z = this.pathrand.nextInt(1 + maxz - minz) + minz;
                if (this.mazeParams.getEndBlock().isFixedToEdge())
                    z = this.length - 1;
                if (distBetweenPoints(x, z, start.x, start.z, allowDiags) <= this.maxPathLength)
                {
                    end = new Cell(x, z);
                }
            }
        }
        return end;
    }
    
    private void addWaypoints(Cell[] grid, Cell start, Cell end, boolean allowDiags)
    {
        // Find all the reachable cells, and select waypoints from them randomly.
        // We could try to maintain this data dynamically as the path is built, but
        // it's much simpler to do it separately now, and it's unlikely to be a performance problem.

        // Initialise graph grid with neutral settings:
        for (int i = 0; i < this.width * this.length; i++)
        {
            if (grid[i] != null)
            {
                grid[i].isVisited = false;
                grid[i].isWaypoint = false;
            }
        }

        // Initialise a vector to enable us to choose random cells:
        ArrayList<Cell> candidates = new ArrayList<Cell>();

        // Now find all cells that are reachable from start:
        ArrayList<Cell> queue = new ArrayList<Cell>();
        queue.add(start);
        while (!queue.isEmpty())
        {
            Cell home = queue.remove(0);
            Cell[] neighbours = new Cell[8];
            int x = home.x;
            int z = home.z;
            populateNeighbours(grid, neighbours, x, z, allowDiags);
            
            for (int n = 0; n < 8; n++)
            {
                if (neighbours[n] != null && !neighbours[n].isVisited && neighbours[n] != end)
                {
                    neighbours[n].isVisited = true;
                    candidates.add(neighbours[n]);
                    queue.add(neighbours[n]);
                }
            }
        }
        
        // All candidates are now in the candidates vector - this should not include the start or end cells.
        // Choose n from the vector randomly:
        int remaining = candidates.size();
        for (int i = 0; i < this.mazeParams.getWaypoints().getQuantity() && remaining > 0; i++)
        {
            int chosen = this.pathrand.nextInt(remaining) + i;
            Cell chosenCell = candidates.get(chosen);
            chosenCell.isWaypoint = true;
            candidates.set(chosen, candidates.get(i));
            candidates.set(i, chosenCell);
            remaining--;
        }
    }

    private void buildPath(Cell[] grid, Cell start, Cell end, boolean allowDiags)
    {
        // Initialise a vector to enable us to choose random cells:
        int[] order = new int[this.width * this.length];
        for (int i = 0; i < this.width * this.length; i++)
            order[i] = i;
        int nextRandomSlot = 0;
        
        boolean refreshPath = true; // Make sure we create the optimal path, even if we don't need to remove any blocks.

        // Iteratively remove cells from the grid, whilst ensuring a path of <= maxPathLength still exists between start and end:
        while (this.gaps > 0 || (this.gaps == 0 && refreshPath))
        {
            Cell targetCell = null; // Cell to consider removing.
            int target = -1;
            if (this.gaps > 0)  // Still need to remove some blocks.
            {
                // Choose random cell to remove:
                do
                {
                    // Get next untried cell (in random order).
                    int targetSlot = nextRandomSlot + this.pathrand.nextInt((this.width * this.length) - nextRandomSlot);
                    target = order[targetSlot];
                    order[targetSlot] = order[nextRandomSlot];
                    order[nextRandomSlot] = target;
                    nextRandomSlot++;
                    targetCell = grid[target];
                }
                while (targetCell == start || targetCell == end);   // Don't remove the start or end blocks!
    
                refreshPath |= targetCell.isOnOptimalPath;  // If cell isn't on the optimal path, we don't need to worry what effect its removal will have.
                grid[target] = null;
            }
            
            if (refreshPath)
            {
                // Now, if this cell is removed, can we still construct a valid path?
                // Perform a simple graph search to find out.
                // Initialise graph grid with neutral settings:
                for (int i = 0; i < this.width * this.length; i++)
                {
                    if (grid[i] != null)
                    {
                        grid[i].dist = this.width * this.length;
                        grid[i].isOnOptimalPath = false;
                        grid[i].predecessor = null;
                    }
                }
                start.dist = 0;
                start.isOnOptimalPath = true;
                end.isOnOptimalPath = true;
    
                // Find optimal path from start to end:
                ArrayList<Cell> queue = new ArrayList<Cell>();
                queue.add(start);
                while (!queue.isEmpty() && queue.get(0) != end)
                {
                    Cell home = queue.remove(0);
                    Cell[] neighbours = new Cell[8];
                    int x = home.x;
                    int z = home.z;
                    populateNeighbours(grid, neighbours, x, z, allowDiags);
                    
                    for (int n = 0; n < 8; n++)
                    {
                        if (neighbours[n] != null && neighbours[n].dist > home.dist + 1)
                        {
                            queue.add(neighbours[n]);
                            neighbours[n].dist = home.dist + 1;
                            neighbours[n].predecessor = home;
                        }
                    }
                }
                int pathLength = end.dist + 1;  // +1 for the start block.
    
                if (pathLength <= this.maxPathLength)
                {
                    // We have a valid path.
                    // Walk backwards to build it.
                    Cell c = end;
                    while (c != start)
                    {
                        c.isOnOptimalPath = true;
                        c = c.predecessor;
                    }
                    // All good, so mark as successful and keep going.
                    this.gaps--;
                    refreshPath = false;    // No need to recalculate until next time we remove a block on the critical path.
                }
                else if (this.gaps > 0)
                {
                    // Can't remove this cell!
                    // Put it back:
                    grid[target] = targetCell;
                }
            }
            else
            {
                // Didn't need to recalculate anything.
                this.gaps--;
            }
        }
    }

    private void populateNeighbours(Cell[] grid, Cell[] neighbours, int x, int z, boolean allowDiags)
    {
        neighbours[0] = (x > 0) ? grid[(x-1) + z*this.width] : null;
        neighbours[1] = (x < this.width-1) ? grid[(x+1) + z*this.width] : null;
        neighbours[2] = (z > 0) ? grid[x + (z-1)*this.width] : null;
        neighbours[3] = (z < this.length-1) ? grid[x + (z+1)*this.width] : null;
        neighbours[4] = (allowDiags && x > 0 && z < this.length-1) ? grid[(x-1) + (z+1)*this.width] : null;
        neighbours[5] = (allowDiags && x > 0 && z > 0) ? grid[(x-1) + (z-1)*this.width] : null;
        neighbours[6] = (allowDiags && x < this.width-1 && z < this.length-1) ? grid[(x+1) + (z+1)*this.width] : null;
        neighbours[7] = (allowDiags && x < this.width-1 && z > 0) ? grid[(x+1) + (z-1)*this.width] : null;
    }

    private void findSubgoals(Cell[] grid, Cell start, Cell end)
    {
        System.out.println("Attempting to find subgoals...");
        
        // Attempt to find subgoals - this represents the "smoothed" optimal path.
        // It uses something akin to line-of-sight smoothing, to reduce the rectilinear path into something a bit more
        // like what a human agent would use.
        
        // First, copy the optimal path into an array:
        ArrayList<Cell> opath = new ArrayList<Cell>();
        Cell cur = end;
        while (cur != start)
        {
            opath.add(0, cur);
            cur = cur.predecessor;
        }
        opath.add(0, start);
        
        // Now walk the path, removing any points that aren't required.
        // For example, if the agent can walk from A directly to C, we can safely remove point B.
        // This will help remove some of the 90 degree turns - eg instead of walking one square north, then one square east,
        // the agent could just walk directly north-east.
        int startindex = 0;
        int removalcandidateindex = 1;
        int destindex = 2;
        if (opath.size() > 2)
        {
            // Walk the path, removing any points we can:
            while (destindex != opath.size())
            {
                Cell smoothstart = opath.get(startindex);
                Cell smoothremovalcandidate = opath.get(removalcandidateindex);
                Cell smoothdest = opath.get(destindex);
    
                // Traverse the shortest line from smoothstart to smoothdest looking for collisions.
                // If there are none, we can safely remove the removal candidate.
                double xa = smoothstart.x + 0.5;
                double za = smoothstart.z + 0.5;
                double xb = smoothdest.x + 0.5;
                double zb = smoothdest.z + 0.5;
                double dist = Math.sqrt((xb-xa)*(xb-xa) + (zb-za)*(zb-za));
                int samplepoints = (int)Math.ceil(dist * 5);
                boolean walkable = true;
                for (int sample = 0; sample < samplepoints && walkable; sample++)
                {
                    double f = (double)sample / (double)samplepoints;
                    double xs = xa + (xb-xa) * f;
                    double zs = za + (zb-za) * f;
                    int cellx = (int)Math.floor(xs);
                    int cellz = (int)Math.floor(zs);
                    // Is this cell blocked?
                    int cellindex = cellx + cellz * width;
                    if (cellindex < 0 || cellindex >= grid.length || grid[cellindex] == null)
                        walkable = false;
                    if (walkable && gapHeight > optimalPathHeight && !gapBlock.getBlock().getDefaultState().equals(Blocks.AIR.getDefaultState()))
                    {
                        // The "gaps" are in fact walls, so we need to be a bit more conservative with our path, since the
                        // player has a width of 0.4 cells. We do this in a very unsophisticated, brute-force manor by testing
                        // the four corner points of the square the player would occupy if he was standing centrally in the cell.
                        int lowerx = (int)Math.floor(xs-0.2);
                        int upperx = (int)Math.floor(xs+0.2);
                        int lowerz = (int)Math.floor(zs-0.2);
                        int upperz = (int)Math.floor(zs+0.2);
                        int[] cellsToTest = new int[4];
                        // Speed is not really an issue here so we don't worry about testing the same cells multiple times.
                        cellsToTest[0] = lowerx + lowerz * width;
                        cellsToTest[1] = lowerx + upperz * width;
                        cellsToTest[2] = upperx + lowerz * width;
                        cellsToTest[3] = upperx + upperz * width;
                        // Are these cells blocked?
                        for (int i = 0; i < 4 && walkable; i++)
                        {
                            int ctt = cellsToTest[i];
                            if (ctt < 0 || ctt >= grid.length || grid[ctt] == null)
                                walkable = false;
                        }
                    }
                }
                if (walkable)
                {
                    // Can safely remove the candidate point - start->dest is walkable without it.
                    opath.remove(removalcandidateindex);   // Will effectively increment destindex and smoothremovalindex.
                }
                else
                {
                    // We need the candidate point, so set that as our new start index.
                    startindex = removalcandidateindex;
                    removalcandidateindex = startindex + 1;
                    destindex = startindex + 2;
                    smoothremovalcandidate.isSubgoal = true;
                }
            }
        }

        if (this.mazeParams.getAddNavigationObservations() != null)
        {
            // Add the subgoals to an observation producer:
            this.navigator = new ObservationFromSubgoalPositionList();
            int scale = this.mazeParams.getSizeAndPosition().getScale();
            double y = 1 + this.optimalPathHeight + this.yOrg;
            int i = 1;
            for (Cell cell : opath)
            {
                double x = scale * (cell.x + 0.5) + this.xOrg;
                double z = scale * (cell.z + 0.5) + this.zOrg;
                PointWithToleranceAndDescription ptd = new PointWithToleranceAndDescription();
                ptd.setTolerance(new BigDecimal(1.0));
                ptd.setX(new BigDecimal(x));
                ptd.setY(new BigDecimal(y));
                ptd.setZ(new BigDecimal(z));
                ptd.setDescription("MazeSubpoint_" + String.valueOf(i));
                i++;
                this.navigator.getPoint().add(ptd);
            }
            System.out.println("Found subgoals.");
        }
    }

    private void placeBlocks(World world, Cell[] grid, Cell start, Cell end)
    {
        BlockDrawingHelper drawContext = new BlockDrawingHelper();
        drawContext.beginDrawing(world);

        int scale = this.mazeParams.getSizeAndPosition().getScale();
        // First remove any entities lying around in our area:
        drawContext.clearEntities(world, this.xOrg, this.yOrg, this.zOrg, this.xOrg + this.width * scale, this.yOrg + this.mazeParams.getSizeAndPosition().getHeight(), this.zOrg + this.length * scale);
        
        // Clear a volume of air, lay a carpet, and put the random pavement over it:
        for (int x = 0; x < this.width * scale; x++)
        {
            for (int z = 0; z < this.length * scale; z++)
            {
                for (int y = 0; y < this.mazeParams.getSizeAndPosition().getHeight(); y++)
                {
                    world.setBlockToAir(new BlockPos(x + this.xOrg, y + this.yOrg, z + this.zOrg));
                }
                BlockPos bp = new BlockPos(x + this.xOrg, this.yOrg, z + this.zOrg);
                drawContext.setBlockState(world, bp, this.floorBlock);
                Cell c = grid[(x/scale) + ((z/scale) * this.width)];
                XMLBlockState bs = (c == null) ? this.gapBlock : (c.isOnOptimalPath ? this.optimalPathBlock : this.pathBlock);
                int h = (c == null) ? this.gapHeight : (c.isOnOptimalPath ? this.optimalPathHeight : this.pathHeight);
                if (c != null && c.isSubgoal)
                {
                    bs = this.subgoalPathBlock;
                    h = this.subgoalHeight;
                }
                if (c != null && c.isWaypoint && x % scale == scale/2 && z % scale == scale/2)
                {
                    if (this.mazeParams.getWaypoints().getWaypointBlock() != null)
                    {
                        bs = this.waypointBlock;
                        h = this.pathHeight;
                    }
                    else if (this.waypointItem != null)
                    {
                        // Place a waypoint item here:
                        int offset = 0;//(scale % 2 == 0) ? 1 : 0;
                        drawContext.placeItem(this.waypointItem.copy(), new BlockPos(x + this.xOrg + offset, this.yOrg + h + 1, z + this.zOrg + offset), world, (scale % 2 == 1));
                    }
                }
                if (c != null && c == start)
                {
                    h = this.startHeight;
                    bs = this.startBlock;
                }
                if (c != null && c == end)
                {
                    h = this.endHeight;
                    bs = this.endBlock;
                }

                for (int y = 1; y <= h; y++)
                {
                    BlockPos pos = new BlockPos(x + this.xOrg, y + this.yOrg, z + this.zOrg);
                    drawContext.setBlockState(world, pos, bs);
                }
            }
        }
    }

    private void recordStartAndEndPoints(Cell start, Cell end, MissionInit missionInit)
    {
        // TODO: how do we set the goal position, now it no longer has a declaration in the Mission xml?
        int scale = this.mazeParams.getSizeAndPosition().getScale();

        // Position the start point:
        PosAndDirection p = new PosAndDirection();
        p.setX(new BigDecimal(scale * (start.x + 0.5) + this.xOrg));
        p.setY(new BigDecimal(1 + this.yOrg + this.startHeight));
        p.setZ(new BigDecimal(scale * (start.z + 0.5) + this.zOrg));
        this.startPosition = p;
        // TODO - for the moment, force all players to being at the maze start point - but this needs to be optional.
        for (AgentSection as : missionInit.getMission().getAgentSection())
        {
            p.setPitch(as.getAgentStart().getPlacement().getPitch());
            p.setYaw(as.getAgentStart().getPlacement().getYaw());
            as.getAgentStart().setPlacement(p);
        }

        if (this.mazeParams.getAddQuitProducer() != null)
        {
            String desc = this.mazeParams.getAddQuitProducer().getDescription();
            this.quitter = new AgentQuitFromReachingPosition();
            PointWithToleranceAndDescription endpoint = new PointWithToleranceAndDescription();
            endpoint.setDescription(desc);
            endpoint.setTolerance(new BigDecimal(0.5 + scale/2.0));

            double endX = scale * (end.x + 0.5) + this.xOrg;
            double endY = 1 + this.optimalPathHeight + this.yOrg;   // Assuming we approach on the optimal path, need the height of the goal to be reachable.
            double endZ = scale * (end.z + 0.5) + this.zOrg;
            endpoint.setX(new BigDecimal(endX));
            endpoint.setY(new BigDecimal(endY));
            endpoint.setZ(new BigDecimal(endZ));
            this.quitter.getMarker().add(endpoint);
        }
    }

    @Override
    public void buildOnWorld(MissionInit missionInit, World world)
    {
        // Set up various parameters according to the XML specs:
        initRNGs();
        initBlocksAndHeights();
        initDimensions();

        // 8-connected or 4-connected?
        boolean allowDiags = this.mazeParams.isAllowDiagonalMovement();

        // Create the end points:
        Cell start = createStartCell();
        Cell end = createEndCell(start, allowDiags);
        
        // Construct the grid graph - a flat array of width x height cells:
        Cell[] grid = new Cell[this.width * this.length];
        for (int i = 0; i < this.width * this.length; i++)
        {
            int x = i % this.width;
            int z = i / this.width;
            grid[i] = new Cell(x, z);
        }
        
        // Put our start and end cells into the grid:
        grid[start.x + start.z*this.width] = start;
        grid[end.x + end.z*this.width] = end;

        // Create the maze:
        buildPath(grid, start, end, allowDiags);
        
        if (this.mazeParams.getWaypoints() != null)
            addWaypoints(grid, start, end, allowDiags);

        // Now split into subgoals:
        findSubgoals(grid, start, end);

        // Now build the actual Minecraft world:
        placeBlocks(world, grid, start, end);

        // Finally, write the start and goal points into the MissionInit data structure for the other MissionHandlers to use:
        recordStartAndEndPoints(start, end, missionInit);
    }

    private int getHeight(MazeBlock mblock, Random rand)
    {
        int h = mblock.getHeight();
        if (mblock.getHeightVariance() != 0)
        {
            // Choose a random number that is within h +/- variance.
            // Eg if variance is 1 and height is 2, we should return either 1, 2 or 3.
            h += rand.nextInt(2 * mblock.getHeightVariance() + 1) - mblock.getHeightVariance();
        }
        return h;
    }
    
    private XMLBlockState getBlock(MazeBlock mblock, Random rand)
    {
        BlockType blockName = chooseBlock(mblock.getType(), rand);
        Colour blockCol = chooseColour(mblock.getColour(), rand);
        Variation blockVar = chooseVariant(mblock.getVariant(), rand);
        return new XMLBlockState(blockName, blockCol, null, blockVar);
    }

    private BlockType chooseBlock(List<BlockType> types, Random r)
    {
        if (types == null || types.size() == 0)
            return BlockType.AIR;
        return types.get(r.nextInt(types.size()));
    }

    private Colour chooseColour(List<Colour> colours, Random r)
    {
        if (colours == null || colours.size() == 0)
            return null;
        return colours.get(r.nextInt(colours.size()));
    }
    
    private Variation chooseVariant(List<Variation> vars, Random r)
    {
        if (vars == null || vars.size() == 0)
            return null;
        return vars.get(r.nextInt(vars.size()));
    }
    
    /** Calculate the number of cells on the shortest path between (x1,z1) and (x2,z2)
     * @param x1
     * @param z1
     * @param x2
     * @param z2
     * @param bAllowDiags Whether the cells are 8-connected or 4-connected.
     * @return The number of cells on the shortest path, including start and end cells.
     */
    private int distBetweenPoints(int x1, int z1, int x2, int z2, boolean bAllowDiags)
    {
        // Total cells is the sum of the distances we need to travel along the x and the z axes, plus one for the end cell.
        int w = Math.abs(x2 - x1);
        int h = Math.abs(z2 - z1);
        if (bAllowDiags)
        {
            // Diagonal movement allows us ignore the shorter of w and h:
            if (w < h)
                w = 0;
            else
                h = 0;
        }
        return w + h + 1;
    }
    
    @Override
    public void update(World world) {}

    @Override
    public boolean getExtraAgentHandlersAndData(List<Object> handlers, Map<String, String> data)
    {
        boolean added = false;
        if (this.quitter != null)
        {
            handlers.add(this.quitter);
            added = true;
        }
        if (this.navigator != null)
        {
            handlers.add(this.navigator);
            added = true;
        }

        // Also add our new start data:
        Float x = this.startPosition.getX().floatValue();
        Float y = this.startPosition.getY().floatValue();
        Float z = this.startPosition.getZ().floatValue();
        String posString = x.toString() + ":" + y.toString() + ":" + z.toString();
        data.put("startPosition", posString);

        return added;
    }

    @Override
    public void prepare(MissionInit missionInit)
    {
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public boolean targetedUpdate(String nextAgentName)
    {
        return false;   // Does nothing.
    }

    @Override
    public void getTurnParticipants(ArrayList<String> participants, ArrayList<Integer> participantSlots)
    {
        // Does nothing.
    }
}
