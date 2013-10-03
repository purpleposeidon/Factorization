package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityGrinder extends TileEntityFactorization implements IChargeConductor {
    ItemStack input, output;
    Charge charge = new Charge(this);
    int progress = 0;
    float energy = 0;
    int speed = 0;
    final int grind_time = 75;

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
        onInventoryChanged();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSlotsFromNBT(tag);
        charge.readFromNBT(tag);
        progress = tag.getInteger("progress");
        energy = tag.getFloat("fenergy");
        speed = tag.getInteger("speed");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSlotsToNBT(tag);
        charge.writeToNBT(tag);
        tag.setInteger("progress", progress);
        tag.setFloat("fenergy", energy);
        tag.setInteger("speed", speed);
    }

    @Override
    public String getInvName() {
        return "Grinder";
    }
    
    final private static int[] INPUT_s = {0}, OUT_s = {1};

    @Override
    public int[] getAccessibleSlotsFromSide(int s) {
        ForgeDirection side = ForgeDirection.getOrientation(s);
        if (side == ForgeDirection.DOWN) {
            return OUT_s;
        }
        return INPUT_s;
    }
    
    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
        return slotIndex == 0;
    }

    @Override
    public Charge getCharge() {
        return charge;
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
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.GrinderSpeed) {
            speed = input.readInt();
            return true;
        }
        return false;
    }

    public int rotation, prev_rotation;

    @Override
    public void updateEntity() {
        prev_rotation = rotation;
        rotation += speed;
        charge.update();
        super.updateEntity();
    }
    
    @Override
    protected
    byte getExtraInfo2() {
        return (byte) speed;
    }
    
    @Override
    protected
    void useExtraInfo2(byte b) {
        speed = b;
    }
    
    @Override
    void doLogic() {
        shareSpeed();
        needLogic();
        if (energy < 30) {
            if (charge.getValue() >= 15) {
                energy += charge.deplete(60) / 10;
            }
        }
        if (energy <= 0) {
            slowDown();
            if (progress > 0) {
                progress--;
            }
            energy = 0;
            return;
        }
        
        boolean powered = getCoord().isPowered();
        boolean grind_flag = canGrind();
        if (grind_flag || powered) {
            if (speed < 50) {
                speed++;
                energy -= 2;
            } else if (grind_flag) {
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
        }
        if (!grind_flag) {
            progress = 0;
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    public static ArrayList<GrinderRecipe> recipes = new ArrayList();

    public static void addRecipe(Object input, ItemStack output, float probability) {
        GrinderRecipe toAdd = new GrinderRecipe(input, output, probability);
        for (GrinderRecipe gr : recipes) {
            if (gr.getOreDictionaryInput().equals(input)) {
                return;
            }
        }
        recipes.add(toAdd);
    }

    boolean canGrind() {
        input = FactorizationUtil.normalize(input);
        if (input == null) {
            return false;
        }
        GrinderRecipe gr = getRecipe();
        if (gr == null) {
            return false;
        }
        if (output == null) {
            return true;
        }
        return FactorizationUtil.couldMerge(output, gr.output) 
                && output.stackSize + Math.ceil(gr.probability) <= output.getMaxStackSize();
    }
    
    GrinderRecipe getRecipe() {
        for (GrinderRecipe gr : recipes) {
            for (ItemStack is : gr.getInput()) {
                if (FactorizationUtil.wildcardSimilar(is, input)) {
                    return gr;
                }
            }
        }
        return null;
    }

    void grind() {
        GrinderRecipe gr = getRecipe();
        if (gr == null) {
            return;
        }
        for (ItemStack is : gr.getInput()) {
            if (!FactorizationUtil.wildcardSimilar(is, input)) {
                continue;
            }
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

    public int getGrindProgressScaled(int total) {
        return total * progress / grind_time;
    }

    public static class GrinderRecipe {
        private String oreName = null;
        private ItemStack itemstack = null;
        private ArrayList<ItemStack> inputArray = new ArrayList();
        public ItemStack output;
        public float probability;

        GrinderRecipe(Object input, ItemStack output, float probability) {
            this.output = output;
            this.probability = probability;
            if (input instanceof Block) {
                itemstack = new ItemStack((Block) input, 1, FactorizationUtil.WILDCARD_DAMAGE);
            } else if (input instanceof Item) {
                itemstack = new ItemStack((Item) input, 1, FactorizationUtil.WILDCARD_DAMAGE);
            } else if (input instanceof ItemStack) {
                itemstack = (ItemStack) input;
            } else {
                this.oreName = (String) input;
                return;
            }
            inputArray.add(itemstack);
        }
        
        public ArrayList<ItemStack> getInput() {
            if (oreName != null) {
                return OreDictionary.getOres(oreName);
            }
            ArrayList<ItemStack> ret = new ArrayList(1);
            return inputArray;
        }
        
        public Object getOreDictionaryInput() {
            if (oreName != null) {
                return oreName;
            }
            return itemstack;
        }
        
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.grinder_top;
    }
}
