package factorization.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.FakePlayer;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.LiquidEvent;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;
import net.minecraftforge.oredict.OreDictionary;
import factorization.api.Coord;
import factorization.api.DeltaCoord;

public class FactorizationUtil {
    //ItemStack handling
    public static final int WILDCARD_DAMAGE = OreDictionary.WILDCARD_VALUE;
    
    public static ItemStack makeWildcard(Item item) {
        return new ItemStack(item, 1, WILDCARD_DAMAGE);
    }
    
    public static ItemStack makeWildcard(Block item) {
        return new ItemStack(item, 1, WILDCARD_DAMAGE);
    }
    
    /**
     * Compare includes NBT and damage value; ignores stack size
     */
    public static boolean couldMerge(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.itemID == b.itemID && a.getItemDamage() == b.getItemDamage() && sameItemTags(a, b);
    }
    
    public static boolean sameItemTags(ItemStack a, ItemStack b) {
        if (a.stackTagCompound == null || b.stackTagCompound == null) {
            return a.stackTagCompound == b.stackTagCompound;
        }
        a.stackTagCompound.setName("tag"); //Notch.
        b.stackTagCompound.setName("tag"); //Notch.
        return a.stackTagCompound.equals(b.stackTagCompound);
    }
    
    /**
     * Compare includes damage value; ignores stack size and NBT
     */
    public static boolean similar(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.itemID == b.itemID && a.getItemDamage() == b.getItemDamage();
    }
    
    /**
     * Compare only itemIDs and damage value, taking into account that a damage value of -1 matches any
     */
    public static boolean wildcardSimilar(ItemStack template, ItemStack stranger) {
        if (template == null || stranger == null) {
            return template == stranger;
        }
        if (template.getItemDamage() == WILDCARD_DAMAGE) {
            return template.itemID == stranger.itemID;
        }
        return similar(template, stranger);
    }
    
    public static boolean oreDictionarySimilar(Object template, ItemStack stranger) {
        if (template instanceof String) {
            ArrayList<ItemStack> ores = OreDictionary.getOres((String) template);
            for (int i = 0; i < ores.size(); i++) {
                if (wildcardSimilar(ores.get(i), stranger)) {
                    return true;
                }
            }
            return false;
        } else if (template instanceof List) {
            for (Object o : (List)template) {
                if (oreDictionarySimilar(o, stranger)) {
                    return true;
                }
            }
            return false;
        } else {
            return wildcardSimilar((ItemStack) template, stranger);
        }
    }
    
    public static int stackSize(ItemStack is) {
        return (is == null) ? 0 : is.stackSize;
    }
    
    public static ItemStack normalDecr(ItemStack is) {
        is.stackSize--;
        return is.stackSize <= 0 ? null : is;
    }
    
    public static NBTTagCompound getTag(ItemStack is) {
        NBTTagCompound ret = is.getTagCompound();
        if (ret == null) {
            ret = new NBTTagCompound();
            is.setTagCompound(ret);
        }
        return ret;
    }
    
    public static String getCustomItemName(ItemStack is) {
        if (is.hasDisplayName()) {
            return is.getDisplayName();
        }
        return null;
    }

    public static boolean itemCanFire(World w, ItemStack is, int tickDelay) {
        NBTTagCompound tag = getTag(is);
        long t = tag.getLong("lf");
        if (t > w.getWorldTime()) {
            tag.setLong("lf", w.getWorldTime());
            return true;
        }
        if (t + tickDelay > w.getWorldTime()) {
            return false;
        }
        tag.setLong("lf", w.getWorldTime());
        return true;
    }

    public static ItemStack normalize(ItemStack is) {
        if (is == null || is.stackSize <= 0) {
            return null;
        }
        return is;
    }

    public static int getStackSize(ItemStack is) {
        if (is == null) {
            return 0;
        }
        return is.stackSize;
    }
    
    public static int getFreeSpace(ItemStack is, int stackLimit) {
        int max = Math.min(is.getMaxStackSize(), stackLimit);
        return Math.max(0, max - is.stackSize);
    }
    
    //Slot transfering

