package factorization.sockets;

import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
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
import factorization.shared.DropCaptureHandler;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FzUtil;
import factorization.shared.ICaptureDrops;
import factorization.shared.ObjectModel;
import factorization.shared.NetworkFactorization.MessageType;

public class SocketScissors extends TileEntitySocketBase implements ICaptureDrops{
    boolean wasPowered = false;
    boolean sound = false;
    byte openCount = 0;
    static byte openTime = 6;

    boolean blocked=false;
    
    boolean dirty=false;
    
    public static Entity lootingPlayer;
    
    ArrayList<ItemStack> buffer = new ArrayList();
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_SCISSORS;
    }
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        wasPowered = data.as(Share.PRIVATE, "pow").putBoolean(wasPowered);
        buffer = data.as(Share.PRIVATE, "buf").putItemArray(buffer);
        openCount = data.as(Share.PRIVATE, "open").putByte(openCount);
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
        if (openCount>0 && ( !powered || openCount < openTime )) {
            openCount--;
            dirty=true;
        }
        if (wasPowered || !powered) {
            wasPowered = powered;
        }
        else {
            wasPowered = true;
            FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
            if (openCount==0 && getBackingInventory(socket) != null) {
                blocked=false;
                rayTrace(socket, coord, orientation, powered, false, true);
                if (!blocked) {
                    sound=true;
                    openCount=openTime;
                    dirty=true;
                }
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
        DropCaptureHandler.startCapture(this);
        try {
            return _handleRay(socket, mop, mopIsThis, powered);
        } finally {
            DropCaptureHandler.endCapture();
        }
    }

    public static final DamageSource ScissorsDamge = new DamageSource("scissors") {
        @Override
        public IChatComponent func_151519_b(EntityLivingBase victim) {
            String ret = "death.attack.scissors.";
            if (victim.worldObj != null) {
                long now = victim.worldObj.getTotalWorldTime();
                ret += (now % 3) + 1;
                System.out.println(now);
            } else {
                ret += "1";
            }
            
            EntityLivingBase attacker = victim.func_94060_bK();
            String fightingMessage = ret + ".player";
            if (attacker != null && StatCollector.canTranslate(fightingMessage)) {
                return new ChatComponentTranslation(fightingMessage, victim.func_145748_c_(), attacker.func_145748_c_());
            } else {
                return new ChatComponentTranslation(ret, victim.func_145748_c_());
            }
        }
        public Entity getEntity()
        {
            return SocketScissors.lootingPlayer;
        }
    };

    public boolean _handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        ItemStack shears=new ItemStack(Items.shears);
        shears.addEnchantment(Enchantment.silkTouch, 1);
        ItemStack sword = new ItemStack(Items.diamond_sword);
        if(worldObj.rand.nextInt(10) > 3)
            sword.addEnchantment(Enchantment.looting, 1);
        if (mop.typeOfHit==MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity entity=mop.entityHit;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living=(EntityLivingBase)entity;
                EntityPlayer player = getFakePlayer();
                player.inventory.mainInventory[0] = sword;
                SocketScissors.lootingPlayer=player;
                living.attackEntityFrom(ScissorsDamge, 2);
                SocketScissors.lootingPlayer.isDead=true;
                return true;
            }
        }
        if (mop.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block=worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            int metadata=worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
            if (block.isAir(worldObj, mop.blockX, mop.blockY, mop.blockZ)) {
                return false;
            }
            if (canCutBlock(worldObj,block,mop.blockX,mop.blockY,mop.blockZ)) {
                EntityPlayer player = getFakePlayer();
                player.inventory.mainInventory[0] = shears;
                if (block instanceof IShearable) {
                     IShearable target = (IShearable)block;
                     if (target.isShearable(shears, worldObj, mop.blockX,mop.blockY,mop.blockZ)) {
                            List<ItemStack> stacks = target.onSheared(shears, worldObj, mop.blockX,mop.blockY,mop.blockZ, 0);
                            for(ItemStack stack : stacks)
                                processCollectedItem(stack);
                            removeBlock(player,block,metadata,mop.blockX,mop.blockY,mop.blockZ);
                            return true;
                     }
                }
                else {
                    boolean didRemove = removeBlock(player,block,metadata,mop.blockX,mop.blockY,mop.blockZ);
                    if (didRemove) {
                        block.harvestBlock(worldObj, player, mop.blockX, mop.blockY, mop.blockZ, metadata);
                    }
                }
            }
            else{
                blocked=true;
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
    public static boolean canCutBlock(World world, Block block, int x, int y, int z) {
        if (block.getMaterial() == Material.leaves || block.getMaterial() == Material.cactus || block.getMaterial()==Material.plants || block == Blocks.web || block == Blocks.tallgrass || block == Blocks.vine || block == Blocks.tripwire || block instanceof IShearable)
            return true;
        if (block.getBlockHardness(world, x, y, z)==0 && block.getMaterial() != Material.circuits && block.getMaterial() != Material.fire && block.getMaterial() != Material.air)
            return true;
        return false;
    }

    @Override
    public boolean captureDrops(int x,int y, int z, ArrayList<ItemStack> stacks) {
        final int maxDist = 2*2;
        int dist = (xCoord - x)*(xCoord - x) + (yCoord - y)*(yCoord - y) + (zCoord - z)*(zCoord - z);
        if (dist > maxDist) {
            return false;
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack is = stacks.get(i);
            processCollectedItem(is);
        }
        return true;
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
        float turn=28*((float)openCount/(float)openTime);
        GL11.glTranslatef(0f, 0.25F-7f/16f, 0);
        float n=-2F/16F;
        GL11.glTranslatef(0, n, 0);
        GL11.glRotatef(turn, 1, 0, 0);
        GL11.glTranslatef(-0, -n, -0);
        float sd = motor == null ? -2F/16F : 3F/16F;
        GL11.glTranslatef(0, sd, 0);
        
        
        if (motor != null) {
            GL11.glTranslatef(0, -6F/16F, 0);
        }
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslatef(-1F+8/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
        GL11.glRotatef(-turn*2, 1, 0, 0);
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(0.5F, 0.5F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glRotatef(180, 1, 0, 0);
        GL11.glTranslatef(-1F+8/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
        GL11.glPushMatrix();

        GL11.glRotatef(turn+180, 1, 0, 0);
        GL11.glTranslatef(0, -.5F-1F/16F, .25F-1F/16F);

        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        glEnable(GL_LIGHTING);
        glDisable(GL11.GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        
        piston_base.render(BlockIcons.socket$mini_piston);
        GL11.glTranslatef(0, 0, -6f/16f);
        piston_base.render(BlockIcons.socket$mini_piston);
        float offset=-1F/16F*((float)openCount/(float)openTime)-1F/16F;
        GL11.glTranslatef(0, offset, 0f);
        piston_head.render(BlockIcons.socket$mini_piston);
        GL11.glTranslatef(0, 0, 6f/16f);
        piston_head.render(BlockIcons.socket$mini_piston);

        glEnable(GL11.GL_CULL_FACE);
        glEnable(GL_LIGHTING);
        GL11.glPopMatrix();
//		renderTinyPistons(motor, Tessellator.instance);
    }

    @SideOnly(Side.CLIENT)
    private static ObjectModel piston_base;
    @SideOnly(Side.CLIENT)
    private static ObjectModel piston_head;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void representYoSelf() {
        super.representYoSelf();
        piston_base = new ObjectModel(Core.getResource("models/mini_piston/mini_piston_base.obj"));
        piston_head = new ObjectModel(Core.getResource("models/mini_piston/mini_piston_head.obj"));
//		piston_base = new ObjectModel(Core.getResource("models/corkscrew.obj"));
//		piston_head = new ObjectModel(Core.getResource("models/corkscrew.obj"));
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
            openCount = input.readByte();
            sound = input.readBoolean();
            return true;
        }
        return false;
    }
    
}