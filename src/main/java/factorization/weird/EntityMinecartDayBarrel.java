package factorization.weird;

import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class EntityMinecartDayBarrel extends EntityMinecart implements IInventory {
    protected TileEntityDayBarrel barrel;
    private int activatorRailTicks = 0;
    private boolean activatorRailPowered;

    public EntityMinecartDayBarrel(World world) {
        super(world);
    }

    public EntityMinecartDayBarrel(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void killMinecart(DamageSource damage) {
        this.setDead();
        if (barrel != null) {
            barrel.dropContents();
        }

        ItemStack itemstack = getCartItem();
        if (this.func_95999_t() != null) {
            itemstack.setStackDisplayName(this.func_95999_t());
        }

        this.entityDropItem(itemstack, 0.0F);
    }

    @Override
    public ItemStack getCartItem() {
        ItemStack stack = barrel.getDroppedBlock();
        ItemStack realStack = new ItemStack(Core.registry.barrelCart, 1);
        realStack.setTagCompound(stack.getTagCompound());
        return realStack;
    }

    @Override
    public int getMinecartType() {
        return 1;
    }

    public void initFromStack(ItemStack is) {
        barrel.loadFromStack(is);
        updateDataWatcher(true);
    }

    private void create_barrel() {
        if (barrel != null) return;
        barrel = new TileEntityDayBarrel();
        barrel.setWorldObj(worldObj);
        barrel.xCoord = barrel.yCoord = barrel.zCoord = 0;
        barrel.validate();
        barrel.orientation = FzOrientation.fromDirection(ForgeDirection.WEST).pointTopTo(ForgeDirection.UP);
        barrel.notice_target = this;
    }

    public void putData(DataHelper data) throws IOException {
        if (data.isReader() && barrel == null) {
            create_barrel();
        }
        if (barrel != null) {
            barrel.putData(data);
            if (data.isReader() && !worldObj.isRemote) {
                updateDataWatcher(true);
            }
        }
    }

    @Override
    protected final void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        DataHelper data = new DataInNBT(tag);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        DataHelper data = new DataOutNBT(tag);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        create_barrel();

        dataWatcher.addObjectByDataType(23, 5);
        dataWatcher.updateObject(23, barrel.item);

        dataWatcher.addObject(24, barrel.getItemCount());
        dataWatcher.addObject(25, barrel.woodLog);
        dataWatcher.addObject(26, barrel.woodSlab);
        dataWatcher.addObject(27, (byte) barrel.orientation.ordinal());
        dataWatcher.addObject(28, (byte) barrel.type.ordinal());
    }

    private void updateDataWatcher(boolean full) {
        if (!worldObj.isRemote) {
            dataWatcher.updateObject(23, barrel.item);
            dataWatcher.updateObject(24, barrel.getItemCount());
            if (full) {
                dataWatcher.updateObject(25, barrel.woodLog);
                dataWatcher.updateObject(26, barrel.woodSlab);
                dataWatcher.updateObject(27, (byte) barrel.orientation.ordinal());
                dataWatcher.updateObject(28, (byte) barrel.type.ordinal());
            }
        }
    }

    @Override
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
        if (powered) {
            activatorRailTicks = barrel.getLogicSpeed();
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (worldObj.isRemote && dataWatcher.hasChanges()) {
            barrel.item = dataWatcher.getWatchableObjectItemStack(23);
            barrel.setItemCount(dataWatcher.getWatchableObjectInt(24));
            barrel.woodLog = dataWatcher.getWatchableObjectItemStack(25);
            barrel.woodSlab = dataWatcher.getWatchableObjectItemStack(26);
            barrel.orientation = FzOrientation.getOrientation(dataWatcher.getWatchableObjectByte(27));
            barrel.type = TileEntityDayBarrel.Type.values()[dataWatcher.getWatchableObjectByte(28)];
        }

        if (!worldObj.isRemote) {
            barrel.xCoord = MathHelper.floor_double(posX);
            barrel.yCoord = MathHelper.floor_double(posY);
            barrel.zCoord = MathHelper.floor_double(posZ);
            if (activatorRailTicks > 0) activatorRailTicks--;

            if (barrel.canUpdate() && activatorRailTicks <= 0 && worldObj.getTotalWorldTime() % barrel.getLogicSpeed() == 0) {
                barrel.doLogic();
                updateDataWatcher(false);
            }
        }
    }

    @Override
    public Block func_145817_o() {
        return Core.registry.factory_block;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float f) {
        if (source.getEntity() instanceof EntityPlayer) {
            int oldItemCount = barrel.getItemCount();
            barrel.click((EntityPlayer) source.getEntity());
            updateDataWatcher(false);
            if (source.getEntity().isSneaking()) {
                return super.attackEntityFrom(source, f);
            }
            if (barrel.type == TileEntityDayBarrel.Type.CREATIVE) {
                return false;
            }
            if (barrel.getItemCount() != oldItemCount) {
                return false;
            }
        }
        return super.attackEntityFrom(source, f);
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.minecart.MinecartInteractEvent(this, player))) {
            return true;
        }

        boolean result = barrel.activate(player, ForgeDirection.UNKNOWN);
        updateDataWatcher(false);
        return result;
    }

    @Override
    public int getSizeInventory() {
        return barrel.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return barrel.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack out = barrel.decrStackSize(slot, amount);
        updateDataWatcher(false);
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return barrel.getStackInSlotOnClosing(slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        barrel.setInventorySlotContents(slot, stack);
        updateDataWatcher(false);
    }

    @Override
    public String getInventoryName() {
        return barrel.getInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return barrel.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        barrel.markDirty();
        updateDataWatcher(false);
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return barrel.isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        barrel.openInventory();
    }

    @Override
    public void closeInventory() {
        barrel.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return barrel.isItemValidForSlot(slot, stack);
    }
}
