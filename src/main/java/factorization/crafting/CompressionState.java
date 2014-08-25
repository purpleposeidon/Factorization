package factorization.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.weird.TileEntityDayBarrel;

public class CompressionState {
    static int MAX_CRAFT = 32;
    
    TileEntityCompressionCrafter start, root, otherEdge;
    ForgeDirection up, right, otherEdgeRight;
    CellInfo[] cells = new CellInfo[9];
    HashSet<TileEntityCompressionCrafter> foundWalls = new HashSet(3*4);
    int height, width;
    EntityPlayer player;
    
    boolean errored;
    
    //Clears the fields
    void reset() {
        start = root = otherEdge = null;
        up = right = ForgeDirection.UNKNOWN;
        foundWalls.clear();
        height = width = -1;
        Arrays.fill(cells, null);
        errored = false;
        pair_height = -2;
        player = null;
    }
    
    void error(TileEntityCompressionCrafter at, String msg, String... args) {
        if (errored) return;
        new Notice(at, msg, args).withStyle(Style.FORCE).send(player);
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
            error(cc, "Broken direction");
            return false;
        }
        root = findEdgeRoot(cc);
        if (root == null) {
            error(cc, "Invalid frame\nNo root");
            return false;
        }
        right = getRightDirection();
        if (right == ForgeDirection.UNKNOWN) {
            error(root, "No friendly compressors");
            return false;
        }
        height = checkPairs(foundWalls, root, up, right);
        int local_pair_width = pair_height;
        if (height == -1) {
            error(root, "no height");
            return false;
        }
        otherEdge = getOtherSideRoot();
        if (otherEdge == null) {
            error(root, "Missing compressors on the left or right");
            return false;
        }
        width = checkPairs(foundWalls, otherEdge, right, otherEdgeRight);
        if (width == -1) {
            error(root, "No width");
            return false;
        }
        if (pair_height != height || local_pair_width != width) {
            error(root, "Missing compression crafters");
            return false;
        }
        
        FzInv[] inv = cc.getAdjacentInventories();
        if (inv[0] == null) {
            error(cc, "Need output inventory\n(Like a chest)");
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
        return ForgeDirection.UNKNOWN;
    }
    
    private int pickSize() {
        double f = Math.random();
        if (f > 0.5) {
            return 3;
        }
        if (f > 0.2) {
            return 2;
        }
        return 1;
    }
    
    void showTutorial(EntityPlayer player, TileEntityCompressionCrafter cc) {
        reset();
        this.player = player;
        errored = true;
        populateState(cc);
        showExample();
        reset();
    }
    
    private void showExample() {
        Notice.clear(player);
        int width = pickSize(), height = pickSize();
        ForgeDirection r = ForgeDirection.WEST;
        if (up.offsetX*r.offsetX != 0) {
            r = ForgeDirection.NORTH;
        }
        Coord c = root.getCoord();
        Coord d = c.copy();
        for (int _ = 0; _ < height + 1; _++) d.adjust(up);
        for (int x = 0; x < width; x++) {
            mark(c);
            c.adjust(r);
            mark(d);
            d.adjust(r);
        }
        Coord hypoRoot = root.getCoord();
        hypoRoot.adjust(up);
        hypoRoot.adjust(r.getOpposite());
        c = hypoRoot;
        d = c.copy();
        for (int _ = 0; _ < width + 1; _++) d.adjust(r);
        for (int x = 0; x < height; x++) {
            mark(c);
            c.adjust(up);
            mark(d);
            d.adjust(up);
        }
        
        errored = false;
        error(root, "Place as marked for %sx%s crafting grid\nThen give a redstone signal", ""+width, ""+height);
    }
    
