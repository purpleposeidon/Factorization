package factorization.servo.iterator;

import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.api.datahelpers.DataInByteBufClientEdited;
import factorization.api.datahelpers.Share;
import factorization.api.energy.*;
import factorization.common.FactoryType;
import factorization.flat.api.FlatCoord;
import factorization.net.FzNetDispatch;
import factorization.net.NetworkFactorization;
import factorization.net.StandardMessageType;
import factorization.servo.rail.Decorator;
import factorization.servo.rail.FlatServoRail;
import factorization.servo.rail.ItemServoRailWidget;
import factorization.servo.rail.ServoComponent;
import factorization.shared.Core;
import factorization.shared.Sound;
import factorization.shared.TileEntityCommon;
import factorization.sockets.GuiDataConfig;
import factorization.sockets.ISocketHolder;
import factorization.sockets.SocketEmpty;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.*;
import factorization.util.InvUtil.FzInv;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static factorization.net.StandardMessageType.DataHelperEditOnEntity;
import static factorization.net.StandardMessageType.TileEntityMessageOnEntity;

public class ServoMotor extends AbstractServoMachine implements IInventory, ISocketHolder {
    public Executioner executioner = new Executioner(this);
    public TileEntitySocketBase socket = new SocketEmpty();
    public boolean isSocketActive = false;
    public boolean isSocketPulsed = false;
    
    ItemStack[] inv = new ItemStack[1], inv_last_sent = new ItemStack[inv.length];
    
    public ServoMotor(World world) {
        super(world);
        setSize(1, 1);
        isImmuneToFire = true;
    }

    public void syncWithSpawnPacket() {
        if (worldObj.isRemote) return;
        Packet p = FMLNetworkHandler.getEntitySpawningPacket(this);
        FzNetDispatch.addPacketFrom(p, this);
    }
    
    
    
    // Serialization

    @Override
    public void putData(DataHelper data) throws IOException {
        super.putData(data);
        executioner.putData(data);
        
        final byte invSize = data.as(Share.VISIBLE, "inv#").putByte((byte) inv.length);
        resizeInventory(invSize);
        for (int i = 0; i < invSize; i++) {
            ItemStack is = NetworkFactorization.nullItem(inv[i]);
            is = data.as(Share.VISIBLE, "inv" + i).putItemStack(is);
            if (is == null) {
                inv[i] = null;
            } else {
                inv[i] = is.getItem() == null ? null : is;
            }
        }
        data.as(Share.VISIBLE, "sock");
        if (data.isReader()) {
            NBTTagCompound tag = data.putTag(new NBTTagCompound());
            TileEntity te = TileEntity.createAndLoadEntity(tag);
            if (te instanceof TileEntitySocketBase) {
                socket = (TileEntitySocketBase) te;
            } else {
                socket = new SocketEmpty();
            }
        } else {
            NBTTagCompound output = new NBTTagCompound();
            socket.writeToNBT(output);
            data.putTag(output);
        }
        isSocketActive = data.as(Share.VISIBLE, "sockon").putBoolean(isSocketActive);
        isSocketPulsed = data.as(Share.VISIBLE, "sockpl").putBoolean(isSocketPulsed);
    }
    
    
    
    
    // Networking

