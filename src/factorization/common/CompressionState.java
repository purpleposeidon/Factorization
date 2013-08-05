package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.notify.Notify;

public class CompressionState {
    TileEntityCompressionCrafter start, root, otherEdge;
    ForgeDirection up, right, otherEdgeRight;
    CellInfo[] cells = new CellInfo[9];
    HashSet<TileEntityCompressionCrafter> foundWalls = new HashSet(3*4);
    int height, width;
    
    boolean errored;
    
    //Clears the fields
    void reset() {
        start = root = otherEdge = null;
        up = right = ForgeDirection.UNKNOWN;
        foundWalls.clear();
        height = width = -1;
        Arrays.fill(cells, null);
        errored = false;
    }
    
    void error(TileEntityCompressionCrafter at, String msg) {
        Notify.send(new Coord(at), msg);
        errored = true;
    }
    
    void unknownError(TileEntityCompressionCrafter at, String msg) {
        if (errored) return;
        Notify.send(new Coord(at), msg);
        errored = true;
    }
    
    //Return true if the passed in CompACT is valid
    boolean populateState(TileEntityCompressionCrafter cc) {
        /*
         * Later I've realized that there's probably a simpler algorithm. This guy here is probably simpler. (Not completely described.)
         * 
         * get_clockwise_direction: TECC -> (direction, direction)
         * 		Returns the two directions that could be 'clockwise' for the TECC.
         * 		lower coords of the other-axiis is not correct. Might lookup in an array, or a rotation might be cool.
         *  find_root: TECC -> (clockwise_direction, root_TECC)
         *  	Similar to before
         *  get_length: ICoord -> expected_face -> clockwise_direction -> int
         *  	counts how many TECC are in direction; returns -1 if count is >3, or if there's one facing the wrong way, or...
         *  	populate the hashset as we check each one
         *  	make sure each faces expected_face
         *  
         *  root, right = find_root here
         *  width = get_length root root.facing right
         *  assert width > 0
         *  height = get_length (root + right*width + facing) right.getOpposite root.facing
         *  assert height > 0
         *  width2 = get_length (root + right*(width - 1) + facing*height) root.facing.getOpposite right.getOpposite
         *  assert width == width2
         *  height2 = get_length (root + right.getOpposite + facing*height) right root.facing.getOpposite
         *  assert height == height2
         */
        start = cc;
        up = cc.getFacing();
        if (up == ForgeDirection.UNKNOWN) {
            unknownError(cc, "Broken direction");
            return false;
        }
        root = findEdgeRoot(cc);
        if (root == null) {
            unknownError(cc, "Invalid frame\nNo root");
            return false;
        }
        right = getRightDirection();
        if (right == ForgeDirection.UNKNOWN) {
            unknownError(root, "No right");
            return false;
        }
        height = checkPairs(foundWalls, root, up, right);
        if (height == -1) {
            unknownError(root, "no height");
            return false;
        }
        otherEdge = getOtherSideRoot();
        if (otherEdge == null) {
            unknownError(root, "Other edge is missing");
            return false;
        }
        width = checkPairs(foundWalls, otherEdge, right, otherEdgeRight);
        if (width == -1) {
            unknownError(root, "No width");
            return false;
        }
        
        getCells();
        return true;
    }
    
