package factorization.charge;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.notify.Notify;
import factorization.shared.BlockClass;
import factorization.shared.FzUtil;
import factorization.shared.Sound;
import factorization.shared.TileEntityFactorization;

public class TileEntityCaliometricBurner extends TileEntityFactorization implements IDataSerializable {
    ItemStack stomache;
    int foodQuality = 0;
    int ticksUntilNextDigestion = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CALIOMETRIC_BURNER;
    }

    @Override
    public String getInventoryName() {
        return "Caliometric Burner";
    }
    
    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        stomache = data.as(Share.PRIVATE, prefix + "stomache").putItemStack(stomache);
        foodQuality = data.as(Share.PRIVATE, prefix + "food").putInt(foodQuality);
        ticksUntilNextDigestion = data.as(Share.PRIVATE, prefix + "digest").putInt(ticksUntilNextDigestion);
        return this;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        try {
            (new DataOutNBT(tag)).as(Share.PRIVATE, "").put(this);
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        try {
            (new DataInNBT(tag)).as(Share.PRIVATE, "").put(this);
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i == 0) {
            return stomache;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i == 0) {
            stomache = itemstack;
        }
    }
    
    @Override
    public int getInventoryStackLimit() {
        return 4;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        if (itemstack == null) {
            return false;
        }
        return getFoodValue(itemstack) > 0;
    }

    private static final int[] nomslots = new int[] {0}, emptySlots = new int[] {};
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (ForgeDirection.getOrientation(side).offsetY != 0) {
            return emptySlots; //Food goes in through the teeth
        }
        return nomslots;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ForgeDirection dir) {
        if (dir.offsetY != 0) {
            return BlockIcons.caliometric_top;
        }
        return BlockIcons.caliometric_side;
    }

    @Override
    protected void doLogic() {
        needLogic();
        Coord here = getCoord();
        if (ticksUntilNextDigestion > 0 && foodQuality > 0) {
            for (Coord c : here.getRandomNeighborsAdjacent()) {
                TileEntitySolarBoiler boiler = c.getTE(TileEntitySolarBoiler.class);
                if (boiler == null) {
                    continue;
                }
                boiler.applyHeat(foodQuality);
                break;
            }
        }
        ticksUntilNextDigestion--;
        if (ticksUntilNextDigestion <= 0 && !here.isPowered()) {
            foodQuality = consumeFood();
        }
    }
    
    @Override
    protected int getLogicSpeed() {
        return 1;
    }
    
    int consumeFood() {
        stomache = FzUtil.normalize(stomache);
        if (stomache == null) {
            return 0;
        }
        int noms = getFoodValue(stomache);
        stomache = FzUtil.normalDecr(stomache);
        markDirty();
        Sound.caliometricDigest.playAt(this);
        ticksUntilNextDigestion = 20*10*noms;
        return 16;
    }
    
    int getFoodValue(ItemStack is) {
        if (is == null) {
            return 0;
        }
        Item it = is.getItem();
        int heal = 0;
        double sat = 0;
        if (it instanceof ItemFood) {
            ItemFood nom = (ItemFood) it;
            heal = nom.func_150905_g(is);
            sat = nom.func_150906_h(is);
        } else if (it == Items.cake) {
            heal = 2*6;
            sat = 0.1F;
        }
        heal += Math.min(0, heal*2*sat);
        int r = (int)(heal*(heal/4F));
        return Math.max(heal, r)/2;
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return true;
        }
        ItemStack is = entityplayer.getHeldItem();
        if (is == null) {
            info(entityplayer);
            return false;
        }
        is = FzUtil.openInventory(this, ForgeDirection.NORTH).push(is);
        entityplayer.setCurrentItemOrArmor(0, is);
        info(entityplayer);
        markDirty();
        return true;
    }
    
    void info(EntityPlayer entityplayer) {
        String append = "";
        if (ticksUntilNextDigestion > 0) {
            int n = (ticksUntilNextDigestion/20);
            int min = n/60;
            int s = n % 60;
            append = "\n" + min + ":";
            if (s < 10) {
                append += "0";
            }
            append += s;
        }
        boolean any = false;
        for (Coord c : getCoord().getRandomNeighborsAdjacent()) {
            TileEntitySolarBoiler boiler = c.getTE(TileEntitySolarBoiler.class);
            if (boiler != null) {
                any = true;
                break;
            }
        }
        if (!any) {
            append += "\n" + "No adjacent boiler!";
        }
        if (stomache == null || stomache.stackSize == 0) {
            Notify.send(entityplayer, this, "Empty" + append);
            return;
        }
        Notify.withItem(stomache);
        Notify.send(entityplayer, this, stomache.stackSize + " {ITEM_NAME}" + append);
    }
    
    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        return false;
    }
}
