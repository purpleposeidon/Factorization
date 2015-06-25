package factorization.crafting;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.crafting.CraftingManagerGeneric;
import factorization.api.crafting.IVexatiousCrafting;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityFactorization;
import factorization.util.CraftUtil;
import factorization.util.InvUtil;
import factorization.util.InvUtil.FzInv;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.io.IOException;
import java.util.*;

public class TileEntityMixer extends TileEntityFactorization implements
        IChargeConductor {
    //inventory: 4 input slots, 4 output slots
    public static final int INPUT_SIZE = 4;
    public ItemStack input[] = new ItemStack[INPUT_SIZE], output[] = new ItemStack[4];
    public static final int[] IN_s = {0, 1, 2, 3}, OUT_s = {4, 5, 6, 7};
    public ArrayList<ItemStack> outputBuffer = new ArrayList();
    public int progress = 0;
    public int speed = 0;
    Charge charge = new Charge(this);
    
    @Override
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.mixer.top;
        case DOWN: return BlockIcons.mixer.bottom;
        default: return BlockIcons.mixer.side;
        }
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        super.putData(data);
        charge.serialize("", data);
        progress = data.as(Share.PRIVATE, "progress").putInt(progress);
        speed = data.as(Share.VISIBLE, "speed").putInt(speed);
        outputBuffer = data.as(Share.PRIVATE, "outBuffer").putItemArray(outputBuffer);
        putSlots(data);
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        if (getWorldObj() != null && getWorldObj().isRemote) {
            return;
        }
        dirty = true;
        cache = null;
    }
    
    @Override
    public void dropContents() {
        super.dropContents();
        Coord here = getCoord();
        for (ItemStack is : outputBuffer) {
            InvUtil.spawnItemStack(here, is);
        }
        markDirty();
    }

    @Override
    public int getSizeInventory() {
        return input.length + output.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < input.length) {
            return input[slot];
        }
        slot -= input.length;
        if (slot >= 0 && slot < output.length) {
            return output[slot];
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        markDirty();
        if (slot >= 0 && slot < input.length) {
            input[slot] = is;
            return;
        }
        slot -= input.length;
        if (slot >= 0 && slot < output.length) {
            output[slot] = is;
        }
    }
    
    @Override
    public ItemStack decrStackSize(int i, int amount) {
        markDirty();
        return super.decrStackSize(i, amount);
    }

    @Override
    public String getInventoryName() {
        return "Mixer";
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int s) {
        ForgeDirection side = ForgeDirection.getOrientation(s);
        if (side == ForgeDirection.DOWN) {
            return OUT_s;
        }
        return IN_s;
    }
    
    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
        return slotIndex < input.length;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        charge.update();
        rotation += speed;
        shareRotationSpeed();
    }

    int last_speed = -1;

    void shareRotationSpeed() {
        if (speed != last_speed) {
            broadcastMessage(null, MessageType.MixerSpeed, speed);
            last_speed = speed;
        }
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.MixerSpeed) {
            speed = input.readInt();
            return true;
        }
        return false;
    }

    @Override
    protected int getLogicSpeed() {
        return 4;
    }

    int getRemainingProgress() {
        return 250 - progress;
    }

    float rotation = 0;

    public float getRotation() {
        return rotation;
    }
    
    public static class WeirdRecipeException extends Throwable {}

    public static final CraftingManagerGeneric<TileEntityMixer> recipes = CraftingManagerGeneric.get(TileEntityMixer.class);

    public static class RecipeMatchInfo implements IVexatiousCrafting<TileEntityMixer> {
        public ArrayList inputs = new ArrayList();
        public ItemStack output;
        public IRecipe theRecipe;
        public int size = 0;

        public void add(Object o) {
            if (o instanceof ItemStack) {
                ItemStack it = (ItemStack) o;
                o = it = it.copy();
                int s = Math.min(1, it.stackSize);
                it.stackSize = s;
                for (int i = 0; i < inputs.size(); i++) {
                    Object h = inputs.get(i);
                    if (h instanceof ItemStack) {
                        ItemStack here = (ItemStack) h;
                        if (ItemUtil.couldMerge(here, it)) {
                            here.stackSize += s;
                            size += s;
                            return;
                        }
                    }
                }
                inputs.add(it);
                size += s;
                return;
            }
            inputs.add(o);
            size++;
        }
        
        public RecipeMatchInfo(List<Object> recipeInput, ItemStack recipeOutput, IRecipe theRecipe) throws WeirdRecipeException {
            for (Object o : recipeInput) {
                if (o instanceof ItemStack) {
                    add(o);
                } else if (o instanceof Collection) {
                    if (((Collection) o).size() == 0) {
                        throw new WeirdRecipeException();
                    }
                    ArrayList<ItemStack> parts = new ArrayList();
                    for (Object p : (Collection)o) {
                        if (p instanceof ItemStack) {
                            parts.add((ItemStack) p);
                        }
                    }
                    add(parts);
                } else {
                    Core.logSevere("Don't know how to use %s in a recipe", o);
                    throw new WeirdRecipeException();
                }
            }
            this.output = recipeOutput;
            this.theRecipe = theRecipe;
        }

        @Override
        public boolean matches(TileEntityMixer machine) {
            ItemStack[] input = machine.input;
            ItemStack[] in = new ItemStack[input.length];
            int inputCount = 0;
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    in[i] = input[i].copy();
                    inputCount++;
                }
            }
            int foundCount = 0;
            for (int i = 0; i < inputs.size(); i++) {
                Object o = inputs.get(i);
                List<ItemStack> all;
                if (o instanceof ItemStack) {
                    all = Arrays.asList((ItemStack)o);
                } else {
                    all = new ArrayList();
                    all.addAll((List<ItemStack>) o);
                }
                boolean found = false;
                for (int R = 0; R < all.size(); R++) {
                    ItemStack recipeItem = all.get(R);
                    if (removeMatching(in, recipeItem)) {
                        found = true;
                        if (all.size() > 1) {
                            all = Arrays.asList(recipeItem);
                        }
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
                foundCount++;
            }
            return foundCount >= this.inputs.size();
        }

        public boolean removeMatching(ItemStack[] hay, ItemStack needle) {
            needle = needle.copy();
            for (int i = 0; i < hay.length; i++) {
                if (ItemUtil.wildcardSimilar(needle, hay[i])) {
                    int delta = Math.min(hay[i].stackSize, needle.stackSize);
                    hay[i].stackSize -= delta;
                    needle.stackSize -= delta;
                    hay[i] = ItemUtil.normalize(hay[i]);
                    if (needle.stackSize <= 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onCraftingStart(TileEntityMixer machine) {

        }

        @Override
        public void onCraftingComplete(TileEntityMixer machine) {
            InventoryCrafting craft = CraftUtil.makeCraftingGrid();
            int craft_slot = 0;
            FzInv inv = InvUtil.openInventory(machine, ForgeDirection.UP);
            ArrayList recipeInputs = new ArrayList();
            for (Object o : this.inputs) {
                if (o instanceof ItemStack) {
                    recipeInputs.add(((ItemStack) o).copy());
                } else {
                    ArrayList<ItemStack> cp = new ArrayList();
                    for (ItemStack is : (Iterable<ItemStack>) o) {
                        cp.add(is.copy());
                    }
                    recipeInputs.add(cp);
                }
            }
            for (int i_input = 0; i_input < recipeInputs.size(); i_input++) {
                Object o = recipeInputs.get(i_input);
                if (o instanceof ItemStack) {
                    ItemStack is = (ItemStack) o;
                    craft_slot = machine.add(craft, craft_slot, inv.pull(is, is.stackSize, false));
                    is.stackSize = 0;
                } else {
                    for (ItemStack is : ((Iterable<ItemStack>) o)) {
                        ItemStack got = ItemUtil.normalize(inv.pull(is, 1, false));
                        if (got != null) {
                            for (Iterator<ItemStack> iterator = ((Iterable<ItemStack>) o).iterator(); iterator.hasNext();) {
                                ItemStack others = iterator.next();
                                if (others != is) {
                                    iterator.remove();
                                }
                            }
                            is.stackSize--;
                            craft_slot = machine.add(craft, craft_slot, got);
                            break;
                        }
                    }
                }
            }
            EntityPlayer fakePlayer = PlayerUtil.makePlayer(machine.getCoord(), "Mixer");
            IInventory craftResult = new InventoryCraftResult();
            ItemStack out = this.output.copy();
            if (out.stackSize < 1) {
                out.stackSize = 1;
            }
            craftResult.setInventorySlotContents(0, out);
            SlotCrafting slot = new SlotCrafting(fakePlayer, craft, craftResult, 0, 0, 0);
            slot.onPickupFromSlot(fakePlayer, out);
            machine.outputBuffer.add(out);
            CraftUtil.addInventoryToArray(craft, machine.outputBuffer);
            CraftUtil.addInventoryToArray(fakePlayer.inventory, machine.outputBuffer);
            machine.markDirty();
            PlayerUtil.recycleFakePlayer(fakePlayer);
        }

        @Override
        public boolean isUnblocked(TileEntityMixer machine) {
            return addItems(copyArray(machine.output), new ItemStack[] { this.output.copy() });
        }
    }

    private static boolean recipes_loaded = false;

    public static void loadShapelessRecipes() {
        if (recipes_loaded) return;
        recipes_loaded = true;
        ArrayList<RecipeMatchInfo> found = new ArrayList<RecipeMatchInfo>();
        outer: for (Object o: CraftingManager.getInstance().getRecipeList()) {
            IRecipe recipe = (IRecipe) o;
            List<Object> inputList = null;
            ItemStack output = null;
            if (recipe instanceof ShapelessRecipes) {
                ShapelessRecipes sr = (ShapelessRecipes) recipe;
                inputList = sr.recipeItems;
                output = sr.getRecipeOutput();
            }
            if (recipe instanceof ShapelessOreRecipe) {
                ShapelessOreRecipe sr = (ShapelessOreRecipe) recipe;
                inputList = sr.getInput();
                output = sr.getRecipeOutput();
            }
            if (inputList == null) {
                continue;
            }
            if (output == null) {
                continue;
            }
            output = output.copy();
            int s = inputList.size();
            if (s <= 1 || s > 9) {
                continue;
            }
            for (int i = 0; i < inputList.size(); i++) {
                Object p = inputList.get(i);
                if (p instanceof String) {
                    ArrayList<ItemStack> ores = OreDictionary.getOres((String) p);
                    for (int X = 0; X < ores.size(); X++) {
                        ores.set(X, ores.get(X).copy());
                    }
                    p = ores;
                }
                if (p instanceof List) {
                    for (ItemStack is : (List<ItemStack>)p) {
                        if (!isOkayRecipeItem(is)) {
                            continue outer;
                        }
                    }
                }
                if (p instanceof ItemStack) {
                    if (!isOkayRecipeItem((ItemStack) p)) {
                        continue outer;
                    }
                }
                
            }
            try {
                found.add(new RecipeMatchInfo(inputList, output, recipe));
            } catch (WeirdRecipeException e) {
                // Ignored...
            }
        }
        Collections.sort(found, new Comparator<RecipeMatchInfo>() {
            @SuppressWarnings("SubtractionInCompareTo")
            @Override
            public int compare(RecipeMatchInfo o1, RecipeMatchInfo o2) {
                return o2.size - o1.size; // The size is tiny.
            }
        });
        for (RecipeMatchInfo rmi : found) {
            recipes.add(rmi);
        }
    }
    
    public static boolean isOkayRecipeItem(ItemStack is) {
        if (is == null) {
            return false;
        }
        is = is.copy();
        Item item = is.getItem();
        if (item == null) {
            return false;
        }
        if (item == Items.paper || item == Items.book) {
            return false;
        }
        /*if (is.getItemDamage() > 0xFF) {
            return false;
        }*/ //??? What was this for?
        if (item.hasContainerItem(is)) {
            //We're going to filter out items like:
            //  Logic matrix programmers
            //  Diamond drawplates
            ItemStack container = ItemUtil.normalize(item.getContainerItem(is));
            if (container == null) {
                return true;
            }
            if (container.getItem() != item) {
                //Like water bucket -> empty bucket; OK
                return true;
            }
            if (container.isItemStackDamageable() || container.isItemDamaged()) {
                //Like a drawplate
                return false;
            }
            if (container.isItemEqual(is)) {
                //Like a logic matrix programmer
                return false;
            }
        }
        return true;
    }
    
    IVexatiousCrafting<TileEntityMixer> getRecipe() {
        boolean empty = true;
        for (int i = 0; i < input.length; i++) {
            if (input[i] != null) {
                empty = false;
                break;
            }
        }
        if (empty) {
            return null;
        }
        loadShapelessRecipes();
        return recipes.find(this);
    }

    static ItemStack[] copyArray(ItemStack src[]) {
        ItemStack clone[] = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                clone[i] = src[i].copy();
            }
        }
        return clone;
    }

    static boolean addItems(ItemStack out[], ItemStack src[]) {
        for (ItemStack is : src) {
            //increase already-started stacks
            for (int i = 0; i < out.length; i++) {
                if (out[i] != null && ItemUtil.couldMerge(is, out[i])) {
                    int free = out[i].getMaxStackSize() - out[i].stackSize;
                    int delta = Math.min(free, is.stackSize);
                    is.stackSize -= delta;
                    out[i].stackSize += delta;
                    is = ItemUtil.normalize(is);
                    if (is == null) {
                        break;
                    }
                }
            }
            if (is == null) {
                continue;
            }
            //create a new stack in an empty slot
            for (int i = 0; i < out.length; i++) {
                if (out[i] == null) {
                    out[i] = is.copy();
                    is = null;
                    break;
                }
            }
            if (is == null) {
                continue;
            }
            normalize(out);
            normalize(src);
            return false;
        }
        normalize(out);
        normalize(src);
        return true;
    }

    IVexatiousCrafting<TileEntityMixer> cache = null;
    boolean dirty = true;

    IVexatiousCrafting<TileEntityMixer> getCachedRecipe() {
        if (!dirty) {
            return cache;
        }
        dirty = false;
        cache = getRecipe();
        return cache;
    }

    void slow() {
        if (progress > 0) {
            progress = (int) Math.max(0, progress * 0.8 - 5);
        }
        if (speed > 0) {
            speed--;
        }
    }

    boolean extractEnergy() {
        int i = Math.max(2, speed);
        return charge.tryTake(i) > 0;
    }
    
    int add(InventoryCrafting craft, int craft_slot, ItemStack is) {
        if (is == null) {
            return craft_slot;
        }
        if (is.stackSize > is.getMaxStackSize() || is.stackSize < 1) {
            Core.logWarning("%s: Trying to craft with %s, which has a stack size of %s", getCoord(), is, is.stackSize);
            craft.setInventorySlotContents(craft_slot++, is);
            return craft_slot;
        }
        while (is.stackSize > 0) {
            craft.setInventorySlotContents(craft_slot++, is.splitStack(1));
        }
        return craft_slot;
    }
    
    boolean dumpBuffer() {
        if (outputBuffer.size() > 0) {
            ItemStack toAdd = outputBuffer.get(0);
            FzInv out = InvUtil.openInventory(this, ForgeDirection.DOWN);
            out.setInsertForce(true);
            Iterator<ItemStack> it = outputBuffer.iterator();
            while (it.hasNext()) {
                if (out.push(it.next()) == null) {
                    it.remove();
                }
            }
        }
        return outputBuffer.size() > 0;
    }

    @Override
    protected void doLogic() {
        needLogic();
        if (dumpBuffer()) {
            return;
        }

        IVexatiousCrafting<TileEntityMixer> mr = getCachedRecipe();
        if (mr == null) {
            slow();
            return;
        }
        if (!mr.isUnblocked(this)) {
            slow();
            return;
        }
        if (speed < 5 && extractEnergy()) {
            speed++;
        } else if (!extractEnergy() && speed > 0) {
            int ns = Math.min(speed - 1, (int)(speed*0.8));
            ns = Math.max(ns, 0);
            speed = ns;
        }
        if (progress == 0 && speed > 0) {
            mr.onCraftingStart(this);
        }
        progress += speed;
        if (getRemainingProgress() <= 0 || Core.cheat) {
            if (!mr.matches(this)) {
                markDirty();
                dirty = true;
                progress = 0;
                return;
            }
            progress = 0;
            mr.onCraftingComplete(this);
            normalize(input);
            speed = Math.min(50, speed + 1);
            dumpBuffer();
        }
    }

    static void normalize(ItemStack is[]) {
        for (int i = 0; i < is.length; i++) {
            is[i] = ItemUtil.normalize(is[i]);
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    public int getMixProgressScaled(int scale) {
        return (progress * scale) / (progress + getRemainingProgress());
    }
}