    void mark(Coord c) {
        if (c.getTE(TileEntityCompressionCrafter.class) != null) {
            return;
        }
        new Notice(c, "").withItem(Core.registry.compression_crafter_item).withStyle(Style.FORCE, Style.EXACTPOSITION, Style.DRAWITEM, Style.DRAWFAR).send(player);
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
        boolean airBlock = true;
        
        public CellInfo(Coord cell, ForgeDirection top) {
            this.cell = cell;
            if (cell.isAir() || cell.getHardness() < 0) {
                airBlock = true;
                return;
            }
            TileEntityDayBarrel barrel = cell.getTE(TileEntityDayBarrel.class);
            if (barrel != null) {
                if (barrel.item == null) {
                    return;
                }
                ItemStack b = barrel.item.copy();
                b.stackSize = Math.min(MAX_CRAFT, barrel.getItemCountSticky());
                b.stackSize = Math.min(b.stackSize, b.getMaxStackSize());
                items[BARREL] = b;
                airBlock = false;
                //Barrel blocks can't be used to craft, even if they're empty.
                return;
            }
            items[PICKED] = cell.getPickBlock(top);
            items[BREAK] = cell.getBrokenBlock();
            if (items[BREAK] != null) {
                ItemStack b = items[BREAK];
                List<ItemStack> craftRes = FzUtil.craft1x1(null, true, b);
                if (craftRes != null && craftRes.size() == 1) {
                    items[SMACKED] = craftRes.get(0);
                }
            }
            airBlock = items[PICKED] == null && items[BREAK] == null && items[SMACKED] == null;
        }
        
        private static final ArrayList<ItemStack> empty = new ArrayList<ItemStack>();
        List<ItemStack> consume(int mode, int amount) {
            ItemStack leftOvers = items[mode];
            if (leftOvers == null) {
                return empty;
            }
            switch (mode) {
            case PICKED:
                cell.setAir();
                break;
            case BREAK:
                leftOvers.stackSize--;
                cell.setAir();
                break;
            case BARREL:
                //leftOvers.stackSize = 0;
                cell.getTE(TileEntityDayBarrel.class).changeItemCount(-amount);
                break;
            case SMACKED:
                List<ItemStack> craftRes = FzUtil.craft1x1(null, true, items[BREAK]);
                for (Iterator<ItemStack> it = craftRes.iterator(); it.hasNext();) {
                    ItemStack is = it.next();
                    if (FzUtil.couldMerge(is, leftOvers)) {
                        is.stackSize = leftOvers.stackSize;
                        if (is.stackSize <= 0) {
                            it.remove();
                        }
                        break;
                    }
                }
                cell.setAir();
                return craftRes;
            }
            List<ItemStack> ret = new ArrayList(1);
            leftOvers = FzUtil.normalize(leftOvers);
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
            if (FzUtil.getStackSize(items[BARREL]) > maxCraft) {
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
    
    
    int pair_height = 0;
    int checkPairs(HashSet<TileEntityCompressionCrafter> walls, TileEntityCompressionCrafter local_root, final ForgeDirection up, final ForgeDirection right) {
        int distance = getPairDistance(walls, local_root);
        if (distance <= 0) {
            return -1;
        }
        Coord here = local_root.getCoord();
        for (int i = 0; i < 3; i++) {
            here.adjust(right);
            TileEntityCompressionCrafter other = here.getTE(TileEntityCompressionCrafter.class);
            pair_height = i + 1;
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
        if (!start.buffer.isEmpty()) {
            error(start, "Buffered output");
            return -1;
        }
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
                if (!cc.buffer.isEmpty()) {
                    error(cc, "Buffered output");
                    return -1;
                }
                walls.add(start);
                walls.add(cc);
                return i;
            }
        }
        if (start.getCoord().add(start.getFacing()).getTE(TileEntityCompressionCrafter.class) != null) {
            error(start, "Must be 1-3 blocks away from\nthe other Compression Crafter");
            return -1;
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
            int maxCraft = MAX_CRAFT;
            boolean[] containerItem = new boolean[9];
            for (int i = 0; i < 9; i++) {
                CellInfo ci = cells[i];
                if (ci == null) {
                    craftingGrid[i] = null;
                    continue;
                }
                ItemStack is = ci.items[ci.getBestMode(mode)];
                craftingGrid[i] = is;
                if (is == null) {
                    if (!ci.airBlock) {
                        continue iteratePermutations;
                    }
                    continue;
                }
                any = true;
                Item it = is.getItem();
                if (is.getMaxStackSize() == 1 && it.hasContainerItem(is) && it.getMaxDamage() > 1) {
                    int useCount = 0;
                    int origDamage = is.getItemDamage();
                    while (useCount < maxCraft) {
                        if (is.getItemDamage() > is.getMaxDamage()) {
                            break;
                        }
                        is = it.getContainerItem(is);
                        if (is == null || is.stackSize == 0 || is.getItemDamage() == origDamage || is.getItem() != it) {
                            break;
                        }
                        useCount++;
                    }
                    if (useCount > 0) {
                        containerItem[i] = true;
                    }
                    maxCraft = Math.min(maxCraft, useCount);
                } else {
                    maxCraft = Math.min(maxCraft, is.stackSize);
                }
                
            }
            if (!any) {
                continue iteratePermutations;
            }
            
            FzUtil.craft3x3(root, true, true, craftingGrid);
            if (!FzUtil.craft_succeeded) {
                continue iteratePermutations;
            }
            
            ArrayList<ItemStack> total = new ArrayList(maxCraft+4);
            int items_used = 0;
            for (int craftCount = 0; craftCount < maxCraft; craftCount++) {
                if (craftCount == 0) {
                    for (int i = 0; i < 9; i++) {
                        CellInfo ci = cells[i];
                        if (ci == null) {
                            continue;
                        }
                        ci.updateBarrelExtraction(maxCraft);
                    }
                }
                List<ItemStack> result = FzUtil.craft3x3(root, fake, !fake && craftCount != maxCraft - 1, craftingGrid);
                if (!FzUtil.craft_succeeded) {
                    if (craftCount == 0) {
                        continue iteratePermutations; //This won't usually happen except for very strange recipes
                    } else {
                        break;
                    }
                }
                for (int i = 0; i < 9; i++) {
                    if (containerItem[i]) {
                        ItemStack got = craftingGrid[i];
                        if (got != null) {
                            craftingGrid[i] = got.getItem().getContainerItem(got);
                        }
                    }
                }
                
                if (fake) {
                    spreadCraftingAction();
                    return true;
                }
                
                items_used++;
                total.addAll(result);
            }
            for (int i = 0; i < 9; i++) {
                CellInfo ci = cells[i];
                if (ci != null) {
                    ci.consume(ci.getBestMode(mode), items_used);
                }
            }
            FzUtil.collapseItemList(total);
            start.buffer = total;
            return true;
        }
        return false;
    }
    
    
    void spreadCraftingAction() {
        for (TileEntityCompressionCrafter cc : foundWalls) {
            cc.informClient();
        }
        int minX, minY, minZ, maxX, maxY, maxZ;
        minX = minY = minZ = maxX = maxY = maxZ = 0; //shadup
        boolean first = true;
        for (int i = 0; i < cells.length; i++) {
            CellInfo ci = cells[i];
            if (ci == null) {
                continue;
            }
            Coord c = ci.cell;
            if (first) {
                minX = maxX = c.x;
                minY = maxY = c.y;
                minZ = maxZ = c.z;
                first = false;
                continue;
            }
            minX = Math.min(c.x, minX);
            minY = Math.min(c.y, minY);
            minZ = Math.min(c.z, minZ);
            maxX = Math.max(c.x, maxX);
            maxY = Math.max(c.y, maxY);
            maxZ = Math.max(c.z, maxZ);
        }
        ForgeDirection axis = right.getRotation(up);
        start.broadcastMessage(null, MessageType.CompressionCrafterBounds, minX, minY, minZ, maxX, maxY, maxZ, (byte) axis.ordinal());
    }
    
    
}
