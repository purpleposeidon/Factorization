package factorization.weird.poster;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class EntityPoster extends EntityFz {
    public ItemStack inv = new ItemStack(Core.registry.spawnPoster);
    public Quaternion rot = new Quaternion();
    public double scale = 1.0;
    public boolean locked = false;

    Quaternion base_rotation = new Quaternion();
    double base_scale = 1.0;
    public short spin_normal = 0, spin_vertical = 0, spin_tilt = 0;
    byte delta_scale = 0;
    ForgeDirection norm = ForgeDirection.NORTH, top = ForgeDirection.UP, tilt = ForgeDirection.EAST;

    public EntityPoster(World w) {
        super(w);
        yOffset = 0;
        setSize(0.5F, 0.5F);
    }

    void updateSize() {
        if (top.offsetY != 0) {
            setSize(0.5F, 0.0001F);
        } else {
            setSize(0.5F, 0.5F);
        }
    }

    public void setBase(double baseScale, Quaternion baseRotation, ForgeDirection norm, ForgeDirection top, AxisAlignedBB bounds) {
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
        SpaceUtil.copyTo(this.boundingBox, bounds);
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
                syncWithSpawnPacket();
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
        syncWithSpawnPacket();
        return true;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        inv = data.as(Share.VISIBLE, "inv").putItemStack(inv);
        rot = data.as(Share.VISIBLE, "rot").put(rot);
        scale = data.as(Share.VISIBLE, "scale").put(scale);
        base_rotation = data.as(Share.PRIVATE, "baseRot").put(base_rotation);
        base_scale = data.as(Share.PRIVATE, "baseScale").put(base_scale);
        spin_normal = data.as(Share.PRIVATE, "spinNormal").putShort(spin_normal);
        spin_vertical = data.as(Share.PRIVATE, "spinVertical").putShort(spin_vertical);
        spin_tilt = data.as(Share.PRIVATE, "spinTilt").putShort(spin_tilt);
        delta_scale = data.as(Share.PRIVATE, "deltaScale").putByte(delta_scale);
        norm = data.as(Share.PRIVATE, "norm").putEnum(norm);
        top = data.as(Share.PRIVATE, "top").putEnum(top);
        tilt = data.as(Share.PRIVATE, "tilt").putEnum(tilt);
        AxisAlignedBB box = boundingBox.copy();
        box = data.as(Share.VISIBLE, "box").putBox(box);
        if (data.isReader()) {
            updateSize();
            if (inv == null) {
                inv = new ItemStack(Core.registry.spawnPoster);
            }
        }
        SpaceUtil.copyTo(this.boundingBox, box);
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
            inv = player.getHeldItem().splitStack(1);
            syncWithSpawnPacket();
            return true;
        }
        int d = player.isSneaking() ? -1 : +1;
        if (ItemUtil.swordSimilar(inv, held)) {
            delta_scale += d;
            updateValues();
            syncWithSpawnPacket();
            return true;
        }
        final boolean hasPoster = held.getItem() == Core.registry.spawnPoster;
        final boolean hasLmp = held.getItem() == Core.registry.logicMatrixProgrammer;
        if (hasPoster || hasLmp) {
            ForgeDirection clickDir = ForgeDirection.getOrientation(SpaceUtil.determineOrientation(player));
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
            syncWithSpawnPacket();
            return true;
        }
        return super.interactFirst(player);
    }

    @Override
    public ItemStack getPickedResult(MovingObjectPosition target) {
        return inv.copy();
    }
}
