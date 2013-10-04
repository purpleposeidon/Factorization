package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.FakePlayer;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.sockets.SocketEmpty;

public abstract class TileEntitySocketBase extends TileEntityCommon implements ISocketHolder, IDataSerializable {
    public static Random rand = new Random();
    public ForgeDirection facing = ForgeDirection.UP;

    @Override
    public final BlockClass getBlockClass() {
        return BlockClass.Socket;
    }

    @Override
    public final byte getExtraInfo() {
        return (byte) facing.ordinal();
    }

    @Override
    public final void useExtraInfo(byte b) {
        facing = ForgeDirection.getOrientation(b);
    }
    
    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        facing = ForgeDirection.getOrientation(side);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        facing = ForgeDirection.getOrientation(tag.getByte("fc"));
        try {
            serialize("", new DataInNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("fc", (byte) facing.ordinal());
        try {
            serialize("", new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected Iterable<Entity> getEntities(Entity ent, Coord c, ForgeDirection top, int d) {
        int one = 1;
        AxisAlignedBB ab = AxisAlignedBB.getAABBPool().getAABB(
                c.x + top.offsetX, c.y + top.offsetY, c.z + top.offsetZ,  
                c.x + one + top.offsetX, c.y + one + top.offsetY, c.z + one + top.offsetZ);
        if (d != 0) {
            ab.minX -= d;
            ab.minY -= d;
            ab.minZ -= d;
            ab.maxX += d;
            ab.maxY += d;
            ab.maxZ += d;
            
            if (top.offsetX + top.offsetY + top.offsetZ > 1) {
                ab.minX += d*top.offsetX;
                ab.minY += d*top.offsetY;
                ab.minZ += d*top.offsetZ;
            } else {
                ab.maxX -= d*top.offsetX;
                ab.maxY -= d*top.offsetY;
                ab.maxZ -= d*top.offsetZ;
            }
        }
        return (Iterable<Entity>)worldObj.getEntitiesWithinAABBExcludingEntity(ent, ab);
    }
    
    protected final boolean rayTrace(final ISocketHolder socket, final Entity ent, final Coord coord, final FzOrientation orientation, final boolean powered, final boolean lookAround, final boolean onlyFirst) {
        final ForgeDirection top = orientation.top;
        final ForgeDirection face = orientation.facing;
        final ForgeDirection right = face.getRotation(top);
        
        
        for (Entity entity : getEntities(ent, coord, top, 0)) {
            if (!entity.canBeCollidedWith()) {
                continue;
            }
            if (handleRay(socket, new MovingObjectPosition(entity), false, powered)) {
                return true;
            }
        }
        
        nullVec.xCoord = nullVec.yCoord = nullVec.zCoord = 0;
        Coord targetBlock = coord.add(top);
        if (mopBlock(targetBlock, top.getOpposite(), socket, false, powered)) return true; //nose-to-nose with the servo
        if (onlyFirst) return false;
        if (mopBlock(targetBlock.add(top), top.getOpposite(), socket, false, powered)) return true; //a block away
        if (mopBlock(targetBlock.add(top.getOpposite()), top, socket, true, powered)) return true;
        if (!lookAround) return false;
        if (mopBlock(targetBlock.add(face), face.getOpposite(), socket, false, powered)) return true; //running forward
        if (mopBlock(targetBlock.add(face.getOpposite()), face, socket, false, powered)) return true; //running backward
        if (mopBlock(targetBlock.add(right), right.getOpposite(), socket, false, powered)) return true; //to the servo's right
        if (mopBlock(targetBlock.add(right.getOpposite()), right, socket, false, powered)) return true; //to the servo's left
        return false;
    }
    
    private static final Vec3 nullVec = Vec3.createVectorHelper(0, 0, 0);
    boolean mopBlock(Coord target, ForgeDirection side, ISocketHolder socket, boolean mopIsThis, boolean powered) {
        if (target.isAir()) {
            return false;
        }
        return handleRay(socket, target.createMop(side, nullVec), mopIsThis, powered);
    }
    
    @Override
    public final ForgeDirection[] getValidRotations() {
        return full_rotation_array;
    }
    
    @Override
    public final boolean rotate(ForgeDirection axis) {
        if (axis == facing) {
            return false;
        }
        facing = axis;
        return true;
    }
    
    @Override
    public final void sendMessage(int msgType, Object ...msg) {
        broadcastMessage(null, msgType, msg);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        if (dir == facing || dir.getOpposite() == facing) {
            return BlockIcons.socket$face;
        }
        return BlockIcons.socket$side;
    }
    
    protected boolean isBlockPowered() {
        return worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
    }
    
    @Override
    public boolean dumpBuffer(List<ItemStack> buffer) {
        if (buffer.size() == 0) {
            return false;
        }
        ItemStack is = buffer.get(0);
        if (is == null) {
            buffer.remove(0);
            return true;
        }
        Coord here = getCoord();
        here.adjust(facing.getOpposite());
        IInventory invTe = here.getTE(IInventory.class);
        if (invTe == null) {
            return true;
        }
        FzInv inv = FactorizationUtil.openInventory(invTe, facing);
        if (inv == null) {
            return true;
        }
        int origSize = is.stackSize;
        int newSize = 0;
        if (inv.push(is) == null) {
            buffer.remove(0);
        } else {
            newSize = is.stackSize;
        }
        if (origSize != newSize) {
            onInventoryChanged();
        }
        return !buffer.isEmpty();
    }
    
    @Override
    public void updateEntity() {
        genericUpdate(this, null, getCoord(), isBlockPowered());
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        if (!(this instanceof SocketEmpty)) {
            getCoord().spawnItem(new ItemStack(Core.registry.socket_part, 1, getFactoryType().md));
        }
    }
    
    @Override
    public ItemStack getPickedBlock() {
        if (this instanceof SocketEmpty) {
            return FactoryType.SOCKET_EMPTY.itemStack();
        }
        return new ItemStack(Core.registry.socket_part, 1, getFactoryType().md);
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        return FactoryType.SOCKET_EMPTY.itemStack();
    }
    
    private static FakePlayer silkyPlayer;
    
    protected FakePlayer getFakePlayer() {
        if (silkyPlayer == null) {
            silkyPlayer = new FakePlayer(worldObj, "[Lacerator]");
        }
        ItemStack pick = new ItemStack(Item.pickaxeDiamond);
        pick.addEnchantment(Enchantment.silkTouch, 1);
        silkyPlayer.worldObj = worldObj;
        silkyPlayer.posX = xCoord;
        silkyPlayer.posY = yCoord;
        silkyPlayer.posZ = zCoord;
        silkyPlayer.inventory.mainInventory[0] = pick;
        return silkyPlayer;
    }
    
    protected IInventory getBackingInventory(ISocketHolder socket) {
        if (socket == this) {
            TileEntity te = worldObj.getBlockTileEntity(xCoord - facing.offsetX,yCoord - facing.offsetY,zCoord - facing.offsetZ);
            if (te instanceof IInventory) {
                return (IInventory) te;
            }
            return null;
        } else if (socket instanceof IInventory) {
            return (IInventory) socket;
        }
        return null;
    }
    
    //Overridable code
    
    public void genericUpdate(ISocketHolder socket, Entity ent, Coord coord, boolean powered) { }
    
    @Override
    public abstract FactoryType getFactoryType();
    
    @Override
    public boolean canUpdate() {
        return false;
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        return super.handleMessageFromServer(messageType, input);
    }
    
    /**
     * return true if mop-searching should stop
     */
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        return true;
    }
    
    /**
     * @return true if successfully uninstalled; return false to block uninstallation
     */
    public boolean uninstall() {
        return true;
    }
    
    @SideOnly(Side.CLIENT)
    public void renderTesr(float partial) {}
    
    @SideOnly(Side.CLIENT)
    public void renderStatic(Tessellator tess) {}
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }
    
    @Override
    public boolean extractCharge(int amount) {
        if (this instanceof IChargeConductor) {
            IChargeConductor cc = (IChargeConductor) this;
            return cc.getCharge().tryTake(amount) >= amount;
        }
        return false;
    }
}