    /**
     * Use transferSlotToSlots
     * @param srcInv
     * @param slotIndex
     * @param destInv
     * @param targetSlots
     * @return
     */
    @Deprecated
    public static ItemStack transferStackToArea(IInventory srcInv, int slotIndex,
            IInventory destInv, Iterable<Integer> targetSlots) {
        //this is probably all wrong. >_>
        ItemStack is = srcInv.getStackInSlot(slotIndex);
        if (is == null || is.stackSize == 0) {
            return null;
        }
        // fill up pre-existing stacks
        for (int i : targetSlots) {
            ItemStack target = destInv.getStackInSlot(i);
            if (target == null) {
                continue;
            }
            if (FactorizationUtil.couldMerge(is, target)) {
                int free_space = target.getMaxStackSize() - target.stackSize;
                int incr = Math.min(free_space, is.stackSize);
                if (incr <= 0) {
                    continue;
                }
                is.stackSize -= incr;
                target.stackSize += incr;
            }
            if (is.stackSize <= 0) {
                srcInv.setInventorySlotContents(slotIndex, null);
                return null;
            }
        }
        // make new stacks
        for (int i : targetSlots) {
            ItemStack target = destInv.getStackInSlot(i);
            if (target == null) {
                destInv.setInventorySlotContents(i, is.copy());
                is.stackSize = 0;
                srcInv.setInventorySlotContents(slotIndex, null);
                return null;
            }
        }
        if (is.stackSize <= 0) {
            srcInv.setInventorySlotContents(slotIndex, null);
            return null;
        }
        srcInv.setInventorySlotContents(slotIndex, is);
        return is;
    }
    
    public static ItemStack transferSlotToSlots(Slot clickSlot, Iterable<Slot> destinations) {
        ItemStack clickStack = normalize(clickSlot.getStack());
        if (clickStack == null) {
            return null;
        }
        //try to fill up partially filled slots
        for (Slot slot : destinations) {
            ItemStack is = normalize(slot.getStack());
            if (is == null || !FactorizationUtil.couldMerge(is, clickStack)) {
                continue;
            }
            int freeSpace = Math.min(is.getMaxStackSize() - is.stackSize, slot.getSlotStackLimit() - is.stackSize);
            if (freeSpace <= 0) {
                continue;
            }
            if (!slot.isItemValid(clickStack)) {
                continue;
            }
            int delta = Math.min(freeSpace, clickStack.stackSize);
            is.stackSize += delta;
            slot.putStack(is);
            clickStack.stackSize -= delta;
            if (clickStack.stackSize <= 0) {
                clickSlot.putStack(null);
                return null;
            }
        }
        //try to fill up empty slots
        for (Slot slot : destinations) {
            if (slot.getHasStack() || !slot.isItemValid(clickStack)) {
                continue;
            }
            int freeSpace = Math.min(slot.getSlotStackLimit(), clickStack.getMaxStackSize());
            int delta = Math.min(freeSpace, clickStack.stackSize);
            ItemStack toPut = clickStack.copy();
            toPut.stackSize = delta;
            slot.putStack(toPut);
            clickStack.stackSize -= delta;
            clickStack = normalize(clickStack);
            if (clickStack == null) {
                clickSlot.putStack(null);
                return null;
            }
        }
        
        clickSlot.putStack(normalize(clickStack));
        return null;
    }
    
    public static abstract class FzInv {
        abstract int size();
        abstract int slotIndex(int i);
        
        boolean forceInsert = false;
        
        final IInventory under;
        
        public FzInv(IInventory inv) {
            this.under = inv;
        }
        
        void setInsertForce(boolean b) {
            forceInsert = b;
        }
        
        ItemStack get(int i) {
            return under.getStackInSlot(slotIndex(i));
        }
        
        void set(int i, ItemStack is) {
            under.setInventorySlotContents(slotIndex(i), is);
            under.onInventoryChanged();
        }
        
        int getFreeSpace(int i) {
            ItemStack dest = get(i);
            if (dest == null) {
                return under.getInventoryStackLimit();
            }
            int dest_free = dest.getMaxStackSize() - dest.stackSize;
            return Math.min(dest_free, under.getInventoryStackLimit());
        }
        
