package factorization.sockets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
import factorization.servo.ServoMotor;
import factorization.shared.*;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class SocketScissors extends TileEntitySocketBase implements ICaptureDrops {
    private boolean wasPowered = false;
    private ArrayList<ItemStack> buffer = new ArrayList();
    private byte openCount = 0;
    private static byte openTime = 6;

    private boolean sound = false;
    private boolean blocked = false;
    private boolean dirty = false;
    
    public static Entity lootingPlayer;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_SCISSORS;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        wasPowered = data.as(Share.PRIVATE, "pow").putBoolean(wasPowered);
        buffer = data.as(Share.PRIVATE, "buf").putItemList(buffer);
        openCount = data.as(Share.VISIBLE, "open").putByte(openCount);
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
        if (sound) {
            worldObj.playSound(xCoord, yCoord, zCoord, "mob.sheep.shear", 1, 1, false);
        }
        sound = false;
        if (worldObj.isRemote) return;
        if (openCount > 0 && (!powered || openCount < openTime)) {
            openCount--;
            dirty = true;
        }
        if (wasPowered || !powered) {
            wasPowered = powered;
        } else {
            wasPowered = true;
            FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
            if (openCount == 0 && getBackingInventory(socket) != null) {
                blocked = false;
                RayTracer tracer = new RayTracer(this, socket, coord, orientation, powered).onlyFrontBlock().checkEnts();
                tracer.trace();
                if (!blocked) {
                    sound = true;
                    openCount = openTime;
                    dirty = true;
                }
            }
        }
        if (socket.dumpBuffer(buffer)) {
            for (Iterator<ItemStack> iterator = buffer.iterator(); iterator.hasNext();) {
                ItemStack is = iterator.next();
                if (ItemUtil.normalize(is) == null) {
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
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, World mopWorld, boolean mopIsThis, boolean powered) {
        DropCaptureHandler.startCapture(this, new Coord(mopWorld, mop), 3);
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
        
        public Entity getEntity() {
            return SocketScissors.lootingPlayer;
        }
    };

    public boolean _handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            ItemStack sword = new ItemStack(Items.diamond_sword, 0 /* In case it somehow gets yoinked from us */);
            if (worldObj.rand.nextInt(10) > 3) {
                sword.addEnchantment(Enchantment.looting, 1);
            }
            Entity entity = mop.entityHit;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                EntityPlayer player = getFakePlayer();
                player.inventory.mainInventory[0] = sword;
                SocketScissors.lootingPlayer = player;
                int prevRecentlyHit = living.recentlyHit;
                living.attackEntityFrom(ScissorsDamge, 2);
                living.recentlyHit = prevRecentlyHit;
                SocketScissors.lootingPlayer.isDead = true;
                PlayerUtil.recycleFakePlayer(player);
                return true;
            }
        }
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            ItemStack shears = new ItemStack(Items.shears, 0 /* In case it somehow gets yoinked from us */);
            shears.addEnchantment(Enchantment.silkTouch, 1);
            Block block = worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            int metadata = worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
            if (block.isAir(worldObj, mop.blockX, mop.blockY, mop.blockZ)) {
                return false;
            }
            EntityPlayer player = getFakePlayer();
            if (canCutBlock(player, worldObj, block, mop.blockX, mop.blockY, mop.blockZ)) {
                player.inventory.mainInventory[0] = shears;
                boolean sheared = false;
                if (block instanceof IShearable) {
                    IShearable shearable = (IShearable) block;
                    if (shearable.isShearable(shears, worldObj, mop.blockX, mop.blockY, mop.blockZ)) {
                        Collection<ItemStack> drops = shearable.onSheared(shears, worldObj, mop.blockX, mop.blockY, mop.blockZ, 0);
                        processCollectedItems(drops);
                        sheared = true;
                    }
                }
                boolean didRemove = removeBlock(player, block, metadata, mop.blockX, mop.blockY, mop.blockZ);
                if (didRemove && !sheared) {
                    block.harvestBlock(worldObj, player, mop.blockX, mop.blockY, mop.blockZ, metadata);
                }
            } else {
                blocked = true;
            }
            PlayerUtil.recycleFakePlayer(player);
        }
        return false;
    }
    
    private boolean removeBlock(EntityPlayer thisPlayerMP, Block block, int md, int x, int y, int z) {
        if (block == null) return false;
        block.onBlockHarvested(worldObj, x, y, z, md, thisPlayerMP);
        if (block.removedByPlayer(worldObj, thisPlayerMP, x, y, z, false)) {
            block.onBlockDestroyedByPlayer(worldObj, x, y, z, md);
            return true;
        }
        return false;
    }

    public static boolean canCutBlock(EntityPlayer player, World world, Block block, int x, int y, int z) {
        int md = world.getBlockMetadata(x, y, z);
        Material mat = block.getMaterial();
        if (block.getBlockHardness(world, x, y, z) == 0 && mat != Material.circuits && mat != Material.fire && mat != Material.air) {
            return true;
        }
        // if (!block.canSilkHarvest(world, player, x, y, z, md)) return false; -- useless; vanilla things return values that don't work for us
        // if (block instanceof IShearable) return true;
        if (block instanceof BlockPortal) return true;
        if (block.getBlockHardness(world, x, y, z) == 0 && mat != Material.circuits && mat != Material.fire && mat != Material.air) {
            return true;
        }
        if (mat == Material.leaves || mat == Material.cactus || mat == Material.plants || mat == Material.cloth || mat == Material.carpet) {
            return true;
        }
        if (block instanceof BlockWeb || block instanceof BlockTallGrass || block instanceof BlockVine || block instanceof BlockTripWire) {
            return true;
        }

        return false;
    }

    @Override
    public boolean captureDrops(ArrayList<ItemStack> stacks) {
        processCollectedItems(stacks);
        return true;
    }

    void processCollectedItems(Collection<ItemStack> items) {
        if (items == null) return;
        buffer.addAll(items);
        items.clear();
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        for (ItemStack is : buffer) {
            InvUtil.spawnItemStack(here, is);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        float turn = 28*((float)openCount / (float)openTime);
        GL11.glTranslatef(0f, 0.25F - 7f/16f, 0);
        float n= -2F/16F;
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
        GL11.glTranslatef(-1F + 8/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
        GL11.glRotatef(-turn*2, 1, 0, 0);
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(0.5F, 0.5F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glRotatef(180, 1, 0, 0);
        GL11.glTranslatef(-1F + 8/16f, 0F, 0.5f);
        FactorizationBlockRender.renderItemIIcon(ItemIcons.socket$half_scissors);
        GL11.glPopMatrix();
        GL11.glPushMatrix();

        GL11.glRotatef(turn + 180, 1, 0, 0);
        GL11.glTranslatef(0, -.5F - 1F/16F, .25F - 1F/16F);

        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        
        piston_base.render(BlockIcons.socket$mini_piston);
        GL11.glTranslatef(0, 0, -6f/16f);
        piston_base.render(BlockIcons.socket$mini_piston);
        float offset = -1F/16F * ((float)openCount/(float)openTime) - 1F/16F;
        GL11.glTranslatef(0, offset, 0f);
        piston_head.render(BlockIcons.socket$mini_piston);
        GL11.glTranslatef(0, 0, 6f/16f);
        piston_head.render(BlockIcons.socket$mini_piston);

        GL11.glPopMatrix();
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
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
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