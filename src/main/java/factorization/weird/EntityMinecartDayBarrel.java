package factorization.weird;

import factorization.api.FzOrientation;
import factorization.shared.Core;
import factorization.shared.NORELEASE;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class EntityMinecartDayBarrel extends EntityMinecart implements IInventory {
    protected TileEntityDayBarrel barrel;
    private ItemStack stack;
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
        this.stack = is;
    }

    private NBTTagCompound writeItemStackToNBT(ItemStack is) {
        NBTTagCompound tag = new NBTTagCompound();
        is.writeToNBT(tag);
        return tag;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);

        if (nbt.hasKey("BarrelType")) {
            barrel.woodLog = DataUtil.tag2item(nbt.getCompoundTag("BarrelLog"), barrel.woodLog);
            barrel.woodSlab = DataUtil.tag2item(nbt.getCompoundTag("BarrelSlab"), barrel.woodSlab);
            barrel.setItemCount(nbt.getInteger("BarrelItemCount"));
            barrel.orientation = FzOrientation.getOrientation(nbt.getByte("BarrelOrientation"));
            barrel.type = TileEntityDayBarrel.Type.values()[nbt.getByte("BarrelType")];
            if (nbt.hasKey("BarrelItem")) {
                barrel.item = DataUtil.tag2item(nbt.getCompoundTag("BarrelItem"), new ItemStack(Blocks.air));
                if (barrel.item.getItem() == Item.getItemFromBlock(Blocks.air)) {
                    barrel.item = null;
                }
            }
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        if (barrel != null) {
            if (barrel.item != null) {
                nbt.setTag("BarrelItem", DataUtil.item2tag(barrel.item));
            }
            nbt.setTag("BarrelLog", DataUtil.item2tag(barrel.woodLog));
            nbt.setTag("BarrelSlab", DataUtil.item2tag(barrel.woodSlab));
            nbt.setInteger("BarrelItemCount", barrel.getItemCount());
            nbt.setByte("BarrelOrientation", (byte) barrel.orientation.ordinal());
            nbt.setByte("BarrelType", (byte) barrel.type.ordinal());
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();

        if (barrel == null) {
            barrel = new TileEntityDayBarrel();
            barrel.setWorldObj(worldObj);
            barrel.xCoord = barrel.yCoord = barrel.zCoord = 0;
            barrel.validate();
            barrel.orientation = FzOrientation.fromDirection(ForgeDirection.WEST).pointTopTo(ForgeDirection.UP);
            if (stack != null) {
                barrel.loadFromStack(stack);
            } else {
                barrel.type = TileEntityDayBarrel.Type.HOPPING;
                TileEntityDayBarrel.getLog(new ItemStack(Blocks.stone_brick_stairs));
                TileEntityDayBarrel.getSlab(new ItemStack(Blocks.stone_brick_stairs));
            }
        }

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
        activatorRailTicks = 2;
        activatorRailPowered = powered;
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
            barrel.draw_active = 2;

            if (barrel.canUpdate()) {
                if (activatorRailTicks > 0) {
                    NORELEASE.fixme("NEPTUNAL TODO: Use activatorRailPowered for something useful.");
                    NORELEASE.fixme("<asie> neptunepunk: one more bug! in the entity's readEntityFromNBT\n" +
                            "[00:57:51] <asie> make the item count load AFTER the item\n" +
                            "[00:57:55] <asie> or it will load \"0 Light Blue Wool\"");
                    NORELEASE.fixme("Notice should be sent to cart entity, not to barrel");
                    NORELEASE.fixme("fix serialization");
                }
                barrel.updateEntity();
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