        ItemStack pushInto(int i, ItemStack is) {
            int slotIndex = slotIndex(i);
            if (!canInsert(i, is)) {
                return is;
            }
            ItemStack dest = under.getStackInSlot(slotIndex);
            if (dest == null) {
                ItemStack toPut = is;
                int stack_limit = under.getInventoryStackLimit();
                if (toPut.stackSize > stack_limit) {
                    toPut = is.splitStack(stack_limit);
                } else {
                    is = null;
                }
                under.setInventorySlotContents(slotIndex, toPut);
                under.onInventoryChanged();
                return is;
            }
            if (!FactorizationUtil.couldMerge(dest, is)) {
                return is;
            }

            int dest_free = getFreeSpace(i);
            if (dest_free < 1) {
                return is;
            }
            int delta = Math.min(dest_free, is.stackSize);
            dest.stackSize += delta;
            is.stackSize -= delta;
            under.setInventorySlotContents(slotIndex, dest);
            under.onInventoryChanged();
            return normalize(is);
        }
        
        public boolean canExtract(int i, ItemStack is) {
            return true;
        }
        
        public boolean canInsert(int i, ItemStack is) {
            if (forceInsert) {
                return true;
            }
            return under.isStackValidForSlot(slotIndex(i), is);
        }
        
        public boolean transfer(int i, FzInv dest_inv, int dest_i, int max_transfer) {
            ItemStack src = normalize(get(i));
            if (src == null) {
                return false;
            }
            if (!canExtract(i, src)) {
                return false;
            }
            ItemStack dest = dest_inv.get(dest_i);
            if (dest == null) {
                dest = src.copy();
                dest.stackSize = 0;
            } else if (!couldMerge(src, dest)) {
                return false;
            }
            if (!dest_inv.canInsert(dest_i, src)) {
                return false;
            }
            int dest_free = dest_inv.getFreeSpace(dest_i);
            if (dest_free < 1) {
                return false;
            }
            int delta = Math.min(dest_free, src.stackSize);
            delta = Math.min(max_transfer, delta);
            dest.stackSize += delta;
            src.stackSize -= delta;
            src = normalize(src);
            dest_inv.set(dest_i, dest);
            set(i, src);
            dest_inv.under.onInventoryChanged();
            under.onInventoryChanged();
            return true;
        }
        
        public ItemStack push(ItemStack is) {
            is = normalize(is);
            //First, fill up already existing stacks
            for (int i = 0; i < size(); i++) {
                if (is == null) {
                    return null;
                }
                ItemStack dest = get(i);
                if (dest != null) {
                    is = normalize(pushInto(i, is));
                }
            }
            //Second, add to null stacks
            for (int i = 0; i < size(); i++) {
                if (is == null) {
                    return null;
                }
                ItemStack dest = get(i);
                if (dest == null) {
                    is = normalize(pushInto(i, is));
                }
            }
            return is;
        }
        
        public ItemStack pull(int slot, int limit) {
            return under.decrStackSize(slotIndex(slot), limit);
        }
        
        public ItemStack pull(ItemStack toMatch, int limit, boolean strict) {
            ItemStack ret = null;
            for (int i = 0; i < size(); i++) {
                ItemStack is = get(i);
                if (strict) {
                    if (!FactorizationUtil.couldMerge(toMatch, is)) {
                        continue;
                    }
                } else {
                    if (!FactorizationUtil.wildcardSimilar(toMatch, is)) {
                        continue;
                    }
                }
                ItemStack pulled = FactorizationUtil.normalize(pull(i, limit));
                if (pulled == null) {
                    continue;
                }
                limit -= pulled.stackSize;
                if (ret == null) {
                    ret = pulled;
                } else {
                    ret.stackSize += pulled.stackSize;
                }
                if (limit <= 0) {
                    break;
                }
            }
            return ret;
        }
    }

    public static class PlainInvWrapper extends FzInv {
        final int length;
        public PlainInvWrapper(IInventory inv) {
            super(inv);
            length = inv.getSizeInventory();
        }
        
        @Override
        int slotIndex(int i) {
            return i;
        }
        
        @Override
        int size() {
            return length;
        }
    }
    
    public static FzInv openInventory(IInventory orig_inv, ForgeDirection side) {
        return openInventory(orig_inv, side.ordinal(), true);
    }
    
