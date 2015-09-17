package factorization.beauty.wickedness;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import factorization.util.EvilUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TileEntityMisanthropicGenerator extends TileEntityCommon implements IEntitySelector {
    ItemStack entheas = null;
    int spawned_eggs = 0;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        entheas = data.as(Share.VISIBLE, "entheas").putItemStack(entheas);
        spawned_eggs = data.as(Share.PRIVATE, "spawnedEggs").putInt(spawned_eggs);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MISANTHROPIC_GEN;
    }

    @Override
    public void click(EntityPlayer player) {
        if (worldObj.isRemote) return;
        if (player instanceof FakePlayer) return; // Specifically forbid machine interaction
        if (entheas == null) return;
        ItemStack give = entheas;
        if (give.stackSize > 16) {
            give = give.splitStack(16);
        } else {
            entheas = null;
        }
        new Coord(this).spawnItem(give).onCollideWithPlayer(player);
        markDirty();
        broadcastMessage(null, getDescriptionPacket());
    }



    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        // Machine interaction's allowed here.
        if (worldObj.isRemote) return false;
        if (ItemUtil.getStackSize(entheas) > 64) return false;
        final ItemStack held = player.getHeldItem();
        if (pushEntheas(held)) {
            player.setCurrentItemOrArmor(0, null);
            markDirty();
            broadcastMessage(null, getDescriptionPacket());
            return true;
        }
        if (held == null) {
            boolean any = false;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack is = player.inventory.getStackInSlot(i);
                if (pushEntheas(is)) {
                    player.inventory.setInventorySlotContents(i, null);
                    any = true;
                }
            }
            if (any) {
                Core.proxy.updatePlayerInventory(player);
                markDirty();
                broadcastMessage(null, getDescriptionPacket());
                return true;
            }
        }
        return super.activate(player, side);
    }

    boolean pushEntheas(ItemStack held) {
        if (!ItemUtil.is(held, Core.registry.entheas)) return false;
        if (entheas == null) {
            entheas = held.copy();
        } else {
            entheas.stackSize += held.stackSize;
        }
        markDirty();
        return true;
    }

    @Override
    public boolean canUpdate() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;
    }

    static int ENTHEAS_PER_EGG = 8;

    boolean isOverfilled() {
        if (entheas == null) return false;
        return entheas.stackSize > ENTHEAS_PER_EGG;
    }

    int rng(int maxish) {
        return (int) (rand.nextGaussian() * maxish);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        int delay = 1600;
        float moon = EvilUtil.getMoonPower(worldObj);
        if (moon > 0) {
            if (moon > 1) moon = 1;
            delay *= (1 - moon);
            if (delay < 400) delay = 400;
        }
        if (worldObj.getTotalWorldTime() % delay != 0) return;
        int r = 12;
        AxisAlignedBB box = SpaceUtil.createAABB(getCoord().add(-r, -r, -r), getCoord().add(+r, +r, +r));
        worldObj.getEntitiesWithinAABBExcludingEntity(null, box, this);
        if (rand.nextInt(4) == 0) {
            trySpawnEgg();
        }
    }

    static final int EGG_RANGE = 8;
    static final int MAX_EGGS = 4;

    boolean trySpawnEgg() {
        if (!isOverfilled()) return true;
        if (spawned_eggs > MAX_EGGS) {
            Coord min = getCoord().add(-EGG_RANGE, -EGG_RANGE, -EGG_RANGE);
            Coord max = getCoord().add(+EGG_RANGE, +EGG_RANGE, +EGG_RANGE);
            final ArrayList<TileEntityMisanthropicEgg> eggs = new ArrayList<TileEntityMisanthropicEgg>();
            Coord.iterateCube(min, max, new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (here.blockHasTE()) {
                        TileEntityMisanthropicEgg egg = here.getTE(TileEntityMisanthropicEgg.class);
                        if (egg != null) eggs.add(egg);
                    }
                }
            });
            spawned_eggs = (byte) eggs.size();
            if (spawned_eggs > MAX_EGGS) {
                Collections.shuffle(eggs);
                for (TileEntityMisanthropicEgg egg : eggs) {
                    if (entheas == null) break;
                    if (egg.evil != null && egg.evilBit != null && egg.evilBit.reasonablyInfinite) {
                        egg.power++;
                        entheas = ItemUtil.normalDecr(entheas);
                    }
                }
                return true;
            }
        }
        return makeEgg();
    }

    private boolean makeEgg() {
        Coord here = getCoord().add(rng(EGG_RANGE), rng(EGG_RANGE), rng(EGG_RANGE));
        if (!here.blockExists()) return false;
        if (!here.isReplacable()) return false;
        while (here.isReplacable()) {
            here.y--;
            if (here.y < 0) return false;
        }
        here.y++;
        if (here.getCombinedLight() > 4) return false;
        here.setId(Core.registry.factory_block);
        TileEntityMisanthropicEgg egg = new TileEntityMisanthropicEgg();
        egg.power = ENTHEAS_PER_EGG;
        entheas.stackSize -= ENTHEAS_PER_EGG;
        entheas = ItemUtil.normalize(entheas);
        here.setTE(egg);
        egg.init(rand);
        broadcastMessage(null, NetworkFactorization.MessageType.MisanthropicSpawn);
        return true;
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) return true;
        if (messageType == NetworkFactorization.MessageType.MisanthropicSpawn) {
            for (double dy = -1; dy < +2; dy += 0.25) {
                double x = xCoord + rand.nextFloat();
                double y = yCoord + rand.nextFloat() + dy;
                double z = zCoord + rand.nextFloat();
                worldObj.spawnParticle("smoke", x, y, z, 0.0D, 0.0D, 0.0D);
                worldObj.spawnParticle("flame", x, y, z, 0.0D, 0.0D, 0.0D);
            }
            return true;
        } else if (messageType == NetworkFactorization.MessageType.MisanthropicCharge) {
            int id = input.readInt();
            Entity ent = worldObj.getEntityByID(id);
            if (ent == null) return true;
            for (int n = 0; n < 20; n++) {
                double x = ent.posX + rand.nextGaussian() / 4;
                double y = ent.posY + rand.nextGaussian() / 4 + ent.getEyeHeight();
                double z = ent.posZ + rand.nextGaussian() / 4;
                float gray = 20 / 255F;
                float r, g, b;
                r = g = b = gray;
                if (n % 3 == 0) {
                    r = 0.9F;
                    g = b = 0.3F;
                }
                worldObj.spawnParticle("mobSpell", x, y, z, r, g, b);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isEntityApplicable(Entity entity) {
        if (!(entity instanceof EntityLiving)) return false;
        EntityLiving mob = (EntityLiving) entity;
        int power = 0;
        if ((mob instanceof IMob)) {
            if (mob.isNoDespawnRequired()) {
                power += 2;
            }
            for (Object o : mob.getActivePotionEffects()) {
                if (o instanceof PotionEffect) {
                    PotionEffect po = (PotionEffect) o;
                    if (po.getIsPotionDurationMax()) {
                        power++;
                    }
                }
            }
            int wither_power = 4;
            if (mob instanceof EntityWither) {
                power += wither_power;
            }
            if (power >= wither_power && rand.nextInt(10) == 0 && EvilUtil.getMoonPower(worldObj) > 1) {
                teleportRandomly(mob);
            }
        } else {
            if (mob.getHealth() <= 0 && !mob.isDead && mob.deathTime > 0 && mob.deathTime < 15) {
                power++;
                mob.setDead();
            }
        }
        if (power > 0) {
            addEntheas(power);
            broadcastMessage(null, NetworkFactorization.MessageType.MisanthropicCharge, mob.getEntityId());
        }
        return false;
    }

    private void addEntheas(int n) {
        if (entheas == null) {
            entheas = new ItemStack(Core.registry.entheas, n);
        } else {
            entheas.stackSize += n;
        }
    }

    public static boolean teleportRandomly(EntityLivingBase dis) {
        double d0 = dis.posX + (rand.nextDouble() - 0.5D) * 64.0D;
        double d1 = dis.posY + (double) (rand.nextInt(64) - 32);
        double d2 = dis.posZ + (rand.nextDouble() - 0.5D) * 64.0D;
        return teleportTo(dis, d0, d1, d2);
    }

    public static boolean teleportTo(EntityLivingBase dis, double p_70825_1_, double p_70825_3_, double p_70825_5_) {
        EnderTeleportEvent event = new EnderTeleportEvent(dis, p_70825_1_, p_70825_3_, p_70825_5_, 0);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        double d3 = dis.posX;
        double d4 = dis.posY;
        double d5 = dis.posZ;
        dis.posX = event.targetX;
        dis.posY = event.targetY;
        dis.posZ = event.targetZ;
        boolean flag = false;
        int i = MathHelper.floor_double(dis.posX);
        int j = MathHelper.floor_double(dis.posY);
        int k = MathHelper.floor_double(dis.posZ);

        if (dis.worldObj.blockExists(i, j, k)) {
            boolean flag1 = false;

            while (!flag1 && j > 0) {
                Block block = dis.worldObj.getBlock(i, j - 1, k);

                if (block.getMaterial().blocksMovement()) {
                    flag1 = true;
                } else {
                    --dis.posY;
                    --j;
                }
            }

            if (flag1) {
                dis.setPosition(dis.posX, dis.posY, dis.posZ);

                if (dis.worldObj.getCollidingBoundingBoxes(dis, dis.boundingBox).isEmpty() && !dis.worldObj.isAnyLiquid(dis.boundingBox)) {
                    flag = true;
                }
            }
        }

        if (!flag) {
            dis.setPosition(d3, d4, d5);
            return false;
        } else {
            short short1 = 128;

            for (int l = 0; l < short1; ++l) {
                double d6 = (double) l / ((double) short1 - 1.0D);
                float f = (rand.nextFloat() - 0.5F) * 0.2F;
                float f1 = (rand.nextFloat() - 0.5F) * 0.2F;
                float f2 = (rand.nextFloat() - 0.5F) * 0.2F;
                double d7 = d3 + (dis.posX - d3) * d6 + (rand.nextDouble() - 0.5D) * (double) dis.width * 2.0D;
                double d8 = d4 + (dis.posY - d4) * d6 + rand.nextDouble() * (double) dis.height;
                double d9 = d5 + (dis.posZ - d5) * d6 + (rand.nextDouble() - 0.5D) * (double) dis.width * 2.0D;
                dis.worldObj.spawnParticle("portal", d7, d8, d9, (double) f, (double) f1, (double) f2);
            }

            dis.worldObj.playSoundEffect(d3, d4, d5, "mob.endermen.portal", 1.0F, 1.0F);
            dis.playSound("mob.endermen.portal", 1.0F, 1.0F);
            return true;
        }
    }

    @Override
    public boolean redrawOnSync() {
        return true;
    }

    @Override
    protected void onRemove() {
        if (entheas == null) return;
        Coord at = new Coord(this);
        while (entheas.stackSize > 0) {
            int free = Math.min(16, entheas.stackSize);
            at.spawnItem(entheas.splitStack(free));
            entheas.stackSize -= 16;
        }
    }
}
