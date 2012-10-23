package factorization.common;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.EnumMovingObjectType;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import factorization.api.Coord;
import factorization.api.IExoUpgrade;
import factorization.common.Core.TabType;

public class ExoWallJump extends Item implements IExoUpgrade, ITickHandler {

    protected ExoWallJump(int par1) {
        super(par1);
        setItemName("exo.wallJump");
        Core.tab(this, TabType.MISC);
        setIconIndex(16 * 10);
        setMaxStackSize(1);
        setTextureFile(Core.texture_file_item);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }

    @Override
    public boolean canUpgradeArmor(ItemStack is, int armorIndex) {
        return armorIndex == 3;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade, boolean isEnabled) {
        NBTTagCompound tag = FactorizationUtil.getTag(upgrade);
        final int maxChargeCount = 1;
        int charges = tag.getInteger("C");
        if (charges < maxChargeCount && !isEnabled) {
            if (!player.onGround) {
                return null;
            } else {
                if (FactorizationUtil.itemCanFire(player.worldObj, upgrade, 20*2)) {
                    tag.setInteger("C", charges + 1);
                    return upgrade;
                }
                return null;
            }
        }
        if (isEnabled && charges > 0) {
            //if the player is looking at a block, and it's solid, and the distance is < 1m, push player away from it
            if (FactorizationUtil.itemCanFire(player.worldObj, upgrade, 20*2)) {
                Vec3 position = Vec3.getVec3Pool().getVecFromPool(player.posX, player.posY, player.posZ);
                Vec3 incidence = player.getLookVec();
                double dist = 2;
                Vec3 var6 = position.addVector(incidence.xCoord * dist, incidence.yCoord * dist, incidence.zCoord * dist);
                MovingObjectPosition selected = player.worldObj.rayTraceBlocks(position, var6);
                if (selected == null || selected.typeOfHit != EnumMovingObjectType.TILE) {
                    return null;
                }
                ForgeDirection dir = ForgeDirection.values()[selected.sideHit];
                if (!player.worldObj.isBlockSolidOnSide(selected.blockX, selected.blockY, selected.blockZ, dir)) {
                    return null;
                }
                if (dir.offsetY != 0) {
                    return null;
                }
                player.fallDistance = 0;
                if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
                    EntityPlayerMP mp = (EntityPlayerMP) player;
                    mp.playerNetServerHandler.ticksForFloatKick = 0;
                    player.rotationYaw += 180;
                    Sound.wallJump.playAt(player);
                } else {
                    if (turningFrames != 0) {
                        if (dir.offsetX != 0) {
                            player.motionX *= -1.1;
                        }
                        if (dir.offsetZ != 0) {
                            player.motionZ *= -1.1;
                        }
                        player.motionY += 0.4F;
                    } else {
                        player.motionY += 0.1;
                    }
                    turningFrames = 0;
                }
                tag.setInteger("C", charges - 1);
                return upgrade;
            }
        }
        return null;
    }	

    @Override
    public void addArmorProperties(ItemStack is, ArmorProperties armor) {}

    @Override
    public int getArmorDisplay(ItemStack is) {
        return 0;
    }

    @Override
    public boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source, int damage, int slot) {
        return false;
    }

    @Override
    public String getDescription() {
        return "Jump against walls";
    }
    
    
    static float targetRotation = 0;
    static int turningFrames = 10;

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        final float totalFrames = 5;
        if (turningFrames < totalFrames && Core.isMainClientThread.get()) {
            turningFrames++;
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            double motionTheta = Math.toDegrees(Math.atan2(player.motionZ, player.motionX));
            motionTheta += 360*(int)(player.rotationYaw/360) - 90;
            float perc = (turningFrames) / totalFrames;
            if (perc < 0.75) {
                perc *= 1.25;
            }
            double diff = motionTheta - player.rotationYaw;
            if (diff > 180) {
                diff -= 360;
            }
            if (diff < -180) {
                diff += 360;
            }
            if (Math.abs(diff) < 0.5) {
                turningFrames = (int) totalFrames;
            } else {
                player.rotationYaw += diff*perc;
            }
            //player.rotationYaw = (float) motionTheta;
        }
    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.CLIENT);
    }

    @Override
    public String getLabel() {
        return "exospring";
    }
    
    @Override
    public void addInformation(ItemStack is, List list) {
        list.add("Exo-Upgrade");
        Core.brand(list);
    }
}