    public static FzInv openInventory(IInventory orig_inv, ForgeDirection side, boolean openBothChests) {
        return openInventory(orig_inv, side.ordinal(), openBothChests);
    }
    
    public static FzInv openInventory(IInventory orig_inv, final int side) {
        return openInventory(orig_inv, side, true);
    }
    
    @SuppressWarnings("deprecation")
    public static FzInv openInventory(IInventory orig_inv, final int side, boolean openBothChests) {
        if (orig_inv instanceof TileEntityChest) {
            orig_inv = openDoubleChest((TileEntityChest) orig_inv, openBothChests);
            if (orig_inv == null) {
                return null;
            }
        }
        if (orig_inv instanceof net.minecraft.inventory.ISidedInventory) {
            final net.minecraft.inventory.ISidedInventory inv = (net.minecraft.inventory.ISidedInventory) orig_inv;
            final int[] slotMap = inv.getSizeInventorySide(side);
            return new FzInv(inv) {
                @Override
                int slotIndex(int i) {
                    return slotMap[i];
                }
                
                @Override
                int size() {
                    return slotMap.length;
                }
                
                @Override
                public boolean canExtract(int i, ItemStack is) {
                    return inv.func_102008_b(slotMap[i], is, side);
                }
                
                @Override
                public boolean canInsert(int i, ItemStack is) {
                    if (forceInsert) {
                        return true;
                    }
                    return inv.func_102007_a(slotMap[i], is, side);
                }};
        } else if (orig_inv instanceof net.minecraftforge.common.ISidedInventory) {
            final net.minecraftforge.common.ISidedInventory inv = (net.minecraftforge.common.ISidedInventory) orig_inv;
            final ForgeDirection fside = ForgeDirection.getOrientation(side);
            final int start_slot = inv.getStartInventorySide(fside);
            final int length = inv.getSizeInventorySide(fside);
            return new FzInv(inv) {
                @Override
                int slotIndex(int i) {
                    return start_slot + i;
                }
                
                @Override
                int size() {
                    return length;
                }};
        } else {
            return new PlainInvWrapper(orig_inv);
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean canAccessSlot(IInventory inv, int slot) {
        if (inv instanceof net.minecraft.inventory.ISidedInventory) {
            net.minecraft.inventory.ISidedInventory isi = (net.minecraft.inventory.ISidedInventory) inv;
            //O(n). Ugh.
            for (int i = 0; i < 6; i++) {
                int[] slots = isi.getSizeInventorySide(i);
                for (int j = 0; j < slots.length; j++) {
                    if (slots[j] == slot) {
                        return true;
                    }
                }
            }
        } else if (inv instanceof net.minecraftforge.common.ISidedInventory) {
            net.minecraftforge.common.ISidedInventory isi = (net.minecraftforge.common.ISidedInventory) inv;
            //O(1). Just PEACHY.
            for (int i = 0; i < 6; i++) {
                ForgeDirection side = ForgeDirection.getOrientation(i);
                int start = isi.getStartInventorySide(side);
                int end = start + isi.getSizeInventorySide(side);
                if (start <= slot && slot < end) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }
    
    /**
     * If you are accessing multiple chests, and some might be adjacent you'll want to treat them as a double chest. Calling this function with a lower chest
     * will return 'null'; calling with an upper chest will return an InventoryLargeChest. If it's a single chest, it'll return that chest.
     * 
     * @param chest
     * @return
     */
    public static IInventory openDoubleChest(TileEntityChest chest, boolean openBothSides) {
        IInventory origChest = (TileEntityChest) chest;
        World world = chest.worldObj;
        int i = chest.xCoord, j = chest.yCoord, k = chest.zCoord;
        int chestBlock = Block.chest.blockID;
        if (world.getBlockId(i - 1, j, k) == chestBlock) {
            return new InventoryLargeChest(origChest.getInvName(), (TileEntityChest) world.getBlockTileEntity(i - 1, j, k), origChest);
        }
        if (world.getBlockId(i, j, k - 1) == chestBlock) {
            return new InventoryLargeChest(origChest.getInvName(), (TileEntityChest) world.getBlockTileEntity(i, j, k - 1), origChest);
        }
        // If we're the lower chest, skip ourselves
        if (world.getBlockId(i + 1, j, k) == chestBlock) {
            if (openBothSides) {
                return new InventoryLargeChest(origChest.getInvName(), origChest, (TileEntityChest) world.getBlockTileEntity(i + 1, j, k));
            }
            return null;
        }
        if (world.getBlockId(i, j, k + 1) == chestBlock) {
            if (openBothSides) {
                return new InventoryLargeChest(origChest.getInvName(), origChest, (TileEntityChest) world.getBlockTileEntity(i, j, k + 1));
            }
            return null;
        }

        return chest;
    }
    
    //Recipe creation
    public static IRecipe createShapedRecipe(ItemStack result, Object... args) {
        String var3 = "";
        int var4 = 0;
        int var5 = 0;
        int var6 = 0;

        if (args[var4] instanceof String[]) {
            String[] var7 = (String[]) ((String[]) args[var4++]);

            for (int var8 = 0; var8 < var7.length; ++var8) {
                String var9 = var7[var8];
                ++var6;
                var5 = var9.length();
                var3 = var3 + var9;
            }
        } else {
            while (args[var4] instanceof String) {
                String var11 = (String) args[var4++];
                ++var6;
                var5 = var11.length();
                var3 = var3 + var11;
            }
        }

        HashMap var12;

        for (var12 = new HashMap(); var4 < args.length; var4 += 2) {
            Character var13 = (Character) args[var4];
            ItemStack var14 = null;

            if (args[var4 + 1] instanceof Item) {
                var14 = new ItemStack((Item) args[var4 + 1]);
            } else if (args[var4 + 1] instanceof Block) {
                var14 = new ItemStack((Block) args[var4 + 1], 1, -1);
            } else if (args[var4 + 1] instanceof ItemStack) {
                var14 = (ItemStack) args[var4 + 1];
            }

            var12.put(var13, var14);
        }

        ItemStack[] var15 = new ItemStack[var5 * var6];

        for (int var16 = 0; var16 < var5 * var6; ++var16) {
            char var10 = var3.charAt(var16);

            if (var12.containsKey(Character.valueOf(var10))) {
                var15[var16] = ((ItemStack) var12.get(Character.valueOf(var10))).copy();
            } else {
                var15[var16] = null;
            }
        }

        return new ShapedRecipes(var5, var6, var15, result);
    }

    public static IRecipe createShapelessRecipe(ItemStack result, Object... args) {
        ArrayList var3 = new ArrayList();
        int var5 = args.length;

        for (int var6 = 0; var6 < var5; ++var6)
        {
            Object var7 = args[var6];

            if (var7 instanceof ItemStack)
            {
                var3.add(((ItemStack) var7).copy());
            }
            else if (var7 instanceof Item)
            {
                var3.add(new ItemStack((Item) var7));
            }
            else
            {
                if (!(var7 instanceof Block))
                {
                    throw new RuntimeException("Invalid shapeless recipy!");
                }

                var3.add(new ItemStack((Block) var7));
            }
        }

        return new ShapelessRecipes(result, var3);
    }

    static Random rand = new Random();

    static EntityItem spawnItemStack(Coord c, ItemStack item) {
        if (item == null) {
            return null;
        }
        double dx = rand.nextFloat() * 0.5 - 0.5;
        double dy = rand.nextFloat() * 0.5 - 0.5;
        double dz = rand.nextFloat() * 0.5 - 0.5;

        EntityItem entityitem = new EntityItem(c.w, c.x + 0.5, c.y + 0.5, c.z + 0.5, item);
        entityitem.motionY = 0.2 + rand.nextGaussian() * 0.02;
        entityitem.motionX = rand.nextGaussian() * 0.02;
        entityitem.motionZ = rand.nextGaussian() * 0.02;
        c.w.spawnEntityInWorld(entityitem);
        return entityitem;
    }
    

    public static int determineOrientation(EntityPlayer player) {
        if (player.rotationPitch > 75) {
            return 0;
        }
        if (player.rotationPitch <= -75) {
            return 1;
        }
        //stolen from BlockPistonBase.determineOrientation. It was reversed, & we handle the y-axis differently
        int var7 = MathHelper.floor_double((double) ((180 + player.rotationYaw) * 4.0F / 360.0F) + 0.5D) & 3;
        return var7 == 0 ? 2 : (var7 == 1 ? 5 : (var7 == 2 ? 3 : (var7 == 3 ? 4 : 0)));
    }
    
    public static DeltaCoord getFlatDiagonalFacing(EntityPlayer player) {
        double angle = Math.toRadians(90 + player.rotationYaw);
        int dx = Math.cos(angle) > 0 ? 1 : -1;
        int dz = Math.sin(angle) > 0 ? 1 : -1;
        return new DeltaCoord(dx, 0, dz);
    }
    
    public static <E extends Enum> E shiftEnum(E current, E values[], int delta) {
        int next = current.ordinal() + delta;
        if (next < 0) {
            return values[values.length - 1];
        }
        if (next >= values.length) {
            return values[0];
        }
        return values[next];
    }
    
    
    //Liquid tank handling
    
    public static void writeTank(NBTTagCompound tag, LiquidTank tank, String name) {
        LiquidStack ls = tank.getLiquid();
        if (ls == null) {
            return;
        }
        NBTTagCompound liquid_tag = new NBTTagCompound(name);
        ls.writeToNBT(liquid_tag);
        tag.setTag(name, liquid_tag);
    }
    
    public static void readTank(NBTTagCompound tag, LiquidTank tank, String name) {
        NBTTagCompound liquid_tag = tag.getCompoundTag(name);
        LiquidStack ls = LiquidStack.loadLiquidStackFromNBT(liquid_tag);
        tank.setLiquid(ls);
    }
    
    public static void spill(Coord where, LiquidStack what) {
        if (what == null || what.amount < 0) {
            return;
        }
        LiquidEvent.fireEvent(new LiquidEvent.LiquidSpilledEvent(what, where.w, where.x, where.y, where.z));
    }
    
    //AxisAlignedBB & Vec3 stuff
    public static Vec3 getMin(AxisAlignedBB aabb) {
        return Vec3.createVectorHelper(aabb.minX, aabb.minY, aabb.minZ);
    }
    
    public static void setMin(AxisAlignedBB aabb, Vec3 v) {
        aabb.minX = v.xCoord;
        aabb.minY = v.yCoord;
        aabb.minZ = v.zCoord;
    }
    
    public static Vec3 getMax(AxisAlignedBB aabb) {
        return Vec3.createVectorHelper(aabb.maxX, aabb.maxY, aabb.maxZ);
    }
    
    public static void setMax(AxisAlignedBB aabb, Vec3 v) {
        aabb.maxX = v.xCoord;
        aabb.maxY = v.yCoord;
        aabb.maxZ = v.zCoord;
    }
    
    public static Vec3 averageVec(Vec3 a, Vec3 b) {
        return Vec3.createVectorHelper((a.xCoord + b.xCoord)/2, (a.yCoord + b.yCoord)/2, (a.zCoord + b.zCoord)/2);
    }
    
    public static boolean intersect(double la, double ha, double lb, double hb) {
        //If we're not intersecting, then one is to the right of the other.
        //<--  (la ha) -- (lb hb) -->
        //<--- (lb hb) -- (la ha) -->
        return !(ha < lb || hb < la);
    }
    
    public static InventoryCrafting makeCraftingGrid() {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer entityplayer) {
                return false;
            }

            @Override
            public void onCraftMatrixChanged(IInventory iinventory) {
            }
        }, 3, 3);
    }
    
    public static EntityPlayer makePlayer(final Coord where, String use) {
        EntityPlayer fakePlayer = new FakePlayer(where.w, "[FZ " + use +  "]") {
            @Override
            public ChunkCoordinates getPlayerCoordinates() { return new ChunkCoordinates(where.x, where.y, where.z); }
        };
        where.setAsEntityLocation(fakePlayer);
        return fakePlayer;
    }
    
    public static void addInventoryToArray(IInventory inv, ArrayList<ItemStack> ret) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = FactorizationUtil.normalize(inv.getStackInSlot(i));
            if (is != null) {
                ret.add(is);
            }
        }
    }
    
    
}
