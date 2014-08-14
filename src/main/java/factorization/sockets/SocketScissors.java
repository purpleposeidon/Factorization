package factorization.sockets;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.ItemIcons;
import factorization.notify.Notice;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntityGrinderRender;
import factorization.oreprocessing.TileEntityGrinder.GrinderRecipe;
import factorization.servo.ServoMotor;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FzUtil;
import factorization.shared.NetworkFactorization.MessageType;

public class SocketScissors extends TileEntitySocketBase{
    boolean wasPowered = false;
    boolean sound = false;
    int openCount = 0;
    static int openTime = 6;
    
    boolean dirty=false;
    
    ArrayList<ItemStack> buffer = new ArrayList();
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_SCISSORS;
    }
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        wasPowered = data.as(Share.PRIVATE, "pow").putBoolean(wasPowered);
        buffer = data.as(Share.PRIVATE, "buf").putItemArray(buffer);
        openCount = data.as(Share.PRIVATE, "buf").putInt(openCount);
        return this;
    }

    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }
    
    @Override
    public ItemStack getCreatingItem() {
        return new ItemStack(Core.registry.giant_scissors);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return false;
        }
        if (!buffer.isEmpty()) {
            new Notice(this, "%s items buffered", "" + buffer.size()).send(entityplayer);
            return false;
        }
        if (getBackingInventory(this) == null) {
            new Notice(this, "No output inventory").send(entityplayer);
            return false;
        }
        return false;
    }

    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (sound)
            worldObj.playSound(xCoord,yCoord,zCoord,"mob.sheep.shear",1,1,false);
        sound=false;
        if (worldObj.isRemote) {
            return;
        }
        if(openCount>0 && !powered)
        {
            openCount--;
            dirty=true;
        }
        if (wasPowered || !powered) {
            wasPowered = powered;
        }
        else 
        {
            wasPowered = true;
            FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
            if(openCount==0 && getBackingInventory(socket) != null)
            {
                sound=true;
                rayTrace(socket, coord, orientation, powered, false, true);
                openCount=openTime;
                dirty=true;
            }
        }
        if (socket.dumpBuffer(buffer)) {
            for (Iterator<ItemStack> iterator = buffer.iterator(); iterator.hasNext();) {
                ItemStack is = iterator.next();
                if (FzUtil.normalize(is) == null) {
                    iterator.remove();
                }
            }
        }
        if (dirty) {
            socket.sendMessage(MessageType.ScissorState, openCount, sound);
            dirty = false;
        }
    }
    @Override
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        dropGrabber = this;
        try {
            return _handleRay(socket, mop, mopIsThis, powered);
        } finally {
            dropGrabber = null;
        }
    }

    public boolean _handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        ItemStack shears=new ItemStack(Items.shears);
        shears.addEnchantment(Enchantment.silkTouch, 1);
        if(mop.typeOfHit==MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity entity=mop.entityHit;
            if(entity instanceof IShearable && ((IShearable) entity).isShearable(shears, worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ)) {
                    List<ItemStack> stacks = ((IShearable) entity).onSheared(shears, worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ, 0);
                    for(ItemStack stack : stacks)
                        processCollectedItem(stack);
                    worldObj.playSound(mop.entityHit.posX, mop.entityHit.posY, mop.entityHit.posZ, "mob.sheep.shear", 1.0F, 1.0F, false);
                    return true;
                }
        }
        if(mop.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block=worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            int metadata=worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
            if(canCutBlock(worldObj,block,mop.blockX,mop.blockY,mop.blockZ))
            {
                EntityPlayer player = getFakePlayer();
                player.inventory.mainInventory[0] = shears;
                if(block instanceof IShearable)
                {
                     IShearable target = (IShearable)block;
                     if (target.isShearable(shears, worldObj, mop.blockX,mop.blockY,mop.blockZ)) {
                            List<ItemStack> stacks = target.onSheared(shears, worldObj, mop.blockX,mop.blockY,mop.blockZ, 0);
                            for(ItemStack stack : stacks)
                                processCollectedItem(stack);
                            removeBlock(player,block,metadata,mop.blockX,mop.blockY,mop.blockZ);
                            return true;
                     }
                }
                else
                {
                    boolean didRemove = removeBlock(player,block,metadata,mop.blockX,mop.blockY,mop.blockZ);
                    if (didRemove) {
                        block.harvestBlock(worldObj, player, mop.blockX, mop.blockY, mop.blockZ, metadata);
                    }
                }
            }
        }
        return false;
    }
    
    private boolean removeBlock(EntityPlayer thisPlayerMP, Block block, int md, int x, int y, int z) {
        if (block == null) return false;
        block.onBlockHarvested(worldObj, x, y, z, md, thisPlayerMP);
        if (block.removedByPlayer(worldObj, thisPlayerMP, x, y, z)) {
            block.onBlockDestroyedByPlayer(worldObj, x, y, z, md);
            return true;
        }
        return false;
    }
    public static boolean canCutBlock(World world, Block block, int x, int y, int z)
    {
        if (block.getMaterial() == Material.leaves || block.getMaterial() == Material.cactus || block.getMaterial()==Material.plants || block == Blocks.web || block == Blocks.tallgrass || block == Blocks.vine || block == Blocks.tripwire || block instanceof IShearable)
            return true;
        if (block.getBlockHardness(world, x, y, z)==0 && block.getMaterial() != Material.circuits && block.getMaterial() != Material.fire && block.getMaterial() != Material.air)
            return true;
        return false;
    }

    static SocketScissors dropGrabber = null;
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void captureDrops(BlockEvent.HarvestDropsEvent event) {
        if (dropGrabber == null) {
            return;
        }
        if (dropGrabber != this) {
            dropGrabber.captureDrops(event);
            return;
        }
        final int maxDist = 2*2;
        int dist = (xCoord - event.x)*(xCoord - event.x) + (yCoord - event.y)*(yCoord - event.y) + (zCoord - event.z)*(zCoord - event.z);
        if (dist > maxDist) {
            return;
        }
        ArrayList<ItemStack> drops = event.drops;
        for (int i = 0; i < drops.size(); i++) {
            ItemStack is = drops.get(i);
            processCollectedItem(is);
        }
        drops.clear();
    }
    
    void processCollectedItem(ItemStack is) {
        buffer.add(is);
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        for (ItemStack is : buffer) {
            FzUtil.spawnItemStack(here, is);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        float turn=18*((float)openCount/(float)openTime);
        GL11.glTranslatef(0f, 0.25F+1f/16f, 0);
        GL11.glRotatef(turn, 1, 0, 0);
        float sd = motor == null ? -2F/16F : 3F/16F;
        GL11.glTranslatef(0, sd, 0);
        
        
        if (motor != null) {
            GL11.glTranslatef(0, -6F/16F, 0);
        }
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslatef(-1F+1/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
        GL11.glRotatef(-turn*2, 1, 0, 0);
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(0.5F, 0.5F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glRotatef(180, 1, 0, 0);
        GL11.glTranslatef(-1F+1/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
    }

    @Override
    public FMLProxyPacket getDescriptionPacket() {
        return getDescriptionPacketWith(MessageType.ScissorState, openCount, sound);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(MessageType messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.ScissorState) {
            openCount = input.readInt();
            sound = input.readBoolean();
            return true;
        }
        return false;
    }
    
    @Override
    public void installedOnServo(ServoMotor servoMotor) {
        super.installedOnServo(servoMotor);
        servoMotor.resizeInventory(4);
    }
}
