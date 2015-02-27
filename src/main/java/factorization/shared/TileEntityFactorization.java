package factorization.shared;

import java.io.IOException;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.IFactoryType;
import factorization.common.FactoryType;
import factorization.shared.NetworkFactorization.MessageType;

public abstract class TileEntityFactorization extends TileEntityCommon
        implements IInventory, ISidedInventory, ICoord, IFactoryType {

    //Save & Share
    public byte draw_active;
    public byte facing_direction = 3;

    //Runtime
    protected boolean need_logic_check = true;

    @Override
    public abstract FactoryType getFactoryType();

    protected void makeNoise() {
    }

    protected abstract void doLogic();

    protected int getLogicSpeed() {
        return 4;
    }

    protected boolean canFaceVert() {
        return false;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        if (player == null) {
            return;
        }
        facing_direction = (byte) side;
    }

    void setFacingDirectionFromEntity(Entity player) {
        float yaw = player.rotationYaw % 360F;
        if (yaw < 0) {
            yaw += 360F;
        }

        if (canFaceVert()) {
            if (player.rotationPitch <= -45F) {
                facing_direction = 0; //-Y
                return;
            }
            else if (player.rotationPitch >= 65F) {
                facing_direction = 1; //+Y
                return;
            }
        }
        int y = ((int) yaw) / 45;
        switch (y) {
        case 7:
        case 0:
            facing_direction = 2;
            break;
        case 1:
        case 2:
            facing_direction = 5;
            break;
        case 3:
        case 4:
            facing_direction = 3;
            break;
        case 5:
        case 6:
            facing_direction = 4;
            break;
        }
    }

    protected void needLogic() {
        need_logic_check = true;
    }

    @Override
    public final Coord getCoord() {
        return new Coord(this);
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        dropContents();
    }

    public void dropContents() {
        // XXX TODO: ModLoader.genericContainerRemoval
        Coord here = getCoord();
        for (int i = 0; i < getSizeInventory(); i++) {
            InvUtil.spawnItemStack(here, getStackInSlot(i));
        }
    }
    
    double round(double c) {
        if (Math.abs(c) < 0.5) {
            return 0;
        }
        return Math.copySign(1, c);
    }

    protected Entity ejectItem(ItemStack is, boolean violent, EntityPlayer player, int to_side) {
        if (worldObj.isRemote) {
            return null;
        }
        if (is == null || is.stackSize == 0) {
            return null;
        }
        if (player == null) {
            to_side = -1;
        }
        double mult = 0.02;
        if (violent) {
            mult = 0.2;
        }
        Vec3 pos = Vec3.createVectorHelper(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
        Vec3 vel = Vec3.createVectorHelper(0, 0, 0);
        if (to_side != -1) {
            ForgeDirection dir = ForgeDirection.getOrientation(to_side);
            /*
            ent.motionX = dir.offsetX;
            ent.motionY = dir.offsetY;
            ent.motionZ = dir.offsetZ;*/
            double d = 0.75;
            pos.xCoord += dir.offsetX*d;
            pos.yCoord += dir.offsetY*d;
            pos.zCoord += dir.offsetZ*d;
            vel.xCoord = dir.offsetX;
            vel.yCoord = dir.offsetY;
            vel.zCoord = dir.offsetZ;
        } else if (player != null) {
            // point velocity towards player
            Vec3 vec = Vec3.createVectorHelper(player.posX - xCoord, player.posY - yCoord,
                    player.posZ - zCoord);
            vec = vec.normalize();
            vel = vec;
            double d = 0.25;
            // move item to near the edge
            pos.xCoord += vec.xCoord*d;
            pos.yCoord += vec.yCoord*d;
            pos.zCoord += vec.zCoord*d;
            //Minecraft.getMinecraft().theWorld.spawnParticle("reddust", ent.posX, ent.posY, ent.posZ, 0, 0, 0);
        } else {
            // random velocity
            vel.xCoord = rand.nextGaussian();
            vel.yCoord = rand.nextGaussian();
            vel.zCoord = rand.nextGaussian();
        }
        Entity ent = getCoord().spawnItem(is);
        ent.motionX = vel.xCoord*mult;
        ent.motionY = vel.yCoord*mult;
        ent.motionZ = vel.zCoord*mult;
        return ent;
    }

    @Override
    public ItemStack decrStackSize(int i, int amount) {
        ItemStack target = ItemUtil.normalize(getStackInSlot(i));
        if (target == null) {
            return null;
        }

        if (target.stackSize <= amount) {
            ItemStack ret = target;
            setInventorySlotContents(i, null);
            return ret;
        }

        ItemStack ret = target.splitStack(amount);
        if (target.stackSize == 0) {
            setInventorySlotContents(i, null);
        }
        return ret;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        needLogic();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return 8 * 8 >= player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
    }

    @Override
    public final void openInventory() {
    }

    @Override
    public final void closeInventory() {
    }
    
    /*@Override
    public boolean acceptsStackInSlot(int i, ItemStack itemstack) {
        return true;
    }*/
    
    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        draw_active = data.as(Share.VISIBLE, "draw_active_byte").putByte(draw_active);
        facing_direction = data.as(Share.VISIBLE, "facing").putByte(facing_direction);
    }

    public final void putSlots(DataHelper data) {
        if (!data.isNBT()) return;
        NBTTagCompound tag = data.getTag();
        if (data.isWriter()) {
            writeSlotsToNBT(tag);
        } else {
            readSlotsFromNBT(tag);
        }
    }

    private void readSlotsFromNBT(NBTTagCompound tag) {
        NBTTagList invlist = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < invlist.tagCount(); i++) {
            NBTTagCompound comp = invlist.getCompoundTagAt(i);
            setInventorySlotContents(comp.getInteger("Slot"), ItemStack.loadItemStackFromNBT(comp));
        }
    }

    private void writeSlotsToNBT(NBTTagCompound tag) {
        NBTTagList invlist = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            NBTTagCompound comp = new NBTTagCompound();
            comp.setInteger("Slot", i);
            stack.writeToNBT(comp);
            invlist.appendTag(comp);
        }
        tag.setTag("Items", invlist);
    }

    protected static void saveItem(String name, NBTTagCompound tag, ItemStack is) {
        // TODO: Are these two as used as they could be?
        if (is == null) {
            return;
        }
        NBTTagCompound itag = new NBTTagCompound();
        is.writeToNBT(itag);
        tag.setTag(name, itag);
    }

    protected static ItemStack readItem(String name, NBTTagCompound tag) {
        if (tag.hasKey(name)) {
            return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(name));
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }
    
    @Override
    public boolean canInsertItem(int i, ItemStack itemstack, int j) {
        return isItemValidForSlot(i, itemstack);
    }
    
    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        return true;
    }

    public void drawActive(int add_time) {
        int new_active = draw_active + add_time;
        if (new_active < 0) {
            new_active = 0;
        }
        if (new_active > 32) {
            new_active = 32;
        }
        if (draw_active != new_active) {
            draw_active = (byte) new_active;
            if (!worldObj.isRemote) {
                broadcastMessage(null, MessageType.DrawActive, draw_active);
            }
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            if (draw_active > 0) {
                makeNoise();
                worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
                draw_active--;
            }
        } else {
            draw_active = (draw_active > 0) ? (byte)(draw_active - 1) : 0;
            if (need_logic_check && 0 == worldObj.getTotalWorldTime() % getLogicSpeed()) {
                need_logic_check = false;
                doLogic();
            }
        }
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.DrawActive) {
            draw_active = input.readByte();
            getCoord().redraw();
            return true;
        }
        return false;
    }
}
