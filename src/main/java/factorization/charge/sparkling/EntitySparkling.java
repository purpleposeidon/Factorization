package factorization.charge.sparkling;

import factorization.charge.enet.TileEntityLeydenJar;
import factorization.common.BlockResource;
import factorization.common.ResourceType;
import factorization.fzds.interfaces.Interpolation;
import factorization.net.FzNetDispatch;
import factorization.net.INet;
import factorization.net.NetworkFactorization;
import factorization.net.StandardMessageType;
import factorization.notify.Notice;
import factorization.notify.NoticeUpdater;
import factorization.shared.Core;
import factorization.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.io.IOException;

public class EntitySparkling extends EntityMob implements INet {
    static final int MAX_SURGE = TileEntityLeydenJar.MAX_STORAGE;
    public static final String MOB_NAME = "fz_sparkling";
    int surge;

    public EntitySparkling(World world) {
        super(world);
        //this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.tasks.addTask(4, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.experienceValue = 3;
        this.isImmuneToFire = true;
        setSurgeLevel(4, false);
    }

    @Override
    protected PathNavigate getNewNavigator(World world) {
        return super.getNewNavigator(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    public int getSurgeLevel() {
        int ret = surge;
        if (ret > MAX_SURGE) return MAX_SURGE;
        return ret;
    }

    void updateSize() {
        // 0.35 min, 2 max...
        double s = getSurgeLevel() * 0.35;
        double min = 4.0 / 16.0;
        s = Math.min(1.5 - min, (1 + Math.log(s)) / 3.6);
        s += min;
        float f = (float) s;
        this.setSize(f, f);
    }

    public void setSurgeLevel(int level, boolean sync) {
        if (level >= MAX_SURGE) level = MAX_SURGE;
        setHealth(level);
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(level);
        surge = level;
        updateSize();
        if (sync && !worldObj.isRemote) {
            Packet toSend = Core.network.entityPacket(this, StandardMessageType.SetAmount, level);
            FzNetDispatch.addPacketFrom(toSend, this);
        }
    }


    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (worldObj.isRemote) {
            if (lastSurge == -1) lastSurge = getSurgeLevel();
            int newSurge = getSurgeLevel();
            if (newSurge > lastSurge) {
                lastSurge = newSurge;
                playSound(getSpawnSound(), getSoundVolume(), getSoundPitch());
            }
        }
    }
    transient int lastSurge = -1;

    @Override
    protected float getSoundPitch() {
        double r = Interpolation.SQUARE.scale(getSurgeLevel() / (float) MAX_SURGE);
        double pitch = NumUtil.interp(2.0, 1.0 / 8.0, r);
        NORELEASE.println("pitch", pitch);
        return (float) pitch;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (doAttack(source, amount)) {
            setSurgeLevel((int) (getSurgeLevel() - amount), true);
            return true;
        }
        return false;
    }

    protected boolean doAttack(DamageSource source, float amount) {
        if (source == DamageSource.fall) {
            return false;
        }
        if (source == DamageSource.inWall) {
            if  (!isEntityInsideOpaqueBlock()) return true;
            for (EnumFacing dir : EnumFacing.VALUES) {
                posX += dir.getFrontOffsetX();
                posY += dir.getFrontOffsetY();
                posZ += dir.getFrontOffsetZ();

                if (!isEntityInsideOpaqueBlock()) {
                    return false;
                }

                posX -= dir.getFrontOffsetX();
                posY -= dir.getFrontOffsetY();
                posZ -= dir.getFrontOffsetZ();
            }
            return true;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected Item getDropItem() {
        return rand.nextBoolean() ? Items.redstone : Items.glowstone_dust;
    }

    @Override
    protected String getLivingSound() {
        return "factorization:mob.sparkling.say";
    }

    @Override
    protected String getHurtSound() {
        return "factorization:mob.sparkling.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "factorization:mob.sparkling.death";
    }

    protected String getSpawnSound() {
        return "factorization:mob.sparkling.spawn";
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt lightningBolt) {
        setSurgeLevel(MAX_SURGE, true);
    }

    public static boolean isGrounded(EntityLivingBase player) {
        int feet = 1;
        ItemStack boots = player.getEquipmentInSlot(feet);
        if (ItemUtil.is(boots, Items.golden_boots)) {
            return true;
        }
        int right_hand = 0;
        ItemStack sword = player.getEquipmentInSlot(right_hand);
        if (ItemUtil.is(sword, Items.golden_sword)) {
            if (player instanceof EntityPlayer) {
                if (((EntityPlayer) player).isBlocking()) {
                    return true;
                }
            }
        }
        return false;
    }

    int getPower() {
        int power = (int) (Math.log(getSurgeLevel())) - 1;
        if (power > 5) return 5;
        return power;
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        setSurgeLevel(getSurgeLevel() - 1, true);
        if (entity instanceof EntityLivingBase) {
            if (isGrounded((EntityLivingBase) entity)) {
                return false;
            }
        }
        if (!super.attackEntityAsMob(entity)) return false;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase player = (EntityLivingBase) entity;
            int power = getPower();
            if (power > 0) {
                player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 3 * 20, power));
                player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 3 * 20, power));
            }
        }
        return true;
    }

    @Override
    public void onLivingUpdate() {
        if (!onGround && motionY < 0.0D) {
            motionY *= 0.6D;
        }
        super.onLivingUpdate();
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 15728880;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }

    @Override
    public boolean interactAt(EntityPlayer player, Vec3 targetVec3) {
        if (worldObj.isRemote) {
            return true;
        }
        if (ItemUtil.is(player.getHeldItem(), Core.registry.charge_meter)) {
            new Notice(this, new NoticeUpdater() {
                @Override
                public void update(Notice msg) {
                    if (EntitySparkling.this.getHealth() <= 0) return;
                    msg.setMessage("factorization:entity.sparkling.info", "" + EntitySparkling.this.getSurgeLevel());
                }
            }).sendTo(player);
            return true;
        }
        if (ItemUtil.is(player.getHeldItem(), Core.registry.leydenjar_item.getItem())) {
            TileEntityLeydenJar jar = new TileEntityLeydenJar();
            jar.loadFromStack(player.getHeldItem());
            if (jar.storage > 0) return false;
            jar.storage = getSurgeLevel();
            ItemStack is = jar.getDroppedBlock();
            PlayerUtil.consumeHeldItem(player);
            InvUtil.givePlayerItemIntoHand(player, is);
            setDead();
            return true;
        }
        if (Core.dev_environ) {
            int surge = getSurgeLevel();
            int level = surge + (player.isSneaking() ? -1 : +1);
            setSurgeLevel(level, true);
            NORELEASE.println("New surge level: " + level);
            return true;
        }
        return false;
    }


    @Override
    protected void doBlockCollisions() {
        double pad = 0.001D;
        BlockPos min = new BlockPos(getEntityBoundingBox().minX + pad, getEntityBoundingBox().minY + pad, getEntityBoundingBox().minZ + pad);
        BlockPos max = new BlockPos(getEntityBoundingBox().maxX - pad, getEntityBoundingBox().maxY - pad, getEntityBoundingBox().maxZ - pad);
        if (!worldObj.isAreaLoaded(min, max)) return;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(0, 0, 0);

        for (int x = min.getX(); x <= max.getX(); ++x) {
            for (int y = min.getY(); y <= max.getY(); ++y) {
                for (int z = min.getZ(); z <= max.getZ(); ++z) {
                    at.set(x, y, z);
                    IBlockState bs = worldObj.getBlockState(at);

                    try {
                        Block block = bs.getBlock();
                        collideWithBlock(at, bs, block);
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Colliding entity with block");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being collided with");
                        CrashReportCategory.addBlockInfo(crashreportcategory, at, bs);
                        throw new ReportedException(crashreport);
                    }
                }
            }
        }
    }

    static DamageSource shorted = new DamageSource("shorted");

    protected void collideWithBlock(BlockPos.MutableBlockPos at, IBlockState bs, Block block) {
        if (block == Blocks.gold_block || (block == Core.registry.resource_block && bs.getValue(BlockResource.TYPE) == ResourceType.COPPER_BLOCK)) {
            attackEntityFrom(shorted, 1);
        }
        block.onEntityCollidedWithBlock(this.worldObj, at, bs, this);
    }

    @Override
    protected void collideWithEntity(Entity ent) {
        if (ent instanceof EntitySparkling) {
            EntitySparkling other = (EntitySparkling) ent;
            if (getSurgeLevel() >= other.getSurgeLevel()) {
                setSurgeLevel(getSurgeLevel() + other.getSurgeLevel(), true);
                other.setDead();
                return;
            }
        }
        super.collideWithEntity(ent);
    }

    @Override
    public Enum[] getMessages() {
        return new Enum[0];
    }

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (messageType == StandardMessageType.SetAmount) {
            setSurgeLevel(input.readInt(), false);
        }
        return false;
    }

    @Override
    public boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setInteger("surge", surge);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);
        if (tagCompund.hasKey("surge")) {
            surge = tagCompund.getInteger("surge");
        }
    }
}