    @Override
    public boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException {
        if (messageType == DataHelperEditOnEntity) {
            DataInByteBufClientEdited di = new DataInByteBufClientEdited(input);
            socket.serialize("", di);
            markDirty();
            return true;
        }
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType.equals(StandardMessageType.OpenDataHelperGuiOnEntity)) {
            if (!worldObj.isRemote) {
                return false;
            } else {
                DataHelper dip = new DataInByteBuf(input, Side.CLIENT);
                socket.serialize("", dip);
                Minecraft.getMinecraft().displayGuiScreen(new GuiDataConfig(socket, this));
            }
            return true;
        } else if (messageType.equals(ServoMessages.servo_item)) {
            while (true) {
                byte index = input.readByte();
                if (index < 0) {
                    break;
                }
                inv[index] = DataUtil.readStack(input);
            }
            return true;
        } else if (messageType.equals(StandardMessageType.TileEntityMessageOnEntity)) {
            byte messageIndex = input.readByte();
            Enum subMsg = NetworkFactorization.getMessage(messageIndex, socket);
            return socket.handleMessageFromServer(subMsg, input);
        } else {
            return socket.handleMessageFromServer(messageType, input);
        }
    }

    // Main logic

    @Override
    public void updateServoLogic() {
        if (worldObj.isRemote) {
            executioner.tick();
            return;
        }
        executioner.tick();
        if (!executioner.stacks_changed) return;
        executioner.stacks_changed = false;
        broadcastFullUpdate();
    }

    @Override
    public void onEntityUpdate() {
        if (ticksExisted == 1 /* 1.8: This is incremented by World before this method is called; is not serialized */) {
            // So it's appropriate for here. If only there was an Entity.onSpawn...
            socket.onLoaded(this);
        }
        super.onEntityUpdate();
    }

    @Override
    public void updateSocket() {
        Coord here = getCurrentPos();
        here.setAsTileEntityLocation(socket);
        socket.facing = getOrientation().top;
        socket.genericUpdate(this, here, isSocketActive ^ isSocketPulsed);
        isSocketPulsed = false;
    }

    @Override
    public void onEnterNewBlock() {
        if (worldObj.isRemote) {
            return;
        }
        socket.onEnterNewBlock();
        motionHandler.onEnterNewBlock();
        FlatServoRail ff = getFlatPos().get(FlatServoRail.class);
        if (ff != null) {
            ff.getComponent().preMotorHit(this);
        }
        executioner.onEnterNewBlock(ff);
    }

    FlatCoord getFlatPos() {
        return new FlatCoord(getCurrentPos(), getOrientation().facing);
    }
    
    
    
    
    
    // Utility functions for client code
    
    public ServoStack getArgStack() {
        return executioner.getArgStack();
    }
    
    public ServoStack getInstructionsStack() {
        return executioner.getInstructionStack();
    }
    
    public ServoStack getEntryInstructionStack() {
        return executioner.getEntryInstructionStack();
    }
    
    public void putError(Object error) {
        executioner.putError(error);
    }




    
    // Entity behavior
    
    @Override
    protected void entityInit() { }
    
    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (worldObj.isRemote) return true;
        executioner.stacks_changed = true;
        ItemStack is = ItemUtil.normalize(player.getHeldItem());
        if (is == null) {
            return false;
        }
        Item item = is.getItem();
        if (item instanceof ItemServoRailWidget) {
            ServoComponent sc = ServoComponent.fromItem(is);
            if (sc == null) {
                // Frowney face
            } else if (player.isSneaking()) {
                if (!sc.onClick(player, this)) {
                    return false;
                }
                ItemStack updated = sc.toItem();
                is.setItemDamage(updated.getItemDamage());
                is.setTagCompound(updated.getTagCompound());
                return true;
            } else {
                if (sc instanceof Decorator) {
                    Decorator dec = (Decorator) sc;
                    dec.motorHit(this);
                    return true;
                }
            }
        }
        if (socket == null) return false;
        if (socket.activateOnServo(player, this)) return false;
        if (ItemUtil.identical(socket.getCreatingItem(), is)) return false;
        for (FactoryType ft : FactoryType.values()) {
            TileEntityCommon tec = ft.getRepresentative();
            if (tec == null) continue;
            if (!(tec instanceof TileEntitySocketBase)) continue;
            TileEntitySocketBase rep = (TileEntitySocketBase) tec;
            ItemStack creator = rep.getCreatingItem();
            if (creator != null && ItemUtil.couldMerge(is, creator)) {
                if (rep.getParentFactoryType() != socket.getFactoryType()) {
                    rep.mentionPrereq(this, player);
                    return false;
                }
                TileEntityCommon upgrade = ft.makeTileEntity();
                if (upgrade != null) {
                    TileEntitySocketBase orig = socket;
                    socket = (TileEntitySocketBase) upgrade;
                    if (!player.capabilities.isCreativeMode) is.stackSize--;
                    Sound.servoInstall.playAt(new Coord(this));
                    socket.addPart(orig.parts, is);
                    socket.installedOnServo(this);
                    return true;
                }
            }
        }
        if (motionHandler.color == FzColor.NO_COLOR) {
            FzColor newColor = FzColor.fromItem(is);
            if (newColor != FzColor.NO_COLOR) {
                motionHandler.color = newColor;
                markDirty();
                PlayerUtil.cheatDecr(player, is);
                return true;
            }
        }
        return false;
    }
    
    protected void dropItemsOnBreak() {
        ArrayList<ItemStack> toDrop = new ArrayList<ItemStack>();
        toDrop.add(new ItemStack(Core.registry.servo_placer));
        toDrop.addAll(Arrays.asList(inv));
        if (socket != null) {
            socket.uninstall();
            FactoryType ft = socket.getFactoryType();
            while (ft != null) {
                TileEntitySocketBase sb = (TileEntitySocketBase) ft.getRepresentative();
                if (sb == null) break;
                final ItemStack is = sb.getCreatingItem();
                if (is != null) toDrop.add(is.copy());
                ft = sb.getParentFactoryType();
            }
        }
        dropItemStacks(toDrop);
    }
    
    public void dropItemStacks(Iterable<ItemStack> toDrop) {
        for (ItemStack is : toDrop) {
            InvUtil.spawnItemStack(this, is);
        }
    }
    
    public void resizeInventory(int newSize) {
        if (newSize == inv.length) return;
        ItemStack[] origInv = inv;
        int min = Math.min(newSize, origInv.length);
        inv = new ItemStack[newSize];
        for (int i = 0; i < min; i++) {
            inv[i] = origInv[i];
            origInv[i] = null;
        }
        for (ItemStack is : origInv) {
            if (is != null) {
                getCurrentPos().spawnItem(is);
            }
        }
        inv_last_sent = new ItemStack[newSize];
    }

    
    
    
    
    // IInventory implementation
    
    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inv[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        ItemStack ret = inv[i].splitStack(j);
        inv[i] = ItemUtil.normalize(inv[i]);
        return ret;
    }

    @Override
    public ItemStack removeStackFromSlot(int i) {
        ItemStack stack = inv[i];
        inv[i] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inv[i] = itemstack;
    }

    @Override
    public IChatComponent getDisplayName() {
        return new ChatComponentTranslation("factorization.servo.inventory");
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }
    
    @Override
    public void markDirty() {
        ArrayList<Object> toSend = new ArrayList<Object>(inv.length*2);
        for (byte i = 0; i < inv.length; i++) {
            if (ItemUtil.identical(inv[i], inv_last_sent[i])) {
                continue;
            }
            toSend.add(i);
            toSend.add(NetworkFactorization.nullItem(inv[i]));
            inv_last_sent[i] = inv[i];
        }
        if (toSend.isEmpty()) {
            return;
        }
        toSend.add(-1);
        broadcast(ServoMessages.servo_item, toSend.toArray());
        getCurrentPos().getChunk().setChunkModified();
        getNextPos().getChunk().setChunkModified();
    }
    
    @Override public boolean isUseableByPlayer(EntityPlayer entityplayer) { return false; }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < inv.length; i++) {
            setInventorySlotContents(i, null);
        }
    }


    // ISocketHolder implementation
    
    private final ArrayList<MovingObjectPosition> ret = new ArrayList<MovingObjectPosition>();
    
    ArrayList<MovingObjectPosition> rayTrace() {
        ret.clear();
        final Coord c = getCurrentPos();
        final FzOrientation fzo = motionHandler.motionAction.srcFzo;
        final EnumFacing top = fzo.top;
        final EnumFacing face = fzo.facing;
        final EnumFacing right = SpaceUtil.rotate(face, top);
        
        AxisAlignedBB ab = new AxisAlignedBB(
                c.x + top.getDirectionVec().getX(), c.y + top.getDirectionVec().getY(), c.z + top.getDirectionVec().getZ(),  
                c.x + 1 + top.getDirectionVec().getX(), c.y + 1 + top.getDirectionVec().getY(), c.z + 1 + top.getDirectionVec().getZ());
        for (Entity entity : worldObj.getEntitiesWithinAABBExcludingEntity(this, ab)) {
            if (!entity.canBeCollidedWith()) {
                continue;
            }
            ret.add(new MovingObjectPosition(entity));
        }
        
        Coord targetBlock = c.add(top);
        mopBlock(ret, targetBlock, top.getOpposite()); //nose-to-nose with the servo
        mopBlock(ret, targetBlock.add(top), top.getOpposite()); //a block away
        mopBlock(ret, targetBlock.add(top.getOpposite()), top);
        if (ret.size() == 0) {
            mopBlock(ret, targetBlock.add(face), face.getOpposite()); //running forward
            mopBlock(ret, targetBlock.add(face.getOpposite()), face); //running backward
            if (ret.size() == 0) {
                mopBlock(ret, targetBlock.add(right), right.getOpposite()); //to the servo's right
                mopBlock(ret, targetBlock.add(right.getOpposite()), right); //to the servo's left
            }
        }
        return ret;
    }
    
    void mopBlock(ArrayList<MovingObjectPosition> list, Coord target, EnumFacing side) {
        if (target.isAir()) {
            return;
        }
        list.add(target.createMop(side, new Vec3(0, 0, 0)));
    }
    
    @Override
    public boolean dumpBuffer(List<ItemStack> buffer) {
        if (buffer.isEmpty()) {
            return false;
        }
        FzInv me = InvUtil.openInventory(this, false);
        ItemStack got = buffer.get(0);
        if (got == null) {
            buffer.remove(0);
            return true;
        }
        ItemStack res = me.push(buffer.get(0));
        if (res == null) {
            buffer.remove(0);
        } else {
            buffer.set(0, res);
        }
        return true;
    }
    
    @Override
    public void sendMessage(StandardMessageType msgType, Object... msg) {
        Object[] buff = new Object[msg.length + 1];
        System.arraycopy(msg, 0, buff, 1, msg.length);
        buff[0] = msgType;
        FMLProxyPacket toSend = Core.network.entityPacket(this, TileEntityMessageOnEntity, buff);
        Core.network.broadcastPacket(null, getCurrentPos(), toSend); 
    }

    @Override
    public Vec3 getServoPos() {
        return SpaceUtil.fromEntPos(this);
    }

    @Override
    public IWorkerContext getContext() {
        return new ContextEntity(this);
    }

    @Override
    public boolean extractAccelerationEnergy() {
        return extractCharge(2);
    }

    @Override
    public Accepted accept(ContextEntity context, WorkUnit unit, boolean simulate) {
        if (socket instanceof IWorker) {
            IWorker sub = (IWorker) socket;
            FzOrientation fzo = getOrientation();
            ContextTileEntity ctx = new ContextTileEntity(socket, fzo.facing.getOpposite(), fzo.top);
            if (sub.accept(ctx, unit, simulate) == Accepted.NOW) {
                return Accepted.NOW;
            }
        }
        return super.accept(context, unit, simulate);
    }
}
