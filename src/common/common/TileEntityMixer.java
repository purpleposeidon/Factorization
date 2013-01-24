package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityMixer extends TileEntityFactorization implements
        IChargeConductor {
    //inventory: 4 input slots, 4 output slots
    ItemStack input[] = new ItemStack[4], output[] = new ItemStack[4];
    int progress = 0;
    int speed = 0;
    Charge charge = new Charge(this);

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag);
        tag.setInteger("progress", progress);
        tag.setInteger("speed", speed);
        writeSlotsToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag);
        progress = tag.getInteger("progress");
        speed = tag.getInteger("speed");
        readSlotsFromNBT(tag);
        dirty = true;
    }

    @Override
    public int getSizeInventory() {
        return 8;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        dirty = true;
        if (slot >= 0 && slot < 4) {
            return input[slot];
        }
        slot -= 4;
        if (slot >= 0 && slot < 4) {
            return output[slot];
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        dirty = true;
        if (slot >= 0 && slot < 4) {
            input[slot] = is;
            return;
        }
        slot -= 4;
        if (slot >= 0 && slot < 4) {
            output[slot] = is;
            return;
        }
    }

    @Override
    public String getInvName() {
        return "Mixer";
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        if (side == ForgeDirection.UP) {
            return 0;
        }
        return 4;
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        return 4;
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
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
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
    int getLogicSpeed() {
        return 4;
    }

    int getRemainingProgress() {
        return 300 - progress;
    }

    float rotation = 0;

    public float getRotation() {
        return rotation;
    }

    public static ArrayList<MixRecipe> recipes = new ArrayList();
    static boolean sorted = false;

    public static class MixRecipe {
        final public ItemStack inputs[], outputs[];

        public MixRecipe(ItemStack inputs[], ItemStack outputs[]) {
            this.inputs = inputs;
            this.outputs = outputs;
        }

        public boolean matches(ItemStack mixer_inv[]) {
            for (ItemStack is : inputs) {
                boolean found = false;
                for (ItemStack inp : mixer_inv) {
                    if (inp != null && FactorizationUtil.identical(is, inp)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
    }

    static void addRecipe(ItemStack inputs[], ItemStack outputs[]) {
        recipes.add(new MixRecipe(inputs, outputs));
        sorted = false;
    }

    MixRecipe getRecipe() {
        if (!sorted) {
            //this is pointless.
            Collections.sort(recipes, new Comparator<MixRecipe>() {
                @Override
                public int compare(MixRecipe a, MixRecipe b) {
                    return a.inputs.length - b.inputs.length;
                }
            });
            sorted = true;
        }
        for (MixRecipe recipe : recipes) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
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

    boolean addItems(ItemStack out[], ItemStack src[]) {
        for (ItemStack is : src) {
            //increase already-started stacks
            for (int i = 0; i < out.length; i++) {
                if (out[i] != null && FactorizationUtil.identical(is, out[i])) {
                    int free = out[i].getMaxStackSize() - out[i].stackSize;
                    int delta = Math.min(free, is.stackSize);
                    is.stackSize -= delta;
                    out[i].stackSize += delta;
                    is = FactorizationUtil.normalize(is);
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

    boolean hasFreeSpace(MixRecipe mr) {
        return addItems(copyArray(output), copyArray(mr.outputs));
    }

    MixRecipe cache = null;
    boolean dirty = true;

    MixRecipe getCachedRecipe() {
        if (!dirty) {
            return cache;
        }
        dirty = false;
        return cache = getRecipe();
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
        return charge.tryTake(1) > 0;
    }

    @Override
    void doLogic() {
        needLogic();
        MixRecipe mr = getCachedRecipe();
        if (mr == null) {
            slow();
            return;
        }
        if (!hasFreeSpace(mr)) {
            slow();
            return;
        }
        if (!extractEnergy() && speed > 0) {
            speed--;
        }
        if (speed < 5 && extractEnergy()) {
            speed++;
        }
        progress += speed;
        if (getRemainingProgress() <= 0 || Core.cheat) {
            progress = 0;
            addItems(output, copyArray(mr.outputs));
            dirty = true;
            for (ItemStack rec : copyArray(mr.inputs)) {
                for (int i = 0; i < input.length; i++) {
                    if (input[i] == null) {
                        continue;
                    }
                    if (FactorizationUtil.identical(rec, input[i])) {
                        int d = Math.min(input[i].stackSize, rec.stackSize);
                        input[i].stackSize -= d;
                        rec.stackSize -= d;
                        input[i] = FactorizationUtil.normalize(input[i]);
                        rec = FactorizationUtil.normalize(rec);
                    }
                    if (rec == null) {
                        break;
                    }
                }
            }
            normalize(input);
        }
    }

    void normalize(ItemStack is[]) {
        for (int i = 0; i < is.length; i++) {
            is[i] = FactorizationUtil.normalize(is[i]);
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    public int getMixProgressScaled(int scale) {
        return (progress * scale) / (progress + getRemainingProgress());
    }

    @Override
    byte getExtraInfo2() {
        return (byte) speed;
    }

    @Override
    void useExtraInfo(byte b) {
        speed = b;
    }
}
