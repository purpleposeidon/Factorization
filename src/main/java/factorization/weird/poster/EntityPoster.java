package factorization.weird.poster;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.shared.Core;
import factorization.shared.EntityFz;
import factorization.util.ItemUtil;
import factorization.util.NumUtil;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class EntityPoster extends EntityFz  {
    public ItemStack inv = new ItemStack(Core.registry.spawnPoster);
    public Quaternion rot = new Quaternion();
    public double scale = 1.0;
    public boolean locked = false;

    Quaternion base_rotation = new Quaternion();
    double base_scale = 1.0;
    public short spin_normal = 0, spin_vertical = 0, spin_tilt = 0;
    byte delta_scale = 0;
    EnumFacing norm = EnumFacing.NORTH, top = EnumFacing.UP, tilt = EnumFacing.EAST;

    public EntityPoster(World w) {
        super(w);
        setSize(0.5F, 0.5F);
    }

    void updateSize() {
        Vec3 here = SpaceUtil.fromEntPos(this);
        Vec3[] parts = new Vec3[6];
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir == norm) continue;
            float s = (float) scale;
            if (dir == norm.getOpposite()) {
                s /= 16;
            } else {
                s /= 2;
            }
            Vec3 d = SpaceUtil.fromDirection(dir);
            parts[dir.ordinal()] = rot.applyRotation(SpaceUtil.scale(d, s)).add(here);
        }
        setEntityBoundingBox(SpaceUtil.newBox(parts));
    }

    public void setItem(ItemStack item) {
        if (item == null) {
            item = new ItemStack(Core.registry.spawnPoster);
        }
        inv = item;
    }

    public ItemStack getItem() {
        if (ItemUtil.is(inv, Core.registry.spawnPoster)) return null;
        return inv;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isInRangeToRender3d(double p_145770_1_, double p_145770_3_, double p_145770_5_) {
        return super.isInRangeToRender3d(p_145770_1_, p_145770_3_, p_145770_5_);
    }

    public void setBase(double baseScale, Quaternion baseRotation, EnumFacing norm, EnumFacing top, AxisAlignedBB bounds) {
        this.scale = this.base_scale = baseScale;
        this.base_rotation = baseRotation;
        this.rot = new Quaternion(base_rotation);
        spin_normal = 0;
        spin_vertical = 0;
        delta_scale = 0;
        this.norm = norm;
        this.top = top;

        Vec3 tiltV = SpaceUtil.fromDirection(norm).crossProduct(SpaceUtil.fromDirection(top));
        this.tilt = SpaceUtil.round(tiltV, norm);

        updateSize();
        setEntityBoundingBox(bounds);
    }

    public void updateValues() {
        delta_scale = (byte) NumUtil.clip(delta_scale, -8, +6);
        scale = base_scale * Math.pow(SCALE_INCR, delta_scale);
        Quaternion rNorm = Quaternion.getRotationQuaternionRadians(spin_normal * SPIN_PER_CLICK, norm);
        Quaternion rVert = Quaternion.getRotationQuaternionRadians(spin_vertical * SPIN_PER_CLICK, top);
        Quaternion rTilt = Quaternion.getRotationQuaternionRadians(spin_tilt * SPIN_PER_CLICK, tilt);
        rot = rVert.multiply(rNorm).multiply(rTilt).multiply(base_rotation);
    }

    @SideOnly(Side.CLIENT)
    public boolean canBeCollidedWith() {
        if (!worldObj.isRemote) return false;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return false;
        if (locked && !PlayerUtil.isPlayerCreative(player)) return false;
        if (player.isSneaking()) return true;
        if (inv.getItem() == Core.registry.spawnPoster) return true;
        ItemStack held = player.getHeldItem();
        if (held == null) return false;
        Item it = held.getItem();
        return it == Core.registry.spawnPoster || it == Core.registry.logicMatrixProgrammer || ItemUtil.swordSimilar(inv, held);
    }

    public boolean hitByEntity(Entity ent) {
        if (worldObj.isRemote) return false;
        if (!(ent instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) ent;
        if (locked && !PlayerUtil.isPlayerCreative(player)) return false;
        Coord at = new Coord(this);
        if (spin_normal != 0 || spin_vertical != 0 || spin_tilt != 0) {
            spin_normal = spin_vertical = spin_tilt = 0;
        } else if (delta_scale != 0) {
            delta_scale = 0;
        } else {
            ItemStack droppedItem = inv;
            inv = new ItemStack(Core.registry.spawnPoster);
            if (droppedItem.getItem() == Core.registry.spawnPoster) {
                setDead();
            } else {
                syncData();
            }
            if (player.capabilities.isCreativeMode) return false;
            Entity newItem = at.spawnItem(droppedItem);
            if (newItem instanceof EntityItem) {
                EntityItem ei = (EntityItem) newItem;
                ei.delayBeforeCanPickup = 0;
            }
            newItem.onCollideWithPlayer(player);
            return true;
        }
        updateValues();
        syncData();
        return true;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        inv = data.as(Share.VISIBLE, "inv").putItemStack(inv);
        rot = data.as(Share.VISIBLE, "rot").putIDS(rot);
        scale = data.as(Share.VISIBLE, "scale").putDouble(scale);
        base_rotation = data.as(Share.PRIVATE, "baseRot").putIDS(base_rotation);
        base_scale = data.as(Share.PRIVATE, "baseScale").putDouble(base_scale);
        spin_normal = data.as(Share.PRIVATE, "spinNormal").putShort(spin_normal);
        spin_vertical = data.as(Share.PRIVATE, "spinVertical").putShort(spin_vertical);
        spin_tilt = data.as(Share.PRIVATE, "spinTilt").putShort(spin_tilt);
        delta_scale = data.as(Share.PRIVATE, "deltaScale").putByte(delta_scale);
        norm = data.as(Share.PRIVATE, "norm").putEnum(norm);
        top = data.as(Share.PRIVATE, "top").putEnum(top);
        tilt = data.as(Share.PRIVATE, "tilt").putEnum(tilt);
        if (data.isReader()) {
            updateSize();
            if (inv == null) {
                inv = new ItemStack(Core.registry.spawnPoster);
            }
        }
        locked = data.as(Share.PRIVATE, "locked").putBoolean(locked);
    }

    @Override
    protected void entityInit() { }

    private static final double SCALE_INCR = 1.125;
    private static final double SPIN_PER_CLICK = Math.PI * 2 / 32;

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (locked) return false;
        if (worldObj.isRemote) return true;
        ItemStack held = player.getHeldItem();
        if (held == null) return false;
        if (inv.getItem() == Core.registry.spawnPoster) {
            if (held.getItem() == Core.registry.spawnPoster) return true;
            inv = player.getHeldItem().splitStack(1);
            syncData();
            return true;
        }
        int d = player.isSneaking() ? -1 : +1;
        if (ItemUtil.swordSimilar(inv, held)) {
            delta_scale += d;
            updateValues();
            syncData();
            return true;
        }
        final boolean hasPoster = held.getItem() == Core.registry.spawnPoster;
        final boolean hasLmp = held.getItem() == Core.registry.logicMatrixProgrammer;
        if (hasPoster || hasLmp) {
            EnumFacing clickDir = SpaceUtil.determineOrientation(player);
            if (hasPoster) {
                if (clickDir == norm || clickDir == norm.getOpposite()) {
                    spin_normal -= d;
                } else {
                    spin_vertical += d;
                }
            } else {
                if (clickDir == norm || clickDir == norm.getOpposite()) {
                    spin_tilt += d;
                } else {
                    spin_vertical += d;
                }
            }
            updateValues();
            syncData();
            return true;
        }
        return super.interactFirst(player);
    }

    @Override
    public ItemStack getPickedResult(MovingObjectPosition target) {
        return inv.copy();
    }

    @Override
    public int getBrightnessForRender(float partial) {
        // Modified version of super; prevents us from rendering odly when there's a block above.
        int x = MathHelper.floor_double(posX);
        int z = MathHelper.floor_double(posZ);

        double d = -0.5;
        if (EnumFacing.UP == top) {
            d = +0;
        }
        BlockPos blockpos = new BlockPos(x, MathHelper.floor_double(posY + d), z);
        return worldObj.getCombinedLight(blockpos, 0);
    }

    @Override
    public boolean isInRangeToRenderDist(double dist) {
        return dist < 32 * 32;
    }
}
