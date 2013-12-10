package factorization.common.sockets;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.client.render.TileEntityGrinderRender;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactoryType;
import factorization.common.ISocketHolder;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.TileEntityDayBarrel;
import factorization.common.TileEntityGrinder;
import factorization.common.TileEntityGrinder.GrinderRecipe;
import factorization.common.TileEntitySocketBase;
import factorization.common.servo.ServoMotor;
import factorization.notify.Notify;

public class SocketLacerator extends TileEntitySocketBase implements IChargeConductor, ISidedInventory {
    Charge charge = new Charge(this);
    @Override public String getInfo() {
        int s = speed*100/max_speed;
        return s + "% speed";
    }
    @Override public Charge getCharge() { return charge; }
    
    short speed = 0;
    short progress;
    short last_shared_speed = 0;
    boolean grab_items = false, grind_items = false;
    long targetHash = -1;
    boolean ticked = false;
    boolean isPowered = false;
    
    final static byte grind_time = 75;
    final static short max_speed = 400;
    final static short min_speed = max_speed/10;
    ArrayList<ItemStack> buffer = new ArrayList();
    
    private float rotation = 0, prev_rotation = 0;
    
    MovingObjectPosition present_breaking_target = null, previous_breaking_target = null;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_LACERATOR;
    }
    
    
    @Override
    public boolean canUpdate() {
        return true;
    }
    
    @Override
    public void updateEntity() {
        charge.update();
        super.updateEntity();
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return false;
        }
        if (!buffer.isEmpty()) {
            Notify.send(this, "%s items buffered", "" + buffer.size());
            return false;
        }
        if (getBackingInventory(this) == null) {
            Notify.send(this, "No output inventory");
            return false;
        }
        return false;
    }
    
    void slowDown() {
        speed = (short) Math.max(0, speed - 1);
    }
    
    void destroyPartially(MovingObjectPosition mop, int amount) {
        if (mop == null) return;
        int blockId = mop.subHit; //We abuse this to store the block ID!
        //This is necessary because vanilla seems to key things by the block ID.
        //Otherwise we'd have partial breaks floating around...
        worldObj.destroyBlockInWorldPartially(blockId, mop.blockX, mop.blockY, mop.blockZ, amount);
    }
    
    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (worldObj.isRemote) {
            prev_rotation = rotation;
            rotation += speed / 4F;
            ticked = true;
            return;
        }
        isPowered = powered;
        genericUpdate_implementation(socket, coord, powered);
        if (FactorizationUtil.significantChange(last_shared_speed, speed)) {
            socket.sendMessage(MessageType.LaceratorSpeed, speed);
            last_shared_speed = speed;
        }
        
        if (previous_breaking_target != null && present_breaking_target != null) {
            if (previous_breaking_target.blockX != present_breaking_target.blockX
                    || previous_breaking_target.blockY != present_breaking_target.blockY
                    || previous_breaking_target.blockZ != present_breaking_target.blockZ) {
                destroyPartially(previous_breaking_target, 99);
            }
        } else if (present_breaking_target == null) {
            destroyPartially(previous_breaking_target, 99);
        }
        
        previous_breaking_target = present_breaking_target;
        if (progress == 0) {
            destroyPartially(present_breaking_target, 99);
            present_breaking_target = null;
        }
    }
    
    private void genericUpdate_implementation(ISocketHolder socket, Coord coord, boolean powered) {
        if (getBackingInventory(socket) == null) {
            slowDown();
            return;
        }
        if (socket.dumpBuffer(buffer)) {
            for (Iterator<ItemStack> iterator = buffer.iterator(); iterator.hasNext();) {
                ItemStack is = iterator.next();
                if (FactorizationUtil.normalize(is) == null) {
                    iterator.remove();
                }
            }
            slowDown();
            return;
        }
        FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
        if (!rayTrace(socket, coord, orientation, powered, false, true)) {
            if (!powered) {
                slowDown();
            } else {
                cantDoWork(socket);
            }
            progress = 0;
        }
        if (grab_items) {
            grab_items = false;
            
            for (Entity entity : getEntities(socket, coord, orientation.top, 1)) {
                if (entity.isDead) {
                    continue;
                }
                if (entity instanceof EntityItem) {
                    EntityItem ei = (EntityItem) entity;
                    if (ei.ticksExisted > 1) {
                        continue;
                    }
                    ItemStack is = ei.getEntityItem();
                    processCollectedItem(is);
                    ei.setDead();
                }
            }
        }
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.LaceratorSpeed) {
            speed = input.readShort();
            return true;
        }
        return false;
    }
    
    @Override
    protected byte getExtraInfo2() {
        return (byte) (Byte.MAX_VALUE*speed/max_speed);
    }
    
    @Override
    protected void useExtraInfo2(byte b) {
        speed = (short) (b*max_speed/Byte.MAX_VALUE);
    }
    
    void processCollectedItem(ItemStack is) {
        if (!grind_items) {
            buffer.add(is);
            return;
        }
        boolean appliedRecipe = false;
        ArrayList<GrinderRecipe> recipes = TileEntityGrinder.recipes;
        for (int i = 0; i < recipes.size(); i++) {
            GrinderRecipe gr = recipes.get(i);
            if (FactorizationUtil.oreDictionarySimilar(gr.getOreDictionaryInput(), is)) {
                ItemStack output = gr.output.copy();
                output.stackSize = 0;
                int min = (int) gr.probability;
                output.stackSize += min;
                output.stackSize += rand.nextFloat() < (gr.probability - min) ? 1 : 0;
                is = output;
                appliedRecipe = true;
                break;
            }
        }
        if (appliedRecipe) {
            grind_items = false; //We only grind 1 drop, to prevent hax
        }
        buffer.add(is);
    }
    
    boolean cantDoWork(ISocketHolder socket) {
        //Calls this in two places in handleRay because we may have unlacerable mops
        if (!isPowered && socket.extractCharge(4)) {
            speed = (short) Math.min(max_speed, speed + 4);
            if (speed == max_speed) {
                return false;
            }
        } else if (speed > 0) {
            speed--;
        }
        return !(speed > min_speed && rand.nextInt(max_speed) < speed/4);
    }
    
    public static final DamageSource laceration = new DamageSource("laceration") {
        @Override
        public ChatMessageComponent getDeathMessage(EntityLivingBase victim) {
            String ret = "death.attack.laceration.";
            if (victim.worldObj != null) {
                long now = victim.worldObj.getTotalWorldTime();
                ret += (now % 6) + 1;
            } else {
                ret += "1";
            }
            
            EntityLivingBase attacker = victim.func_94060_bK();
            String fightingMessage = ret + ".player";
            if (attacker != null && StatCollector.func_94522_b(fightingMessage)) {
                return ChatMessageComponent.createFromTranslationWithSubstitutions(fightingMessage, victim.getTranslatedEntityName(), attacker.getTranslatedEntityName());
            } else {
                return ChatMessageComponent.createFromTranslationWithSubstitutions(ret, victim.getTranslatedEntityName());
            }
        }
    };
    
    static SocketLacerator dropGrabber = null;
    
    @Override
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        dropGrabber = this;
        try {
            return _handleRay(socket, mop, mopIsThis, powered);
        } finally {
            dropGrabber = null;
        }
    }
    
    @ForgeSubscribe(priority = EventPriority.LOWEST)
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
    
    private static Field hitField = ReflectionHelper.findField(EntityLivingBase.class, "field_70718_bc", "recentlyHit");
    static {
        if (hitField == null) {
            Core.logSevere("SocketLacerator didn't find field for EntityLivingBase.recentlyHit!");
        }
    }
    private boolean _handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        if (mop == null) return false;
        if (mopIsThis) return false;
        
        if (mop.typeOfHit == EnumMovingObjectType.ENTITY) {
            if (!(mop.entityHit instanceof EntityLivingBase)) {
                return false;
            }
            EntityLivingBase elb = (EntityLivingBase) mop.entityHit;
            if (elb.isDead || elb.getHealth() <= 0) return false;
            if (cantDoWork(socket)) return true && !grab_items;
            socket.extractCharge(1); //It's fine if it fails
            float damage = 4F*speed/max_speed;
            if (elb.getHealth() <= damage && rand.nextInt(20) == 1) { 
                if (hitField != null) {
                    try {
                        hitField.set(elb, 100);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
            if (elb.attackEntityFrom(laceration, damage)) {
                if (elb.getHealth() <= 0) {
                    grab_items = true;
                }
                targetHash = -1;
            }
            progress = 0;
            return true;
        } else if (mop.typeOfHit == EnumMovingObjectType.TILE) {
            int id = worldObj.getBlockId(mop.blockX, mop.blockY, mop.blockZ);
            Block block = Block.blocksList[id];
            TileEntity te = null;
            TileEntityDayBarrel barrel = null;
            if (block == Core.registry.factory_block) {
                te = worldObj.getBlockTileEntity(mop.blockX, mop.blockY, mop.blockZ);
                if (te instanceof TileEntityDayBarrel) {
                    barrel = (TileEntityDayBarrel) te;
                    if (barrel.item == null) {
                        return false;
                    }
                }
            }
            if (cantDoWork(socket)) return true;
            socket.extractCharge(1); //It's fine if it fails
            
            //Below: A brief demonstration of why Coord exists
            long foundHash = (mop.blockX) + (mop.blockY << 2) + (mop.blockZ << 4);
            int md = worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
            float hardness = block.getBlockHardness(worldObj, mop.blockX, mop.blockY, mop.blockZ);
            if (hardness < 0) {
                speed -= max_speed/5;
                return true;
            }
            foundHash = (foundHash << 4) + id*16 + md;
            if (barrel != null) {
                foundHash += barrel.item.itemID*5 + barrel.item.getItemDamage()*10;
            }
            if (foundHash != targetHash) {
                targetHash = foundHash;
                progress = 0;
            } else {
                progress++;
            }
            boolean doBreak = progress >= grind_time*hardness || Core.cheat;
            mop.subHit = id; //We abuse this to store the block ID!
            if (barrel == null && !doBreak) {
                float perc = progress/((float)grind_time*hardness);
                int breakage = (int) (perc*10);
                destroyPartially(mop, breakage);
                present_breaking_target = mop;
            } else if (doBreak) {
                destroyPartially(mop, 99);
            }
            if (doBreak) {
                grab_items = true;
                grind_items = true;
                if (barrel == null) {
                    int l = id;
                    int i1 = md;
                    worldObj.playAuxSFX(2001, mop.blockX, mop.blockY, mop.blockZ, id + (md << 12));
                    
                    EntityPlayer player = getFakePlayer();
                    ItemStack pick = new ItemStack(Item.pickaxeDiamond);
                    pick.addEnchantment(Enchantment.silkTouch, 1);
                    player.inventory.mainInventory[0] = pick;
                    {
                        ItemStack itemstack = pick;
                        boolean canHarvest = false;
                        if (block != null) {
                            canHarvest = block.canHarvestBlock(player, md);
                        }

                        boolean didRemove = removeBlock(player, block, md, mop.blockX, mop.blockY, mop.blockZ);
                        if (didRemove && canHarvest) {
                            Block.blocksList[id].harvestBlock(worldObj, player, mop.blockX, mop.blockY, mop.blockZ, md);
                        }
                    }
                    block.onBlockHarvested(worldObj, mop.blockX, mop.blockY, mop.blockZ, md, player);
                    if (block.removeBlockByPlayer(worldObj, player, mop.blockX, mop.blockY, mop.blockZ)) {
                        block.onBlockDestroyedByPlayer(worldObj, mop.blockX, mop.blockY, mop.blockZ, md);
                        block.harvestBlock(worldObj, player, mop.blockX, mop.blockY, mop.blockZ, 0);
                    }
                    player.inventory.mainInventory[0] = null;
                } else if (barrel.getItemCount() > 0) {
                    ItemStack is = barrel.item.copy();
                    is.stackSize = 1;
                    barrel.changeItemCount(-1);
                    grind_items = true;
                    processCollectedItem(is);
                }
                progress = 0;
            }
            return true;
        } else {
            return false;
        }
    }
    
    private boolean removeBlock(EntityPlayer thisPlayerMP, Block block, int md, int x, int y, int z) {
        if (block == null) return false;
        block.onBlockHarvested(worldObj, x, y, z, md, thisPlayerMP);
        if (block.removeBlockByPlayer(worldObj, thisPlayerMP, x, y, z)) {
            block.onBlockDestroyedByPlayer(worldObj, x, y, z, md);
            return true;
        }
        return false;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        charge = data.as(Share.PRIVATE, "charge").put(charge);
        speed = data.as(Share.VISIBLE, "spd").putShort(speed);
        progress = data.as(Share.PRIVATE, "prg").putShort(progress);
        buffer = data.as(Share.PRIVATE, "buf").putItemArray(buffer);
        grab_items = data.as(Share.PRIVATE, "grb").putBoolean(grab_items);
        targetHash = data.as(Share.PRIVATE, "hsh").putLong(targetHash);
        grind_items = data.as(Share.PRIVATE, "grn").putBoolean(grind_items);
        return this;
    }
    
    
    
    //Inventory stuff
    @Override public boolean canExtractItem(int i, ItemStack itemstack, int j) { return true; }
    @Override public boolean canInsertItem(int i, ItemStack itemstack, int j) { return false; }
    @Override public void closeChest() { }
    @Override public void openChest() { }
    @Override public String getInvName() { return "lacerator"; }
    @Override public int getSizeInventory() { return 1; }
    @Override public ItemStack getStackInSlotOnClosing(int i) { return null; }
    @Override public boolean isInvNameLocalized() { return false; }
    @Override public boolean isUseableByPlayer(EntityPlayer entityplayer) { return false; }
    @Override public boolean isItemValidForSlot(int i, ItemStack itemstack) { return false; }
    
    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (slot != 0 || buffer.isEmpty()) {
            return null;
        }
        ItemStack it = buffer.get(0);
        ItemStack is = it.splitStack(count);
        if (it.stackSize <= 0) {
            buffer.remove(0);
        }
        return is;
    }
    
    private static int[] back = new int[] {0}, none = new int[0];
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (side == facing.getOpposite().ordinal()) {
            return back;
        }
        return none;
    }
    
    @Override
    public int getInventoryStackLimit() {
        return 64; //could be 0, but too weird.
    }
    
    @Override
    public ItemStack getStackInSlot(int i) {
        if (i != 0 || buffer.isEmpty()) {
            return null;
        }
        return buffer.get(0);
    }
    
    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (buffer.isEmpty()) {
            Core.logSevere("Someone's doing stupid things to %s with a %s", getCoord(), itemstack);
            Thread.dumpStack();
            buffer.add(itemstack);
        } else {
            buffer.set(0, itemstack);
        }
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        for (ItemStack is : buffer) {
            FactorizationUtil.spawnItemStack(here, is);
        }
    }
    
    
    //Render code
    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        float turn = FactorizationUtil.interp(prev_rotation, rotation, partial) / 5.0F;
        GL11.glRotatef(turn, 0, 1, 0);
        float sd = motor == null ? 0 : 3F/16F;
        GL11.glTranslatef(0, -4F/16F + sd + (float) Math.abs(Math.sin(turn/800))/32F, 0);
        TileEntityGrinderRender.renderGrindHead();
        if (ticked) {
            ticked = false;
            if (speed > max_speed/3) {
                addParticles();
            }
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(ServoMotor motor, Tessellator tess) {
        Icon metal = BlockIcons.motor_texture;
        float d = 4.0F / 16.0F;
        float yd = -d + 0.003F;
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTextures(null, null,
                metal, metal,
                metal, metal);
        float yoffset = 5F/16F;
        float sd = motor == null ? 0 : 2F/16F;
        block.setBlockBounds(d, d + yd + yoffset + 2F/16F + sd, d, 1 - d, 1 - (d + 0F/16F) + yd + yoffset, 1 - d);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        block.renderRotated(tess, xCoord, yCoord, zCoord);
    }
    
    
    private static MovingObjectPosition particleMop = new MovingObjectPosition(0, 0, 0, 0, Vec3.createVectorHelper(0, 0, 0));
    
    @SideOnly(Side.CLIENT)
    static EffectRenderer particleTweaker, origER;
    @SideOnly(Side.CLIENT)
    static SocketLacerator me;
    @SideOnly(Side.CLIENT)
    static int px, py, pz;
    @SideOnly(Side.CLIENT)
    static double facex, facey, facez;
    
    @SideOnly(Side.CLIENT)
    void addParticles() {
        Minecraft mc = Minecraft.getMinecraft();
        if (particleTweaker == null) {
            particleTweaker = new ParticleWarper(worldObj, mc.renderEngine);
        }
        px = xCoord + facing.offsetX;
        py = yCoord + facing.offsetY;
        pz = zCoord + facing.offsetZ;
        
        ForgeDirection op = facing.getOpposite();
        
        facex = px + 0.5 + 0.5*op.offsetX;
        facey = py + 0.5 + 0.5*op.offsetY;
        facez = pz + 0.5 + 0.5*op.offsetZ;
        
        int id = worldObj.getBlockId(px, py, pz);
        
        Block b = Block.blocksList[id];
        if (b == null) {
            return;
        }
        origER = mc.effectRenderer;
        me = this;
        mc.effectRenderer = particleTweaker;
        try {
            for (int i = 0; i < 1; i++) {
                particleTweaker.addBlockHitEffects(px, py, pz, op.ordinal());
            }
        } finally {
            me = null;
            mc.effectRenderer = origER;
        }
    }
    
    static class ParticleWarper extends EffectRenderer {

        public ParticleWarper(World world, TextureManager textureManager) {
            super(world, textureManager);
        }
        
        @Override
        public void addEffect(EntityFX particle) {
            if (particle == null) {
                return;
            }
            ForgeDirection fd = me.facing;
            if (fd.offsetY != 0) {
                origER.addEffect(particle);
                return;
            }
            particle.posX = facex;
            particle.posY = facey;
            particle.posZ = facez;
            float theta = (float) (Math.random()*Math.PI*2);
            double dist = 4.0/16.0;
            
            
            Vec3 dir = me.worldObj.getWorldVec3Pool().getVecFromPool(0, dist, 0);
            
            
            if (fd.offsetX != 0) {
                dir.rotateAroundX(theta);
            } else if (fd.offsetY != 0) {
                dir.rotateAroundY(theta);
            } else if (fd.offsetZ != 0) {
                dir.rotateAroundZ(theta);
            }
            particle.posX += dir.xCoord;
            particle.posY += dir.yCoord;
            particle.posZ += dir.zCoord;
            theta = (float) (Math.PI/2);
            if (fd.offsetX != 0) {
                dir.rotateAroundX(theta);
            } else if (fd.offsetY != 0) {
                dir.rotateAroundY(theta);
            } else if (fd.offsetZ != 0) {
                dir.rotateAroundZ(theta);
            }
            float speed = 0.8F*me.speed/max_speed;
            particle.motionX = dir.xCoord*speed;
            particle.motionY = dir.yCoord*speed;
            particle.motionZ = dir.zCoord*speed;
            if (particle.motionY > speed/4) {
                particle.motionY *= 3;
            }
            particle.multipleParticleScaleBy(1 + rand.nextFloat()*2/3);
            origER.addEffect(particle);
        }
    }
    
    @ForgeSubscribe
    @SideOnly(Side.CLIENT)
    public void resetEffectRenderer(WorldEvent.Unload loadEvent) {
        particleTweaker = null;
    }
    
    @Override
    public void uninstall() {
        if (!worldObj.isRemote) {
            destroyPartially(present_breaking_target, 99);
            destroyPartially(previous_breaking_target, 99);
        }
    }
}
