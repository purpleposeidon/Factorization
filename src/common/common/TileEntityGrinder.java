package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Charge;
import factorization.api.ChargeSink;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityGrinder extends TileEntityFactorization implements IChargeConductor {
    ItemStack input, output;
    ChargeSink chargeSink = new ChargeSink();
    int progress = 0;
    int energy = 0;
    int speed = 0;
    final int grind_time = 400;

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) {
            return input;
        }
        if (slot == 1) {
            return output;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        if (slot == 0) {
            input = is;
        }
        if (slot == 1) {
            output = is;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSlotsFromNBT(tag);
        chargeSink.readFromNBT(tag);
        progress = tag.getInteger("progress");
        energy = tag.getInteger("energy");
        speed = tag.getInteger("speed");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSlotsToNBT(tag);
        chargeSink.writeToNBT(tag);
        tag.setInteger("progress", progress);
        tag.setInteger("energy", energy);
        tag.setInteger("speed", speed);
    }

    @Override
    public String getInvName() {
        return "Grinder";
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        if (side == ForgeDirection.UP) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        return 1;
    }

    @Override
    public Charge getCharge() {
        return chargeSink.getCharge();
    }

    @Override
    public String getInfo() {
        float p = speed * 100 / 50F;
        return "Speed: " + ((int) p) + "%";
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.GRINDER;
    }

    void slowDown() {
        if (speed > 0) {
            speed--;
        }
    }

    int last_speed = 0;

    void shareSpeed() {
        if (speed != last_speed) {
            last_speed = speed;
            broadcastMessage(null, MessageType.GrinderSpeed, speed);
        }
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input)
            throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.GrinderSpeed) {
            speed = input.readInt();
            return true;
        }
        return false;
    }

    public int rotation;

    @Override
    public void updateEntity() {
        rotation += speed;
        super.updateEntity();
    }
    
    @Override
    byte getExtraInfo2() {
        return (byte) speed;
    }
    
    @Override
    void useExtraInfo2(byte b) {
        speed = b;
    }

    @Override
    void doLogic() {
        shareSpeed();
        needLogic();
        Charge.update(this);
        if (worldObj.getWorldTime() % 3 == 0) {
            if (energy < 30) {
                int to_take = chargeSink.takeCharge(30);
                energy += to_take / 5;
            }
        }
        if (energy <= 0) {
            slowDown();
            if (progress > 0) {
                progress--;
            }
            return;
        }
        if (canGrind()) {
            if (speed < 50) {
                speed++;
                energy -= 2;
            } else {
                if (progress == grind_time || Core.cheat) {
                    progress = 0;
                    grind();
                } else {
                    progress++;
                }
                energy -= 1;
            }
        } else {
            slowDown();
            progress = 0;
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    static ArrayList<GrinderRecipe> recipes = new ArrayList();

    public static void addRecipe(ItemStack input, ItemStack output, float probability) {
        recipes.add(new GrinderRecipe(input, output, probability));
    }

    boolean canGrind() {
        input = FactorizationUtil.normalize(input);
        if (input == null) {
            return false;
        }
        for (GrinderRecipe gr : recipes) {
            if (gr.input.isItemEqual(input)) {
                if (output == null) {
                    return true;
                }
                if (!output.isItemEqual(gr.output)) {
                    return false;
                }
                if (output.stackSize + ((int) gr.probability + .99) > output.getMaxStackSize()) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    void grind() {
        for (GrinderRecipe gr : recipes) {
            if (gr.input.isItemEqual(input)) {
                if (output == null) {
                    output = gr.output.copy();
                    output.stackSize = 0;
                }
                int min = (int) gr.probability;
                output.stackSize += min;
                output.stackSize += rand.nextFloat() < (gr.probability - min) ? 1 : 0;
                input.stackSize--;
                input = FactorizationUtil.normalize(input);
                return;
            }
        }
    }

    public int getGrindProgressScaled(int total) {
        return total * progress / grind_time;
    }

    private static class GrinderRecipe {
        ItemStack input, output;
        float probability;

        GrinderRecipe(ItemStack input, ItemStack output, float probability) {
            this.input = input;
            this.output = output;
            this.probability = probability;
        }
    }
}