    private static ForgeDirection[] forgeDirections = new ForgeDirection[] { ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.WEST };
    private static TileEntityCompressionCrafter findEdgeRoot(TileEntityCompressionCrafter at) {
        final ForgeDirection cd = at.getFacing();
        TileEntityCompressionCrafter first = at;
        for (int directionIndex = 0; directionIndex < forgeDirections.length; directionIndex++) {
            ForgeDirection d = forgeDirections[directionIndex];
            if (d == cd || d.getOpposite() == cd) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < 2; i++) {
                TileEntityCompressionCrafter l = at.look(d);
                if (l == null || l.getFacing() != cd) {
                    break;
                }
                at = l;
                found = true;
            }
            if (found) {
                return at;
            }
        }
        return first;
    }
    

    private ForgeDirection getRightDirection() {
        //"The right chest is obviously the right one" -- etho, badly quoted.
        Coord here = root.getCoord();
        ForgeDirection[] validDirections = ForgeDirection.VALID_DIRECTIONS;
        for (int i = 0; i < validDirections.length; i++) {
            ForgeDirection dir = validDirections[i];
            if (dir == up || dir == up.getOpposite()) {
                continue;
            }
            TileEntityCompressionCrafter cc = here.add(dir).getTE(TileEntityCompressionCrafter.class);
            if (cc != null && cc.getFacing() == up) {
                return dir;
            } else if (cc != null) {
                error(cc, "Inconsistent direction");
                return ForgeDirection.UNKNOWN;
            }
        }
        //Could be 1xn.
        Coord front = here.add(up);
        for (int i = 0; i < validDirections.length; i++) {
            ForgeDirection dir = validDirections[i];
            if (dir == up || dir == up.getOpposite()) {
                continue;
            }
            TileEntityCompressionCrafter cc = front.add(dir).getTE(TileEntityCompressionCrafter.class);
            if (cc != null && cc.getFacing().getOpposite() == dir) {
                return dir;
            }
        }
        error(root, "?");
        return ForgeDirection.UNKNOWN;
    }
    
    private TileEntityCompressionCrafter getOtherSideRoot() {
        Coord c = root.getCoord();
        c.adjust(right.getOpposite());
        c.adjust(up);
        TileEntityCompressionCrafter cc = c.getTE(TileEntityCompressionCrafter.class);
        if (cc == null) {
            return null;
        }
        TileEntityCompressionCrafter newRoot = findEdgeRoot(cc);
        if (newRoot == cc) {
            otherEdgeRight = up;
        } else {
            otherEdgeRight = up.getOpposite();
        }
        return newRoot;
    }
    
    private void getCells() {
        Coord corner = root.getCoord();
        corner.adjust(up);
        ForgeDirection out = right.getRotation(up);
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                Coord c = corner.add(dx*right.offsetX + dy*up.offsetX, dx*right.offsetY + dy*up.offsetY, dx*right.offsetZ + dy*up.offsetZ);
                cells[cellIndex(dx, dy)] = new CellInfo(c, out);
            }
        }
    }
    

    static class CellInfo {
        static final int BARREL = 0, BREAK = 1, SMACKED = 2, PICKED = 3, length = 4;
        //TODO: Crafting with liquids would be rad
        
        final Coord cell;
        ItemStack[] items = new ItemStack[length];
        
        public CellInfo(Coord cell, ForgeDirection top) {
            this.cell = cell;
            TileEntityDayBarrel barrel = cell.getTE(TileEntityDayBarrel.class);
            if (barrel != null) {
                if (barrel.item == null) {
                    return;
                }
                ItemStack b = barrel.item.copy();
                b.stackSize = Math.min(16, barrel.getItemCount());
                b.stackSize = Math.min(b.stackSize, b.getMaxStackSize());
                items[BARREL] = b;
                return;
            }
            items[PICKED] = cell.getPickBlock(top);
            items[BREAK] = cell.getBrokenBlock();
            if (items[BREAK] != null) {
                ItemStack b = items[BREAK];
                List<ItemStack> craftRes = FactorizationUtil.craft1x1(null, true, b);
                if (craftRes != null && craftRes.size() == 1) {
                    items[SMACKED] = craftRes.get(0);
                }
            }
        }
        
        private static final ArrayList<ItemStack> empty = new ArrayList<ItemStack>();
        List<ItemStack> consume(int mode, int amount) {
            ItemStack leftOvers = items[mode];
            if (leftOvers == null) {
                return empty;
            }
            switch (mode) {
            case PICKED:
                cell.setId(0);
                break;
            case BREAK:
                leftOvers.stackSize--;
                cell.setId(0);
                break;
            case BARREL:
                leftOvers.stackSize = 0;
                cell.getTE(TileEntityDayBarrel.class).changeItemCount(-amount);
                break;
            case SMACKED:
                List<ItemStack> craftRes = FactorizationUtil.craft1x1(null, true, items[BREAK]);
                for (Iterator<ItemStack> it = craftRes.iterator(); it.hasNext();) {
                    ItemStack is = it.next();
                    if (FactorizationUtil.couldMerge(is, leftOvers)) {
                        is.stackSize = leftOvers.stackSize;
                        if (is.stackSize <= 0) {
                            it.remove();
                        }
                        break;
                    }
                }
                cell.setId(0);
                return craftRes;
            }
            List<ItemStack> ret = new ArrayList(1);
            leftOvers = FactorizationUtil.normalize(leftOvers);
            if (leftOvers != null) {
                ret.add(leftOvers);
            }
            return ret;
        }
        
        int getBestMode(int lastModeAllowed) {
            int last_valid = lastModeAllowed;
            for (int i = 0; i <= lastModeAllowed; i++) {
                if (items[i] != null) {
                    last_valid = i;
                }
            }
            return last_valid;
        }

        public void updateBarrelExtraction(int maxCraft) {
            if (FactorizationUtil.getStackSize(items[BARREL]) > maxCraft) {
                items[BARREL].stackSize = Math.min(items[BARREL].stackSize, maxCraft);
            }
        }
    }
    
    
    private static final int[][] cellIndices = new int[][] {
        {0, 1, 2},
        {3, 4, 5},
        {6, 7, 8}
    };
    
    private static int cellIndex(int x, int y) {
        return cellIndices[2 - y][x];
    }
    
    int checkPairs(HashSet<TileEntityCompressionCrafter> walls, TileEntityCompressionCrafter local_root, final ForgeDirection up, final ForgeDirection right) {
        int distance = getPairDistance(walls, local_root);
        if (distance <= 0) {
            return -1;
        }
        Coord here = local_root.getCoord();
        for (int i = 0; i < 3; i++) {
            here.adjust(right);
            TileEntityCompressionCrafter other = here.getTE(TileEntityCompressionCrafter.class);
            if (other == null) {
                break; //short frame
            }
            if (other.getFacing() != up) {
                error(other, "Inconsistent direction");
                return -1;
            }
            if (getPairDistance(walls, other) != distance) {
                error(other, "Inconsistent height");
                return -1;
            }
        }
        return distance;
    }
    
    static boolean isFrame(Coord c) {
        return c.getTE(TileEntityCompressionCrafter.class) != null;
    }

    int getPairDistance(HashSet<TileEntityCompressionCrafter> walls, TileEntityCompressionCrafter start) {
        final ForgeDirection cd = start.getFacing();
        Coord here = start.getCoord();
        here.adjust(cd);
        for (int i = 1; i < 4; i++) {
            here.adjust(cd);
            TileEntityCompressionCrafter cc = here.getTE(TileEntityCompressionCrafter.class);
            if (cc != null) {
                if (cc.getFacing().getOpposite() != cd) {
                    error(cc, "Facing the wrong way");
                    return -1;
                }
                walls.add(start);
                walls.add(cc);
                return i;
            }
        }
        error(start, "Not facing another Compression Crafter");
        return -1;
    }
    
    
    
    
    
    
    public void craft(boolean fake, TileEntityCompressionCrafter start) {
        reset();
        if (!populateState(start)) {
            return;
        }
        do_craft(fake);
        reset();
    }
    
    

    ItemStack[] craftingGrid = new ItemStack[9];
    /** return true if something is/canbe crafted */
    private boolean do_craft(boolean fake) {
        Arrays.fill(craftingGrid, null);
        iteratePermutations: for (int mode = 0; mode < CellInfo.length; mode++) {
            boolean any = false;
            int maxCraft = 16;
            for (int i = 0; i < 9; i++) {
                CellInfo ci = cells[i];
                if (ci == null) {
                    craftingGrid[i] = null;
                    continue;
                }
                ItemStack is = ci.items[ci.getBestMode(mode)];
                craftingGrid[i] = is;
                if (is != null) {
                    any = true;
                    maxCraft = Math.min(maxCraft, is.stackSize);
                }
            }
            if (!any) {
                continue iteratePermutations;
            }
            
            ArrayList<ItemStack> total = new ArrayList(maxCraft);
            int items_used = 0;
            for (int craftCount = 0; craftCount < maxCraft; craftCount++) {
                if (craftCount == 1) {
                    for (int i = 0; i < 9; i++) {
                        CellInfo ci = cells[i];
                        if (ci == null) {
                            continue;
                        }
                        ci.updateBarrelExtraction(maxCraft - 1);
                    }
                }
                List<ItemStack> result = FactorizationUtil.craft3x3(root, fake, !fake && craftCount != maxCraft - 1, craftingGrid);
                if (!FactorizationUtil.craft_succeeded) {
                    if (craftCount == 0) {
                        continue iteratePermutations;
                    } else {
                        break;
                    }
                }
                
                if (fake) {
                    spreadCraftingAction();
                    return true;
                }
                total.addAll(result);
                items_used++;
            }
            {
                double mx = 0, my = 0, mz = 0;
                int count = 0;
                for (int i = 0; i < 9; i++) {
                    CellInfo ci = cells[i];
                    if (ci != null) {
                        ci.consume(ci.getBestMode(mode), items_used);
                        mx += ci.cell.x;
                        my += ci.cell.y;
                        mz += ci.cell.z;
                        count++;
                    }
                }
                if (count == 0) {
                    mx = root.xCoord;
                    my = root.yCoord;
                    mz = root.zCoord;
                } else {
                    mx /= count;
                    my /= count;
                    mz /= count;
                }
                for (ItemStack is : total) {
                    EntityItem ei = new EntityItem(root.worldObj, mx + 0.5, my + 0.5, mz + 0.5, is);
                    root.worldObj.spawnEntityInWorld(ei);
                }
            }
            return true;
        }
        return false;
    }
    
    
    void spreadCraftingAction() {
        for (TileEntityCompressionCrafter cc : foundWalls) {
            cc.informClient();
        }
    }
    
    
}
